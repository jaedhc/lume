package com.example.lume.ui.screens.receiver

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lume.data.OcrResult
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.viewmodel.ShareReceiverUiState
import com.example.lume.viewmodel.ShareReceiverViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import com.example.lume.data.db.CategoryEntity
import com.example.lume.data.db.AccountWithType
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lume.data.db.AccountEntity
import com.example.lume.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiverScreen(
    uris: List<Uri>,
    onDone: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: ShareReceiverViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState()
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            // 'it' is the number of saved transactions (Int)
            onDone()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            ReviewTopBar(onClose = onDone)
        }
    ) { innerPadding ->
        if (uris.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No images shared", color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(
                    items = uris,
                    key = { it.toString() } // Stable key for Uris
                ) { uri ->
                    TransactionReviewCard(
                        uri = uri, 
                        viewModel = viewModel,
                        onOpenCategorySelector = { showCategorySheet = true },
                        onOpenAccountSelector = { showAccountSheet = true }
                    )
                }
            }
        }
    }

    if (showCategorySheet) {
        val categories by viewModel.categories.collectAsState(initial = emptyList())
        val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

        CategorySelectorBottomSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onClose = { showCategorySheet = false },
            onCategorySelected = {
                viewModel.onCategorySelected(it)
                showCategorySheet = false
            },
            onAddNew = {
                showCategorySheet = false
                onNavigate(Screen.CreateCategory.route)
            }
        )
    }

    if (showAccountSheet) {
        val accounts by viewModel.accounts.collectAsState(initial = emptyList())
        val selectedAccountId by viewModel.selectedAccountId.collectAsState()

        AccountSelectorBottomSheet(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onClose = { showAccountSheet = false },
            onAccountSelected = {
                viewModel.onAccountSelected(it)
                showAccountSheet = false
            },
            onAddNew = {
                showAccountSheet = false
                onNavigate(Screen.SelectAccountType.route)
            }
        )
    }
}

@Composable
fun TransactionReviewCard(
    uri: Uri,
    viewModel: ShareReceiverViewModel,
    onOpenCategorySelector: () -> Unit,
    onOpenAccountSelector: () -> Unit
) {
    LaunchedEffect(uri) {
        viewModel.processImage(uri)
    }

    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (uiState) {
            is ShareReceiverUiState.Loading -> {
                CircularProgressIndicator(color = ActiveGold)
                Text("Analizando recibo...", color = TextGray)
            }
            is ShareReceiverUiState.Error -> {
                Text("Error al analizar", color = Color.Red)
            }
            is ShareReceiverUiState.Success -> {
                val result = (uiState as ShareReceiverUiState.Success).result
                TransactionForm(result, viewModel, onOpenCategorySelector, onOpenAccountSelector)
            }
            else -> {}
        }
    }
}

@Composable
fun TransactionTypeSwitch(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        val isEgreso = selectedType == "egreso"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isEgreso) ActiveGold else Color.Transparent)
                .clickable { onTypeSelected("egreso") },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = if (isEgreso) Color.Black else Color(0xFFEF4444))
                Text("Gasto", color = if (isEgreso) Color.Black else Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            }
        }
        val isIngreso = selectedType == "ingreso"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isIngreso) ActiveGold else Color.Transparent)
                .clickable { onTypeSelected("ingreso") },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = if (isIngreso) Color.Black else Color(0xFF2D9F24))
                Text("Ingreso", color = if (isIngreso) Color.Black else Color(0xFF2D9F24), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ReviewTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 32.dp, 16.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
        Text("Revisar Transacción", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = {}) { Icon(Icons.Outlined.HelpOutline, contentDescription = "Help", tint = TextGray) }
    }
}

