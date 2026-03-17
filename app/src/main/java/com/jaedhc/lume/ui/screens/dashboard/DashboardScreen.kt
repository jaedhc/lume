package com.jaedhc.lume.ui.screens.dashboard
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.ui.components.TransactionItem
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.CategorySpend
import com.jaedhc.lume.viewmodel.DashboardViewModel
import com.jaedhc.lume.viewmodel.TdcReminder
import com.jaedhc.lume.utils.FormatUtils.formatCurrency
import com.jaedhc.lume.utils.CategoryIconUtils.getCategoryIcon
import com.jaedhc.lume.data.mappers.TransactionMappers.isMsi
import com.jaedhc.lume.data.mappers.TransactionMappers.cleanConcept
import com.jaedhc.lume.ui.theme.MsiPurple
import com.jaedhc.lume.viewmodel.TdcReminderType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardHeader(navController)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            if (uiState.tdcReminders.isNotEmpty()) {
                item {
                    TdcRemindersSection(uiState.tdcReminders)
                }
            }

            item {
                TotalBalanceSection(
                    balance = uiState.totalBalance,
                    isVisible = uiState.isSensitiveDataVisible,
                    onToggleVisibility = { viewModel.toggleSensitiveDataVisibility() }
                )
            }
            item {
                IncomeExpenseSummary(
                    income = uiState.totalIncome,
                    expenses = uiState.totalExpenses,
                    debt = uiState.totalDebt,
                    isVisible = uiState.isSensitiveDataVisible
                )
            }
            item {
                CategorySpendingSection(
                    breakdown = uiState.categoriesBreakdown,
                    totalExpenses = uiState.totalExpenses,
                    isVisible = uiState.isSensitiveDataVisible
                )
            }
            
            if (uiState.deferredPayments.isNotEmpty()) {
                item {
                    DeferredPaymentsHeader()
                }
                items(
                    items = uiState.deferredPayments,
                    key = { "deferred_${it.id}" } // Stability
                ) { deferredPayment ->
                    DeferredPaymentCard(deferredPayment, isVisible = uiState.isSensitiveDataVisible)
                }
            }
            
            item{
                RecentTransactionsHeader()
            }

            items(
                items = uiState.recentTransactions,
                key = { it.transaction.id } // STABLE KEY for performance
            ) { txWithCat ->
                TransactionItem(
                    txWithCat = txWithCat,
                    displayTitle = txWithCat.cleanConcept(),
                    isMsi = txWithCat.isMsi(),
                    isVisible = uiState.isSensitiveDataVisible
                )
            }
            
            item { Spacer(modifier = Modifier.height(140.dp)) } // More robust than contentPadding
        }
    }
}

@Composable
fun DashboardHeader(navController: androidx.navigation.NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { navController.navigate(com.jaedhc.lume.ui.navigation.Screen.Perfil.route) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.White)
            }
            Column {
                Text("BUENOS DÍAS,", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Usuario", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        IconButton(
            onClick = { },
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceDark)
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
        }
    }
}

@Composable
fun TotalBalanceSection(
    balance: Double,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SALDO TOTAL", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle Visibility",
                tint = TextGray,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onToggleVisibility() }
            )
        }
        Text(
            text = if (isVisible) formatCurrency(balance) else "***",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .background(Color(0xFF1E241E), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ActiveGold, modifier = Modifier.size(12.dp))
                Text("+2.4% ESTE MES", color = ActiveGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IncomeExpenseSummary(income: Double, expenses: Double, debt: Double, isVisible: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "INGRESOS",
                amount = income,
                icon = Icons.Default.ArrowDownward, 
                iconColor = Color(0xFF2D9F24),
                modifier = Modifier.weight(1f),
                isVisible = isVisible
            )
            SummaryCard(
                label = "EGRESOS",
                amount = expenses,
                icon = Icons.Default.ArrowUpward,
                iconColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f),
                isVisible = isVisible
            )
        }
        SummaryCard(
            label = "DEUDA PROYECTADA",
            amount = debt,
            icon = Icons.Default.CreditCard,
            iconColor = Color(0xFFFACC15),
            modifier = Modifier.fillMaxWidth(),
            isVisible = isVisible
        )
    }
}

