package com.jaedhc.lume.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jaedhc.lume.data.db.CategoryEntity
import com.jaedhc.lume.ui.theme.ActiveGold
import com.jaedhc.lume.ui.theme.BackgroundDark
import com.jaedhc.lume.ui.navigation.Screen
import com.jaedhc.lume.ui.theme.SurfaceDark
import com.jaedhc.lume.ui.theme.TextGray
import com.jaedhc.lume.viewmodel.CategoriesViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ManageCategoriesScreen(
    navController: NavHostController,
    viewModel: CategoriesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredCategories by viewModel.filteredCategories.collectAsState()
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveCategory(from.index, to.index)
    }

    Scaffold(
        topBar = {
            CategoriesTopBar(onBackClick = { navController.popBackStack() })
        },
        bottomBar = {
            AddCategoryButton(
                modifier = Modifier.padding(16.dp),
                onClick = { navController.navigate(Screen.CreateCategory.route) }
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SearchField(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "TUS CATEGORÍAS",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp), // Added padding for bottom button
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredCategories, key = { _, item -> item.id }) { index, category ->
                    ReorderableItem(reorderableLazyListState, key = category.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                        
                        CategoryItem(
                            category = category,
                            modifier = Modifier
                                .shadow(elevation, shape = RoundedCornerShape(16.dp))
                                .zIndex(if (isDragging) 1f else 0f),
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStarted = { },
                                onDragStopped = { }
                            )
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesTopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text("Manage Categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundDark
        ),
        windowInsets = WindowInsets(0)
    )
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark),
        placeholder = { Text("Search categories...", color = TextGray) },
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
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    val categoryColor = remember(category.color) {
        if (category.color.isNullOrBlank()) {
            ActiveGold
        } else {
            try { Color(android.graphics.Color.parseColor(category.color)) } catch (e: Exception) { ActiveGold }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2C2C35)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                getCategoryIcon(category.icon ?: ""),
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category.name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                "Sub-categorías y detalles",
                color = TextGray,
                fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextGray, modifier = Modifier.size(20.dp))
            Icon(
                Icons.Default.DragHandle, 
                contentDescription = "Reorder", 
                tint = TextGray, 
                modifier = Modifier
                    .size(20.dp)
                    .then(dragHandleModifier)
            )
        }
    }
}

@Composable
fun AddCategoryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ActiveGold),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.Black)
            Text("Agregar Categoría", color = Color.Black, fontWeight = FontWeight.Bold)
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
        else -> Icons.Default.Category
    }
}
