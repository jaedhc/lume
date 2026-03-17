package com.jaedhc.lume.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.jaedhc.lume.data.mappers.TransactionMappers.enrichWithMsiAndClean

data class CategorySpend(
    val category: CategoryEntity,
    val amount: Double
)

enum class TdcReminderType { CORTE, PAGO }

data class TdcReminder(
    val bankName: String,
    val last4: String,
    val day: Int,
    val daysLeft: Int,
    val type: TdcReminderType,
    val amountToPay: Double = 0.0
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
                                accountId = originalTxWithCat.transaction.accountId,
                                type = "egreso",
                                isSubscription = false,
                                note = "Pago automático",
                                msiInstallment = plan.totalInstallments, // Placeholder or calculated?
                                msiTotal = plan.totalInstallments
                            ),
                            category = originalTxWithCat.category
                        )
                    }

                    // 2. Will calculate reminders later after debt aggregation

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

                    val tdcDebtFromTxns = (pendingDebtTxns + virtualInstallments.filter { it.transaction.accountId in creditAccountIds })
                        .sumOf { it.transaction.amount }

                    // Calculate Reminders for TDCs
                    val reminders = accountsWithType.filter { 
                        it.type?.id == "CREDIT" && it.account.closingDay != null
                    }.flatMap { accWithType ->
                        val acc = accWithType.account
                        val list = mutableListOf<TdcReminder>()

                        // Helper to get ISO date for a specific closing day
                        fun getClosingDateIso(offsetMonths: Int): String {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, acc.closingDay!!)
                                add(Calendar.MONTH, offsetMonths)
                                // If setting it to today's month but the day hasn't passed, 
                                // it might still be in the "future" relative to our target of "last closing"
                            }
                            // Adjust to ensure we get the most recent past or current closing date
                            val now = Calendar.getInstance()
                            if (offsetMonths == 0 && cal.after(now)) {
                                cal.add(Calendar.MONTH, -1)
                            }
                            
                            val y = cal.get(Calendar.YEAR)
                            val m = cal.get(Calendar.MONTH) + 1
                            val d = cal.get(Calendar.DAY_OF_MONTH)
                            return String.format("%04d-%02d-%02d", y, m, d)
                        }

                        val lastClosingDate = getClosingDateIso(0)
                        val secondLastClosingDate = getClosingDateIso(-1)

                        val accountTxns = pendingDebtTxns.filter { it.transaction.accountId == acc.id }
                        val accountVirtuals = virtualInstallments.filter { it.transaction.accountId == acc.id }

                        val amountForCorte = accountTxns
                            .filter { it.transaction.dateIso > lastClosingDate }
                            .sumOf { it.transaction.amount }

                        val amountForPago = accountTxns
                            .filter { it.transaction.dateIso > secondLastClosingDate && it.transaction.dateIso <= lastClosingDate }
                            .sumOf { it.transaction.amount } + accountVirtuals.sumOf { it.transaction.amount }

                        fun calculateDaysLeft(targetDay: Int): Int {
                            val target = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, targetDay)
                                if (get(Calendar.DAY_OF_MONTH) < today) {
                                    add(Calendar.MONTH, 1)
                                }
                            }
                            val diffMillis = target.timeInMillis - Calendar.getInstance().timeInMillis
                            return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        }

                        // Closing Day Reminder
                        acc.closingDay?.let { day ->
                            val daysLeft = calculateDaysLeft(day)
                            if (daysLeft in 0..3) {
                                list.add(TdcReminder(acc.bankName, acc.last4, day, daysLeft, TdcReminderType.CORTE, amountForCorte))
                            }
                        }

                        // Due Day Reminder (Payment Day)
                        acc.dueDay?.let { day ->
                            val daysLeft = calculateDaysLeft(day)
                            if (daysLeft in 0..3) {
                                list.add(TdcReminder(acc.bankName, acc.last4, day, daysLeft, TdcReminderType.PAGO, amountForPago))
                            }
                        }
                        
                        list
                    }.sortedBy { it.daysLeft }

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
