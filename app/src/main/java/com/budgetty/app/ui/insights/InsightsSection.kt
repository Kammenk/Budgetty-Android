package com.budgetty.app.ui.insights

import androidx.annotation.StringRes
import com.budgetty.app.R

/**
 * The toggleable content sections of the phone Insights screen, listed in the header's customization
 * menu. A section is shown unless its [key] is in the user's hidden-sections setting. [key] is
 * persisted, so existing values must stay stable. (Some sections also only appear when there's data
 * to fill them — the toggle gates a section on top of that, it doesn't force it visible.)
 */
enum class InsightsSection(val key: String, @param:StringRes val labelRes: Int) {
    BREAKDOWN("breakdown", R.string.insights_breakdown),
    SUMMARY("summary", R.string.insights_summary),
    // Rule-based narrative callouts (biggest movers, new / dominant categories).
    HIGHLIGHTS("highlights", R.string.insights_highlights),
    TREND("trend", R.string.insights_trend),
    // Period-over-period spend comparison, sitting under the trend it summarizes.
    PERIOD_COMPARISON("period_comparison", R.string.insights_period_comparison),
    BUDGET("budget", R.string.insights_budget),
    // Income & recurring-payment cards (money-flow), grouped after the budget section.
    INCOME_SPENDING("income_spending", R.string.insights_income_spending),
    SAVINGS_RATE("savings_rate", R.string.insights_savings_rate),
    FIXED_FLEXIBLE("fixed_flexible", R.string.insights_fixed_flexible),
    UPCOMING_BILLS("upcoming_bills", R.string.insights_upcoming_bills),
    INCOME_BY_SOURCE("income_by_source", R.string.insights_income_by_source),
    TOP_CATEGORIES("top_categories", R.string.insights_top_categories),
    TOP_STORES("top_stores", R.string.insights_top_stores),
    // Biggest single line-item purchases in the period.
    BIGGEST_PURCHASES("biggest_purchases", R.string.insights_biggest_purchases),
}
