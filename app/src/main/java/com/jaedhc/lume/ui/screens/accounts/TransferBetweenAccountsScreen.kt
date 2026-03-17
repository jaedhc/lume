package com.jaedhc.lume.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.data.db.AccountEntity
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.TransferViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBetweenAccountsScreen(
    navController: NavHostController,
    viewModel: TransferViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSourceSheet by remember { mutableStateOf(false) }
    var showDestSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transferir entre cuentas",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = ActiveGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account Selectors Row
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccountSelectorCard(
                        label = "ORIGEN",
                        account = uiState.sourceAccount,
                        modifier = Modifier.weight(1f),
                        onClick = { showSourceSheet = true }
                    )
                    AccountSelectorCard(
                        label = "DESTINO",
                        account = uiState.destinationAccount,
                        modifier = Modifier.weight(1f),
                        onClick = { showDestSheet = true }
                    )
                }
                
                // Swap Icon Overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceDark, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .clickable { /* Logic to swap source and dest? optional */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = ActiveGold, modifier = Modifier.size(20.dp))
                }
            }

            // Amount Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MONTO A TRANSFERIR", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("$", color = ActiveGold, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.onAmountChange(it) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(ActiveGold),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            }

            // Quick Select Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("100", "500", "1,000", "Max").forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { 
                                if (label == "Max") {
                                    viewModel.onAmountChange(uiState.sourceAccount?.balance?.toString() ?: "0.00")
                                } else {
                                    viewModel.onAmountChange(label.replace(",", ""))
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (label == "Max") "Max" else "$$label",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Date Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .clickable { /* Show date picker */ }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("FECHA", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Hoy, ${uiState.dateIso}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextGray)
                }
            }

            // Note Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("NOTA (OPCIONAL)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        BasicTextField(
                            value = uiState.note,
                            onValueChange = { viewModel.onNoteChange(it) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                if (uiState.note.isEmpty()) {
                                    Text("¿Para qué es esta transferencia?", color = TextGray.copy(alpha = 0.4f), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // Info Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ActiveGold.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .border(1.dp, ActiveGold.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ActiveGold, modifier = Modifier.size(24.dp))
                    Text(
                        "Esta transferencia se reflejerá de forma inmediata en ambos balances. No aplica ninguna comisión por movimiento interno.",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            uiState.error?.let {
                Text(it, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Confirm Button
            Button(
                onClick = { viewModel.confirmTransfer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Transferencia", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Account Selection Sheets
        if (showSourceSheet) {
            AccountPickerSheet(
                accounts = uiState.accounts.filter { it.id != uiState.destinationAccount?.id },
                onDismiss = { showSourceSheet = false },
                onAccountSelected = { viewModel.onSourceAccountSelected(it) }
            )
        }
        if (showDestSheet) {
            AccountPickerSheet(
                accounts = uiState.accounts.filter { it.id != uiState.sourceAccount?.id },
                onDismiss = { showDestSheet = false },
                onAccountSelected = { viewModel.onDestinationAccountSelected(it) }
            )
        }
    }
}

@Composable
fun AccountSelectorCard(
    label: String,
    account: AccountEntity?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            if (account != null) {
                val accountColor = try {
                    Color(android.graphics.Color.parseColor(account.color ?: "#6366F1"))
                } catch (e: Exception) {
                    Color(0xFF6366F1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accountColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = accountColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(account.bankName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            NumberFormat.getCurrencyInstance(Locale.US).format(account.balance),
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Seleccionar", color = TextGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAccountSelected: (AccountEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Selecciona una cuenta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            accounts.forEach { account ->
                val accountColor = try {
                    Color(android.graphics.Color.parseColor(account.color ?: "#6366F1"))
                } catch (e: Exception) {
                    Color(0xFF6366F1)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .clickable {
                            onAccountSelected(account)
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accountColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = accountColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.bankName, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("•••• ${account.last4}", color = TextGray, fontSize = 12.sp)
                    }
                    Text(
                        NumberFormat.getCurrencyInstance(Locale.US).format(account.balance),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// BasicTextField with yellow style for amount
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    cursorBrush: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.SolidColor(Color.White),
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField -> innerTextField() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        modifier = modifier,
        cursorBrush = cursorBrush,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        decorationBox = decorationBox
    )
}
