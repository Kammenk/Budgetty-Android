package com.budgetty.app.ui.wellbeing

import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure, on-device scoring + tips for the Wellbeing screen. No Android, no Room, no network — every
 * value comes from metrics the app already derives (savings rate, budgets, goals, spend trend,
 * subscriptions), so this object is unit-testable in isolation (mirrors [com.budgetty.app.ui.util.SubscriptionDetector]).
 *
 * The single 0–100 score is a weight-renormalising mean of up to five component sub-scores; a
 * component with no data ([WellbeingComponent.score] == null) is left out and the remaining weights
 * carry the score. Tips are ranked candidates that each quote a real figure and deep-link to the fix.
 */
object WellbeingEngine {

    // Weights (from the approved design). Renormalised across whichever components have data.
    const val W_SAVINGS = 25
    const val W_BUDGET = 25
    const val W_GOALS = 20
    const val W_TREND = 15
    const val W_SUBSCRIPTIONS = 15

    /** Below this many logged receipts we can't score meaningfully → first-run state. */
    const val MIN_RECEIPTS_TO_SCORE = 5

    /**
     * A subscription share of 0% only counts as a genuine "no subscriptions" win once there's at least
     * this many months of history. Before that, a brand-new user simply hasn't accrued subscription
     * data — scoring the absence as a perfect 100 would hand them a bogus top score, since it can be
     * their only scored component. See [subscriptionsComponentScore].
     */
    const val MIN_MONTHS_FOR_ZERO_SUBS = 2

    const val MONTHLY_TIP_CAP = 5
    const val WEEKLY_TIP_CAP = 3

    fun band(score: Int): WellbeingBand = when {
        score >= 80 -> WellbeingBand.THRIVING
        score >= 60 -> WellbeingBand.HEALTHY
        score >= 40 -> WellbeingBand.GETTING_THERE
        else -> WellbeingBand.NEEDS_WORK
    }

    /** Bar/label tier for a single component sub-score: ≥70 good, 40–69 careful, <40 over. */
    fun tier(score: Int): WellbeingTier = when {
        score >= 70 -> WellbeingTier.GOOD
        score >= 40 -> WellbeingTier.WARN
        else -> WellbeingTier.BAD
    }

    private fun clampScore(v: Double): Int = v.roundToInt().coerceIn(0, 100)

    // ── Component sub-scores (0–100, higher = better) ─────────────────────────────

    /** Full marks at ≥20% kept; 0 at ≤−10%; linear between. */
    fun savingsScore(ratePercent: Int): Int =
        clampScore((ratePercent + 10).coerceIn(0, 30) / 30.0 * 100.0)

    /**
     * Blends how many budgeted scopes stayed within plan (60%) with the size of the overspend
     * relative to what was budgeted (40%). Full marks when nothing is over.
     */
    fun budgetScore(budgetedCount: Int, overCount: Int, overspend: BigDecimal, budgeted: BigDecimal): Int {
        if (budgetedCount <= 0 || budgeted.signum() <= 0) return 100
        val within = (budgetedCount - overCount).coerceAtLeast(0).toDouble() / budgetedCount
        val overFrac = (overspend.toDouble() / budgeted.toDouble()).coerceIn(0.0, 1.0)
        return clampScore(within * 60.0 + (1.0 - (overFrac * 4).coerceAtMost(1.0)) * 40.0)
    }

    /** Average of per-goal marks: on-pace (or reached) = 100, behind = 40. Null when no active goal. */
    fun goalsScore(goals: List<GoalPace>): Int? {
        if (goals.isEmpty()) return null
        val each = goals.map { if (it.reached || !it.behind) 100 else 40 }
        return clampScore(each.average())
    }

    /** Flat or falling spend scores full marks; each 1% above the trailing average costs 3 points. */
    fun trendScore(percentVsAverage: Int): Int =
        clampScore(100.0 - percentVsAverage.coerceAtLeast(0) * 3.0)

    /** Under 5% of spend on subscriptions scores full marks; each 1% above costs 11 points. */
    fun subscriptionsScore(sharePercent: Int): Int =
        clampScore(100.0 - (sharePercent - 5).coerceAtLeast(0) * 11.0)

