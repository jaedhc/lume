package com.example.lume.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementParserTest {

    @Test
    fun `test parse inline format - common bank`() {
        val rawText = """
            22 FEB OXXO RIO PANUCO $22.00
            23/02 AMAZON PRIME $199.00
            24-02-2024 NETFLIX -$179.00
        """.trimIndent()

        val result = StatementParser.parse(rawText)

        assertEquals(3, result.transactions.size)
        
        // Test 1: OXXO
        assertEquals("OXXO RIO PANUCO", result.transactions[0].merchant)
        assertEquals(22.0, result.transactions[0].amount, 0.01)
        assert(!result.transactions[0].isCredit)

        // Test 3: Netflix (Negative amount should be credit/ingress if it's a card statement usually, 
        // but wait, in StatementParser.kt parseAmount: isCredit = cleaned.startsWith("-"))
        // Actually, parseAmount says: isCredit = cleaned.startsWith("-")
        // So a negative sign means isCredit = true.
        assertEquals(179.0, result.transactions[2].amount, 0.01)
        assertTrue(result.transactions[2].isCredit)
    }

    @Test
    fun `test parse split column format - Nu style`() {
        val rawText = """
            Resumen de cuenta
            22 FEB
            STARBUCKS
            $85.00
            23 FEB
            ¡Gracias por tu pago!
            $500.00
        """.trimIndent()

        val result = StatementParser.parse(rawText)

        // It should zip 22 FEB STARBUCKS with 85.00 and 23 FEB Pago with 500.00
        assertEquals(2, result.transactions.size)
        
        assertEquals("STARBUCKS", result.transactions[0].merchant)
        assertEquals(85.0, result.transactions[0].amount, 0.01)
        
        assertEquals("¡Gracias por tu pago!", result.transactions[1].merchant)
        // Post-processing should catch "Gracias por tu pago" and mark it as isCredit = true
        assertTrue(result.transactions[1].isCredit)
    }

    @Test
    fun `test noise filtering`() {
        val rawText = """
            FECHA DESCRIPCION MONTO
            22 FEB COMERCIO $100.00
            TOTAL $100.00
            PAGAR $100.00
        """.trimIndent()

        val result = StatementParser.parse(rawText)

        // Should only find the middle transaction
        assertEquals(1, result.transactions.size)
        assertEquals("COMERCIO", result.transactions[0].merchant)
    }

    @Test
    fun `test trailing amount - table format`() {
        val rawText = """
            22/02/2024 RESTAURANTE LA LUNA $450.00 MXN
            23/02/2024 GASOLINERA SUR $1,200.50
        """.trimIndent()

        val result = StatementParser.parse(rawText)

        assertEquals(2, result.transactions.size)
        assertEquals(450.0, result.transactions[0].amount, 0.01)
        assertEquals(1200.50, result.transactions[1].amount, 0.01)
    }
}
