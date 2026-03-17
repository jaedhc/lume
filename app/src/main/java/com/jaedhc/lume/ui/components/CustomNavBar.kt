package com.jaedhc.lume.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaedhc.lume.ui.navigation.Screen

import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.InactiveGray

@Composable
fun FinanceBottomBar(
    currentRoute: String = Screen.Dashboard.route,
    onNavigate: (String) -> Unit = {}
) {
    // Using colors from theme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp), // Aumentamos la altura total para que la luz respire
        contentAlignment = Alignment.BottomCenter
    ) {

        // --- RESPLANDOR (GLOW) ---
        // Lo colocamos primero para que esté detrás de todo
        Canvas(
            modifier = Modifier
                .size(150.dp) // Tamaño grande para que el degradado sea suave
                .offset(y = (-10).dp) // Centrado con el botón
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ActiveGold.copy(alpha = 0.2f), // Más intenso en el centro
                        ActiveGold.copy(alpha = 0.1f),
                        ActiveGold.copy(alpha = 0.05f),// Se desvanece
                        Color.Transparent              // Desaparece
                    ),
                    center = center,
                    radius = size.width / 2
                ),
                radius = size.width / 2
            )
        }

        // --- CUERPO DE LA BARRA ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp),
            color = BackgroundDark,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
                    .padding(0.dp, 0.dp, 0.dp,10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(
                        icon = Icons.Default.GridView,
                        label = "Dashboard",
                        isSelected = currentRoute == Screen.Dashboard.route,
                        onClick = { onNavigate(Screen.Dashboard.route) }
                    )
                    NavItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "Insights",
                        isSelected = currentRoute == Screen.Insights.route,
                        onClick = { onNavigate(Screen.Insights.route) }
                    )
                }

                Spacer(modifier = Modifier.width(80.dp))

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "Gastos",
                        isSelected = currentRoute == Screen.Gastos.route,
                        onClick = { onNavigate(Screen.Gastos.route) }
                    )
                    NavItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Cuentas",
                        isSelected = currentRoute == com.jaedhc.lume.ui.navigation.Screen.ManageAccounts.route,
                        onClick = { onNavigate(com.jaedhc.lume.ui.navigation.Screen.ManageAccounts.route) }
                    )
                }
            }
        }

        // --- BOTÓN FLOTANTE (FAB) ---
        Box(
            modifier = Modifier
                .offset(y = (-40).dp)
                .size(64.dp)
                .background(ActiveGold, shape = CircleShape)
                .clickable { onNavigate(Screen.Scan.route) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 1. Animamos el color basándonos en el estado isSelected
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) ActiveGold else InactiveGray,
        // Opcional: Personaliza la duración o curva de la animación
        animationSpec = tween(durationMillis = 400),
        label = "colorAnimation"
    )

    // 2. InteractionSource para desactivar el efecto visual del clic (opcional)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(
            onClick = onClick,
            interactionSource = interactionSource,
            indication = null // Esto quita el "ripple" gris para que sea más premium
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedColor, // Usamos el color animado
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = animatedColor, // Usamos el color animado
            fontSize = 11.sp
        )
    }
}

@Preview
@Composable
fun PreviewFinanceBar() {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.BottomCenter) {
        FinanceBottomBar()
    }
}