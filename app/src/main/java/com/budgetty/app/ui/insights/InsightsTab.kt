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

/**
 * One row of the consolidated "things to set up" checklist on the Overview tab (P3). Each item names a
 * piece of one-time setup that unlocks more of Insights, links to where it's done, and can be dismissed
 * with the ✕. An item is shown only while its setup is genuinely incomplete (see `activeSetupItems` in
 * `InsightsScreen`) so the card empties itself out and then disappears entirely.
 *
 * [key] is persisted (dismissals live in `SettingsStore.dismissedInsightsSetup`, except [OVERLAY]
 * which reuses the older `insightsOverlayNudgeDismissed` flag) — keep the keys stable.
 */
enum class InsightsSetupItem(
    val key: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val ctaRes: Int,
) {
    SAVINGS("savings", R.string.insights_setup_item_savings, R.string.insights_setup_cta_savings),
    INCOME("income", R.string.insights_setup_item_income, R.string.insights_setup_cta_income),
    OVERLAY("overlay", R.string.insights_setup_item_overlay, R.string.insights_setup_cta_overlay),
    BUCKETS("buckets", R.string.insights_setup_item_buckets, R.string.insights_setup_cta_buckets),
}
