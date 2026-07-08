package com.budgetty.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgetty.app.R

/** App navigation routes. */
object Routes {
    const val HOME = "home"
    const val INSIGHTS = "insights"
    const val ACCOUNT = "account"
    const val UPLOAD = "upload/{source}?receiptId={receiptId}"
    const val UPLOAD_ARG_SOURCE = "source"
    const val UPLOAD_ARG_RECEIPT_ID = "receiptId"
    const val BUDGET = "budget"
    const val PAYWALL = "paywall"
    const val HISTORY = "history"
    const val WIDGETS = "widgets"
    const val CATEGORY_RULES = "category_rules"

    /** Upload route for a given source: "camera", "file", or "manual". */
    fun upload(source: String) = "upload/$source"

    /** Upload route in edit mode, pre-loading an existing receipt by its id. */
    fun editReceipt(receiptId: Long) = "upload/edit?receiptId=$receiptId"
}

/** Destinations shown in the phone's bottom navigation bar. */
enum class BottomNavDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    HISTORY(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
    INSIGHTS(Routes.INSIGHTS, R.string.nav_insights, Icons.Filled.PieChart),
    ACCOUNT(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.AccountCircle),
}

/**
 * Destinations shown in the tablet's side navigation rail. The wider rail has room to promote
 * Budget to a top-level destination (it stays a pushed sub-screen on the phone), so it lists all
 * five primary screens.
 */
enum class RailDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    HISTORY(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
    INSIGHTS(Routes.INSIGHTS, R.string.nav_insights, Icons.Filled.PieChart),
    BUDGET(Routes.BUDGET, R.string.account_budget, Icons.Filled.AccountBalanceWallet),
    ACCOUNT(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.AccountCircle),
}
