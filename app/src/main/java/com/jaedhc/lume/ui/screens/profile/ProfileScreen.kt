package com.jaedhc.lume.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray

import androidx.navigation.NavHostController
import com.jaedhc.lume.ui.navigation.Screen
import com.jaedhc.lume.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val accountCount by viewModel.accountCount.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshBalances()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Limpiar todos los datos?", color = Color.White) },
            text = { Text("Esta acción eliminará todas tus transacciones, cuentas y planes. No se puede deshacer.", color = TextGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData {
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text("LIMPIAR TODO", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCELAR", color = Color.White)
                }
            },
            containerColor = SurfaceDark,
            textContentColor = Color.White
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp)
    ) {
        item {
            ProfileHeader()
            Spacer(modifier = Modifier.height(48.dp))
        }

        item {
            SettingsSection(title = "PERSONALIZACIÓN") {
                ThemeToggleItem()
                Divider(color = Color(0xFF2C2C35), thickness = 1.dp)
                SettingsNavigationItem(
                    icon = Icons.Default.Category,
                    title = "Categorías",
                    onClick = { navController.navigate(Screen.ManageCategories.route) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            SettingsSection(title = "GESTIÓN FINANCIERA") {
                SettingsNavigationItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Cuentas y Tarjetas",
                    trailingText = "$accountCount Activas",
                    onClick = { navController.navigate(Screen.SelectAccountType.route) }
                )
                Divider(color = Color(0xFF2C2C35), thickness = 1.dp)
                SettingsNavigationItem(
                    icon = Icons.Default.Sync,
                    title = "Sincronizar saldos",
                    subtitle = if (isRefreshing) "Sincronizando..." else "Recalcular balances",
                    onClick = { viewModel.refreshBalances() }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            SettingsSection(title = "SEGURIDAD Y PRIVACIDAD") {
                SettingsNavigationItem(
                    icon = Icons.Default.Description,
                    title = "Exportar Datos",
                    subtitle = "CSV, JSON, PDF",
                    trailingIcon = Icons.Default.Download
                )
                Divider(color = Color(0xFF2C2C35), thickness = 1.dp)
                SettingsNavigationItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Limpiar todos los datos",
                    subtitle = "Borrar transacciones y cuentas",
                    trailingIcon = Icons.Default.Warning,
                    onClick = { showDeleteDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(64.dp))
        }

        item {
            ProfileFooter()
        }
    }
}

@Composable
fun ProfileHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
            ) {
                AsyncImage(
                    model = "https://https://avatars.githubusercontent.com/u/128942921?s=96&v=4",
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF1DA1F2),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Usuario",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
        ) {
            content()
        }
    }
}

@Composable
fun ThemeToggleItem() {
    var isDarkTheme by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color.White)
            Text("Tema", color = Color.White, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { isDarkTheme = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ActiveGold,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = TextGray, fontSize = 12.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trailingText != null) {
                Text(trailingText, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Icon(trailingIcon, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Lume AI Expense Manager v2.4.1",
            color = TextGray,
            fontSize = 11.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hecho con", color = TextGray, fontSize = 11.sp)
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Text("para tus finanzas", color = TextGray, fontSize = 11.sp)
        }
        Spacer(
            modifier = Modifier.height(88.dp)
        )
    }
}
