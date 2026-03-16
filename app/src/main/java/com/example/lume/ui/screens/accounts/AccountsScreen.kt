package com.example.lume.ui.screens.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.lume.data.db.AccountWithType
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.viewmodel.AccountGroup
import com.example.lume.viewmodel.AccountsViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun AccountsScreen(
    navController: NavHostController,
    viewModel: AccountsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 80.dp) // Avoid overlap with bottom nav
            ) {
                // Secondary FAB for Transfers
                androidx.compose.material3.Surface(
                    onClick = { navController.navigate(com.example.lume.ui.navigation.Screen.TransferBetweenAccounts.route) },
                    shape = CircleShape,
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ActiveGold.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", tint = ActiveGold)
                    }
                }

                // Main FAB for Adding Account
                FloatingActionButton(
                    onClick = { navController.navigate(com.example.lume.ui.navigation.Screen.SelectAccountType.route) },
                    containerColor = ActiveGold,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account", tint = Color.Black)
                }
            }
        },
        topBar = {
            AccountsHeader()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                uiState.groups.forEach { group ->
                    item {
                        AccountGroupHeader(group)
                    }
                    items(group.accounts) { accountWithType ->
                        AccountCard(accountWithType, viewModel, navController)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsHeader() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("Cuentas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        windowInsets = WindowInsets(0)
    )
}

@Composable
fun AccountGroupHeader(group: AccountGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(group.typeName, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            formatCurrency(group.totalBalance),
            color = if (group.typeId == "CREDIT") Color(0xFFEF4444) else ActiveGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(accountWithType: AccountWithType, viewModel: AccountsViewModel, navController: NavHostController) {
    val account = accountWithType.account
    val type = accountWithType.type
    val accountColor = remember(account.color) {
        try {
            Color(android.graphics.Color.parseColor(account.color ?: "#6366F1"))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Eliminar cuenta", color = Color.White) },
            text = {
                Text(
                    "¿Eliminar ${account.bankName} •••• ${account.last4} y todas sus transacciones? Esta acción no se puede deshacer.",
                    color = TextGray
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccountWithTransactions(account.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = TextGray)
                }
            },
            containerColor = SurfaceDark
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (type?.id == "CREDIT") {
                        navController.navigate("tdc_detail/${account.id}")
                    }
                },
                onLongClick = { showDeleteDialog = true }
            )
            .then(if (showDeleteDialog) Modifier.border(1.dp, Color(0xFFEF4444), RoundedCornerShape(24.dp)) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accountColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getAccountIcon(type?.icon ?: "AccountBalance"),
                        contentDescription = null,
                        tint = accountColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.bankName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("•••• ${account.last4}", color = TextGray, fontSize = 12.sp)
                }
                Text(
                    text = if (type?.id == "CREDIT") "-${formatCurrency(account.balance)}" else formatCurrency(account.balance),
                    color = if (type?.id == "CREDIT") Color(0xFFEF4444) else accountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            // Specialized Bottom Sections
            when (type?.id) {
                "CREDIT" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Próximo pago", color = TextGray, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFACC15).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                calculateNextPaymentDate(account.dueDay),
                                color = Color(0xFFFACC15),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "SAVINGS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("META: ${formatCurrency(account.targetAmount ?: 0.0)}", color = TextGray, fontSize = 10.sp)
                            val progress = if (account.targetAmount != null && account.targetAmount!! > 0) 
                                (account.balance / account.targetAmount!! * 100).toInt() else 0
                            Text("$progress%", color = TextGray, fontSize = 10.sp)
                        }
                        LinearProgressIndicator(
                            progress = if (account.targetAmount != null && account.targetAmount!! > 0) 
                                (account.balance / account.targetAmount!!).toFloat() else 0f,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = ActiveGold,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
                "INVESTMENT" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Balance Total", color = TextGray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                            Text("+1.2% hoy", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}

private fun getAccountIcon(iconName: String): ImageVector {
    return when (iconName) {
        "AccountBalance" -> Icons.Default.AccountBalance
        "CreditCard" -> Icons.Default.CreditCard
        "Savings" -> Icons.Default.Savings
        "ShowChart" -> Icons.Default.ShowChart
        "Payments" -> Icons.Default.Payments
        else -> Icons.Default.AccountBalanceWallet
    }
}

private fun calculateNextPaymentDate(dueDay: Int?): String {
    if (dueDay == null) return "N/A"

    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // If today is past the due day, the next payment is next month
    if (currentDay > dueDay) {
        calendar.add(Calendar.MONTH, 1)
    }

    // Set the day. Handle months with fewer days than dueDay
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, minOf(dueDay, maxDays))

    val sdf = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(calendar.time).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
