package com.example.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.AccountEntity
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionWithCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import com.example.lume.data.mappers.TransactionMappers.enrichWithMsiAndClean

data class TdcDetailUiState(
    val account: AccountEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalDebt: Double = 0.0,
    val currentCycleDebt: Double = 0.0,
    val currentCycleTransactions: List<TransactionWithCategory> = emptyList(),
    val liquidAccounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true
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
            // Get Account
            val account = transactionDao.getAccountById(accountId)
            
            // Get Liquid Accounts for Payment
            val allAccounts = transactionDao.getAccountsSnapshot()
            val liquid = allAccounts.filter { it.accountTypeId != "CREDIT" }

            transactionDao.getTransactionsByAccountFlow(accountId).collect { rawTxs ->
                val today = LocalDate.now()
                val closingDay = account?.closingDay ?: 1
                
                // Fetch deferred plans to enrich MSI info
                val deferredPlans = transactionDao.getActiveDeferredPlansSnapshot()
                
                val txs = rawTxs.map { it.enrichWithMsiAndClean(deferredPlans, today) }
                
                // Logic for "Deuda del Mes" (Current Billing Cycle)
                // If closingDay is 15:
                // If today is 16 Mar -> cycle is 16 Feb to 15 Mar (to pay soon) OR 16 Mar to 15 Apr?
                // Typically: Transactions between last closing date and current closing date.
                
                val currentClosingDate = if (today.dayOfMonth <= closingDay) {
                    today.withDayOfMonth(minOf(closingDay, today.lengthOfMonth()))
                } else {
                    today.plusMonths(1).withDayOfMonth(minOf(closingDay, today.plusMonths(1).lengthOfMonth()))
                }
                
                val startOfCycle = currentClosingDate.minusMonths(1).plusDays(1)
                
                val unpaid = txs.filter { !it.transaction.isPaid && it.transaction.type == "egreso" }
                val totalDebt = unpaid.sumOf { it.transaction.amount }
                
                val cycleTxs = unpaid.filter { 
                    val date = LocalDate.parse(it.transaction.dateIso)
                    (date.isEqual(startOfCycle) || date.isAfter(startOfCycle)) && 
                    (date.isEqual(currentClosingDate) || date.isBefore(currentClosingDate))
                }
                val cycleDebt = cycleTxs.sumOf { it.transaction.amount }

                _uiState.update { 
                    it.copy(
                        account = account,
                        transactions = txs,
                        totalDebt = totalDebt,
                        currentCycleDebt = cycleDebt,
                        currentCycleTransactions = cycleTxs,
                        liquidAccounts = liquid,
                        isLoading = false
                    )
                }
            }
        }
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