@Composable
fun SummaryCard(
    label: String,
    amount: Double,
    icon: ImageVector,
    iconColor: Color,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            }
            Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = if (isVisible) formatCurrency(amount) else "***",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategorySpendingSection(breakdown: List<CategorySpend>, totalExpenses: Double, isVisible: Boolean) {
    val currentMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase() }
    val chartTotal = remember(breakdown) { breakdown.sumOf { it.amount } }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Gastos por Categoría", color = Color.White, fontWeight = FontWeight.Bold)
            Text(currentMonth, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutChart(breakdown, chartTotal)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isVisible) formatCurrency(chartTotal) else "***",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("TOTAL GASTADO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val chunked = remember(breakdown) { breakdown.chunked(2) }
            chunked.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { spend ->
                        val percentage = remember(spend.amount, chartTotal) { 
                            if (chartTotal > 0) (spend.amount / chartTotal * 100).toInt() else 0 
                        }
                        val categoryColor = remember(spend.category.color) { 
                            spend.category.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { ActiveGold } } ?: ActiveGold
                        }
                        CategoryLegendItem(
                            label = spend.category.name,
                            percentage = percentage,
                            color = categoryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(breakdown: List<CategorySpend>, total: Double) {
    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        if (total == 0.0) {
            drawArc(
                color = Color.DarkGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            breakdown.forEach { spend ->
                val sweepAngle = (spend.amount / total * 360f).toFloat()
                val categoryColor = spend.category.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { ActiveGold } } ?: ActiveGold
                drawArc(
                    color = categoryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun CategoryLegendItem(label: String, percentage: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("${label.capitalize()} ($percentage%)", color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun RecentTransactionsHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Transacciones recientes", color = Color.White, fontWeight = FontWeight.Bold)
        Text("VER TODO", color = ActiveGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeferredPaymentsHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Pagos diferidos de este mes", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeferredPaymentCard(item: com.jaedhc.lume.viewmodel.DeferredPaymentItem, isVisible: Boolean) {
    val categoryColor = remember(item.categoryColor) { 
        try { Color(android.graphics.Color.parseColor(item.categoryColor)) } catch(e: Exception) { TextGray } 
    }
    
    val categoryIcon = remember(item.categoryIcon) { 
        getCategoryIcon(item.categoryIcon) 
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(categoryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIcon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.concept,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
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
                
                // Installment Counter
                Box(
                    modifier = Modifier
                        .background(MsiPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${item.currentMonth}/${item.totalMonths}",
                        color = MsiPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("Monto mensual", color = TextGray, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isVisible) formatCurrency(item.monthlyPayment) else "***",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(item.paymentDueDate, color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun TdcRemindersSection(reminders: List<TdcReminder>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { reminder ->
            val isUrgent = reminder.daysLeft == 0
            val color = when(reminder.type) {
                TdcReminderType.CORTE -> if (isUrgent) Color(0xFFEF4444) else Color(0xFFFACC15)
                TdcReminderType.PAGO -> if (isUrgent) Color(0xFFEF4444) else Color(0xFF3B82F6) // Blue for payment
            }
            
            val title = when(reminder.type) {
                TdcReminderType.CORTE -> if (isUrgent) "¡CORTE HOY!" else "Corte en ${reminder.daysLeft} días"
                TdcReminderType.PAGO -> if (isUrgent) "¡PAGO HOY!" else "Pago en ${reminder.daysLeft} días"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reminder.type == TdcReminderType.CORTE) Icons.Default.PriorityHigh else Icons.Default.CreditCard, 
                            contentDescription = null, 
                            tint = color
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${reminder.bankName} •••• ${reminder.last4}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    if (reminder.amountToPay > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(reminder.amountToPay),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "POR PAGAR",
                                color = TextGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
