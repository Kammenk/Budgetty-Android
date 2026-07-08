package com.budgetty.app.widget

import java.math.BigDecimal

/**
 * Immutable snapshot the home-screen widgets render from, produced by [WidgetDataProvider]. Holds
 * everything the three widget types need (monthly/weekly budget progress + the monthly summary) so
 * a widget's `provideContent` is a pure function of this data.
 */
data class WidgetData(
    val currencySymbol: String = "лв",
    /** Effective monthly budget: the saved MONTHLY budget, else the weekly budget scaled up, else ZERO. */
    val monthlyBudget: BigDecimal = BigDecimal.ZERO,
    val monthlySpent: BigDecimal = BigDecimal.ZERO,
    /** Effective weekly budget: the saved WEEKLY budget, else the monthly budget scaled down, else ZERO. */
    val weeklyBudget: BigDecimal = BigDecimal.ZERO,
    val weeklySpent: BigDecimal = BigDecimal.ZERO,
    /** Which period the user actually set (Monthly wins if both or neither): the adaptive Budget
     *  widget shows this period and derives the other, mirroring the Home budget block. */
    val budgetIsMonthly: Boolean = true,
    /** Spend in the previous calendar month, for the summary's "vs last month" line. */
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    /** % change of this month vs last month (positive = spent more); null when last month had no spend. */
    val vsLastMonthPercent: Int? = null,
    /** Spend in the previous Mon–Sun week, for the This Week widget's comparison bar. */
    val lastWeekTotal: BigDecimal = BigDecimal.ZERO,
    /** % change of this week vs last week (positive = spent more); null when last week had no spend. */
    val vsLastWeekPercent: Int? = null,
    /** Short label for the current Mon–Sun week, e.g. "Jun 24–30". */
    val weekLabel: String = "",
    /** Distinct receipts uploaded this calendar month. */
    val monthReceiptCount: Int = 0,
    /** This month's biggest categories (up to 3), each with its spend and ARGB color. */
    val topCategories: List<WidgetCategory> = emptyList(),
    /** Short month label, e.g. "Jun 2026". */
    val monthLabel: String = "",
    /** True when the user has Premium (unlimited scans → the Scan widget hides the quota line). */
    val isPremium: Boolean = false,
    /** Free-tier scans remaining this month, for the Scan widget's footnote. */
    val scansRemaining: Int = 0,
) {
    /** True once the user has a top-level budget (so the budget widgets show progress, not a CTA). */
    val hasMonthlyBudget: Boolean get() = monthlyBudget.signum() > 0
    val hasWeeklyBudget: Boolean get() = weeklyBudget.signum() > 0

    /** This month's total spend (== [monthlySpent]); the summary widget's headline figure. */
    val monthTotal: BigDecimal get() = monthlySpent
}

/** One row of the summary widget's "top categories" list. */
data class WidgetCategory(
    val name: String,
    val amount: BigDecimal,
    val colorArgb: Int,
)
