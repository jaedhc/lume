package com.example.lume.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.example.lume.data.mappers.TransactionMappers.enrichWithMsiAndClean

data class CategorySpend(
    val category: CategoryEntity,
    val amount: Double
)

data class TdcReminder(
    val bankName: String,
    val last4: String,
    val closingDay: Int,
    val daysLeft: Int
)

data class DeferredPaymentItem(
    val id: String,
    val concept: String,
    val categoryIcon: String,
    val categoryColor: String,
    val currentMonth: Int,
    val totalMonths: Int,
    val monthlyPayment: Double,
    val paymentDueDate: String
)

data class DashboardUiState(
    val totalBalance: Double = 0.0, // Liquid Assets (Debit + Cash)
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalDebt: Double = 0.0,    // TDC Liabilities + Deferred Plans
    val totalInvestments: Double = 0.0, // Investment Current Value
    val tdcReminders: List<TdcReminder> = emptyList(),
    val deferredPayments: List<DeferredPaymentItem> = emptyList(),
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val categoriesBreakdown: List<CategorySpend> = emptyList(),
    val isSensitiveDataVisible: Boolean = true,
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            combine(
                transactionDao.getAllTransactionsWithCategory(),
                transactionDao.getActiveDeferredPlans(),
                transactionDao.getAllAccountsWithType()
            ) { transactions, deferredPlans, accountsWithType ->
                withContext(Dispatchers.Default) {
                    val calendar = java.util.Calendar.getInstance()
                    val today = calendar.get(java.util.Calendar.DAY_OF_MONTH)

                    // 1. Calculate Virtual Transactions from Deferred Plans
                    val virtualInstallments = deferredPlans.mapNotNull { plan ->
                        val originalTxWithCat = transactions.find { it.transaction.id == plan.transactionId }
                        if (originalTxWithCat?.category == null) return@mapNotNull null
                        
                        TransactionWithCategory(
                            transaction = TransactionEntity(
                                id = "virtual_${plan.id}",
                                amount = plan.monthlyPayment,
                                currency = "MXN",
                                dateIso = "", // Virtual
                                merchant = "Plan Diferido",
                                concept = "Mensualidad",
                                categoryId = originalTxWithCat.category.id,
                                accountId = null,
                                type = "egreso",
                                isSubscription = false,
                                note = "Pago automático",
                                msiInstallment = plan.totalInstallments, // Placeholder or calculated?
                                msiTotal = plan.totalInstallments
                            ),
                            category = originalTxWithCat.category
                        )
                    }

                    // 2. TDC Reminders
                    val reminders = accountsWithType.filter { 
                        it.type?.id == "CREDIT" && it.account.closingDay != null 
                    }.mapNotNull { accWithType ->
                        val acc = accWithType.account
                        val closingDay = acc.closingDay!!
                        val diff = closingDay - today
                        if (diff in 0..3) {
                            TdcReminder(acc.bankName, acc.last4, closingDay, diff)
                        } else null
                    }

                    // 3. Balance Calculations
                    val liquidBalance = accountsWithType.filter { 
                        it.type?.id == "DEBIT" || it.type?.id == "CASH" || it.type?.id == "SAVINGS"
                    }.sumOf { it.account.balance }

                    val investmentsValue = accountsWithType.filter { 
                        it.type?.id == "INVESTMENT" 
                    }.sumOf { it.account.currentValue ?: 0.0 }

                    // Deduplicate Expenses: Exclude the massive parent transactions of Deferred Plans
                    val deferredTxIds = deferredPlans.map { it.transactionId }.toSet()
                    
                    // Identify credit accounts
                    val creditAccountIds = accountsWithType
                        .filter { it.type?.id == "CREDIT" }
                        .map { it.account.id }
                        .toSet()

                    // Split transactions into "Real Expenses" vs "Pending Debt"
                    // Real Expenses: all non-credit accounts + PAID credit transactions
                    // Pending Debt: UNPAID credit transactions (isPaid = false)
                    val (pendingDebtTxns, realExpenseTxns) = transactions
                        .filter { it.transaction.type == "egreso" && it.transaction.id !in deferredTxIds }
                        .partition { 
                            it.transaction.accountId in creditAccountIds && !it.transaction.isPaid 
                        }

                    val tdcDebtFromTxns = pendingDebtTxns.sumOf { it.transaction.amount }

                    // Virtual installments shouldn't be real expenses either, they are projected debt until paid.
                    // Real expenses = Cash/Debit/Savings purchases + Paid credit card transactions (payments)
                    val income = transactions
                        .filter { it.transaction.type == "ingreso" && it.transaction.categoryId != "finanzas" }
                        .sumOf { it.transaction.amount }
                    
                    val expensesList = realExpenseTxns
                    
                    val expenses = expensesList
                        .filter { it.transaction.categoryId != "finanzas" }
                        .sumOf { it.transaction.amount }
                    
                    val allCategorizableExpensesList = expensesList + pendingDebtTxns + virtualInstallments
                    
                    val breakdown = allCategorizableExpensesList
                        .filter { it.category != null && it.category!!.id != "finanzas" }
                        .groupBy { it.category!!.id }
                        .map { (categoryId, list) -> 
                            // Use the first category instance found for this ID
                            CategorySpend(list.first().category!!, list.sumOf { it.transaction.amount }) 
                        }
                        .sortedByDescending { it.amount }

                    val totalDebt = tdcDebtFromTxns

                    // 4. Map Deferred Payments for UI
                    val deferredPaymentItems = deferredPlans.mapNotNull { plan ->
                        val originalTxWithCat = transactions.find { it.transaction.id == plan.transactionId } ?: return@mapNotNull null
                        val originalTx = originalTxWithCat.transaction
                        val category = originalTxWithCat.category
                        
                        val account = accountsWithType.find { it.account.id == originalTx.accountId }?.account
                        
                        val startYear = plan.startDateIso.take(4).toIntOrNull() ?: calendar.get(java.util.Calendar.YEAR)
                        val startMonth = plan.startDateIso.drop(5).take(2).toIntOrNull() ?: (calendar.get(java.util.Calendar.MONTH) + 1)
                        val currentYear = calendar.get(java.util.Calendar.YEAR)
                        val currentMonthNum = calendar.get(java.util.Calendar.MONTH) + 1
                        
                        val monthsPassed = (currentYear - startYear) * 12 + (currentMonthNum - startMonth)
                        val currentInstallment = (monthsPassed + 1).coerceIn(1, plan.totalInstallments)
                        
                        val accountLabel = account?.let { "${it.bankName} ${it.last4}" } ?: "Cuenta"
                        val paymentDueDateStr = account?.dueDay?.let { dueDay ->
                            val currentMonthStr = currentMonthNum.toString().padStart(2, '0')
                            val dueDayStr = dueDay.toString().padStart(2, '0')
                            "$accountLabel - $currentMonthStr/$dueDayStr"
                        } ?: "$accountLabel - Este mes"

                        // Clean concept: if it looks like garbage or a timestamp, use merchant or fallback
                        val enrichedTx = originalTxWithCat.enrichWithMsiAndClean(deferredPlans).transaction
                        val cleanedConcept = enrichedTx.concept ?: "Compra Diferida"

                        DeferredPaymentItem(
                            id = plan.id,
                            concept = cleanedConcept,
                            categoryIcon = category?.icon ?: "Category",
                            categoryColor = category?.color ?: "#B894FF",
                            currentMonth = currentInstallment,
                            totalMonths = plan.totalInstallments,
                            monthlyPayment = plan.monthlyPayment,
                            paymentDueDate = paymentDueDateStr
                        )
                    }

                    _uiState.value.copy(
                        totalBalance = liquidBalance,
                        totalIncome = income,
                        totalExpenses = expenses,
                        totalDebt = totalDebt,
                        totalInvestments = investmentsValue,
                        tdcReminders = reminders,
                        deferredPayments = deferredPaymentItems,
                        recentTransactions = transactions.sortedByDescending { it.transaction.createdAt }.take(6).map { txWithCat ->
                            txWithCat.enrichWithMsiAndClean(deferredPlans)
                        },
                        categoriesBreakdown = breakdown,
                        isLoading = false
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
                Log.d("DashboardVM", "State updated with ${newState.recentTransactions.size} transactions")
            }
        }
    }

    fun toggleSensitiveDataVisibility() {
        _uiState.update { it.copy(isSensitiveDataVisible = !it.isSensitiveDataVisible) }
    }
}
