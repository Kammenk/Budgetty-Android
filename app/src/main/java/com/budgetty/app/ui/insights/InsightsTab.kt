package com.budgetty.app.ui.insights

import androidx.annotation.StringRes
import com.budgetty.app.R

/**
 * Phone-only grouping of the Insights [InsightsSection]s into a few tabs, so the screen reads as a
 * short set of focused pages instead of one long scroll (chosen "Hybrid" direction, P1).
 *
 * P1 ships three content tabs; the Overview and Custom tabs from the design arrive in later phases.
 * [InsightsSection.WELLBEING] is pinned outside the tab strip, so it maps to `null`. The tablet keeps
 * its own two-pane layout and does not use this grouping.
 */
enum class InsightsTab(@param:StringRes val labelRes: Int) {
    SPENDING(R.string.insights_tab_spending),
    MONEY(R.string.insights_tab_money),
    TRENDS(R.string.insights_tab_trends),
}

/** Which tab a section belongs to, or `null` for sections rendered outside the tab strip (WELLBEING). */
fun InsightsSection.tab(): InsightsTab? = when (this) {
    InsightsSection.BREAKDOWN,
    InsightsSection.SUMMARY,
    InsightsSection.TOP_CATEGORIES,
    InsightsSection.TOP_STORES,
    InsightsSection.BIGGEST_PURCHASES,
    InsightsSection.SUBSCRIPTIONS,
    -> InsightsTab.SPENDING

    InsightsSection.INCOME_SPENDING,
    InsightsSection.SAVINGS_RATE,
    InsightsSection.NEEDS_WANTS_SAVINGS,
    InsightsSection.INCOME_BY_SOURCE,
    -> InsightsTab.MONEY

    InsightsSection.HIGHLIGHTS,
    InsightsSection.TREND,
    InsightsSection.PERIOD_COMPARISON,
    -> InsightsTab.TRENDS

    InsightsSection.WELLBEING -> null
}
