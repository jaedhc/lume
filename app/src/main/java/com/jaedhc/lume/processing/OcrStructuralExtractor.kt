package com.jaedhc.lume.processing

import com.jaedhc.lume.data.StructuralOcrResult
import java.text.Normalizer

class OcrStructuralExtractor {

    /**
     * Entry point for Stage A structural extraction.
     */
    fun extract(text: String): StructuralOcrResult {
        val normalized = normalizeText(text)
        val amountCandidates = extractAmounts(normalized)
        val dateCandidates = extractDates(normalized)
        val sanitizedLines = sanitizeLines(normalized)

        return StructuralOcrResult(
            amount_candidates = amountCandidates,
            date_candidates = dateCandidates,
            text_lines = sanitizedLines,
            categories = emptyList()
        )
    }

    /**
     * Normalizes text: lowercase, remove accents, unify spaces and line breaks.
     */
    fun normalizeText(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutAccents = temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        var normalized = withoutAccents.lowercase()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\r?\\n"), "\n")
            .trim()
            
        // Fix missing decimals in MSI formats (e.g., Mercado Libre "12x $ 12676" -> "12x $ 126.76")
        val missingDotPattern = Regex("""(\b\d{1,2}x\s*\$\s*)(?<!\.)(\d+)(\d{2})(?!\.)\b""")
        normalized = missingDotPattern.replace(normalized, "$1$2.$3")
        
        return normalized
    }

    /**
     * Extracts potential currency amounts.
     * Matches numbers with 2 decimals or preceded by a dollar sign.
     */
    fun extractAmounts(text: String): List<Double> {
        // Pre-process: remove commas that act as thousands separators
        val t = text.replace(Regex("(\\d),(\\d{3})"), "$1$2")
        
        // Regex for:
        // 1. Symbol/Prefix ($, s, S, pesos, etc.) followed by number
        // 2. Number followed by suffix (mn, mxn, usd)
        // 3. Any number with exactly 2 decimals
        val regex = Regex("""(?:(?:\$|s|S|pesos|mxn|usd)\s*(\d+(?:\.\d{1,2})?))|(?:(\d+(?:\.\d{1,2})?)\s*(?:mn|mxn|usd|pesos))|(\d+\.\d{2})""", RegexOption.IGNORE_CASE)
        
        return regex.findAll(t)
            .mapNotNull { match ->
                val group1 = match.groupValues.getOrNull(1)
                val group2 = match.groupValues.getOrNull(2)
                val group3 = match.groupValues.getOrNull(3)
                val valueStr = sequenceOf(group1, group2, group3).firstOrNull { !it.isNullOrEmpty() }
                valueStr?.toDoubleOrNull()
            }
            .distinct()
            .filter { it > 0 && it < 1_000_000_000 }
            .toList()
    }

    /**
     * Extracts dates in common formats.
     */
    fun extractDates(text: String): List<String> {
        val dateRegexes = listOf(
            Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b"""), // dd/mm/yyyy, dd-mm-yy
            Regex("""\b(\d{4}[/-]\d{2}[/-]\d{2})\b""")        // yyyy-mm-dd
        )
        
        return dateRegexes.flatMap { regex ->
            regex.findAll(text).map { it.value }
        }.distinct().toList()
    }

    /**
     * Filters and cleans lines.
     * Excludes empty lines, lines with 12+ consecutive digits, and suspected card numbers.
     */
    fun sanitizeLines(text: String): List<String> {
        val lines = text.split("\n")
        return lines.filter { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@filter false
            
            // Exclude lines with 12+ consecutive digits (accounts, CLABE, etc.)
            if (Regex("""\d{12,}""").containsMatchIn(trimmed)) return@filter false
            
            // Exclude suspected card numbers (13-19 digits, possibly with spaces)
            if (Regex("""\b(?:\d[ \t]?){13,19}\b""").containsMatchIn(trimmed)) return@filter false
            
            true
        }.map { it.trim() }
    }
}
