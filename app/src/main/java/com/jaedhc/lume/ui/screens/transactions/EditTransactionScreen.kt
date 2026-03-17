package com.jaedhc.lume.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.EditTransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    navController: NavHostController,
    viewModel: EditTransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Editar Transacción", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading || uiState.transaction == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ActiveGold)
            }
        } else {
            val tx = uiState.transaction!!
            
            var amount by remember { mutableStateOf(tx.amount.toString()) }
            var merchant by remember { mutableStateOf(tx.merchant ?: "") }
            var concept by remember { mutableStateOf(tx.concept ?: "") }
            var dateIso by remember { mutableStateOf(tx.dateIso) }
            var type by remember { mutableStateOf(tx.type) } // "ingreso" or "egreso"
            var categoryId by remember { mutableStateOf(tx.categoryId) }
            var accountId by remember { mutableStateOf(tx.accountId) }
            var isPaid by remember { mutableStateOf(tx.isPaid) }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Content type selector (Ingreso/Egreso)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeSelectorOption(
                        title = "Gasto",
                        isSelected = type == "egreso",
                        onClick = { type = "egreso" },
                        modifier = Modifier.weight(1f)
                    )
                    TypeSelectorOption(
                        title = "Ingreso",
                        isSelected = type == "ingreso",
                        onClick = { type = "ingreso" },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ActiveGold,
                        unfocusedBorderColor = SurfaceDark,
                        cursorColor = ActiveGold
                    )
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Comercio / Entidad", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ActiveGold,
                        unfocusedBorderColor = SurfaceDark,
                        cursorColor = ActiveGold
                    )
                )

                OutlinedTextField(
                    value = concept,
                    onValueChange = { concept = it },
                    label = { Text("Concepto", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ActiveGold,
                        unfocusedBorderColor = SurfaceDark,
                        cursorColor = ActiveGold
                    )
                )

                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Fecha (YYYY-MM-DD)", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ActiveGold,
                        unfocusedBorderColor = SurfaceDark,
                        cursorColor = ActiveGold
                    )
                )

                // Category Dropdown
                var expandedCat by remember { mutableStateOf(false) }
                val selectedCat = uiState.categories.find { it.id == categoryId }
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = it }
                ) {
                    OutlinedTextField(
                        value = selectedCat?.name ?: "Seleccione categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría", color = TextGray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ActiveGold,
                            unfocusedBorderColor = SurfaceDark,
                            cursorColor = ActiveGold
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        uiState.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = Color.White) },
                                onClick = { 
                                    categoryId = cat.id
                                    expandedCat = false 
                                }
                            )
                        }
                    }
                }

                // Account Dropdown
                var expandedAcc by remember { mutableStateOf(false) }
                val selectedAcc = uiState.accounts.find { it.id == accountId }
                ExposedDropdownMenuBox(
                    expanded = expandedAcc,
                    onExpandedChange = { expandedAcc = it }
                ) {
                    OutlinedTextField(
                        value = selectedAcc?.bankName ?: "No asociado (Flujo Efectivo)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta Asociada", color = TextGray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ActiveGold,
                            unfocusedBorderColor = SurfaceDark,
                            cursorColor = ActiveGold
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAcc,
                        onDismissRequest = { expandedAcc = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguna (Flujo Efectivo)", color = TextGray) },
                            onClick = { 
                                accountId = null
                                expandedAcc = false 
                            }
                        )
                        uiState.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.bankName} • ${acc.last4}", color = Color.White) },
                                onClick = { 
                                    accountId = acc.id
                                    expandedAcc = false 
                                }
                            )
                        }
                    }
                }
                
                // Is Paid Switch (mostly for credit)
                val accountType = uiState.accounts.find { it.id == accountId }?.accountTypeId
                if (accountType == "CREDIT" && type == "egreso") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estado del Gasto", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("¿Ya lo liquidaste en tu último corte?", color = TextGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ActiveGold, checkedTrackColor = SurfaceDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val finalAmount = amount.toDoubleOrNull() ?: tx.amount
                        val finalTx = tx.copy(
                            amount = finalAmount,
                            merchant = merchant.takeIf { it.isNotBlank() },
                            concept = concept.takeIf { it.isNotBlank() },
                            dateIso = dateIso,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            isPaid = if (accountType == "CREDIT" && type == "egreso") isPaid else false
                        )
                        viewModel.saveChanges(finalTx)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TypeSelectorOption(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ActiveGold.copy(alpha = 0.2f) else SurfaceDark)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) ActiveGold else TextGray,
            fontWeight = FontWeight.Bold
        )
    }
}
