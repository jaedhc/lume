package com.example.lume.ui.screens.transactions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.lume.data.db.TransactionWithCategory
import com.example.lume.ui.components.TransactionItem
import com.example.lume.data.mappers.TransactionMappers.isMsi
import com.example.lume.data.mappers.TransactionMappers.cleanConcept
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.BackgroundDark
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import com.example.lume.viewmodel.TransactionsViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavHostController,
    viewModel: TransactionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTxForOptions by remember { mutableStateOf<TransactionWithCategory?>(null) }

    if (selectedTxForOptions != null) {
        val tx = selectedTxForOptions!!
        AlertDialog(
            onDismissRequest = { selectedTxForOptions = null },
            icon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = ActiveGold) },
            title = { Text("Opciones de Transacción", color = Color.White) },
            text = { Text("¿Qué deseas hacer con esta transacción?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = tx.transaction.id
                        selectedTxForOptions = null
                        navController.navigate("edit_transaction/$id")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGold)
                ) {
                    Text("Editar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        selectedTxForOptions = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFEF4444))
                }
            },
            containerColor = SurfaceDark
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Transacciones", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Añadir", tint = ActiveGold)
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
        ) {
            MonthFilters()
            
            if (uiState.isLoading && !uiState.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ActiveGold)
                }
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = uiState.isRefreshing,
                            color = ActiveGold,
                            containerColor = SurfaceDark
                        )
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        //verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Adjusted padding for FAB
                    ) {
                        val lineColor = Color(0xFF333333)

                        if (uiState.isLoading || uiState.isRefreshing) {
                            item {
                                TimelineHeader("CARGANDO...", Icons.Default.Sync, ActiveGold)
                            }
                            items(5) {
                                TimelineItemWrapper(lineColor, isLast = it == 4) {
                                    TransactionItemSkeleton()
                                }
                            }
                        } else {
                            // ESTE MES (Current Month including Today)
                            if (uiState.esteMesTransactions.isNotEmpty()) {
                            item {
                                TimelineHeader("ESTE MES", Icons.Default.Event, Color(0xFF2D9F24))
                            }
                            items(uiState.esteMesTransactions, key = { it.transaction.id }) { tx ->
                                TimelineItemWrapper(lineColor) { 
                                    TransactionItem(
                                        txWithCat = tx,
                                        displayTitle = tx.cleanConcept(),
                                        isMsi = tx.isMsi(),
                                        onLongClick = { selectedTxForOptions = tx }
                                    )
                                }
                            }
                        }

                        // FUTURE MONTHS (Grouped)
                        uiState.futureTransactionsGrouped.forEach { (monthName, transactions) ->
                            item {
                                TimelineHeader(monthName.uppercase(), Icons.Default.CalendarToday, ActiveGold)
                            }
                            items(transactions, key = { it.transaction.id }) { tx ->
                                TimelineItemWrapper(lineColor) { 
                                    TransactionItem(
                                        txWithCat = tx,
                                        displayTitle = tx.cleanConcept(),
                                        isMsi = tx.isMsi(),
                                        onLongClick = { selectedTxForOptions = tx }
                                    )
                                }
                            }
                        }

                        // PAST (Before current month)
                        if (uiState.pastTransactions.isNotEmpty()) {
                            item {
                                TimelineHeader("ANTERIORES", Icons.Default.History, TextGray)
                            }
                            items(uiState.pastTransactions, key = { it.transaction.id }) { tx ->
                                TimelineItemWrapper(lineColor, isLast = tx == uiState.pastTransactions.last()) { 
                                    TransactionItem(
                                        txWithCat = tx,
                                        displayTitle = tx.cleanConcept(),
                                        isMsi = tx.isMsi(),
                                        onLongClick = { selectedTxForOptions = tx }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
fun TransactionItemSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color(0xFF252525),
        Color(0xFF323232),
        Color(0xFF252525),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(horizontalAlignment = Alignment.End) {
            // Amount
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Status
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun TimelineItemWrapper(lineColor: Color, isLast: Boolean = false, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 8.dp)
            .drawBehind {
                if (!isLast) {
                    drawLine(
                        color = lineColor,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(16.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    ) {
        Spacer(modifier = Modifier.width(48.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun MonthFilters() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val months = listOf("Todos", "Este Mes", "Anterior")
        months.forEachIndexed { index, month ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (index == 0) ActiveGold else SurfaceDark)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = month,
                    color = if (index == 0) Color.Black else TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TimelineHeader(title: String, icon: ImageVector, iconTint: Color) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.2f))
                .border(2.dp, iconTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

