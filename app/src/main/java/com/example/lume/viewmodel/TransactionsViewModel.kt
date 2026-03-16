package com.example.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.example.lume.utils.DateUtils
import com.example.lume.data.mappers.TransactionMappers.enrichWithMsiAndClean
import java.util.Locale

data class TransactionsUiState(
    val esteMesTransactions: List<TransactionWithCategory> = emptyList(),
    val futureTransactionsGrouped: List<Pair<String, List<TransactionWithCategory>>> = emptyList(),
    val pastTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        observeJob?.cancel()
        observeJob = observeTransactions()
    }

    private fun observeTransactions(): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            combine(
                transactionDao.getAllTransactionsWithCategory(),
                transactionDao.getActiveDeferredPlans()
            ) { transactions, plans ->
                val today = LocalDate.now()
                val enrichedTransactions = transactions.map { txWithCat ->
                    txWithCat.enrichWithMsiAndClean(plans, today)
                }

                val esteMes = mutableListOf<TransactionWithCategory>()
                val future = mutableListOf<TransactionWithCategory>()
                val past = mutableListOf<TransactionWithCategory>()

                enrichedTransactions.forEach { tx ->
                    val date = DateUtils.parseSafeLocalDate(tx.transaction.dateIso)
                    if (date == null) {
                        past.add(tx)
                    } else {
                        if (date.year == today.year && date.month == today.month) {
                            esteMes.add(tx)
                        } else if (date.isAfter(today)) {
                            future.add(tx)
                        } else {
                            past.add(tx)
                        }
                    }
                }

                // Sort and Group
                esteMes.sortByDescending { it.transaction.dateIso }

                val futureGrouped = future.groupBy {
                    val date = DateUtils.parseSafeLocalDate(it.transaction.dateIso) ?: today
                    val monthName = date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "MX"))
                        .replaceFirstChar { char -> char.uppercase() }
                    "$monthName ${date.year}"
                }.toList().sortedBy { pair ->
                    DateUtils.parseSafeLocalDate(pair.second.first().transaction.dateIso) ?: today
                }

                past.sortByDescending { it.transaction.dateIso }

                TransactionsUiState(
                    esteMesTransactions = esteMes,
                    futureTransactionsGrouped = futureGrouped,
                    pastTransactions = past,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun deleteTransaction(txWithCat: TransactionWithCategory) {
        viewModelScope.launch {
            transactionDao.deleteTransactionWithBalanceUpdate(txWithCat.transaction)
        }
    }
}
