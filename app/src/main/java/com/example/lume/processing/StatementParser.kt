package com.example.lume.processing

import android.util.Log
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

data class ParsedTransaction(
    val dateIso: String,   // YYYY-MM-DD
    val merchant: String,
    val amount: Double,
    val isCredit: Boolean = false  // true = abono/refund, false = cargo/charge
)

data class ParseResult(
    val transactions: List<ParsedTransaction>,
    val confidence: Confidence,
    val mode: String            // for debug logging
)

enum class Confidence { HIGH, MEDIUM, LOW }

// ─────────────────────────────────────────────────────────────────────────────
// StatementParser — fully local, no network calls
// ─────────────────────────────────────────────────────────────────────────────

object StatementParser {

    private val TAG = "LumeParser"

    // ── Month name → number ────────────────────────────────────────────────
    private val MONTH_MAP = mapOf(
        "ene" to 1, "jan" to 1, "enero" to 1, "january" to 1,
        "feb" to 2, "febrero" to 2, "february" to 2,
        "mar" to 3, "marzo" to 3, "march" to 3,
        "abr" to 4, "apr" to 4, "abril" to 4, "april" to 4,
        "may" to 5, "mayo" to 5,
        "jun" to 6, "junio" to 6, "june" to 6,
        "jul" to 7, "julio" to 7, "july" to 7,
        "ago" to 8, "aug" to 8, "agosto" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "septiembre" to 9, "september" to 9,
        "oct" to 10, "octubre" to 10, "october" to 10,
        "nov" to 11, "noviembre" to 11, "november" to 11,
        "dic" to 12, "dec" to 12, "diciembre" to 12, "december" to 12
    )

    private val MONTH_ABBREVS = MONTH_MAP.keys
        .filter { it.length == 3 }
        .joinToString("|")

    // ── Core regex patterns ────────────────────────────────────────────────

    /**
     * MODE A — Inline: date + merchant + amount on one line.
     * Handles:
     *   "22 FEB OXXO RIO PANUCO $22.00"
     *   "22/02 AMAZON PRIME $199.00"
     *   "22/02/2024 NETFLIX -$179.00"
     */
    private val INLINE = Regex(
        """(\d{1,2})[/ ]($MONTH_ABBREVS|\d{2})(?:[/ ]\d{2,4})?\s+(.+?)\s+([-–]?\$?\s*[\d,]+\.\d{2})""",
        setOf(RegexOption.IGNORE_CASE)
    )

    /**
     * MODE C — ISO date inline: YYYY-MM-DD or DD-MM-YYYY
     * Handles:
     *   "2024-02-22 STARBUCKS 85.00"
     *   "22-02-2024 STARBUCKS 85.00"
     */
    private val ISO_INLINE = Regex(
        """(\d{4}-\d{2}-\d{2}|\d{2}-\d{2}-\d{4})\s+(.+?)\s+([-–]?\$?\s*[\d,]+\.\d{2})"""
    )

    /**
     * MODE D — Slash date with full year on same line.
     * "22/02/2024 COMERCIO $123.45"
     */
    private val SLASH_FULL_DATE_INLINE = Regex(
        """(\d{1,2}/\d{2}/\d{4})\s+(.+?)\s+([-–]?\$?\s*[\d,]+\.\d{2})"""
    )

