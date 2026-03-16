package com.example.lume.ui.screens.tdc

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.lume.data.db.AccountEntity
import com.example.lume.data.db.TransactionWithCategory
import com.example.lume.ui.components.TransactionItem
import com.example.lume.data.mappers.TransactionMappers.isMsi
import com.example.lume.data.mappers.TransactionMappers.cleanConcept
import com.example.lume.utils.FormatUtils.formatCurrency
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.viewmodel.TdcDetailViewModel
import java.text.NumberFormat
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
                title = { Text(uiState.account?.bankName ?: "Detalle TDC", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ActiveGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    DebtOverviewCard(
                        totalDebt = uiState.totalDebt,
                        currentCycleDebt = uiState.currentCycleDebt,
                        onPayClick = { showPaymentDialog = true }
                    )
                }

                item {
                    Text(
                        "MOVIMIENTOS DEL CORTE",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.currentCycleTransactions.isEmpty()) {
                    item {
                        Text("No hay transacciones pendientes este mes.", color = TextGray, fontSize = 14.sp)
                    }
                } else {
                    items(uiState.currentCycleTransactions) { tx ->
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
    totalDebt: Double,
    currentCycleDebt: Double,
    onPayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text("Saldo Total", color = TextGray, fontSize = 14.sp)
                Text(
                    formatCurrency(totalDebt),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Deuda del Mes", color = TextGray, fontSize = 13.sp)
                    Text(
                        formatCurrency(currentCycleDebt),
                        color = Color(0xFFF87171),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onPayClick,
                    enabled = currentCycleDebt > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActiveGold,
                        contentColor = Color.Black,
                        disabledContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pagar Corte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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