    /**
     * The subscriptions component's sub-score, or null when there isn't enough signal to score it — a
     * 0% share only counts once there's at least [MIN_MONTHS_FOR_ZERO_SUBS] months of history (see the
     * const). This is what stops a brand-new user (no income/budget/goals/trend yet, no subscriptions)
     * from scoring a bogus 100 off their only scored component.
     */
    fun subscriptionsComponentScore(inputs: WellbeingInputs): Int? {
        if (inputs.subsCount == 0 && inputs.monthsTracked < MIN_MONTHS_FOR_ZERO_SUBS) return null
        return inputs.subsSharePercent?.let { subscriptionsScore(it) }
    }

    // ── Aggregate ─────────────────────────────────────────────────────────────────

    /** Weighted, renormalising mean over the components that have a score. Null if none do. */
    fun aggregate(components: List<WellbeingComponent>): Int? {
        val scored = components.filter { it.score != null }
        val totalWeight = scored.sumOf { it.weight }
        if (totalWeight <= 0) return null
        val sum = scored.sumOf { (it.score!! * it.weight).toDouble() }
        return (sum / totalWeight).roundToInt().coerceIn(0, 100)
    }

    /** Builds the five components (order matches the design: Savings, Budget, Trend, Subscriptions, Goals). */
    fun components(inputs: WellbeingInputs): List<WellbeingComponent> = listOf(
        WellbeingComponent(
            key = WellbeingComponentKey.SAVINGS,
            weight = W_SAVINGS,
            score = if (inputs.hasIncome) savingsScore(inputs.savingsRatePercent) else null,
        ),
        WellbeingComponent(
            key = WellbeingComponentKey.BUDGET,
            weight = W_BUDGET,
            score = if (inputs.hasAnyBudget)
                budgetScore(inputs.budgetedCount, inputs.overCount, inputs.overspendTotal, inputs.budgetedTotal)
            else null,
        ),
        WellbeingComponent(
            key = WellbeingComponentKey.TREND,
            weight = W_TREND,
            score = inputs.trendPercent?.let { trendScore(it) },
        ),
        WellbeingComponent(
            key = WellbeingComponentKey.SUBSCRIPTIONS,
            weight = W_SUBSCRIPTIONS,
            score = subscriptionsComponentScore(inputs),
        ),
        WellbeingComponent(
            key = WellbeingComponentKey.GOALS,
            weight = W_GOALS,
            score = goalsScore(inputs.goals),
        ),
    )

    /** The full score result, or null total when there isn't enough data to score yet (first run). */
    fun score(inputs: WellbeingInputs): WellbeingScore {
        val comps = components(inputs)
        val total = if (inputs.receiptsLogged >= MIN_RECEIPTS_TO_SCORE) aggregate(comps) else null
        return WellbeingScore(
            score = total,
            band = total?.let { band(it) },
            components = comps,
            trendDeltaVsPrevious = inputs.previousScore?.let { prev -> total?.let { it - prev } },
        )
    }

    // ── Tips ────────────────────────────────────────────────────────────────────

    private val severity = mapOf(
        TipTone.ALERT to 0, TipTone.CAUTION to 1, TipTone.OPPORTUNITY to 2, TipTone.WIN to 3,
    )

    /** Ranks candidates alert→caution→opportunity, caps the list, always keeping one win when available. */
    fun rank(candidates: List<WellbeingTip>, cap: Int): List<WellbeingTip> {
        val ordered = candidates.sortedBy { severity[it.tone] ?: 9 }
        val wins = ordered.filter { it.tone == TipTone.WIN }
        val others = ordered.filter { it.tone != TipTone.WIN }
        return when {
            others.isEmpty() -> wins.take(cap)
            wins.isEmpty() -> others.take(cap)
            else -> others.take((cap - 1).coerceAtLeast(1)) + wins.take(1)
        }
    }

    fun tips(inputs: WellbeingInputs): List<WellbeingTip> = rank(monthlyCandidates(inputs), MONTHLY_TIP_CAP)

    fun weeklyTips(week: WeeklyInputs): List<WellbeingTip> = rank(weeklyCandidates(week), WEEKLY_TIP_CAP)

