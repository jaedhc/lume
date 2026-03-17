package com.jaedhc.lume.utils

import android.os.Build
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateUtils {
    
    // Only initialized if API >= O, safely wrapped later
    private val dateFormatter by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
        } else null
    }
    
    private val dmyFormatter by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        } else null
    }

    /**
     * Tries to parse dates in either ISO (yyyy-MM-dd) or DD/MM/YYYY format.
     */
    fun parseSafeLocalDate(dateStr: String): LocalDate? {
        if (dateStr.isBlank()) return null
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        
        // Try ISO format first (e.g. 2026-03-16)
        if (dateStr.contains("-")) {
            // Might have time, take only the date part
            val pureDateStr = dateStr.substringBefore("T").substringBefore(" ")
            return try {
                LocalDate.parse(pureDateStr)
            } catch (e: DateTimeParseException) {
                null
            }
        }
        
        // Try DD/MM/YYYY (e.g. 14/03/2026)
        if (dateStr.contains("/")) {
            return try {
                dmyFormatter?.let { LocalDate.parse(dateStr, it) }
            } catch (e: DateTimeParseException) {
                null
            }
        }
        
        return null
    }

    /**
     * Formats a potentially tricky date string into "dd MMM" (e.g. "14 Mar").
     */
    fun toSafeFormattedDate(dateStr: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return dateStr
        val date = parseSafeLocalDate(dateStr) ?: return dateStr
        return dateFormatter?.let { date.format(it).replaceFirstChar { char -> char.uppercase() } } ?: dateStr
    }

    /**
     * Normalizes any date string (ISO or DD/MM/YYYY) to ISO (YYYY-MM-DD).
     * Falls back to today's date if parsing fails.
     */
    fun normalizeDateToIso(dateStr: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Fallback for older APIs — return as is if looks like ISO, or just return dateStr
            return if (dateStr.contains("-")) dateStr else dateStr
        }
        val date = parseSafeLocalDate(dateStr) ?: LocalDate.now()
        return date.toString()
    }
}
