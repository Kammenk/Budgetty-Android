package com.budgetty.app.ui.home

import androidx.annotation.StringRes
import com.budgetty.app.R

/**
 * The toggleable content sections of the phone Home screen, listed in the header's customization
 * menu. A section is shown unless its [key] is in the user's hidden-sections setting. [key] is
 * persisted, so existing values must stay stable.
 */
enum class HomeSection(val key: String, @param:StringRes val labelRes: Int) {
    TOTAL_SPENT("total_spent", R.string.home_section_total_spent),
    BUDGETS("budgets", R.string.home_budgets),
    // Recurring bills due soon, sitting under the budget plan (moved here from Insights).
    UPCOMING_BILLS("upcoming_bills", R.string.insights_upcoming_bills),
    RECEIPTS("receipts", R.string.home_receipts),
}
