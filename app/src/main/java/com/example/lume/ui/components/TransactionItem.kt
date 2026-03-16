package com.example.lume.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lume.data.db.TransactionWithCategory
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.utils.DateUtils
import com.example.lume.utils.FormatUtils.formatCurrency
import com.example.lume.utils.CategoryIconUtils.getCategoryIcon
import com.example.lume.ui.theme.MsiPurple
import com.example.lume.ui.theme.DeferredYellow
import com.example.lume.ui.theme.PaidGreen

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    txWithCat: TransactionWithCategory,
    displayTitle: String,
    isMsi: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
    isVisible: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val transaction = txWithCat.transaction
    val category = txWithCat.category
    
    val categoryColor = remember(category?.color) { 
        if (category?.id == "finanzas") {
            ActiveGold
        } else {
            category?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { TextGray } } ?: TextGray
        }
    }
    val categoryIconName = category?.icon ?: "Category"
    val categoryIcon = remember(categoryIconName) { getCategoryIcon(categoryIconName) }
    

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        CategoryIcon(icon = categoryIcon, color = categoryColor)
        
        Spacer(modifier = Modifier.width(8.dp)) // Reduced spacer
        
        // Main Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    displayTitle,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false) // Ensure text doesn't push tags out
                )
                
                // MSI Indicator Pills
                MsiPills(
                    isMsi = isMsi,
                    installment = transaction.msiInstallment,
                    total = transaction.msiTotal
                )
            }
            
            // Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dateLabel = if (transaction.id.startsWith("virtual_")) "Pago mes" else DateUtils.toSafeFormattedDate(transaction.dateIso)
                Text(
                    "$dateLabel • ${category?.name ?: "Otros"}",
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (transaction.usedAi) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MsiPurple,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Amount and Status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.type == "egreso") "-" else "+") + (if (isVisible) formatCurrency(transaction.amount) else "***"),
                color = if (transaction.type == "egreso") Color.White else Color(0xFF2D9F24),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            // Status tag
            StatusBadge(
                isPaid = transaction.isPaid,
                type = transaction.type,
                accountId = transaction.accountId,
                msiInstallment = transaction.msiInstallment,
                isFuturePayment = transaction.isFuturePayment
            )
        }
    }
}

@Composable
private fun CategoryIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MsiPills(
    isMsi: Boolean,
    installment: Int?,
    total: Int?,
    modifier: Modifier = Modifier
) {
    if (isMsi) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // MSI Label
            Box(
                modifier = Modifier
                    .background(MsiPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "MSI",
                    color = MsiPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Installment Counter (if available)
            if (installment != null && total != null) {
                Box(
                    modifier = Modifier
                        .background(MsiPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$installment/$total",
                        color = MsiPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    isPaid: Boolean,
    type: String,
    accountId: String?,
    msiInstallment: Int? = null,
    isFuturePayment: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor, statusBg) = remember(isPaid, type, accountId, msiInstallment, isFuturePayment) {
        when {
            // Cuota MSI futura — aún no se ha cobrado
            msiInstallment != null && isFuturePayment ->
                Triple("PENDIENTE", TextGray, SurfaceDark)

            // Cuota MSI actual — ya se cargó este mes
            msiInstallment != null && !isFuturePayment ->
                Triple("PAGADO", PaidGreen, PaidGreen.copy(alpha = 0.1f))

            // TDC normal sin pagar
            !isPaid && type == "egreso" && accountId != null ->
                Triple("PENDIENTE", TextGray, SurfaceDark)

            // Cualquier cosa marcada como pagada
            isPaid ->
                Triple("PAGADO", PaidGreen, PaidGreen.copy(alpha = 0.1f))

            // Ingreso u otros
            else ->
                Triple("COMPLETADO", TextGray, SurfaceDark)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(statusBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            statusText,
            color = statusColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
