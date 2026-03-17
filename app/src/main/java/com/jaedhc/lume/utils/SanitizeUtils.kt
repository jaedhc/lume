package com.jaedhc.lume.utils

import android.util.Log

object SanitizeUtils {
    private const val TAG = "SanitizeUtils"
    private const val REDACTED = "[REDACTADO]"

    // Regex patterns for PII
    private val CARD_PATTERN = Regex("""\b(?:\d[ -]*?){13,16}\b""")
    private val MASKED_CARD_PATTERN = Regex("""\b[\d\*]{4,}[ -]*?[\d\*]{4,}[ -]*?[\d\*]{4,}[ -]*?\d{4}\b""")
    private val CLABE_PATTERN = Regex("""\b\d{18}\b""")
    private val EMAIL_PATTERN = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""")
    private val PHONE_PATTERN = Regex("""\b\d{10}\b""")
    
    // Heuristic patterns for names after keywords
    private val NAME_KEYWORDS = listOf("TITULAR", "CLIENTE", "NOMBRE", "BENEFICIARIO", "ATENDIDO POR")

    /**
     * Redacts PII (Personally Identifiable Information) from the given text.
     * Card numbers, emails, CLABEs, and phones are replaced with [REDACTADO].
     */
    fun redactPII(text: String): String {
        var sanitized = text

        // 1. Redact direct patterns
        sanitized = CARD_PATTERN.replace(sanitized, REDACTED)
        sanitized = MASKED_CARD_PATTERN.replace(sanitized, REDACTED)
        sanitized = CLABE_PATTERN.replace(sanitized, REDACTED)
        sanitized = EMAIL_PATTERN.replace(sanitized, REDACTED)
        sanitized = PHONE_PATTERN.replace(sanitized, REDACTED)

        // 2. Redact names after keywords (Line-based heuristic)
        val lines = sanitized.lines().map { line ->
            var updatedLine = line
            NAME_KEYWORDS.forEach { keyword ->
                if (line.uppercase().contains(keyword)) {
                    // If line is like "TITULAR: JUAN PEREZ", redact the part after ":"
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        updatedLine = "${parts[0]}: $REDACTED"
                    } else {
                        // If it's just "TITULAR" followed by name on same line, or just suspicious line
                        // We redact the whole line if it's short and contains the keyword
                        if (line.length < 50) {
                            updatedLine = "$keyword $REDACTED"
                        }
                    }
                }
            }
            updatedLine
        }

        val result = lines.joinToString("\n")
        
        Log.d(TAG, "PII Redaction complete. Length reduced: ${text.length} -> ${result.length}")
        return result
    }
}
