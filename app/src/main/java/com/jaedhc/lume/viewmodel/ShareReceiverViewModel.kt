package com.jaedhc.lume.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.OcrResult
import com.jaedhc.lume.data.TxFields
import com.jaedhc.lume.data.StructuralOcrResult
import com.jaedhc.lume.network.LumeClient
import com.jaedhc.lume.data.db.DeferredPlanEntity
import com.jaedhc.lume.data.db.LumeDatabase
import com.jaedhc.lume.data.db.TransactionEntity
import com.jaedhc.lume.processing.OcrStructuralExtractor
import com.jaedhc.lume.processing.ParsedTransaction
import com.jaedhc.lume.processing.PdfOcrProcessor
import com.jaedhc.lume.processing.StatementParser
import com.jaedhc.lume.utils.SanitizeUtils
import com.jaedhc.lume.utils.DateUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jaedhc.lume.data.mappers.AccountMappers.mapWithDynamicBalances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// UI States
// ─────────────────────────────────────────────────────────────────────────────

sealed class ShareReceiverUiState {
    object Idle : ShareReceiverUiState()
    object Loading : ShareReceiverUiState()

    /** Single-transaction result (screenshot/photo) — goes to review screen */
    data class Success(val result: OcrResult) : ShareReceiverUiState()

    /**
     * Multi-transaction result (full bank statement PDF/image).
     * Shows a batch review list before saving.
     */
    data class StatementParsed(
        val transactions: List<ParsedTransaction>,
        val rawText: String,
        val accountId: String?
    ) : ShareReceiverUiState()

    data class Error(val message: String) : ShareReceiverUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class ShareReceiverViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "LumeShareVM"

    private val _uiState = MutableStateFlow<ShareReceiverUiState>(ShareReceiverUiState.Idle)
    val uiState: StateFlow<ShareReceiverUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Int>() // emits count of saved transactions
    val saveSuccess: SharedFlow<Int> = _saveSuccess.asSharedFlow()

    private val db = LumeDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private var processingJob: Job? = null

    val categories = transactionDao.getAllCategories().map { list ->
        list.filter { it.id != "finanzas" }
    }
    val accounts = combine(
        transactionDao.getAllAccountsWithType(),
        transactionDao.getAllTransactions()
    ) { accounts, transactions ->
        accounts.mapWithDynamicBalances(transactions)
    }

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    fun onCategorySelected(id: String) { _selectedCategoryId.value = id }
    fun onAccountSelected(id: String?) { _selectedAccountId.value = id }
    fun onDateSelected(dateIso: String) { _selectedDate.value = dateIso }

    // ── Single image (screenshot) ─────────────────────────────────────────

