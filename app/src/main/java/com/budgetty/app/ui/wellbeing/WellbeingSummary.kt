package com.budgetty.app.ui.wellbeing

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** The weekly check-in "pace card" figures. */
data class WeeklyPace(
    val spent: BigDecimal,
    val weeklyBudget: BigDecimal?,
    /** Spent ÷ budget, clamped 0..1 (0 when no weekly budget). */
    val fractionUsed: Float,
    /** Where the week "should" be by now (days elapsed ÷ 7), for the pace marker. */
    val paceFraction: Float,
    val remaining: BigDecimal,
    val daysLeft: Int,
    val deltaPercentVsLastWeek: Int?,
    val onTrack: Boolean,
)

/** Raw figures behind one component, for the tap-to-expand explanation copy (localised in the UI). */
data class ComponentDetail(
    val savingsRatePercent: Int? = null,
    val saved: BigDecimal? = null,
    val income: BigDecimal? = null,
    val overCount: Int? = null,
    val budgetedCount: Int? = null,
    val overspend: BigDecimal? = null,
    val trendPercent: Int? = null,
    val avgSpend: BigDecimal? = null,
    val subsMonthly: BigDecimal? = null,
    val subsSharePercent: Int? = null,
    val goalsOnPace: Int? = null,
    val goalsTotal: Int? = null,
)

/** A wins-strip pill. Copy is localised in the UI from [type]; [emoji] is the leading glyph. */
data class WellbeingWin(val emoji: String, val type: TipType, val label: String? = null, val percent: Int? = null)

/**
 * Everything the Wellbeing screen, the Home banner and the Insights row read — computed once by
 * [WellbeingProvider] from the repositories. Tips here are ranked but NOT yet filtered by the user's
 * dismissals; the ViewModel drops dismissed ids so dismissal survives recomposition without the
 * provider re-running.
 */
data class WellbeingSummary(
    val score: WellbeingScore,
    val monthlyTips: List<WellbeingTip>,
    val weekly: WeeklyPace,
    val weeklyTips: List<WellbeingTip>,
    val wins: List<WellbeingWin>,
    val detail: Map<WellbeingComponentKey, ComponentDetail>,
    val receiptsLogged: Int,
    val hasBudget: Boolean,
    /** Pay-cycle month key (e.g. "2026-06"); tip dismissals are scoped to it so tips resurface next month. */
    val periodId: String,
    val monthYear: YearMonth,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
) {
    val hasScore: Boolean get() = score.hasScore
}