@Composable
fun TransactionForm(
    result: OcrResult, 
    viewModel: ShareReceiverViewModel,
    onOpenCategorySelector: () -> Unit,
    onOpenAccountSelector: () -> Unit
) {
    var transactionType by remember { mutableStateOf(result.fields.type ?: "egreso") }
    var isSubscription by remember { mutableStateOf(result.fields.is_subscription) }
    var concept by remember { mutableStateOf(result.fields.concept ?: result.fields.merchant ?: "") }
    var note by remember { mutableStateOf("") }

    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val selectedId by viewModel.selectedCategoryId.collectAsState()
    val selectedCategory = remember(categories, selectedId) { categories.find { it.id == selectedId } }

    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedAccountWithType = remember(accounts, selectedAccountId) { accounts.find { it.account.id == selectedAccountId } }

    // Form State
    var amountStr by remember { mutableStateOf(result.fields.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var msiTotalStr by remember { mutableStateOf(result.fields.msi_total?.toString() ?: result.fields.msi?.toString() ?: "") }
    var msiCurrentStr by remember { mutableStateOf(result.fields.msi_current?.toString() ?: "1") }
    
    val formattedAmount = remember(amountStr) { 
        val currentAmount = amountStr.toDoubleOrNull() ?: 0.0
        NumberFormat.getCurrencyInstance(Locale.US).format(currentAmount) 
    }

    // Badges
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // OCR Detected Badge
        Box(
            modifier = Modifier
                .background(Color(0xFF2A2A1E), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ActiveGold, modifier = Modifier.size(12.dp))
                Text("OCR DETECTADO", color = ActiveGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (result.usedAi) {
            // AI Powered Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E242E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFB894FF), modifier = Modifier.size(12.dp))
                    Text("LUME AI CLASIFICADO", color = Color(0xFFB894FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Amount
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = formattedAmount, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text(text = "Extraído de tu captura de pantalla", color = Color(0xFF53535E), fontSize = 14.sp)
    }

    // Insight Chip (MI)
    val msiTotal = msiTotalStr.toIntOrNull() ?: 0
    val msiCurrent = msiCurrentStr.toIntOrNull() ?: 1
    if (msiTotal > 1) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E242E), RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFB894FF))
                Text("Pago a $msiTotal MSI ($msiCurrent/$msiTotal)", color = Color(0xFFB894FF), fontWeight = FontWeight.Medium)
            }
        }
    }

    TransactionTypeSwitch(selectedType = transactionType, onTypeSelected = { transactionType = it })

    Text(
        text = "DETALLES DE TRANSACCIÓN",
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
    )

    if (msiTotal > 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = msiCurrentStr,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,2}\$"))) msiCurrentStr = it },
                modifier = Modifier.weight(1f),
                label = { Text("Pago No.", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ActiveGold,
                    focusedLabelColor = ActiveGold,
                    unfocusedLabelColor = TextGray,
                    disabledLabelColor = TextGray
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = msiTotalStr,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,2}\$"))) msiTotalStr = it },
                modifier = Modifier.weight(1f),
                label = { Text("de Total Meses", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ActiveGold,
                    focusedLabelColor = ActiveGold,
                    unfocusedLabelColor = TextGray,
                    disabledLabelColor = TextGray
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true
            )
        }
    }

    val accountColor = selectedAccountWithType?.account?.color?.let { 
        try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { ActiveGold } 
    } ?: ActiveGold
    
    val accountIcon = when (selectedAccountWithType?.type?.id) {
        "CREDIT" -> Icons.Default.CreditCard
        "SAVINGS" -> Icons.Default.Savings
        "INVESTMENT" -> Icons.Default.ShowChart
        "CASH" -> Icons.Default.Payments
        else -> Icons.Default.AccountBalance
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
    ) {
        DetailRow(
            icon = accountIcon,
            iconColor = accountColor,
            label = "CUENTA",
            value = selectedAccountWithType?.let { "${it.account.bankName} •••• ${it.account.last4}" } ?: "Seleccionar cuenta",
            showDivider = true,
            onClick = onOpenAccountSelector
        )
        DetailRow(
            icon = getCategoryIcon(selectedCategory?.icon ?: "Category"),
            iconColor = selectedCategory?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { ActiveGold } } ?: ActiveGold,
            label = "CATEGORÍA",
            value = selectedCategory?.name ?: "Otros",
            showDivider = true,
            onClick = onOpenCategorySelector
        )
        DetailRow(
            icon = Icons.Outlined.CalendarMonth,
            label = "FECHA",
            value = result.fields.date ?: "Hoy, ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
            showDivider = false
        )
    }

    // Subscription Toggle
    // Only show subscription toggle if NOT MSI
    if (msiTotal <= 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2C2C35), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFA78BFA))
                }
                Column {
                    Text("Suscripción", color = Color.White, fontWeight = FontWeight.Medium)
                    Text("Repetir cada mes", color = TextGray, fontSize = 12.sp)
                }
            }
            Switch(
                checked = isSubscription,
                onCheckedChange = { isSubscription = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = ActiveGold,
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = BackgroundDark
                )
            )
        }
    }

    OutlinedTextField(
        value = concept,
        onValueChange = { concept = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Concepto", color = TextGray) },
        placeholder = { Text("Nombre del comercio o concepto...", color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextGray) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            cursorColor = ActiveGold,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        label = { Text("Nota (opcional)", color = TextGray) },
        placeholder = { Text("Agregar una nota adicional...", color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = TextGray) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            cursorColor = ActiveGold,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    )

    Button(
        onClick = { 
            val editedAmount = amountStr.toDoubleOrNull() ?: 0.0
            val editedMsiTotal = msiTotalStr.toIntOrNull()
            val editedMsiCurrent = msiCurrentStr.toIntOrNull()
            viewModel.saveTransaction(result, transactionType, isSubscription, note, editedAmount, concept, editedMsiTotal, editedMsiCurrent) 
        },
        colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Confirmar y Guardar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    
    Spacer(Modifier.height(16.dp))
    Text("LUME AI • INTELIGENCIA FINANCIERA", color = Color(0xFF333333), fontSize = 10.sp, letterSpacing = 1.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorBottomSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onClose: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddNew: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories = remember(categories, searchQuery) { 
        categories.filter { it.name.contains(searchQuery, ignoreCase = true) } 
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF16161D),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Categoría", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.background(SurfaceDark, CircleShape).size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark),
                placeholder = { Text("Buscar categoría...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = ActiveGold,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(
                    items = filteredCategories,
                    key = { it.id }
                ) { category ->
                    val isSelected = category.id == selectedCategoryId
                    CategoryGridItem(
                        category = category,
                        isSelected = isSelected,
                        onClick = { onCategorySelected(category.id) }
                    )
                }
                item {
                    CategoryGridItem(
                        name = "Nuevo",
                        icon = Icons.Default.Add,
                        iconColor = TextGray,
                        isDashed = true,
                        onClick = onAddNew
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryEntity? = null,
    name: String = "",
    icon: ImageVector? = null,
    iconColor: Color = ActiveGold,
    isSelected: Boolean = false,
    isDashed: Boolean = false,
    onClick: () -> Unit
) {
    val finalName = category?.name ?: name
    val finalIcon = category?.let { getCategoryIcon(it.icon ?: "Category") } ?: icon ?: Icons.Default.Category
    val finalIconColor = category?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { ActiveGold } } ?: iconColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .then(
                    if (isSelected) Modifier.border(2.dp, ActiveGold, RoundedCornerShape(16.dp)) 
                    else if (isDashed) Modifier.border(1.dp, TextGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) // Dash not easy in modifiers, border is fine
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(finalIconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(finalIcon, contentDescription = null, tint = finalIconColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = finalName,
            color = if (isSelected) ActiveGold else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    iconColor: Color = TextGray,
    label: String,
    value: String,
    showDivider: Boolean,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2C2C35), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Color.White, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                thickness = 1.dp,
                color = Color(0xFF2C2C35)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorBottomSheet(
    accounts: List<com.example.lume.data.db.AccountWithType>,
    selectedAccountId: String?,
    onClose: () -> Unit,
    onAccountSelected: (String) -> Unit,
    onAddNew: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF16161D),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        // Ensure system navigation bar stays dark
        val view = LocalView.current
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            val window = (context as? Activity)?.window
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.BLACK
                // Disable light navigation bar icons
                WindowInsetsControllerCompat(window, view).isAppearanceLightNavigationBars = false
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Seleccionar Cuenta", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.background(SurfaceDark, CircleShape).size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(accounts) { accountWithType ->
                    AccountSelectorItem(
                        accountWithType = accountWithType,
                        isSelected = accountWithType.account.id == selectedAccountId,
                        onClick = { onAccountSelected(accountWithType.account.id) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp, 
                                color = ActiveGold.copy(alpha = 0.3f), // Approximate dashed looks with low alpha or custom painter
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onAddNew() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ActiveGold, modifier = Modifier.size(20.dp))
                            Text("Vincular nueva cuenta", color = ActiveGold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AccountSelectorItem(
    accountWithType: com.example.lume.data.db.AccountWithType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val account = accountWithType.account
    val type = accountWithType.type
    val accountColor = try { Color(android.graphics.Color.parseColor(account.color ?: "#B894FF")) } catch(e: Exception) { ActiveGold }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SurfaceDark else Color.Transparent)
            .border(1.dp, if (isSelected) ActiveGold else Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accountColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (type?.id) {
                    "CREDIT" -> Icons.Default.CreditCard
                    "SAVINGS" -> Icons.Default.Savings
                    "INVESTMENT" -> Icons.Default.ShowChart
                    "CASH" -> Icons.Default.Payments
                    else -> Icons.Default.AccountBalance
                },
                contentDescription = null,
                tint = accountColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(account.bankName, color = Color.White, fontWeight = FontWeight.SemiBold)
            val balanceStr = if (type?.id == "CREDIT") {
                val available = (account.creditLimit ?: 0.0) - account.balance
                "${formatCurrency(available)} disp."
            } else {
                formatCurrency(account.balance)
            }
            Text("${type?.name ?: "Cuenta"} •••• ${account.last4} • $balanceStr", color = TextGray, fontSize = 12.sp)
        }
        
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActiveGold)
        }
    }
}

private fun getCategoryIcon(iconName: String): ImageVector {
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
        else -> Icons.Default.Category
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
