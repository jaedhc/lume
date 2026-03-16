package com.example.lume.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = TextGray.copy(alpha = 0.5f)) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = ActiveGold) } },
            prefix = prefix?.let { { Text(it, color = Color.White) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ActiveGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                disabledContainerColor = SurfaceDark,
                disabledBorderColor = Color.White.copy(alpha = 0.05f),
                disabledTextColor = Color.White.copy(alpha = 0.5f)
            ),
            singleLine = true,
            enabled = enabled
        )
    }
}

@Composable
fun ColorSelector(selectedColor: String, onSelect: (String) -> Unit, onCustomClick: () -> Unit) {
    val defaultColors = listOf("#FFB800", "#2ECC71", "#3498DB", "#E74C3C", "#9B59B6", "#E91E63", "#F39C12", "#1abc9c")
    
    // Ensure the selected color is always in the list being rendered (so the user sees what they picked)
    val colors = if (selectedColor !in defaultColors) defaultColors + selectedColor else defaultColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("COLOR DE CUENTA", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { colorStr ->
                val color = try { Color(colorStr.toColorInt()) } catch(e: Exception) { Color.Gray }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == colorStr) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onSelect(colorStr) }
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, TextGray.copy(alpha = 0.3f), CircleShape)
                    .clickable { onCustomClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Color Personalizado", tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SecurityNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = ActiveGold, modifier = Modifier.size(24.dp))
        Text(
            "Sus datos bancarios están cifrados y se almacenan únicamente en su dispositivo. Lume nunca comparte su información financiera.",
            color = TextGray,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun CashPreviewCard(
    bank: String, 
    subtitle: String, 
    balance: String, 
    color: String
) {
    val displayColor = try { Color(android.graphics.Color.parseColor(color)) } catch(e: Exception) { ActiveGold }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("VISTA PREVIA EN DASHBOARD", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    bank,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text("BALANCE LÍQUIDO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (balance.isBlank()) "$0.00" else "$$balance",
                    color = displayColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .background(displayColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = displayColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun InvestmentPreviewCard(
    bank: String,
    target: String,
    balance: String,
    color: String
) {
    val displayColor = try { Color(android.graphics.Color.parseColor(color)) } catch(e: Exception) { ActiveGold }
    val balanceValue = balance.toFloatOrNull() ?: 0f
    val targetValue = target.toFloatOrNull() ?: 1f
    val progress = (balanceValue / targetValue).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("META DE AHORRO / INVERSIÓN", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    bank,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% completado", color = Color.White, fontSize = 11.sp)
                        Text("Meta: $$target", color = TextGray, fontSize = 11.sp)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = displayColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text("CANTIDAD ACTUAL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (balance.isBlank()) "$0.00" else "$$balance",
                    color = displayColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .background(displayColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = displayColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}
@Composable
fun EstadoDeCuentaSection(
    fileName: String?,
    status: String?,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ESTADO DE CUENTA (OPCIONAL)", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = if (fileName != null) ActiveGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { if (fileName == null) onPickFile() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (fileName != null) ActiveGold.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (fileName != null) Icons.Default.Description else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (fileName != null) ActiveGold else TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    if (fileName != null) {
                        Text(fileName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(status ?: "Archivo cargado", color = ActiveGold, fontSize = 12.sp)
                    } else {
                        Text("Subir PDF o Imagen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Importa transacciones automáticamente", color = TextGray, fontSize = 12.sp)
                    }
                }
                
                if (fileName != null) {
                    IconButton(onClick = onClearFile) {
                        Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = TextGray)
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
                }
            }
        }
    }
}
