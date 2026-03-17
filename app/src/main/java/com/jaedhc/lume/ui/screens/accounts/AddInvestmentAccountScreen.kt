package com.jaedhc.lume.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.CreateAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentAccountScreen(
    navController: NavHostController,
    viewModel: CreateAccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize as a INVESTMENT account type
    LaunchedEffect(Unit) {
        viewModel.setAccountType("INVESTMENT")
        // Default color might be blue (indicative of portfolios or savings)
        if (uiState.selectedColor == "#FFB800") {
            viewModel.onColorSelect("#3498DB")
        }
    }

    val navBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<String>("selected_color")?.observeForever { hex ->
            viewModel.onColorSelect(hex)
            savedStateHandle.remove<String>("selected_color")
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navController.navigate(com.jaedhc.lume.ui.navigation.Screen.ManageAccounts.route) {
                popUpTo(com.jaedhc.lume.ui.navigation.Screen.ManageAccounts.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(modifier = Modifier.background(BackgroundDark)) {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("ADD INVESTMENT ACCOUNT", color = ActiveGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Detalles del Ahorro", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        Spacer(modifier = Modifier.size(48.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                    windowInsets = WindowInsets(0)
                )
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(ActiveGold))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Configura tu Meta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Define los detalles de tu nuevo objetivo de ahorro.", color = TextGray, fontSize = 14.sp)
            }
            
            InvestmentPreviewCard(
                bank = if (uiState.bankName.isBlank()) "Viaje a Japón" else uiState.bankName,
                balance = uiState.initialBalance,
                target = uiState.targetAmount,
                color = uiState.selectedColor
            )

            // Form Fields - simplified
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(
                    label = "NOMBRE DE LA META O PORTAFOLIO",
                    value = uiState.bankName,
                    onValueChange = { viewModel.onBankNameChange(it) },
                    placeholder = "Ej. Viaje a Japón, CETES, GBM...",
                    icon = Icons.Default.ShowChart
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "CAPITAL AHORRADO",
                        value = uiState.initialBalance,
                        onValueChange = { viewModel.onBalanceChange(it) },
                        placeholder = "$ 0.00",
                        prefix = "$ ",
                        keyboardType = KeyboardType.Decimal
                    )
                    
                    FormField(
                        modifier = Modifier.weight(1f),
                        label = "META (OPCIONAL)",
                        value = uiState.targetAmount,
                        onValueChange = { viewModel.onTargetAmountChange(it) },
                        placeholder = "$ 0.00",
                        prefix = "$ ",
                        keyboardType = KeyboardType.Decimal
                    )
                }

                // Invisible field to supply valid last4 to ViewModel validation if needed
                LaunchedEffect(Unit) {
                    viewModel.onLast4Change("INV")
                }

                FormField(
                    label = "DESCRIPCIÓN (OPCIONAL)",
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    placeholder = "Ej. Fondo de emergencia, Acciones...",
                    icon = Icons.Default.Notes
                )
            }

            ColorSelector(
                selectedColor = uiState.selectedColor, 
                onSelect = { viewModel.onColorSelect(it) },
                onCustomClick = {
                    navController.navigate("custom_color_picker/${uiState.selectedColor.replace("#", "%23")}")
                }
            )

            SecurityNote()

            Button(
                onClick = { viewModel.saveAccount() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
                enabled = !uiState.isSaving && uiState.bankName.isNotBlank() && uiState.initialBalance.isNotBlank()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Text("Crear Meta o Inversión", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


