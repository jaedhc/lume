package com.example.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.AccountEntity
import com.example.lume.data.db.CategoryEntity
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class EditTransactionUiState(
    val transaction: TransactionEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false
)

class EditTransactionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private var originalTransaction: TransactionEntity? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val tx = transactionDao.getTransactionById(transactionId)
            originalTransaction = tx
            _uiState.value = _uiState.value.copy(
                transaction = tx,
                isLoading = false
            )

            launch {
                transactionDao.getAllCategories().map { list ->
                    list.filter { it.id != "finanzas" }
                }.collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            }
            launch {
                transactionDao.getAllAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(accounts = accounts)
                }
            }
        }
    }

    fun saveChanges(updatedTx: TransactionEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            originalTransaction?.let { oldTx ->
                transactionDao.updateTransactionWithBalanceUpdate(oldTx, updatedTx)
            }
            _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
        }
    }
}
