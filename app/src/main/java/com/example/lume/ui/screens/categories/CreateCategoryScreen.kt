package com.example.lume.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryScreen(
    navController: NavHostController,
    viewModel: CategoriesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(Icons.Default.Receipt) }
    var selectedColor by remember { mutableStateOf(ActiveGold) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva Categoría", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark),
                windowInsets = WindowInsets(0)
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        viewModel.saveCategory(name, selectedIcon.name, selectedColor.toHexString())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Guardar Categoría", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preview Bubble
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, selectedColor, CircleShape)
                    .background(selectedColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(selectedIcon, contentDescription = null, tint = selectedColor, modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vista Previa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Personaliza tu burbuja de gastos", color = TextGray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Name Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("NOMBRE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        viewModel.clearErrors()
                    },
                    isError = uiState.nameExistsError,
                    placeholder = { Text("E.g. Suscripciones", color = TextGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = ActiveGold,
                        unfocusedIndicatorColor = TextGray.copy(alpha = 0.3f),
                        cursorColor = ActiveGold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                if (uiState.nameExistsError) {
                    Text(
                        "Esta categoría ya existe",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Icon Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ELEGIR ICONO", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                val icons = listOf(
                    Icons.Default.Receipt, Icons.Default.Payments, Icons.Default.Restaurant,
                    Icons.Default.ShoppingCart, Icons.Default.DirectionsCar, Icons.Default.Home,
                    Icons.Default.MedicalServices, Icons.Default.FitnessCenter
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(icons) { icon ->
                        IconChoice(
                            icon = icon,
                            isSelected = selectedIcon == icon,
                            onClick = { selectedIcon = icon }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Color Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("PALETA DE COLORES", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                val colors = listOf(
                    ActiveGold, Color(0xFF3B82F6), Color(0xFF10B981),
                    Color(0xFFEF4444), Color(0xFF6366F1), Color(0xFFA855F7),
                    Color(0xFF2DD4BF)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(colors) { color ->
                        ColorChoice(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconChoice(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ActiveGold else SurfaceDark)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ColorChoice(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

// Utility extension for helper consistency (Room expects String or we wrap it)
private fun Color.toHexString(): String {
    return String.format("#%02X%02X%02X%02X", (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}

// Extension to get icon name for saving
private val ImageVector.name: String
    get() = this.name.substringAfterLast('.')
