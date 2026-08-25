package com.budgetty.app.ui.streaks

import java.math.BigDecimal

/**
 * A purchased line reduced to just what a streak needs, tagged with the [periodIndex] it falls in.
 * Index 0 is the MOST RECENT CLOSED period, increasing into the past (1 = the period before it, …).
 * The caller owns the timestamp → [periodIndex] mapping (pay-cycle months for [StreakKind.BUDGET_MONTH],
 * locale weeks for [StreakKind.BUDGET_WEEK]), so the engine stays period-unit-agnostic and Android-free.
 * [amount] is the NET line contribution (price × quantity); per-receipt paid adjustments (tax on top,
 * fees, discounts) are supplied separately via [BudgetStreakInput.monthlyAdjustmentByPeriod] because
 * they apply to the whole-budget scope only, never to a per-category scope.
 */
data class StreakTxn(
    val periodIndex: Int,
    val category: String,
    val amount: BigDecimal,
)

/** The in-flight (open) period's spend, used only to derive [Streak.liveOnTrack] — never counted. */
data class LiveBudgetPeriod(
    val transactions: List<StreakTxn>,
    /** Whole-budget paid adjustment for the open period (added to the monthly scope only). */
    val monthlyAdjustment: BigDecimal = BigDecimal.ZERO,
)

/**
 * Everything the engine needs to compute budget streaks in one pass. [transactions] are already tagged
 * with a [StreakTxn.periodIndex]; anything outside `0 until MAX_STREAK` is ignored. Per-category budgets
 * take precedence over [monthlyBudget] (mirrors the app's budget model): when any category budget is
 * set, the monthly scope is not used; otherwise the single monthly scope is.
 */
data class BudgetStreakInput(
    val transactions: List<StreakTxn>,
    val categoryBudgets: Map<String, BigDecimal>,
    val monthlyBudget: BigDecimal?,
    val kind: StreakKind,
    /** Label for the single whole-budget scope when only a monthly budget is set. */
    val monthlyLabel: String = "MONTHLY",
    /** periodIndex → whole-budget paid adjustment for that closed period (net + tax/fees − discount). */
    val monthlyAdjustmentByPeriod: Map<Int, BigDecimal> = emptyMap(),
    val live: LiveBudgetPeriod? = null,
)

/** A single closed (or the live) buying-limit window: how many matched, and whether it had any data. */
data class LimitWindow(
    val count: Int,
    /** False when the window held no receipts at all — "no data", never scored as a met window. */
    val hasData: Boolean,
)

/**
 * Everything the engine needs for one buying limit's streak. [closedWindows] index 0 = the most recent
 * CLOSED window, increasing into the past. Keyword matching + counting is [ui.util.BuyingLimitCounter]'s
 * job upstream; the engine only decides met / missed / no-data and walks it.
 */
data class LimitStreakInput(
    val label: String,
    val cap: Int,
    val closedWindows: List<LimitWindow>,
    val live: LimitWindow? = null,
)

/**
 * Pure, on-device outcome-streak math. No Android, no Room, no network — it takes plain data in and
 * returns [Streak]s, so it is unit-testable in isolation and ports 1:1 to iOS (mirrors
 * [com.budgetty.app.ui.wellbeing.WellbeingEngine] and [com.budgetty.app.ui.util.BuyingLimitCounter]).
 *
 * The model, from §2 of the retention spec:
 *  - **Per-scope, not all-or-nothing** (§2.2): one streak per budgeted category, or a single monthly
 *    scope, plus one per buying limit. [allScopesStreak] additionally reproduces the legacy every-scope
 *    aggregate that [com.budgetty.app.ui.recap.RecapProvider] renders in its monthly recap.
 *  - **Closed periods only** (§2.3): [Streak.current] counts completed periods; the open period feeds
 *    [Streak.liveOnTrack] and is never counted.
 *  - **Strict reset + always-computed best** (§2.5): a miss resets [Streak.current] to 0; [Streak.best]
 *    is the longest met run within the [MAX_STREAK] window, so no new persistence is needed.
 *  - **No-data ≠ met** : a closed period with no receipts at all is [PeriodOutcome.NO_DATA] and breaks
 *    a run, whereas a budgeted scope with a budget and zero spend in a period that DID have receipts is
 *    [PeriodOutcome.MET]. These two must never be conflated.
 *  - **One pass** (§2.8): transactions are grouped by `(periodIndex, category)` exactly once, folded
 *    into per-scope per-period totals, then each scope is walked backwards — never a full budget-outcome
 *    re-derivation per period per scope.
 *
 * Copy is the surface's concern; this object contains no user-facing strings and no loss framing.
 */
