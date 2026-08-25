package com.budgetty.app.ui.wellbeing

import java.math.BigDecimal

/** Overall grade band; drives the ring/arc colour and the band word. */
enum class WellbeingBand { NEEDS_WORK, GETTING_THERE, HEALTHY, THRIVING }

/** Bar/label colour tier for a single component sub-score. */
enum class WellbeingTier { GOOD, WARN, BAD }

/** The five areas the score is built from. Order here is presentation order on the screen. */
enum class WellbeingComponentKey { SAVINGS, BUDGET, TREND, SUBSCRIPTIONS, GOALS }

enum class TipTone { ALERT, CAUTION, OPPORTUNITY, WIN }

enum class TipType {
    NEGATIVE_CASHFLOW, OVER_BUDGET, CATEGORY_SPIKE, SUBSCRIPTION_COST, MISSING_BUDGET,
    NO_GOAL, GOAL_OFF_TRACK, SAVINGS_WIN, CATEGORY_IMPROVED,
    BUDGET_PACE, SMALL_PURCHASE_LEAK, UNDER_PACE_WIN,
}

/** One goal's pace, distilled from [com.budgetty.app.ui.util.SavingsMath]. */
data class GoalPace(
    val name: String,
    val reached: Boolean,
    val behind: Boolean,
    /** Whole months the goal is projected to miss its target date by, when known. */
    val monthsLate: Int? = null,
)

/** Per-category spend this period plus context used by the tip detectors. */
data class CategorySpend(
    val category: String,
    /** Spend in the current period. */
    val current: BigDecimal,
    /** The category's trailing per-period average (0 if no history). */
    val average: BigDecimal,
    /** The category's trailing monthly average, for the "no budget on ~€X/mo" detector. */
    val monthlyAverage: BigDecimal,
    val hasBudget: Boolean,
    /** Number of transactions in the period (for small-purchase-leak detection). */
    val count: Int = 0,
)

/** A category burning its weekly budget with days left. */
data class PacedCategory(val name: String, val percentUsed: Int, val remaining: BigDecimal)

/** A category with many small purchases this week. */
data class LeakCategory(val name: String, val count: Int, val total: BigDecimal)

/** A category tracking under its weekly pace. */
data class UnderPaceCategory(val name: String, val under: BigDecimal)

/** Everything the weekly check-in tips need. */
data class WeeklyInputs(
    val spent: BigDecimal,
    val weeklyBudget: BigDecimal?,
    val daysElapsed: Int,
    val daysInWeek: Int,
    val deltaPercentVsLastWeek: Int?,
    val pacedCategory: PacedCategory? = null,
    val leakCategory: LeakCategory? = null,
    val underPaceCategory: UnderPaceCategory? = null,
)

/**
 * The fully-derived, Android-free inputs to [WellbeingEngine]. Built by [WellbeingProvider] from the
 * repositories; kept pure so the whole score + tip pipeline is unit-testable with plain JUnit.
 */
data class WellbeingInputs(
    // Savings
    val hasIncome: Boolean,
    val savingsRatePercent: Int,
    val income: BigDecimal,
    val saved: BigDecimal,
    val netCashflow: BigDecimal,
    // Budgets
    val hasAnyBudget: Boolean,
    val budgetedCount: Int,
    val overCount: Int,
    val overspendTotal: BigDecimal,
    val budgetedTotal: BigDecimal,
    // Trend (null = not enough history)
    val trendPercent: Int?,
    // Subscriptions (share null = not enough history)
    val subsSharePercent: Int?,
    val subsMonthly: BigDecimal,
    val subsCount: Int,
    // Goals
    val goals: List<GoalPace>,
    // Category detail for tip detectors
    val categories: List<CategorySpend>,
    val spend: BigDecimal,
    // Context
    val receiptsLogged: Int,
    val monthsTracked: Int,
    /** Last month's total score, when computable, so the screen can show a trend chip. */
    val previousScore: Int? = null,
)

/** A single component's sub-score. [score] == null means "not counted" (no data → excluded, renormalised). */
data class WellbeingComponent(
    val key: WellbeingComponentKey,
    val weight: Int,
    val score: Int?,
)

/** The scored result. [score]/[band] are null when there isn't enough data yet (first-run state). */
data class WellbeingScore(
    val score: Int?,
    val band: WellbeingBand?,
    val components: List<WellbeingComponent>,
    val trendDeltaVsPrevious: Int?,
) {
    val hasScore: Boolean get() = score != null
    val hasExcludedComponent: Boolean get() = components.any { it.score == null }
}

/**
 * One ranked tip. [type] selects the localized copy in the UI, [id] is the stable dismissal key,
 * and the loosely-typed payload carries the real figures the copy quotes.
 *
 * [projectedGain] (§3.3) is the modelled "+N to your score" — how much acting on this one tip would
 * move the total, computed by [WellbeingEngine.projectedGain]. It is a MODELLED delta under an explicit
 * assumption, NOT a promise, and it is only shown when it clears [WellbeingEngine.showsProjectedGain]
 * (≥ [WellbeingEngine.MIN_PROJECTED_GAIN]); a non-positive value is never rendered. Null for tips with
 * nothing to act on (win-tone) or for the weekly tactical tips.
 */
data class WellbeingTip(
    val type: TipType,
    val id: String,
    val tone: TipTone,
    val amount: BigDecimal? = null,
    val amount2: BigDecimal? = null,
    val percent: Int? = null,
    val count: Int? = null,
    val label: String? = null,
    val projectedGain: Int? = null,
)

/**
 * The band-up nudge (§3.5): the score sits within [WellbeingEngine.BAND_UP_WINDOW] points below the
 * next band boundary (40 / 60 / 80), so a concrete near-term target is worth showing — "3 points to
 * Healthy". Null (no nudge) whenever the score isn't close to a boundary. Pure data; the surface
 * renders it in the accessible warn tone (never the bright band hue) for legibility.
 */
data class BandUp(val pointsAway: Int, val nextBand: WellbeingBand)