    /**
     * Process a single screenshot/photo. Uses local structural extraction
     * and matches category/account heuristically.
     * The backend LLM is NOT called — all processing is on-device.
     */
    fun processImage(uri: Uri) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.value = ShareReceiverUiState.Loading
            try {
                val resolver = getApplication<Application>().contentResolver
                val result = withContext(Dispatchers.IO) {
                    val rawText = runOcr(resolver, uri)
                    Log.d(TAG, "RAW OCR TEXT: $rawText")
                    Log.d(TAG, "OCR text (${rawText.length} chars)")
                    buildSingleTxResult(rawText)
                }
                _selectedCategoryId.value = result.selectedCategoryId
                _selectedAccountId.value = result.suggestedAccountId
                _selectedDate.value = result.fields.date
                _uiState.value = ShareReceiverUiState.Success(result)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                Log.e(TAG, "processImage failed", e)
                _uiState.value = ShareReceiverUiState.Error("Error al procesar imagen: ${e.message}")
            }
        }
    }

    // ── Statement (PDF or image with many transactions) ───────────────────

    /**
     * Process a bank statement file (PDF or image). Uses PdfOcrProcessor to
     * extract text and StatementParser to find all transactions locally.
     * No data leaves the device.
     */
    fun processStatement(uri: Uri) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.value = ShareReceiverUiState.Loading
            try {
                val resolver = getApplication<Application>().contentResolver
                val (transactions, rawText, accountId) = withContext(Dispatchers.IO) {
                    val text = PdfOcrProcessor.extractText(resolver, uri)
                    Log.d(TAG, "Statement OCR text (${text.length} chars, ${text.lines().size} lines)")

                    val parseResult = StatementParser.parse(text)
                    Log.d(TAG, "Parsed ${parseResult.transactions.size} txns — mode=${parseResult.mode}, confidence=${parseResult.confidence}")

                    val dbAccounts = transactionDao.getAccountsSnapshot()
                    val suggestedAccount = matchAccount(text, dbAccounts)

                    Triple(parseResult.transactions, text, suggestedAccount?.id)
                }

                _selectedAccountId.value = accountId

                if (transactions.isEmpty()) {
                    _uiState.value = ShareReceiverUiState.Error(
                        "No se encontraron transacciones en el documento. " +
                        "Verifica que el PDF contenga texto seleccionable o intenta con una imagen de mayor resolución."
                    )
                } else {
                    _uiState.value = ShareReceiverUiState.StatementParsed(transactions, rawText, accountId)
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                Log.e(TAG, "processStatement failed", e)
                _uiState.value = ShareReceiverUiState.Error("Error al procesar estado de cuenta: ${e.message}")
            }
        }
    }

    // ── Save (single transaction) ─────────────────────────────────────────
    fun saveTransaction(
        result: OcrResult,
        type: String,
        isSubscription: Boolean,
        note: String?,
        editedAmount: Double = result.fields.amount ?: 0.0,
        editedConcept: String? = result.fields.concept ?: result.fields.merchant,
        msiTotal: Int? = result.fields.msi_total ?: result.fields.msi,
        msiCurrent: Int? = result.fields.msi_current,
        editedDate: String? = _selectedDate.value
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val account = transactionDao.getAccountById(_selectedAccountId.value)
                    Log.d(TAG, "Account: $account")
                    val isCredit = account?.accountTypeId == "CREDIT"
                    val isPaid = when (account?.accountTypeId) {
                        "CREDIT"     -> false
                        "DEBIT"      -> true
                        "SAVINGS"    -> true
                        "CASH"       -> true
                        "INVESTMENT" -> true
                        else         -> true
                    }

                    val baseDate = editedDate ?: result.fields.date ?: today()
                    val totalInstallments = msiTotal ?: 1
                    val currentPaymentNumber = msiCurrent ?: 1

                    if (totalInstallments > 1) {
                        val remaining = totalInstallments - currentPaymentNumber
                        val planId = UUID.randomUUID().toString()

                        val monthlyBase = (editedAmount / totalInstallments * 100).toLong() / 100.0
                        val totalCalculated = monthlyBase * totalInstallments
                        val remainder = ((editedAmount - totalCalculated) * 100).toLong() / 100.0

                        for (i in 0..remaining) {
                            val installmentNo = currentPaymentNumber + i
                            val isFuture = i > 0 || isCredit
                            val txDate = if (i == 0) baseDate else incrementMonth(baseDate, i)
                            val txAmount = if (installmentNo == totalInstallments) {
                                monthlyBase + remainder
                            } else {
                                monthlyBase
                            }

                            val transaction = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                amount = txAmount,
                                currency = result.fields.currency ?: "MXN",
                                dateIso = txDate,
                                merchant = result.fields.merchant,
                                concept = "${editedConcept ?: result.fields.merchant} ($installmentNo/$totalInstallments)",
                                categoryId = _selectedCategoryId.value ?: "otros",
                                accountId = _selectedAccountId.value,
                                type = type,
                                isSubscription = isSubscription,
                                note = note,
                                usedAi = result.fields.usedAi,
                                msiInstallment = installmentNo,
                                msiTotal = totalInstallments,
                                isPaid = when {
                                    isCredit -> false   // TDC: ninguna cuota MSI nace como pagada
                                    isFuture -> false   // cuota futura en cualquier cuenta
                                    else     -> isPaid  // débito/ahorro/efectivo actual
                                },
                                isFuturePayment = isFuture,
                                deferredPlanId = planId
                            )

                            transactionDao.insertTransactionWithBalanceUpdate(transaction)

                            if (i == 0) {
                                transactionDao.insertDeferredPlan(
                                    DeferredPlanEntity(
                                        id = planId,
                                        transactionId = transaction.id,
                                        totalAmount = editedAmount,
                                        totalInstallments = totalInstallments,
                                        monthlyPayment = monthlyBase,
                                        startDateIso = baseDate
                                    )
                                )
                            }
                        }
                    } else {
                        // Transacción simple
                        val transaction = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            amount = editedAmount,
                            currency = result.fields.currency ?: "MXN",
                            dateIso = baseDate,
                            merchant = result.fields.merchant,
                            concept = editedConcept,
                            categoryId = _selectedCategoryId.value ?: "otros",
                            accountId = _selectedAccountId.value,
                            type = type,
                            isSubscription = isSubscription,
                            note = note,
                            isPaid = isPaid,
                            usedAi = result.fields.usedAi
                        )
                        transactionDao.insertTransactionWithBalanceUpdate(transaction)
                    }
                }
                _saveSuccess.emit(1)
            } catch (e: Exception) {
                Log.e(TAG, "saveTransaction failed", e)
                _uiState.value = ShareReceiverUiState.Error("Error al guardar: ${e.message}")
            }
        }
    }

    private fun incrementMonth(dateIso: String, months: Int): String =
        LocalDate.parse(dateIso).plusMonths(months.toLong()).toString()

    // ── Save (batch from statement) ───────────────────────────────────────

    /**
     * Save all transactions from a parsed bank statement.
     * @param transactions List of ParsedTransaction to save (may be a filtered subset).
     * @param accountId Account to associate transactions with.
     * @param defaultCategoryId Category id to use when no specific category is known.
     */
    fun saveStatementTransactions(
        transactions: List<ParsedTransaction>,
        accountId: String?,
        defaultCategoryId: String = "otros"
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    transactions.forEach { tx ->
                        val entity = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            amount = tx.amount,
                            currency = "MXN",
                            dateIso = tx.dateIso,
                            merchant = tx.merchant,
                            concept = null,
                            categoryId = defaultCategoryId,
                            accountId = accountId ?: _selectedAccountId.value,
                            type = if (tx.isCredit) "ingreso" else "egreso",
                            isSubscription = false,
                            note = null
                        )
                        transactionDao.insertTransactionWithBalanceUpdate(entity)
                    }
                }
                Log.d(TAG, "Saved ${transactions.size} statement transactions")
                _saveSuccess.emit(transactions.size)
            } catch (e: Exception) {
                Log.e(TAG, "saveStatementTransactions failed", e)
            }
        }
    }

    fun clearState() {
        _uiState.value = ShareReceiverUiState.Idle
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Build an OcrResult for a single transaction from raw OCR text.
     * Uses only local heuristics (OcrStructuralExtractor + StatementParser).
     * No network calls.
     */
    private suspend fun buildSingleTxResult(text: String): OcrResult {
        val extractor = OcrStructuralExtractor()
        val structural = extractor.extract(text)

        // Try StatementParser to get the best single transaction found
        val parseResult = StatementParser.parse(text)
        val topTx = parseResult.transactions.firstOrNull()

        // MSI detection (Local)
        val msiPattern1 = Regex("""\b(\d{1,2})\s*x\s*\$""", RegexOption.IGNORE_CASE)
        val msiPattern2 = Regex("""\b(\d{1,2})\s*pagos mensuales""", RegexOption.IGNORE_CASE)
        var msiTotal: Int? = (msiPattern1.find(text) ?: msiPattern2.find(text))
            ?.groupValues?.get(1)?.toIntOrNull()
        var msiCurrent: Int? = null

        val dbCategories = transactionDao.getCategoriesSnapshot()
        val categoryNames = dbCategories.map { it.name }
        val dbAccounts = transactionDao.getAccountsSnapshot()
        val suggestedAccount = matchAccount(text, dbAccounts)

        val merchant = topTx?.merchant ?: structural.text_lines.getOrNull(1) ?: "Desconocido"
        val amount = topTx?.amount ?: structural.amount_candidates.maxOrNull()
        
        // Normalize date to ISO (yyyy-MM-dd)
        val rawDate = topTx?.dateIso ?: structural.date_candidates.firstOrNull() ?: today()
        val date = DateUtils.normalizeDateToIso(rawDate)

        // ── Step 1: Local Heuristics ────────────────────────
        var categoryId: String? = matchLocalCategory(merchant)
        var isSubscription = false
        var txType = if (topTx?.isCredit == true) "ingreso" else "egreso"
        var usedAi = false
        var finalMerchant: String = merchant
        var finalConcept: String? = structural.text_lines.firstOrNull()

        // ── Step 2: AI Fallback ─────────────────────────────
        // If merchant is unknown OR category is neutral/null, we ask the AI.
        if (merchant == "Desconocido" || categoryId == null || categoryId == "otros") {
            try {
                Log.d(TAG, "Local heuristic inconclusive for '$merchant'. Calling AI classification...")
                
                // Redact PII before sending to Backend
                val sanitizedText = SanitizeUtils.redactPII(text)
                Log.d(TAG, "SANITIZED OCR TEXT: $sanitizedText")

                val request = StructuralOcrResult(
                    amount_candidates = listOfNotNull(amount),
                    date_candidates = listOfNotNull(date),
                    text_lines = sanitizedText.lines().filter { it.isNotBlank() }.take(100),
                    categories = categoryNames
                )
                val aiFields = LumeClient.apiService.classifyTransaction(request)
                usedAi = true
                Log.d(TAG, "AI classification response received")
                
                // Map AI category name to DB ID
                val aiId = dbCategories.find { it.name.equals(aiFields.category, ignoreCase = true) }?.id
                    ?: dbCategories.find { it.name.equals(aiFields.suggested_category, ignoreCase = true) }?.id
                
                if (aiId != null) {
                    categoryId = aiId
                    Log.d(TAG, "AI categories mapped to ID: $categoryId")
                }
                
                isSubscription = aiFields.is_subscription
                if (aiFields.type == "ingreso" || aiFields.type == "egreso") {
                    txType = aiFields.type
                }

                if (!aiFields.merchant.isNullOrBlank()) {
                    finalMerchant = aiFields.merchant
                }

                if (!aiFields.concept.isNullOrBlank()) {
                    finalConcept = aiFields.concept
                }
                
                // Try to get MSI from AI
                val aiMsiTotal = aiFields.msi_total ?: aiFields.msi
                if (aiMsiTotal != null && aiMsiTotal > 1) {
                    msiTotal = aiMsiTotal
                }
                if (aiFields.msi_current != null) {
                    msiCurrent = aiFields.msi_current
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI classification failed, falling back to local result/otros", e)
            }
        }

        val finalCategoryId = categoryId ?: "otros"
        val selectedCategory = dbCategories.find { it.id == finalCategoryId }

        val fields = TxFields(
            amount = amount,
            currency = if (text.contains("USD", ignoreCase = true)) "USD" else "MXN",
            date = date,
            merchant = finalMerchant,
            concept = finalConcept,
            category = selectedCategory?.name ?: "Otros",
            type = txType,
            msi = msiTotal,
            msi_total = msiTotal,
            msi_current = msiCurrent,
            is_subscription = isSubscription,
            usedAi = usedAi
        )

        return OcrResult(
            text = text,
            fields = fields,
            selectedCategoryId = finalCategoryId,
            suggestedCategory = selectedCategory,
            suggestedAccountId = suggestedAccount?.id,
            usedAi = usedAi
        )
    }

    private fun matchLocalCategory(merchant: String): String? {
        val m = merchant.lowercase()
        return when {
            m.containsAny("oxxo", "7-eleven", "seven", "superama", "walmart", "chedraui", "soriana") -> "comida"
            m.containsAny("netflix", "spotify", "prime", "disney", "hbo", "apple") -> "entretenimiento"
            m.containsAny("uber", "didi", "taxi", "gasolinera", "pemex") -> "transporte"
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }

    private fun matchAccount(
        text: String,
        accounts: List<com.jaedhc.lume.data.db.AccountEntity>
    ): com.jaedhc.lume.data.db.AccountEntity? {
        val normalized = text.lowercase()
        // 1. Match by last 4 card digits
        accounts.find { it.last4.isNotBlank() && normalized.contains(it.last4) }
            ?.let { return it }
        // 2. Match by bank name
        return accounts.find { it.bankName.isNotBlank() && normalized.contains(it.bankName.lowercase()) }
    }

    private suspend fun runOcr(resolver: ContentResolver, uri: Uri): String {
        val image = InputImage.fromBitmap(loadBitmap(resolver, uri), 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return recognizer.process(image).await().text
    }

    private fun loadBitmap(resolver: ContentResolver, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(resolver, uri)
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