object StreakEngine {

    /** History window: streaks look back at most this many closed periods, and [Streak.best] within it. */
    const val MAX_STREAK = 24

    /** Below this a "streak" is just "this period" — callers only surface [Streak.current] at or above it (§2.7). */
    const val MIN_TO_SURFACE = 2

    /** A single closed period's result for one scope. [NO_DATA] is a period with no receipts at all. */
    private enum class PeriodOutcome { MET, MISSED, NO_DATA }

    /** A budgeted scope reduced to its per-closed-period outcomes and whether the open period is on track. */
    private class Scope(
        val label: String,
        val outcomes: List<PeriodOutcome>,
        val liveOnTrack: Boolean,
    )

    /** Transactions grouped once by `(periodIndex, category)`, plus which periods held any data. */
    private class Grouped(
        val netByPeriodCat: Map<Int, Map<String, BigDecimal>>,
        val periodsWithData: Set<Int>,
        /** Highest closed-period index to scan (inclusive), bounded by [MAX_STREAK]; −1 when no data. */
        val maxPeriod: Int,
    )

    // ── Public API ────────────────────────────────────────────────────────────────

    /** Keeps only streaks worth showing (§2.7): [Streak.current] ≥ [MIN_TO_SURFACE]. */
    fun surfaced(streaks: List<Streak>): List<Streak> = streaks.filter { it.current >= MIN_TO_SURFACE }

    /**
     * One [Streak] per budgeted scope: per category when any category budget is set, otherwise a single
     * monthly-budget scope. Never all-or-nothing — one over-budget category doesn't zero another's run.
     */
    fun budgetStreaks(input: BudgetStreakInput): List<Streak> {
        val grouped = group(input.transactions)
        return scopesOf(input, grouped).map { buildStreak(input.kind, it.label, it.outcomes, it.liveOnTrack) }
    }

    /**
     * The legacy every-scope aggregate: consecutive closed periods where EVERY budgeted scope stayed
     * under (a period with no receipts breaks it). This is exactly what
     * [com.budgetty.app.ui.recap.RecapProvider.streakMonths] returned — re-sourced here so there is one
     * implementation. [Streak.current] is that count; [Streak.best] the best such run in the window.
     */
    fun allScopesStreak(input: BudgetStreakInput): Streak {
        val grouped = group(input.transactions)
        val scopes = scopesOf(input, grouped)
        if (scopes.isEmpty()) return Streak(input.kind, input.monthlyLabel, 0, 0, 0, false)
        val length = scopes.first().outcomes.size
        val combined = (0 until length).map { i ->
            val column = scopes.map { it.outcomes[i] }
            when {
                column.any { it == PeriodOutcome.NO_DATA } -> PeriodOutcome.NO_DATA
                column.all { it == PeriodOutcome.MET } -> PeriodOutcome.MET
                else -> PeriodOutcome.MISSED
            }
        }
        val liveOnTrack = input.live != null && scopes.all { it.liveOnTrack }
        return buildStreak(input.kind, input.monthlyLabel, combined, liveOnTrack)
    }

    /** One buying limit's streak: consecutive closed windows whose matched count stayed at or under the cap. */
    fun limitStreak(input: LimitStreakInput): Streak {
        val outcomes = input.closedWindows.take(MAX_STREAK).map { w ->
            when {
                !w.hasData -> PeriodOutcome.NO_DATA
                w.count <= input.cap -> PeriodOutcome.MET
                else -> PeriodOutcome.MISSED
            }
        }
        // The open window is on track whenever it is still within cap — zero purchases counts as on track.
        val liveOnTrack = input.live?.let { it.count <= input.cap } ?: false
        return buildStreak(StreakKind.LIMIT, input.label, outcomes, liveOnTrack)
    }