    /** Detects a date-only or date+merchant start (Mode B split-column, e.g. Nu) */
    private val DATE_MERCHANT_LINE = Regex(
        """^(\d{1,2})[/ ]($MONTH_ABBREVS|\d{2})(?:[/ ]\d{2,4})?\s+(.+)$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    /** Amount-only lines (Mode B — Nu style). Also allows negative amounts. */
    private val AMOUNT_ONLY = Regex(
        """^[-–]?\$?\s*([\d,]+\.\d{2})\s*(?:MXN|MN|USD)?$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    /** Standalone amounts that appear at end of line (BBVA/Santander table) */
    private val TRAILING_AMOUNT = Regex(
        """([-–]?\$?\s*[\d,]+\.\d{2})(?:\s+(?:MXN|MN|USD))?$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    /** Lines that are table headers, noise, or summaries — skip them */
    private val NOISE = Regex(
        """^(fecha|descripci[oó]n?|monto|saldo|cargo|concepto|importe|total|subtotal|
            |iva|tipo de cambio|estado de cuenta|resumen|movimientos|periodo|numero de cuenta|
            |grafic|kB/s|pagar|^\d{4}${'$'}|.*\(\*\s*\d+\)|limite de cr[eé]dito|disponible)""".trimMargin(),
        setOf(RegexOption.IGNORE_CASE)
    )

    /**
     * Keywords in the merchant/description field that identify a PAYMENT to the card
     * (i.e. the customer paid their credit card bill) → isCredit = true.
     * Banks like Nu show these as positive amounts with no negative sign.
     */
    private val PAYMENT_KEYWORDS = setOf(
        "gracias por tu pago", "pago recibido", "pago de contado",
        "pago en linea", "pago en línea", "pago automatico", "pago automático",
        "pago domiciliado", "abono", "abono recibido", "liquidacion", "liquidación",
        "pago tarjeta", "deposito a cuenta", "depósito a cuenta",
        "transferencia recibida", "spei recibido", "pago oportunidad",
        "pago cuenta"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse a raw OCR text and extract transactions.
     * Tries multiple strategies in order of reliability, returns the best result.
     */
    fun parse(rawText: String): ParseResult {
        val lines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !isNoiseLine(it) }

        // Strategy 1 — ISO dates inline (high precision)
        val isoResults = parseIsoInline(lines)
        if (isoResults.size >= 2) {
            Log.d(TAG, "Mode=ISO_INLINE → ${isoResults.size} txns")
            return ParseResult(postProcess(isoResults), Confidence.HIGH, "ISO_INLINE")
        }

        // Strategy 2 — Full slash date inline (DD/MM/YYYY merchant amount)
        val slashResults = parseSlashFullDateInline(lines)
        if (slashResults.size >= 2) {
            Log.d(TAG, "Mode=SLASH_FULL_INLINE → ${slashResults.size} txns")
            return ParseResult(postProcess(slashResults), Confidence.HIGH, "SLASH_FULL_INLINE")
        }

        // Strategy 3 — Inline (date + merchant + amount on same line)
        val inlineResults = parseInline(lines)
        if (inlineResults.size >= 2) {
            Log.d(TAG, "Mode=INLINE → ${inlineResults.size} txns")
            return ParseResult(postProcess(inlineResults), Confidence.HIGH, "INLINE")
        }

        // Strategy 4 — Split column (Nu-style: date+merchant rows, then amounts)
        val splitResults = parseSplitColumn(lines)
        if (splitResults.isNotEmpty()) {
            val confidence = if (splitResults.size >= 3) Confidence.MEDIUM else Confidence.LOW
            Log.d(TAG, "Mode=SPLIT_COLUMN → ${splitResults.size} txns, confidence=$confidence")
            return ParseResult(splitResults, confidence, "SPLIT_COLUMN")
        }

        // Nothing found
        Log.d(TAG, "Mode=NONE → 0 txns")
        return ParseResult(emptyList(), Confidence.LOW, "NONE")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy implementations
    // ─────────────────────────────────────────────────────────────────────────

    /** ISO date inline: "2024-02-22 MERCHANT 199.00" */
    private fun parseIsoInline(lines: List<String>): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        for (line in lines) {
            val m = ISO_INLINE.find(line) ?: continue
            val (dateRaw, merchant, amountRaw) = m.destructured
            val iso = parseIsoDate(dateRaw) ?: continue
            val (amount, isCredit) = parseAmount(amountRaw) ?: continue
            results += ParsedTransaction(iso, cleanMerchant(merchant), amount, isCredit)
        }
        return results
    }

    /** Slash full date inline: "22/02/2024 MERCHANT 199.00" */
    private fun parseSlashFullDateInline(lines: List<String>): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        for (line in lines) {
            val m = SLASH_FULL_DATE_INLINE.find(line) ?: continue
            val (dateRaw, merchant, amountRaw) = m.destructured
            val iso = parseSlashFullDate(dateRaw) ?: continue
            val (amount, isCredit) = parseAmount(amountRaw) ?: continue
            results += ParsedTransaction(iso, cleanMerchant(merchant), amount, isCredit)
        }
        return results
    }

    /** Inline mode: "22 FEB OXXO $22.00" or "22/02 OXXO $22.00" */
    private fun parseInline(lines: List<String>): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        for (line in lines) {
            val m = INLINE.find(line) ?: continue
            val (day, monthRaw, merchant, amountRaw) = m.destructured
            val iso = buildIso(day.toIntOrNull() ?: continue, monthRaw) ?: continue
            val (amount, isCredit) = parseAmount(amountRaw) ?: continue
            results += ParsedTransaction(iso, cleanMerchant(merchant), amount, isCredit)
        }
        return results
    }

    /**
     * Split-column mode for Nu and similar banks.
     * Reads date+merchant lines and standalone amount lines, then zips them.
     *
     * Also handles tables where amounts appear as the LAST column on date+merchant lines
     * (e.g., BBVA with extra trailing amount).
     */
    private fun parseSplitColumn(lines: List<String>): List<ParsedTransaction> {
        val dateMerchants = mutableListOf<Pair<String, String>>() // iso to merchant
        val amounts = mutableListOf<Pair<Double, Boolean>>()      // amount to isCredit

        for (line in lines) {
            // Check for amount-only line first
            val amountOnlyMatch = AMOUNT_ONLY.matchEntire(line.trim())
            if (amountOnlyMatch != null) {
                parseAmount(amountOnlyMatch.groupValues[1])?.let { amounts += it }
                continue
            }

            // Check for date+merchant line (optionally trailing amount at end)
            val dmMatch = DATE_MERCHANT_LINE.find(line.trim())
            if (dmMatch != null) {
                val (day, monthRaw, rest) = dmMatch.destructured
                val iso = buildIso(day.toIntOrNull() ?: continue, monthRaw) ?: continue

                // Check if there's a trailing amount on this same line
                val trailingMatch = TRAILING_AMOUNT.find(rest.trim())
                if (trailingMatch != null) {
                    val merchant = rest.substring(0, trailingMatch.range.first).trim()
                    val (amt, isCredit) = parseAmount(trailingMatch.groupValues[1]) ?: continue
                    // If trailing amount found, treat as a complete inline record
                    dateMerchants += iso to merchant
                    amounts += amt to isCredit
                } else {
                    dateMerchants += iso to rest.trim()
                }
                continue
            }
        }

        Log.d(TAG, "SplitCol: ${dateMerchants.size} date+merchant, ${amounts.size} amounts")

        if (dateMerchants.isEmpty() || amounts.isEmpty()) return emptyList()

        // Nu sometimes has an extra first amount = statement total — drop it
        val amountList = when {
            amounts.size == dateMerchants.size + 1 -> amounts.drop(1)
            else -> amounts
        }

        // Only zip if counts roughly match (allow ±1 mismatch)
        if (kotlin.math.abs(dateMerchants.size - amountList.size) > 1) {
            Log.d(TAG, "SplitCol: count mismatch (${dateMerchants.size} vs ${amountList.size}), skipping")
            return emptyList()
        }

        return dateMerchants.zip(amountList).map { (dm, amtPair) ->
            ParsedTransaction(dm.first, dm.second, amtPair.first, amtPair.second)
        }
    }

    /**
     * Post-processing step applied to all successful parse results.
     * Identifies un-signed payments (e.g., "¡Gracias por tu pago!") using keywords
     * and forces isCredit = true so they are correctly classified as ingress.
     */
    private fun postProcess(transactions: List<ParsedTransaction>): List<ParsedTransaction> {
        return transactions.map { tx ->
            val merchantLower = (tx.merchant ?: "").lowercase()
            val isPayment = PAYMENT_KEYWORDS.any { keyword -> merchantLower.contains(keyword) }
            
            if (isPayment && !tx.isCredit) {
                // It's a payment but wasn't detected as credit (no minus sign)
                // Force it to be considered a credit
                tx.copy(isCredit = true)
            } else {
                tx
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun isNoiseLine(line: String): Boolean = NOISE.containsMatchIn(line)

    /**
     * Parse amount string; return (value, isCredit).
     * Negative sign or "abono"/"cr" → isCredit = true.
     */
    private fun parseAmount(raw: String): Pair<Double, Boolean>? {
        val cleaned = raw.trim()
        val isCredit = cleaned.startsWith("-") || cleaned.startsWith("–")
        val numeric = cleaned
            .replace(Regex("""^[-–\$\s]+"""), "")
            .replace(",", "")
            .trim()
        val value = numeric.toDoubleOrNull() ?: return null
        return if (value > 0) Pair(value, isCredit) else null
    }

    /** Build ISO date from day (Int) + month name or 2-digit string */
    private fun buildIso(day: Int, monthRaw: String): String? {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val monthNum = monthRaw.lowercase().let { raw ->
            MONTH_MAP[raw] ?: raw.toIntOrNull()
        } ?: return null
        if (day < 1 || day > 31 || monthNum < 1 || monthNum > 12) return null
        return "%04d-%02d-%02d".format(year, monthNum, day)
    }

    /** Parse "2024-02-22" or "22-02-2024" */
    private fun parseIsoDate(raw: String): String? {
        val parts = raw.split("-")
        if (parts.size != 3) return null
        return if (parts[0].length == 4) {
            // YYYY-MM-DD
            val (y, m, d) = parts
            if (m.toIntOrNull() in 1..12 && d.toIntOrNull() in 1..31)
                raw else null
        } else {
            // DD-MM-YYYY
            val (d, m, y) = parts
            if (m.toIntOrNull() in 1..12 && d.toIntOrNull() in 1..31)
                "$y-$m-$d" else null
        }
    }

    /** Parse "22/02/2024" → "2024-02-22" */
    private fun parseSlashFullDate(raw: String): String? {
        val parts = raw.split("/")
        if (parts.size != 3) return null
        val (d, m, y) = parts
        if (m.toIntOrNull() !in 1..12 || d.toIntOrNull() !in 1..31) return null
        return "%s-%02d-%02d".format(y, m.toInt(), d.toInt())
    }

    /** Remove common OCR artifacts and leading/trailing noise from merchant names */
    private fun cleanMerchant(raw: String): String {
        return raw
            .replace(Regex("""[|\\*]{2,}"""), " ")   // OCR artifacts
            .replace(Regex("""\s{2,}"""), " ")          // double spaces
            .trim()
            .take(80)                                    // cap length
    }
}
