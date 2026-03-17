package com.jaedhc.lume.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaedhc.lume.data.StructuralOcrResult
import com.jaedhc.lume.network.LumeClient
import com.jaedhc.lume.processing.Confidence
import com.jaedhc.lume.processing.ParsedTransaction
import com.jaedhc.lume.processing.PdfOcrProcessor
import com.jaedhc.lume.processing.StatementParser
import com.jaedhc.lume.data.db.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportStatementUiState(
    val pickedStatementUri: Uri? = null,
    val statementFileName: String? = null,
    val statementStatus: String? = null,
    val parsedTransactions: List<ParsedTransaction> = emptyList()
)

class ImportStatementViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ImportStatement"

    private val _uiState = MutableStateFlow(ImportStatementUiState())
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    fun onStatementPicked(uri: Uri, fileName: String) {
        _uiState.update {
            it.copy(
                pickedStatementUri = uri,
                statementFileName = fileName,
                statementStatus = "Procesando...",
                parsedTransactions = emptyList()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val text = PdfOcrProcessor.extractText(resolver, uri)
                val parseResult = StatementParser.parse(text)
                val txns = parseResult.transactions
                Log.d(TAG, "Parsed ${txns.size} txns — mode=${parseResult.mode}, confidence=${parseResult.confidence}")

                val statusMsg = when {
                    txns.isEmpty() -> "No se detectaron transacciones"
                    parseResult.confidence == Confidence.LOW ->
                        "${txns.size} transacciones detectadas (baja confianza)"
                    else -> "${txns.size} transacciones detectadas ✓"
                }
                _uiState.update { it.copy(statementStatus = statusMsg, parsedTransactions = txns) }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando estado de cuenta", e)
                _uiState.update { it.copy(statementStatus = "Error al leer archivo") }
            }
        }
    }

    fun onStatementCleared() {
        _uiState.update {
            it.copy(
                pickedStatementUri = null,
                statementFileName = null,
                statementStatus = null,
                parsedTransactions = emptyList()
            )
        }
    }

    /**
     * Call AI API strictly to get a category ID for a given transaction.
     * Contains local fallbacks first to reduce latency and cost.
     */
    suspend fun classifyCategory(
        tx: ParsedTransaction,
        dbCategories: List<CategoryEntity>,
        categoryNames: List<String>
    ): String {
        val merchant = (tx.merchant ?: "").lowercase()

        // ── Local heuristics (instant, no network) ────────────────────────
        val localMatch = when {
            merchant.containsAny("oxxo", "7-eleven", "seven", "superama", "walmart",
                "chedraui", "soriana", "costco", "la comer", "bodega aurrera",
                "starbucks", "mcdonald", "burger", "kfc", "domino", "pizza",
                "subway", "sushi", "tacos", "taqueria", "restauran", "comida") ->
                "comida"

            merchant.containsAny("netflix", "spotify", "amazon prime", "disney",
                "hbo", "apple", "google play", "youtube", "suscripcion", "prime") ->
                "entretenimiento"

            merchant.containsAny("uber", "didi", "cabify", "taxi", "autobus",
                "gasolinera", "pemex", "shell", "bp", "mobil", "estacion",
                "tag", "caseta", "valet") ->
                "transporte"

            merchant.containsAny("farmacia", "medic", "doctor", "hospital",
                "clinica", "laboratorio", "dentista", "gym", "fitness") ->
                "salud"

            merchant.containsAny("cfe", "telmex", "telcel", "at&t", "movistar",
                "izzi", "totalplay", "megacable", "agua", "gas", "luz",
                "internet", "telefon") ->
                "servicios"

            merchant.containsAny("paypal", "mercado pago", "bbva", "santander",
                "banamex", "hsbc", "nu ", "nubank", "transferencia", "spei",
                "retiro", "deposito", "banco", "inversion", "cetes") ->
                "finanzas"

            else -> null
        }

        if (localMatch != null) {
            val category = dbCategories.find { it.id == localMatch }
            if (category != null) {
                Log.d(TAG, "Local heuristic applied for $merchant -> $localMatch")
                return category.id
            }
        }

        // ── AI classification ───────────────────────────────────────────────
        return try {
            val request = StructuralOcrResult(
                amount_candidates = listOf(tx.amount),
                date_candidates = listOf(tx.dateIso),
                text_lines = listOfNotNull(tx.merchant),
                categories = categoryNames
            )
            val fields = LumeClient.apiService.classifyTransaction(request)
            val aiCategoryName = fields.category ?: ""
            Log.d(TAG, "AI correctly answered category: $aiCategoryName for $merchant")
            dbCategories.find { it.name.equals(aiCategoryName, ignoreCase = true) }?.id ?: "otros"
        } catch (e: Exception) {
            // Log the exception fully instead of silently falling back
            Log.e(TAG, "AI classification failed or threw serialization error for '${tx.merchant}'", e)
            "otros"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