    // ── Core walk ─────────────────────────────────────────────────────────────────

    /**
     * Walks one scope's closed-period [outcomes] (index 0 = most recent closed) into the past:
     *  - [Streak.current] = consecutive [PeriodOutcome.MET] from index 0, stopping at the first miss or gap.
     *  - [Streak.best] = the longest met run anywhere in the window (a miss OR a gap breaks a run).
     *  - [Streak.periodsChecked] = closed periods that actually held data ([PeriodOutcome.NO_DATA] excluded).
     */
    private fun buildStreak(
        kind: StreakKind,
        label: String,
        outcomes: List<PeriodOutcome>,
        liveOnTrack: Boolean,
    ): Streak {
        var current = 0
        var currentOpen = true
        var run = 0
        var best = 0
        var checked = 0
        for (outcome in outcomes) {
            when (outcome) {
                PeriodOutcome.MET -> {
                    run += 1
                    if (run > best) best = run
                    if (currentOpen) current += 1
                    checked += 1
                }
                PeriodOutcome.MISSED -> {
                    run = 0
                    currentOpen = false
                    checked += 1
                }
                PeriodOutcome.NO_DATA -> {
                    run = 0
                    currentOpen = false
                }
            }
        }
        return Streak(kind, label, current, best, checked, liveOnTrack)
    }

    // ── One-pass grouping + scope derivation (§2.8) ─────────────────────────────────

    private fun group(transactions: List<StreakTxn>): Grouped {
        val inWindow = transactions.filter { it.periodIndex in 0 until MAX_STREAK }
        val netByPeriodCat = inWindow.groupBy { it.periodIndex }
            .mapValues { (_, list) ->
                list.groupBy { it.category }
                    .mapValues { (_, lines) -> lines.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount } }
            }
        val periods = netByPeriodCat.keys
        val maxPeriod = (periods.maxOrNull() ?: -1).coerceAtMost(MAX_STREAK - 1)
        return Grouped(netByPeriodCat, periods, maxPeriod)
    }

    private fun scopesOf(input: BudgetStreakInput, grouped: Grouped): List<Scope> {
        val liveNetByCat = input.live?.transactions
            ?.groupBy { it.category }
            ?.mapValues { (_, lines) -> lines.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount } }
            ?: emptyMap()
        val periods = 0..grouped.maxPeriod
        return if (input.categoryBudgets.isNotEmpty()) {
            input.categoryBudgets.map { (category, budget) ->
                val outcomes = periods.map { p ->
                    outcomeAt(grouped, p, grouped.netByPeriodCat[p]?.get(category) ?: BigDecimal.ZERO, budget)
                }
                Scope(category, outcomes, input.live != null && (liveNetByCat[category] ?: BigDecimal.ZERO) <= budget)
            }
        } else {
            val monthly = input.monthlyBudget ?: return emptyList()
            val outcomes = periods.map { p ->
                outcomeAt(grouped, p, monthlySpendAt(grouped, input, p), monthly)
            }
            val liveMonthly = liveNetByCat.values.fold(BigDecimal.ZERO) { acc, v -> acc + v } +
                (input.live?.monthlyAdjustment ?: BigDecimal.ZERO)
            listOf(Scope(input.monthlyLabel, outcomes, input.live != null && liveMonthly <= monthly))
        }
    }

    /** Whole-budget spend for closed period [p]: net across all categories plus that period's paid adjustment. */
    private fun monthlySpendAt(grouped: Grouped, input: BudgetStreakInput, p: Int): BigDecimal =
        (grouped.netByPeriodCat[p]?.values?.fold(BigDecimal.ZERO) { acc, v -> acc + v } ?: BigDecimal.ZERO) +
            (input.monthlyAdjustmentByPeriod[p] ?: BigDecimal.ZERO)

    private fun outcomeAt(grouped: Grouped, p: Int, spend: BigDecimal, budget: BigDecimal): PeriodOutcome = when {
        p !in grouped.periodsWithData -> PeriodOutcome.NO_DATA
        spend <= budget -> PeriodOutcome.MET
        else -> PeriodOutcome.MISSED
    }
}
