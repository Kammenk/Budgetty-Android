package com.budgetty.app.ui.insights

import androidx.annotation.StringRes
import com.budgetty.app.R

/**
 * Phone-only grouping of the Insights [InsightsSection]s into a few tabs, so the screen reads as a
 * short set of focused pages instead of one long scroll (chosen "Hybrid" direction).
 *
 * OVERVIEW is a bespoke summary (rendered by `OverviewTabContent`, not from sections) and is the
 * default landing tab; SPENDING/MONEY/TRENDS hold the existing section cards. The Custom tab from
 * the design arrives in a later phase. [InsightsSection.WELLBEING] is pinned outside the tab strip
 * so it maps to `null`, and [InsightsSection.SUMMARY]'s stats live in the Overview hero so it maps
 * to OVERVIEW (its standalone card is not rendered in any tab). The tablet keeps its own two-pane
 * layout and does not use this grouping.
 */
enum class InsightsTab(@param:StringRes val labelRes: Int) {
    OVERVIEW(R.string.insights_overview_tab),
    SPENDING(R.string.insights_tab_spending),
    MONEY(R.string.insights_tab_money),
    TRENDS(R.string.insights_tab_trends),
}

/** Which tab a section belongs to, or `null` for sections rendered outside the tab strip (WELLBEING). */
fun InsightsSection.tab(): InsightsTab? = when (this) {
    // Summary's stat tiles live in the Overview hero, so its standalone card isn't rendered in a tab.
    InsightsSection.SUMMARY -> InsightsTab.OVERVIEW

    InsightsSection.BREAKDOWN,
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
