package com.jaedhc.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.db.AccountWithType
import com.jaedhc.lume.data.db.LumeDatabase
import com.jaedhc.lume.data.mappers.AccountMappers.mapWithDynamicBalances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountGroup(
    val typeName: String,
    val typeId: String,
    val accounts: List<AccountWithType>,
    val totalBalance: Double
)

data class AccountsUiState(
    val groups: List<AccountGroup> = emptyList(),
    val isLoading: Boolean = true
)

class AccountsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        observeAccounts()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            combine(
                transactionDao.getAllAccountsWithType(),
                transactionDao.getAllTransactions()
            ) { accounts, transactions ->
                // Overwrite CREDIT account balances dynamically with their pending debt
                val updatedAccounts = accounts.mapWithDynamicBalances(transactions)

                withContext(Dispatchers.Default) {
                    val grouped = updatedAccounts.groupBy { it.type?.id ?: "OTHER" }
                        .map { (typeId, list) ->
                            val typeName = list.firstOrNull()?.type?.name ?: "Otros"
                            // Calculate subtotal based on account type logic
                            val subtotal = when (typeId) {
                                "CREDIT" -> -list.sumOf { it.account.balance } // Debt is negative
                                "INVESTMENT" -> list.sumOf { it.account.currentValue ?: 0.0 }
                                else -> list.sumOf { it.account.balance }
                            }
                            AccountGroup(
                                typeName = typeName.uppercase(),
                                typeId = typeId,
                                accounts = list,
                                totalBalance = subtotal
                            )
                        }
                        .sortedBy { groupPriority(it.typeId) }

                    AccountsUiState(groups = grouped, isLoading = false)
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun groupPriority(typeId: String): Int = when (typeId) {
        "DEBIT" -> 0
        "CREDIT" -> 1
        "SAVINGS" -> 2
        "INVESTMENT" -> 3
        "CASH" -> 4
        else -> 5
    }

    fun deleteAccountWithTransactions(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteTransactionsByAccountId(accountId)
            transactionDao.deleteAccountById(accountId)
        }
    }
}
