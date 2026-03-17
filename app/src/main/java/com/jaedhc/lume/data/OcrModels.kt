package com.jaedhc.lume.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.jaedhc.lume.data.db.CategoryEntity

@Serializable
data class TxFields(
    @SerialName("amount") val amount: Double? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("merchant") val merchant: String? = null,
    @SerialName("concept") val concept: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("suggested_category") val suggested_category: String? = null,
    @SerialName("type") val type: String? = null, // "ingreso" or "egreso"
    @SerialName("is_subscription") val is_subscription: Boolean = false,
    @SerialName("msi") val msi: Int? = null,
    @SerialName("msi_total") val msi_total: Int? = null,
    @SerialName("msi_current") val msi_current: Int? = null,
    @SerialName("usedAi") var usedAi: Boolean = false
)

data class OcrResult(
    val text: String,
    val fields: TxFields,
    val selectedCategoryId: String,
    val suggestedCategory: CategoryEntity? = null,
    val suggestedAccountId: String? = null,
    val usedAi: Boolean = false
)

@Serializable
data class StructuralOcrResult(
    val amount_candidates: List<Double>,
    val date_candidates: List<String>,
    val text_lines: List<String>,
    val categories: List<String>
)
