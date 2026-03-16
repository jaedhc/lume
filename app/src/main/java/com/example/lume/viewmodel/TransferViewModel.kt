package com.example.lume.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.db.AccountEntity
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class TransferUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val sourceAccount: AccountEntity? = null,
    val destinationAccount: AccountEntity? = null,
    val amount: String = "0.00",
    val note: String = "",
    val dateIso: String = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            transactionDao.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun onSourceAccountSelected(account: AccountEntity) {
        _uiState.update { it.copy(sourceAccount = account, error = null) }
    }

    fun onDestinationAccountSelected(account: AccountEntity) {
        _uiState.update { it.copy(destinationAccount = account, error = null) }
    }

    fun onAmountChange(amount: String) {
        // Simple numeric validation
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(amount = amount, error = null) }
        }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun confirmTransfer() {
        val state = _uiState.value
        val source = state.sourceAccount
        val destination = state.destinationAccount
        val amount = state.amount.toDoubleOrNull() ?: 0.0

        if (source == null || destination == null) {
            _uiState.update { it.copy(error = "Selecciona ambas cuentas") }
            return
        }

        if (source.id == destination.id) {
            _uiState.update { it.copy(error = "Las cuentas deben ser diferentes") }
            return
        }

        if (amount <= 0) {
            _uiState.update { it.copy(error = "El monto debe ser mayor a 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val transferId = UUID.randomUUID().toString()
                
                // 1. Egreso from Source
                val egreso = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    currency = "MXN",
                    dateIso = state.dateIso,
                    merchant = "Transacción Interna",
                    concept = "Transferencia a ${destination.bankName}",
                    categoryId = "finanzas",
                    accountId = source.id,
                    type = "egreso",
                    isSubscription = false,
                    note = state.note
                )

                // 2. Ingreso to Destination
                val ingreso = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    currency = "MXN",
                    dateIso = state.dateIso,
                    merchant = "Transacción Interna",
                    concept = "Transferencia desde ${source.bankName}",
                    categoryId = "finanzas",
                    accountId = destination.id,
                    type = "ingreso",
                    isSubscription = false,
                    note = state.note
                )

                // Insert both within the same coroutine/transaction context 
                // Note: DAO updateAccountBalance is already @Transaction in its usage
                db.transactionDao().insertTransactionWithBalanceUpdate(egreso)
                db.transactionDao().insertTransactionWithBalanceUpdate(ingreso)

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al realizar la transferencia") }
            }
        }
    }
}
