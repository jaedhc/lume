package com.jaedhc.lume.processing

import org.junit.Assert.assertEquals
import org.junit.Test

class BanorteProcessorTest {

    private val processor = BanorteProcessor()

    @Test
    fun testParseAmountFormats() {
        val testCases = listOf(
            "COMPRA EN BANORTE: $108.00 MXN" to 108.0,
            "Tu transferencia fue exitosa 108.00 MN" to 108.0,
            "Transferencia exitosa de 2,000.00 MN" to 2000.0,
            "Monto: $2,500.50" to 2500.5,
            "Costo total: 1,234.56 MXN" to 1234.56,
            "Pago de 500 MN en establecimiento" to 500.0
        )

        testCases.forEach { (text, expectedAmount) ->
            val result = processor.parse(text)
            assertEquals("Failed for text: $text", expectedAmount, result.amount)
        }
    }

    @Test
    fun testParseCurrency() {
        val testCases = listOf(
            "$108.00 MN" to "MXN",
            "100.00 USD" to "USD",
            "Monto: 50.00 MXN" to "MXN"
        )

        testCases.forEach { (text, expectedCurrency) ->
            val result = processor.parse(text)
            assertEquals("Failed for text: $text", expectedCurrency, result.currency)
        }
    }
}
