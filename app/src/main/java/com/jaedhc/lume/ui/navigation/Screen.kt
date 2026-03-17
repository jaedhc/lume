package com.jaedhc.lume.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Receiver : Screen("receiver") // Only reached via Intent? Or navigation?
    object Insights : Screen("insights")
    object Gastos : Screen("gastos")
    object Perfil : Screen("perfil")
    object Scan : Screen("scan")
    object ManageCategories : Screen("manage_categories")
    object CreateCategory : Screen("create_category")
    object ManageAccounts : Screen("accounts")
    object SelectAccountType : Screen("select_account_type")
    object CreateAccount : Screen("create_account/{typeId}")
    object AddCashAccount : Screen("add_cash_account")
    object AddInvestmentAccount : Screen("add_investment_account")
    object EditTransaction : Screen("edit_transaction/{transactionId}")
    object CustomColorPicker : Screen("custom_color_picker/{initialColor}")
    object TransferBetweenAccounts : Screen("transfer_between_accounts")
    object TdcDetail : Screen("tdc_detail/{accountId}")
}
