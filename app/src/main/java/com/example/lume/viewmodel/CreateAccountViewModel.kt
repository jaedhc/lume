package com.example.lume.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lume.data.StructuralOcrResult
import com.example.lume.data.db.AccountEntity
import com.example.lume.data.db.LumeDatabase
import com.example.lume.data.db.TransactionEntity
import com.example.lume.network.LumeClient
import com.example.lume.processing.Confidence
import com.example.lume.processing.ParsedTransaction
import com.example.lume.processing.PdfOcrProcessor
import com.example.lume.processing.StatementParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class CreateAccountUiState(
    val bankName: String = "",
    val last4: String = "",
    val accountTypeId: String = "DEBIT",
    val currency: String = "MXN",
    val initialBalance: String = "",
    val description: String = "",
    val selectedColor: String = "#FFB800",

    // TDC Specific
    val creditLimit: String = "",
    val closingDay: String = "",
    val dueDay: String = "",
    
    // Savings / Investment Specific
    val targetAmount: String = "",

    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,

    // Estado de cuenta (OCR + Parser) ya se maneja en ImportStatementViewModel
)

class CreateAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "LumeCreateAcct"

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun setAccountType(typeId: String) = _uiState.update { it.copy(accountTypeId = typeId) }
    fun onBankNameChange(name: String) = _uiState.update { it.copy(bankName = name) }
    fun onDescriptionChange(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onColorSelect(color: String) = _uiState.update { it.copy(selectedColor = color) }

    fun onLast4Change(last4: String) {
        if (last4 == "CASH" || last4 == "INV" || (last4.length <= 4 && last4.all { it.isDigit() }))
            _uiState.update { it.copy(last4 = last4) }
    }

    fun onBalanceChange(balance: String) {
        if (balance.isEmpty() || balance.toDoubleOrNull() != null || balance == "-")
            _uiState.update { it.copy(initialBalance = balance) }
    }

    fun onCreditLimitChange(limit: String) {
        if (limit.isEmpty() || limit.toDoubleOrNull() != null)
            _uiState.update { it.copy(creditLimit = limit) }
    }

    fun onClosingDayChange(day: String) {
        if (day.isEmpty() || (day.toIntOrNull() != null && day.toInt() in 1..31))
            _uiState.update { it.copy(closingDay = day) }
    }

    fun onDueDayChange(day: String) {
        if (day.isEmpty() || (day.toIntOrNull() != null && day.toInt() in 1..31))
            _uiState.update { it.copy(dueDay = day) }
    }

    fun onTargetAmountChange(amount: String) {
        if (amount.isEmpty() || amount.toDoubleOrNull() != null)
            _uiState.update { it.copy(targetAmount = amount) }
    }

    // ── Save account + import transactions ────────────────────────────────

    fun saveAccount(
        parsedTransactions: List<ParsedTransaction> = emptyList(),
        classifier: suspend (ParsedTransaction, List<com.example.lume.data.db.CategoryEntity>, List<String>) -> String = { _, _, _ -> "otros" }
    ) {
        val state = _uiState.value
        if (state.bankName.isBlank()) return
        if (state.accountTypeId != "CASH" && state.accountTypeId != "INVESTMENT" && state.accountTypeId != "SAVINGS" && state.last4.length < 4) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val accountId = UUID.randomUUID().toString()
            val newAccount = AccountEntity(
                id = accountId,
                bankName = state.bankName,
                last4 = if (state.accountTypeId == "CASH") "EFE" else if (state.accountTypeId == "INVESTMENT" || state.accountTypeId == "SAVINGS") "INV" else state.last4,
                label = state.description.takeIf { it.isNotBlank() },
                accountTypeId = state.accountTypeId,
                balance = state.initialBalance.toDoubleOrNull() ?: 0.0,
                color = state.selectedColor,
                creditLimit = state.creditLimit.toDoubleOrNull(),
                closingDay = state.closingDay.toIntOrNull(),
                dueDay = state.dueDay.toIntOrNull(),
                targetAmount = state.targetAmount.toDoubleOrNull()
            )

            transactionDao.insertAccount(newAccount)
            Log.d(TAG, "Account saved: $accountId (${state.bankName})")

            if (parsedTransactions.isNotEmpty()) {
                val isCreditAccount = state.accountTypeId == "CREDIT"

                // Compute the start of the current billing cycle (only for TDC).
                // All transactions BEFORE this date belong to a previous cycle → isPaid = true.
                val cycleStartDate: LocalDate? = if (isCreditAccount) {
                    computeCycleStart(state.closingDay.toIntOrNull())
                } else null

                val dbCategories = transactionDao.getCategoriesSnapshot()
                val categoryNames = dbCategories.map { it.name }

                var imported = 0
                parsedTransactions.forEach { tx ->
                    // ── Determine if already paid (TDC only) ──────────────
                    val isPaid = isCreditAccount &&
                        cycleStartDate != null &&
                        runCatching { LocalDate.parse(tx.dateIso) < cycleStartDate }.getOrDefault(false)

                    // ── Classify category ─────────────────────────────────
                    val categoryId = classifier(tx, dbCategories, categoryNames)

                    val entity = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        amount = tx.amount,
                        currency = state.currency,
                        dateIso = tx.dateIso,
                        merchant = tx.merchant,
                        concept = null,
                        categoryId = categoryId,
                        accountId = accountId,
                        type = if (tx.isCredit) "ingreso" else "egreso",
                        isSubscription = false,
                        isPaid = isPaid,
                        note = null
                    )
                    transactionDao.insertTransactionWithBalanceUpdate(entity)
                    imported++
                    Log.d(TAG, "TX: ${tx.merchant} | ${tx.dateIso} | \$${tx.amount} | paid=$isPaid | cat=$categoryId")
                }
                Log.d(TAG, "Imported $imported transactions for account $accountId")
            }

            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    // ── Billing cycle helpers ─────────────────────────────────────────────

    /**
     * Returns the start date of the current billing cycle.
     * If closingDay is the 17th and today is Feb 22, returns Jan 17.
     * Transactions before this date are from a previous cycle → already paid.
     *
     * Only called for CREDIT accounts — always returns null for other types.
     */
    private fun computeCycleStart(closingDay: Int?): LocalDate? {
        if (closingDay == null) return null
        val today = LocalDate.now()
        val closingThisMonth = runCatching {
            today.withDayOfMonth(closingDay)
        }.getOrNull() ?: return null

        return if (today >= closingThisMonth) {
            // We're past this month's closing → cycle started on closingDay this month
            closingThisMonth
        } else {
            // We're before this month's closing → cycle started on closingDay last month
            closingThisMonth.minusMonths(1)
        }
    }

}
