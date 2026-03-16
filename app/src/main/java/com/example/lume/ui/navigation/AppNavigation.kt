package com.example.lume.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lume.ui.components.FinanceBottomBar
import com.example.lume.ui.screens.dashboard.DashboardScreen
import com.example.lume.ui.screens.receiver.ShareReceiverScreen
import com.example.lume.ui.screens.placeholder.PlaceholderScreen
import com.example.lume.ui.screens.profile.ProfileScreen
import com.example.lume.ui.screens.categories.ManageCategoriesScreen
import com.example.lume.ui.screens.categories.CreateCategoryScreen
import com.example.lume.ui.screens.accounts.AccountsScreen
import com.example.lume.ui.screens.accounts.CreateAccountScreen
import com.example.lume.ui.screens.accounts.AddCashAccountScreen
import com.example.lume.ui.screens.accounts.AddInvestmentAccountScreen
import com.example.lume.ui.screens.accounts.SelectAccountTypeScreen
import com.example.lume.ui.screens.transactions.EditTransactionScreen
import com.example.lume.ui.screens.transactions.TransactionsScreen
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.lume.ui.screens.accounts.CustomColorPickerScreen
import com.example.lume.ui.screens.accounts.TransferBetweenAccountsScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route,
    uris: List<Uri> = emptyList(), // Passed for receiver flow
    onFinishReceiver: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val showBottomBar = currentRoute !in listOf(
        Screen.Receiver.route,
        Screen.ManageCategories.route,
        Screen.CreateCategory.route,
        Screen.SelectAccountType.route,
        Screen.CreateAccount.route,
        Screen.AddCashAccount.route,
        Screen.AddInvestmentAccount.route,
        "edit_transaction/{transactionId}",
        Screen.TransferBetweenAccounts.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FinanceBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        // Make background explicitly dark to match theme
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding()
            )
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = navController)
            }
            composable(Screen.Receiver.route) { backStackEntry ->
                val uris = backStackEntry.arguments?.getString("uris")?.split(",")?.map { Uri.parse(it) } ?: emptyList()
                ShareReceiverScreen(
                    uris = uris,
                    onDone = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            // Placeholders
            composable(Screen.Insights.route) { PlaceholderScreen("Reportes") }
            composable(Screen.Gastos.route) { TransactionsScreen(navController = navController) }
            composable(Screen.Perfil.route) { ProfileScreen(navController = navController) }
            composable(Screen.ManageAccounts.route) {
                AccountsScreen(navController = navController)
            }
            composable(Screen.SelectAccountType.route) {
                SelectAccountTypeScreen(navController = navController)
            }
            composable(Screen.CreateAccount.route) { backStackEntry ->
                val typeId = backStackEntry.arguments?.getString("typeId") ?: "DEBIT"
                CreateAccountScreen(navController = navController, typeId = typeId)
            }
            composable(Screen.AddCashAccount.route) {
                AddCashAccountScreen(navController = navController)
            }
            composable(Screen.AddInvestmentAccount.route) {
                AddInvestmentAccountScreen(navController = navController)
            }
            composable(Screen.Scan.route) { PlaceholderScreen("Escanear") }
            composable(Screen.ManageCategories.route) {
                ManageCategoriesScreen(navController = navController)
            }
            composable(Screen.CreateCategory.route) {
                CreateCategoryScreen(navController = navController)
            }
            composable(Screen.EditTransaction.route) { backStackEntry ->
                // transactionId is provided automatically through SavedStateHandle to the ViewModel
                EditTransactionScreen(navController = navController)
            }
            composable(
                route = Screen.CustomColorPicker.route,
                arguments = listOf(navArgument("initialColor") { type = NavType.StringType })
            ) { backStackEntry ->
                val initialColor = backStackEntry.arguments?.getString("initialColor") ?: "#FFB800"
                CustomColorPickerScreen(
                    initialColorHex = initialColor,
                    onColorSelected = { hex ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_color", hex)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.TransferBetweenAccounts.route) {
                TransferBetweenAccountsScreen(navController = navController)
            }
            composable(Screen.TdcDetail.route) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                com.example.lume.ui.screens.tdc.TdcDetailScreen(navController = navController, accountId = accountId)
            }
        }
    }
}
