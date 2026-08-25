package com.budgetty.app.ui.wellbeing

import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import java.math.BigDecimal
import java.math.RoundingMode
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

    /** How close (in points) to a band boundary the score must be for the band-up nudge to show (§3.5). */
    const val BAND_UP_WINDOW = 3

    /** Below this a modelled "+N to your score" is noise (or a renormalisation artefact) and is hidden (§3.3). */
    const val MIN_PROJECTED_GAIN = 2

    /** Up to this many budget streaks show as evidence under the Budget component (§2.6). */
    const val MAX_STREAK_EVIDENCE = 2

    private val BAND_BOUNDARIES = listOf(40, 60, 80)

    fun band(score: Int): WellbeingBand = when {
        score >= 80 -> WellbeingBand.THRIVING
        score >= 60 -> WellbeingBand.HEALTHY
        score >= 40 -> WellbeingBand.GETTING_THERE
        else -> WellbeingBand.NEEDS_WORK
    }

    /**
     * The band-up nudge target (§3.5): the next band boundary (40 / 60 / 80) when the score sits within
     * [BAND_UP_WINDOW] points below it, else null. "57 → 3 points to Healthy"; suppressed at e.g. 72.
     */
    fun bandUp(score: Int): BandUp? {
        val boundary = BAND_BOUNDARIES.firstOrNull { it > score && it - score <= BAND_UP_WINDOW } ?: return null
        return BandUp(pointsAway = boundary - score, nextBand = band(boundary))
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

    // ── Attributable tips: "what this is worth" (§3.3) ──────────────────────────────

    /** The tip types with a modelled single-action projection (per the §3.3 table). Others get no pill. */
    private val ACTIONABLE_PROJECTION = setOf(
        TipType.MISSING_BUDGET, TipType.OVER_BUDGET, TipType.SUBSCRIPTION_COST,
        TipType.NO_GOAL, TipType.GOAL_OFF_TRACK,
    )

    /**
     * Whether a tip's [WellbeingTip.projectedGain] clears the display floor and its pill may be shown.
     * Suppresses noise (< 2) AND — critically — any non-positive projection from the renormalisation
     * trap (see [projectedGain]); a "+1", "+0" or "−N" pill is never rendered.
     */
    fun showsProjectedGain(gain: Int?): Boolean = gain != null && gain >= MIN_PROJECTED_GAIN

    /**
     * The modelled "+N to your score" for an actionable [tip] (§3.3): the delta if the user took exactly
     * that one action, computed by re-running [aggregate] with the affected component's sub-score replaced
     * by its post-action value (per the §3.3 table). Null for tips with nothing to act on (win-tone, and
     * the alert/spike tips that have no clean single-component model), or when there is no current total
     * to move against.
     *
     * This is a MODELLED delta under an EXPLICIT ASSUMPTION (the action lands exactly as described) — NOT
     * a promise.
     *
     * ⚠️ Renormalisation trap: [aggregate] is a weight-renormalising mean, so an action that ADDS a
     * previously-null component (NO_GOAL, or MISSING_BUDGET for a user with no budgets at all) shifts the
     * denominator. When the entering component sits below the current renormalised mean, the total can
     * move by nothing — or, in principle, DOWNWARD. The returned delta may therefore be zero or negative;
     * callers MUST gate display through [showsProjectedGain], which drops everything below
     * [MIN_PROJECTED_GAIN] so a "−N" is never shown next to correct advice.
     */
    fun projectedGain(inputs: WellbeingInputs, tip: WellbeingTip): Int? {
        if (tip.type !in ACTIONABLE_PROJECTION) return null
        val base = aggregate(components(inputs)) ?: return null
        val projected = aggregate(projectedComponents(inputs, tip)) ?: return null
        return projected - base
    }

    /** The component list after modelling [tip]'s single action (§3.3 table); unaffected components unchanged. */
    private fun projectedComponents(inputs: WellbeingInputs, tip: WellbeingTip): List<WellbeingComponent> {
        val comps = components(inputs).toMutableList()
        fun replace(key: WellbeingComponentKey, newScore: Int) {
            val i = comps.indexOfFirst { it.key == key }
            if (i >= 0) comps[i] = comps[i].copy(score = newScore)
        }
        when (tip.type) {
            // Add a budget for the named category, assumed within plan → budgetedCount + 1, overspend as-is.
            TipType.MISSING_BUDGET -> {
                val newBudget = tip.amount ?: BigDecimal.ZERO
                replace(
                    WellbeingComponentKey.BUDGET,
                    budgetScore(
                        inputs.budgetedCount + 1, inputs.overCount,
                        inputs.overspendTotal, inputs.budgetedTotal + newBudget,
                    ),
                )
            }
            // Bring one over-budget scope back in line → overCount − 1 and its (average) overspend removed.
            TipType.OVER_BUDGET -> if (inputs.overCount > 0) {
                val perOver = inputs.overspendTotal.divide(BigDecimal(inputs.overCount), 2, RoundingMode.HALF_UP)
                replace(
                    WellbeingComponentKey.BUDGET,
                    budgetScore(
                        inputs.budgetedCount, inputs.overCount - 1,
                        (inputs.overspendTotal - perOver).max(BigDecimal.ZERO), inputs.budgetedTotal,
                    ),
                )
            }
            // Cancel one subscription → the share falls proportionally (same spend denominator).
            TipType.SUBSCRIPTION_COST -> inputs.subsSharePercent?.let { share ->
                val count = inputs.subsCount.coerceAtLeast(1)
                val newShare = (share.toDouble() * (count - 1) / count).roundToInt()
                replace(WellbeingComponentKey.SUBSCRIPTIONS, subscriptionsScore(newShare))
            }
            // Create a goal, on pace → goals enters the mean at 100 (the renormalisation case).
            TipType.NO_GOAL -> replace(WellbeingComponentKey.GOALS, 100)
            // Bring the off-track goal back on pace → its mark 40 → 100.
            TipType.GOAL_OFF_TRACK -> {
                val fixed = inputs.goals.map {
                    if (it.name == tip.label && it.behind && !it.reached) it.copy(behind = false) else it
                }
                goalsScore(fixed)?.let { replace(WellbeingComponentKey.GOALS, it) }
            }
            else -> Unit
        }
        return comps
    }

    /**
     * The up-to-[MAX_STREAK_EVIDENCE] budget streaks to list as evidence under the Budget component
     * (§2.6): only those already surfaced (current ≥ [StreakEngine.MIN_TO_SURFACE]), longest current run
     * first. Keeps the "which streaks to show" decision in a pure, testable seam.
     */
    fun budgetStreakEvidence(streaks: List<Streak>): List<Streak> =
        StreakEngine.surfaced(streaks).sortedByDescending { it.current }.take(MAX_STREAK_EVIDENCE)

    // ── Tips ────────────────────────────────────────────────────────────────────

    private val severity = mapOf(
        TipTone.ALERT to 0, TipTone.CAUTION to 1, TipTone.OPPORTUNITY to 2, TipTone.WIN to 3,
    )

    /**
     * Ranks candidates alert→caution→opportunity, caps the list, always keeping one win when available.
     * Secondary key (§3.4): within the same tone, a larger modelled [WellbeingTip.projectedGain] leads,
     * so the top tip is both important AND impactful. Tips with no gain sort as 0 (insertion order kept).
     */
    fun rank(candidates: List<WellbeingTip>, cap: Int): List<WellbeingTip> {
        val ordered = candidates.sortedWith(
            compareBy<WellbeingTip> { severity[it.tone] ?: 9 }.thenByDescending { it.projectedGain ?: 0 },
        )
        val wins = ordered.filter { it.tone == TipTone.WIN }
        val others = ordered.filter { it.tone != TipTone.WIN }
        return when {
            others.isEmpty() -> wins.take(cap)
            wins.isEmpty() -> others.take(cap)
            else -> others.take((cap - 1).coerceAtLeast(1)) + wins.take(1)
        }
    }

    fun tips(inputs: WellbeingInputs): List<WellbeingTip> {
        // A "+N to your score" pill only makes sense once there IS a score to move (§3.3); in the
        // first-run "—" state (too few receipts / no scored component) no gain is attached.
        val scored = inputs.receiptsLogged >= MIN_RECEIPTS_TO_SCORE && aggregate(components(inputs)) != null
        val withGains = monthlyCandidates(inputs).map {
            it.copy(projectedGain = if (scored) projectedGain(inputs, it) else null)
        }
        return rank(withGains, MONTHLY_TIP_CAP)
    }

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
