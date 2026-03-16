package com.example.lume.ui.screens.accounts

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
import androidx.compose.material.icons.outlined.HelpOutline
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
import androidx.navigation.NavHostController
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountTypeScreen(navController: NavHostController) {
    var selectedType by remember { mutableStateOf<String?>(null) }

    val accountTypes = listOf(
        AccountTypeItem("DEBIT", "TDD (Debit)", "Manage your daily spending and liquid funds.", Icons.Default.AccountBalanceWallet),
        AccountTypeItem("CREDIT", "TDC (Credit)", "Track your credit limits and monthly repayments.", Icons.Default.CreditCard),
        AccountTypeItem("SAVINGS", "Savings", "Monitor your long-term goals and emergency funds.", Icons.Default.Savings),
        AccountTypeItem("INVESTMENT", "Investments", "Analyze your portfolio and asset growth.", Icons.Default.ShowChart),
        AccountTypeItem("CASH", "Cash", "Log manual transactions for physical currency.", Icons.Default.Payments)
    )

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(modifier = Modifier.background(BackgroundDark)) {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("STEP 1 OF 2", color = ActiveGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Setup Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Help */ }) {
                            Icon(Icons.Outlined.HelpOutline, contentDescription = "Help", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
                    windowInsets = WindowInsets(0)
                )
                // Custom Progress Bar (Full Width)
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(SurfaceDark)) {
                    Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().background(ActiveGold))
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select Account Type",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Lume's AI engine optimizes categorization based on your account type. Choose the category that best fits.",
                    color = TextGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                accountTypes.forEach { type ->
                    TypeSelectionCard(
                        type = type,
                        isSelected = selectedType == type.id,
                        onClick = { selectedType = type.id }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { 
                        selectedType?.let { typeId ->
                            if (typeId == "CASH") {
                                navController.navigate(com.example.lume.ui.navigation.Screen.AddCashAccount.route)
                            } else if (typeId == "INVESTMENT" || typeId == "SAVINGS") {
                                navController.navigate(com.example.lume.ui.navigation.Screen.AddInvestmentAccount.route)
                            } else {
                                navController.navigate("create_account/$typeId")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
                    enabled = selectedType != null
                ) {
                    Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Text(
                    "You can add more accounts later in settings.",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TypeSelectionCard(
    type: AccountTypeItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ActiveGold else Color.Transparent
    val backgroundColor = if (isSelected) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(type.icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(type.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(type.description, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ActiveGold else Color.Transparent)
                    .border(2.dp, if (isSelected) ActiveGold else TextGray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class AccountTypeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)
