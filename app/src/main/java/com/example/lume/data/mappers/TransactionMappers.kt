package com.example.lume.data.mappers

import com.example.lume.data.db.TransactionWithCategory
import com.example.lume.data.db.DeferredPlanEntity
import java.time.LocalDate

object TransactionMappers {
    fun TransactionWithCategory.isMsi(): Boolean {
        val tx = this.transaction
        val title = tx.concept ?: tx.merchant ?: ""
        return tx.msiTotal != null || 
               tx.deferredPlanId != null || 
               tx.id.startsWith("virtual_") ||
               title.contains("mercado pago", ignoreCase = true)
    }

    fun TransactionWithCategory.cleanConcept(): String {
        val tx = this.transaction
        // Priority: concept > merchant
        // We prefer 'concept' because it represents the user's explicitly entered name 
        // or the AI's cleaned interpretation, whereas 'merchant' is the raw, unrefined string from the bank.
        val rawName = tx.concept ?: tx.merchant ?: "Transacción"
        var tmp = rawName.replace(Regex("\\s+"), " ").trim()
        if (tmp.endsWith("-")) tmp = tmp.dropLast(1).trim()
        return tmp
    }

    /**
     * Enriches a transaction with its DeferredPlan (if any) and cleans its concept.
     * This ensures Dashboard, Transactions, and TdcDetail all show identical, clean data.
     */
    fun TransactionWithCategory.enrichWithMsiAndClean(
        plans: List<DeferredPlanEntity>, 
        today: LocalDate = LocalDate.now()
    ): TransactionWithCategory {
        val tx = this.transaction
        val plan = plans.find { it.transactionId == tx.id }
        
        // 1. Enrich with MSI data if plan exists and transaction doesn't have it natively yet
        val enrichedTx = if (plan != null && tx.msiTotal == null) {
            val startYear = plan.startDateIso.take(4).toIntOrNull() ?: today.year
            val startMonth = plan.startDateIso.drop(5).take(2).toIntOrNull() ?: today.monthValue
            val monthsPassed = (today.year - startYear) * 12 + (today.monthValue - startMonth)
            val currentInstallment = (monthsPassed + 1).coerceIn(1, plan.totalInstallments)
            
            tx.copy(
                msiInstallment = currentInstallment,
                msiTotal = plan.totalInstallments,
                deferredPlanId = plan.id
            )
        } else tx

        // 2. Clean concept
        val rawConcept = enrichedTx.concept ?: enrichedTx.merchant ?: "Transacción"
        var cleanedConcept = rawConcept.replace(Regex("\\s+"), " ").trim()
        if (cleanedConcept.endsWith("-")) {
            cleanedConcept = cleanedConcept.dropLast(1).trim()
        }
        // Fallback to merchant if concept is pure garbage like "9:14 o."
        if (cleanedConcept.matches(Regex(".*\\d:\\d\\d.*")) || cleanedConcept.length < 2) {
            cleanedConcept = enrichedTx.merchant ?: "Transacción"
        }

        return this.copy(
            transaction = enrichedTx.copy(concept = cleanedConcept)
        )
    }
}
