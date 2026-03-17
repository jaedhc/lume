package com.jaedhc.lume.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconUtils {
    fun getCategoryIcon(iconName: String): ImageVector {
        val normalizedName = iconName.substringAfterLast('.')
        return when (normalizedName) {
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "ConfirmationNumber" -> Icons.Default.ConfirmationNumber
            "MedicalServices" -> Icons.Default.MedicalServices
            "Payments" -> Icons.Default.Payments
            "Lightbulb" -> Icons.Default.Lightbulb
            "Receipt" -> Icons.Default.Receipt
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Home" -> Icons.Default.Home
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "Add" -> Icons.Default.Add
            "SwapHoriz" -> Icons.Default.SwapHoriz
            else -> Icons.Default.Category
        }
    }
}
