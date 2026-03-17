package com.jaedhc.lume.ui.screens.tdc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.data.db.AccountEntity
import com.jaedhc.lume.ui.components.TransactionItem
import com.jaedhc.lume.data.mappers.TransactionMappers.isMsi
import com.jaedhc.lume.data.mappers.TransactionMappers.cleanConcept
import com.jaedhc.lume.utils.FormatUtils.formatCurrency
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.TdcDetailViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TdcDetailScreen(
    navController: NavHostController,
    accountId: String
) {
    val factory = remember(accountId) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TdcDetailViewModel(navController.context.applicationContext as android.app.Application, accountId) as T
            }
        }
    }
    val viewModel: TdcDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Estado de Cuenta", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* share or export */ }) {
                        Icon(Icons.Default.IosShare, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
                item {
                    Crossfade(
                        targetState = uiState.isLoading,
                        animationSpec = tween(durationMillis = 400),
                        label = "topCards"
                    ) { loading ->
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            if (loading) {
                                CardSkeleton(height = 140.dp)
                                CardSkeleton(height = 64.dp)
                            } else {
                                DebtOverviewCard(
                                    bankName = uiState.account?.bankName ?: "",
                                    last4 = uiState.account?.last4 ?: "",
                                    totalDebt = uiState.totalDebt,
                                    currentCycleDebt = uiState.currentCycleDebt,
                                    onPayClick = { showPaymentDialog = true }
                                )

                                StatementDatesSection(
                                    closingDay = uiState.account?.closingDay ?: 1,
                                    dueDay = uiState.account?.dueDay ?: 1,
                                    offset = uiState.selectedOffset
                                )
                            }
                        }
                    }
                }

                item {
                    CycleSelector(
                        cycles = uiState.availableCycles,
                        selectedOffset = uiState.selectedOffset,
                        onCycleSelected = { viewModel.onCycleSelected(it) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TRANSACCIONES",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ver todas",
                            color = ActiveGold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { /* logic to see all */ }
                        )
                    }
                }

                item {
                    Crossfade(
                        targetState = uiState.isLoading,
                        animationSpec = tween(durationMillis = 400),
                        label = "txList"
                    ) { loading ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (loading) {
                                repeat(5) {
                                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                        TransactionSkeleton()
                                    }
                                }
                            } else if (uiState.currentCycleTransactions.isEmpty()) {
                                Text(
                                    "No hay transacciones en este periodo.", 
                                    color = TextGray, 
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                uiState.currentCycleTransactions.forEach { tx ->
                                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                        TransactionItem(
                                            txWithCat = tx,
                                            displayTitle = tx.cleanConcept(),
                                            isMsi = tx.isMsi(),
                                            isVisible = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showPaymentDialog) {
        TdcPaymentDialog(
            amount = uiState.currentCycleDebt,
            liquidAccounts = uiState.liquidAccounts,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { sourceId ->
                viewModel.payCurrentCycle(sourceId, uiState.currentCycleDebt)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
fun DebtOverviewCard(
    bankName: String,
    last4: String,
    totalDebt: Double,
    currentCycleDebt: Double,
    onPayClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("SALDO TOTAL", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                formatCurrency(totalDebt),
                color = ActiveGold,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                label = "DEUDA DEL MES",
                value = formatCurrency(currentCycleDebt),
                valueColor = Color(0xFFFACC15),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "BANCO",
                value = bankName.ifEmpty { "Tarjeta" },
                subValue = "•••• $last4",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subValue != null) {
                Text(subValue, color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun StatementDatesSection(
    closingDay: Int,
    dueDay: Int,
    offset: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateInfoItem(
                label = "FECHA DE CORTE",
                date = getStatementDateLabel(closingDay, offset)
            )
            
            Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))

            DateInfoItem(
                label = "FECHA DE PAGO",
                date = getStatementDateLabel(dueDay, offset, isDue = true),
                dateColor = ActiveGold
            )
        }
    }
}

@Composable
fun DateInfoItem(
    label: String,
    date: String,
    dateColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(date, color = dateColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CycleSelector(
    cycles: List<com.jaedhc.lume.viewmodel.TdcCycleInfo>,
    selectedOffset: Int,
    onCycleSelected: (Int) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Scroll to the current month (offset 0) or the selected one on first load
    LaunchedEffect(cycles) {
        val index = cycles.indexOfFirst { it.offset == selectedOffset }
        if (index != -1) {
            listState.scrollToItem(index)
        }
    }

    androidx.compose.foundation.lazy.LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(cycles) { cycle ->
            val isSelected = cycle.offset == selectedOffset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCycleSelected(cycle.offset) }
            ) {
                Text(
                    text = cycle.label,
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ActiveGold)
                    )
                }
            }
        }
    }
}

fun getStatementDateLabel(day: Int, offset: Int, isDue: Boolean = false): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    
    // Simple logic for the label
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", Locale("es", "MX"))
    cal.set(Calendar.DAY_OF_MONTH, day)
    
    // If due day is less than closing day, it's usually next month
    // But for the label we just want to show the date of that month cycle
    return sdf.format(cal.time).replaceFirstChar { it.uppercase() }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TdcPaymentDialog(
    amount: Double,
    liquidAccounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(liquidAccounts.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pago", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Se registrará un pago de ${formatCurrency(amount)} a tu tarjeta.",
                    color = TextGray
                )
                Text("Pagar desde:", color = Color.White, fontSize = 14.sp)
                
                liquidAccounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedAccountId == account.id) ActiveGold.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedAccountId = account.id }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAccountId == account.id,
                            onClick = { selectedAccountId = account.id },
                            colors = RadioButtonDefaults.colors(selectedColor = ActiveGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(account.bankName, color = Color.White, fontSize = 14.sp)
                            Text(formatCurrency(account.balance), color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selectedAccountId.isNotEmpty()) onConfirm(selectedAccountId) },
                enabled = selectedAccountId.isNotEmpty()
            ) {
                Text("CONFIRMAR PAGO", color = ActiveGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.White)
            }
        },
        containerColor = SurfaceDark
    )
}
@Composable
fun CardSkeleton(height: androidx.compose.ui.unit.Dp) {
    val shimmerColors = listOf(
        SurfaceDark,
        SurfaceDark.copy(alpha = 0.6f),
        SurfaceDark,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
    )
}

@Composable
fun TransactionSkeleton() {
    val shimmerColors = listOf(
        SurfaceDark,
        SurfaceDark.copy(alpha = 0.6f),
        SurfaceDark,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}
