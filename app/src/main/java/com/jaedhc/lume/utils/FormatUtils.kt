package com.jaedhc.lume.utils

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    fun formatCurrency(amount: Double): String = currencyFormatter.format(amount)
}
