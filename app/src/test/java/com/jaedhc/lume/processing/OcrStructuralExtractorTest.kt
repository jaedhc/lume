package com.jaedhc.lume.processing

import org.junit.Assert.*
import org.junit.Test

class OcrStructuralExtractorTest {

    private val extractor = OcrStructuralExtractor()

    @Test
    fun testNormalizeText() {
        val input = "COMPRA EN MÉXICO: $100.00\n  Doble  Espacio  "
        val expected = "compra en mexico: $100.00\ndoble espacio"
        assertEquals(expected, extractor.normalizeText(input))
    }

    @Test
    fun testExtractAmounts() {
        val input = "Importe: $1,250.50 Total: 500.00 MN Sin decimals: $100 "
        val result = extractor.extractAmounts(input)
        
        assertTrue(result.contains(1250.5))
        assertTrue(result.contains(500.0))
        assertTrue(result.contains(100.0))
        assertEquals(3, result.size)
    }

    @Test
    fun testExtractDates() {
        val input = "Fecha: 22/02/2026 Pago el 2026-01-15 y 12-05-24"
        val result = extractor.extractDates(input)
        
        assertTrue(result.contains("22/02/2026"))
        assertTrue(result.contains("2026-01-15"))
        assertTrue(result.contains("12-05-24"))
        assertEquals(3, result.size)
    }

    @Test
    fun testSanitizeLines_FiltersSensitiveData() {
        val input = """
            Compra exitosa
            Tarjeta: **** **** **** 1234
            Cuenta: 123456789012345678
            CLABE: 012345678901234567
            Importe: $100.00
            
            Terminacion: 5678
        """.trimIndent()
        
        val result = extractor.sanitizeLines(input)
        
        assertTrue(result.contains("compra exitosa"))
        assertTrue(result.contains("importe: $100.00"))
        assertTrue(result.contains("terminacion: 5678"))
        
        // Should NOT contain the long numbers
        assertFalse(result.any { it.contains("123456789012345678") })
        assertFalse(result.any { it.contains("012345678901234567") })
        assertFalse(result.any { it.contains("****") })
    }

    @Test
    fun testEndToEndExtraction() {
        val ocrRaw = """
            Banorte Movil
            Fecha: 22/FEB/2026 14:30
            Comercio: OXXO GAS
            Importe: $550.00 MN
            Referencia: 123456
            Tarjeta: 4567 XXXX XXXX 9012
        """.trimIndent()
        
        // Note: normalizeText will convert FEB to feb, but extractDates in Stage A uses numeric regex only for now
        // Based on the user request, they specified dd/mm/yyyy. 
        // I might want to add month name support if it's common, but sticking to requested formats for now.
        
        val result = extractor.extract(ocrRaw)
        
        assertTrue(result.amount_candidates.contains(550.0))
        assertTrue(result.text_lines.contains("comercio: oxxo gas"))
        assertFalse(result.text_lines.any { it.contains("4567") && it.contains("9012") })
    }
}
