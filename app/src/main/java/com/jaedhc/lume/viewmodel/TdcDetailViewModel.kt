package com.jaedhc.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.db.AccountEntity
import com.jaedhc.lume.data.db.LumeDatabase
import com.jaedhc.lume.data.db.TransactionWithCategory
import com.jaedhc.lume.data.db.TransactionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.jaedhc.lume.data.mappers.TransactionMappers.enrichWithMsiAndClean
import java.util.Locale

data class TdcDetailUiState(
    val account: AccountEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalDebt: Double = 0.0,
    val currentCycleDebt: Double = 0.0,
    val currentCycleTransactions: List<TransactionWithCategory> = emptyList(),
    val liquidAccounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedOffset: Int = 0,
    val availableCycles: List<TdcCycleInfo> = emptyList()
)

data class TdcCycleInfo(
    val label: String,
    val offset: Int
)

class TdcDetailViewModel(application: Application, private val accountId: String) : AndroidViewModel(application) {
    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(TdcDetailUiState())
    val uiState: StateFlow<TdcDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val account = transactionDao.getAccountById(accountId)
            val closingDay = account?.closingDay ?: 1
            
            val cycles = (-6..12).map { offset ->
                val date = LocalDate.now().plusMonths(offset.toLong())
                val label = date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "MX"))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val finalLabel = if (date.year != LocalDate.now().year) "$label ${date.year}" else label
                TdcCycleInfo(finalLabel, offset)
            }

            val allAccounts = transactionDao.getAccountsSnapshot()
            val liquid = allAccounts.filter { it.accountTypeId != "CREDIT" }

            combine(
                transactionDao.getTransactionsByAccountFlow(accountId),
                transactionDao.getActiveDeferredPlans(), 
                _uiState.map { it.selectedOffset }.distinctUntilChanged()
            ) { rawTxs, deferredPlans, offset ->
                val today = LocalDate.now()
                val targetCycleDate = today.plusMonths(offset.toLong())
                
                // Logic for "Deuda del Mes" (Cycle-based)
                val closingDate = if (targetCycleDate.dayOfMonth <= closingDay) {
                    targetCycleDate.withDayOfMonth(minOf(closingDay, targetCycleDate.lengthOfMonth()))
                } else {
                    targetCycleDate.plusMonths(1).withDayOfMonth(minOf(closingDay, targetCycleDate.plusMonths(1).lengthOfMonth()))
                }
                val startOfCycle = closingDate.minusMonths(1).plusDays(1)
                
                // Fetch all categories for virtual tx enrichment
                val categories = transactionDao.getCategoriesSnapshot().associateBy { it.id }
                
                // 1. Regular Transactions (excluding parents of deferred plans)
                val deferredTxIds = deferredPlans.map { it.transactionId }.toSet()
                val txs = rawTxs.map { it.enrichWithMsiAndClean(deferredPlans, targetCycleDate) }
                
                val normalCycleTxs = txs.filter { tx ->
                    val date = LocalDate.parse(tx.transaction.dateIso)
                    tx.transaction.id !in deferredTxIds &&
                    (date.isEqual(startOfCycle) || date.isAfter(startOfCycle)) && 
                    (date.isEqual(closingDate) || date.isBefore(closingDate))
                }

                // 2. Virtual Transactions from Deferred Plans
                val virtualCycleTxs = deferredPlans
                    .filter { plan -> rawTxs.any { it.transaction.id == plan.transactionId } }
                    .mapNotNull { plan ->
                        val startDate = LocalDate.parse(plan.startDateIso)
                        
                        // 1. Calculate the first closing date that includes this purchase
                        // If purchase day > closingDay, it goes to the next month's closing
                        val firstClosingDate = if (startDate.dayOfMonth <= closingDay) {
                            startDate.withDayOfMonth(minOf(closingDay, startDate.lengthOfMonth()))
                        } else {
                            startDate.plusMonths(1).withDayOfMonth(minOf(closingDay, startDate.plusMonths(1).lengthOfMonth()))
                        }

                        // 2. An installment only exists if THIS cycle's closing date is >= firstClosingDate
                        if (closingDate.isBefore(firstClosingDate)) return@mapNotNull null

                        val monthsPassed = (closingDate.year - firstClosingDate.year) * 12 + (closingDate.monthValue - firstClosingDate.monthValue)
                        val currentInstallment = monthsPassed + 1
                        
                        // Check if we already have a REAL transaction for this installment to avoid duplicates
                        val isAlreadyReal = rawTxs.any { 
                            it.transaction.deferredPlanId == plan.id && 
                            it.transaction.msiInstallment == currentInstallment
                        }
                        if (isAlreadyReal) return@mapNotNull null
                        
                        if (currentInstallment in 1..plan.totalInstallments) {
                            val originalTx = rawTxs.find { it.transaction.id == plan.transactionId }
                            val category = categories[originalTx?.transaction?.categoryId]
                            
                            TransactionWithCategory(
                                transaction = TransactionEntity(
                                    id = "virtual_${plan.id}_$offset",
                                    amount = plan.monthlyPayment,
                                    currency = "MXN",
                                    dateIso = closingDate.toString(),
                                    merchant = originalTx?.transaction?.merchant ?: "Plan Diferido",
                                    concept = originalTx?.transaction?.concept ?: "Mensualidad",
                                    categoryId = originalTx?.transaction?.categoryId ?: "finanzas",
                                    accountId = accountId,
                                    type = "egreso",
                                    isSubscription = false,
                                    note = null,
                                    msiInstallment = currentInstallment,
                                    msiTotal = plan.totalInstallments,
                                    deferredPlanId = plan.id,
                                    isPaid = offset < 0 // Basic assumption: past cycles are paid
                                ),
                                category = category
                            )
                        } else null
                    }

                val allCycleTxs = (normalCycleTxs + virtualCycleTxs)
                    .distinctBy { it.transaction.id }
                    .sortedByDescending { it.transaction.dateIso }
                
                // Total Debt remains the global unpaid debt
                val totalDebt = txs.filter { !it.transaction.isPaid && it.transaction.type == "egreso" }.sumOf { it.transaction.amount }
                val cycleDebt = allCycleTxs.filter { !it.transaction.isPaid }.sumOf { it.transaction.amount }

                _uiState.update { 
                    it.copy(
                        account = account,
                        transactions = txs,
                        totalDebt = totalDebt,
                        currentCycleDebt = cycleDebt,
                        currentCycleTransactions = allCycleTxs,
                        liquidAccounts = liquid,
                        availableCycles = cycles,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onCycleSelected(offset: Int) {
        _uiState.update { it.copy(selectedOffset = offset, isLoading = true) }
    }

    fun payCurrentCycle(sourceAccountId: String, amount: Double) {
        viewModelScope.launch {
            val txIdsToPay = _uiState.value.currentCycleTransactions.map { it.transaction.id }
            transactionDao.registerTdcPayment(
                sourceAccountId = sourceAccountId,
                tdcAccountId = accountId,
                amount = amount,
                txIdsToPay = txIdsToPay,
                dateIso = LocalDate.now().toString()
            )
        }
    }
}