    private fun monthlyCandidates(inputs: WellbeingInputs): List<WellbeingTip> {
        val out = mutableListOf<WellbeingTip>()

        // Alert — spent more than earned.
        if (inputs.hasIncome && inputs.netCashflow.signum() < 0) {
            out += WellbeingTip(TipType.NEGATIVE_CASHFLOW, "negative_cashflow", TipTone.ALERT, amount = inputs.netCashflow.abs())
        }
        // Alert — over budget in one or more categories.
        if (inputs.overCount > 0 && inputs.overspendTotal.signum() > 0) {
            out += WellbeingTip(TipType.OVER_BUDGET, "over_budget", TipTone.ALERT, amount = inputs.overspendTotal, count = inputs.overCount)
        }
        // Caution — a spending category well above its own average.
        inputs.categories
            .filter { it.average.signum() > 0 && it.current > it.average.multiply(BigDecimal("1.30")) }
            .maxByOrNull { (it.current - it.average).toDouble() }
            ?.let { c ->
                val pct = ((c.current.toDouble() / c.average.toDouble() - 1.0) * 100).roundToInt()
                out += WellbeingTip(TipType.CATEGORY_SPIKE, "spike:${c.category}", TipTone.CAUTION, label = c.category, percent = pct, amount = c.current, amount2 = c.average)
            }
        // Caution — subscription load is heavy.
        if (inputs.subsSharePercent != null && inputs.subsSharePercent >= 8 && inputs.subsCount > 0) {
            out += WellbeingTip(TipType.SUBSCRIPTION_COST, "subs_cost", TipTone.CAUTION, amount = inputs.subsMonthly, count = inputs.subsCount, percent = inputs.subsSharePercent)
        }
        // Opportunity — a material category with no budget.
        inputs.categories
            .filter { !it.hasBudget && it.monthlyAverage.signum() > 0 && it.monthlyAverage >= BigDecimal("50") }
            .maxByOrNull { it.monthlyAverage.toDouble() }
            ?.let { c -> out += WellbeingTip(TipType.MISSING_BUDGET, "missing_budget:${c.category}", TipTone.OPPORTUNITY, label = c.category, amount = c.monthlyAverage) }
        // Opportunity — no savings goal yet.
        if (inputs.goals.isEmpty()) {
            out += WellbeingTip(TipType.NO_GOAL, "no_goal", TipTone.OPPORTUNITY)
        }
        // Opportunity — a goal is off track.
        inputs.goals.firstOrNull { it.behind && !it.reached }?.let { g ->
            out += WellbeingTip(TipType.GOAL_OFF_TRACK, "goal_off:${g.name}", TipTone.OPPORTUNITY, label = g.name, count = g.monthsLate)
        }
        // Win — a healthy savings rate.
        if (inputs.hasIncome && inputs.savingsRatePercent >= 15) {
            out += WellbeingTip(TipType.SAVINGS_WIN, "savings_win", TipTone.WIN, percent = inputs.savingsRatePercent, amount = inputs.saved)
        }
        // Win — a category clearly down.
        inputs.categories
            .filter { it.average.signum() > 0 && it.current < it.average.multiply(BigDecimal("0.85")) && it.average >= BigDecimal("40") }
            .maxByOrNull { (it.average - it.current).toDouble() }
            ?.let { c ->
                val pct = ((1.0 - c.current.toDouble() / c.average.toDouble()) * 100).roundToInt()
                out += WellbeingTip(TipType.CATEGORY_IMPROVED, "improved:${c.category}", TipTone.WIN, label = c.category, percent = pct)
            }
        return out
    }

    private fun weeklyCandidates(week: WeeklyInputs): List<WellbeingTip> {
        val out = mutableListOf<WellbeingTip>()
        // Caution — a category is burning its weekly budget with days left.
        week.pacedCategory?.let { p ->
            if (p.percentUsed >= 70) {
                out += WellbeingTip(TipType.BUDGET_PACE, "pace:${p.name}", TipTone.CAUTION, label = p.name, percent = p.percentUsed, amount = p.remaining)
            }
        }
        // Caution — a leak of many small purchases.
        week.leakCategory?.let { l ->
            out += WellbeingTip(TipType.SMALL_PURCHASE_LEAK, "leak:${l.name}", TipTone.CAUTION, label = l.name, count = l.count, amount = l.total)
        }
        // Win — a category is under pace this week.
        week.underPaceCategory?.let { u ->
            out += WellbeingTip(TipType.UNDER_PACE_WIN, "under:${u.name}", TipTone.WIN, label = u.name, amount = u.under)
        }
        return out
    }
}
