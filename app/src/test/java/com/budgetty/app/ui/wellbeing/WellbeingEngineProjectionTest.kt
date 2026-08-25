package com.budgetty.app.ui.wellbeing

import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Pins the §3 meta-progression math the iOS port must mirror exactly: attributable-tip projections
 * (§3.3) per [TipType], the renormalisation guard that suppresses a non-positive "+N" pill, the
 * projected-gain secondary ranking (§3.4), the band-up nudge thresholds (§3.5), and the budget-streak
 * evidence surfacing (§2.6). Pure engine, plain JUnit.
 */
class WellbeingEngineProjectionTest {

    // All components default to "not scored" so each test isolates exactly the component it acts on.
    private fun inputs(
        hasIncome: Boolean = false,
        savingsRatePercent: Int = 0,
        hasAnyBudget: Boolean = false,
        budgetedCount: Int = 0,
        overCount: Int = 0,
        overspendTotal: String = "0",
        budgetedTotal: String = "0",
        trendPercent: Int? = null,
        subsSharePercent: Int? = null,
        subsCount: Int = 0,
        monthsTracked: Int = 1,
        goals: List<GoalPace> = emptyList(),
        receiptsLogged: Int = 10,
    ) = WellbeingInputs(
        hasIncome = hasIncome, savingsRatePercent = savingsRatePercent,
        income = BigDecimal.ZERO, saved = BigDecimal.ZERO, netCashflow = BigDecimal.ZERO,
        hasAnyBudget = hasAnyBudget, budgetedCount = budgetedCount, overCount = overCount,
        overspendTotal = BigDecimal(overspendTotal), budgetedTotal = BigDecimal(budgetedTotal),
        trendPercent = trendPercent, subsSharePercent = subsSharePercent,
        subsMonthly = BigDecimal.ZERO, subsCount = subsCount, goals = goals,
        categories = emptyList(), spend = BigDecimal("100"),
        receiptsLogged = receiptsLogged, monthsTracked = monthsTracked,
    )

    private fun tip(type: TipType, amount: String? = null, count: Int? = null, label: String? = null) =
        WellbeingTip(
            type, id = type.name, tone = TipTone.OPPORTUNITY,
            amount = amount?.let { BigDecimal(it) }, count = count, label = label,
        )

    // Budget the only scored component: score(4,2,200,1000) = 38.
    private fun budgetOnly() = inputs(
        hasAnyBudget = true, budgetedCount = 4, overCount = 2, overspendTotal = "200", budgetedTotal = "1000",
    )

    // ── §3.3 projection math per TipType ────────────────────────────────────────────

    @Test fun missing_budget_projects_a_positive_gain() {
        // Add a within-plan €200 budget → score(5,2,200,1200) = 49; renormalisation-free → gain 49 − 38.
        val t = tip(TipType.MISSING_BUDGET, amount = "200", label = "Dining")
        assertThat(WellbeingEngine.projectedGain(budgetOnly(), t)).isEqualTo(11)
    }

    @Test fun over_budget_projects_a_positive_gain() {
        // Bring one of two over categories in line: overCount 2→1, its avg overspend (100) removed → 69.
        val gain = WellbeingEngine.projectedGain(budgetOnly(), tip(TipType.OVER_BUDGET, amount = "200", count = 2))
        assertThat(gain).isEqualTo(31)
    }

    @Test fun subscription_cost_projects_a_positive_gain() {
        // Subscriptions the only scored component: share 9% → 56. Cancel one of three → share 6% → 89.
        val i = inputs(subsSharePercent = 9, subsCount = 3, monthsTracked = 2)
        assertThat(WellbeingEngine.projectedGain(i, tip(TipType.SUBSCRIPTION_COST, count = 3))).isEqualTo(33)
    }

    @Test fun goal_off_track_projects_a_positive_gain() {
        // Two goals (one behind=40, one on-pace=100 → mean 70). Bring the behind one on pace → 100.
        val goals = listOf(
            GoalPace("Vacation", reached = false, behind = true),
            GoalPace("Car", reached = false, behind = false),
        )
        val gain = WellbeingEngine.projectedGain(inputs(goals = goals), tip(TipType.GOAL_OFF_TRACK, label = "Vacation"))
        assertThat(gain).isEqualTo(30)
    }

    @Test fun no_goal_projects_a_positive_gain_when_other_components_sit_below_the_new_goal() {
        // Savings 60 + Budget 30 → mean 45. A new on-pace goal (100, weight 20) ENTERS → mean 61.
        val i = inputs(
            hasIncome = true, savingsRatePercent = 8,
            hasAnyBudget = true, budgetedCount = 2, overCount = 1, overspendTotal = "50", budgetedTotal = "200",
        )
        assertThat(WellbeingEngine.projectedGain(i, tip(TipType.NO_GOAL))).isEqualTo(16)
    }

    // ── §3.3 the renormalisation trap — a non-positive projection is NEVER shown ─────

    @Test fun no_goal_that_models_a_non_positive_gain_is_suppressed() {
        // The renormalisation trap: the only scored component is already at 100 (a 20%+ savings rate),
        // so adding a new goal at 100 leaves the renormalised mean at 100 — a genuinely good action that
        // models to ZERO (and, with a lower entering component, could model NEGATIVE). The engine returns
        // that honest delta; the guard is what stops a "+0"/"−N" pill ever reaching the user.
        val i = inputs(hasIncome = true, savingsRatePercent = 25) // savings = 100, the only scored component
        val gain = WellbeingEngine.projectedGain(i, tip(TipType.NO_GOAL))
        assertThat(gain).isNotNull()
        assertThat(gain!!).isAtMost(0)
        assertThat(WellbeingEngine.showsProjectedGain(gain)).isFalse() // pill suppressed
    }

    @Test fun shows_projected_gain_gate_hides_noise_and_never_shows_a_negative() {
        assertThat(WellbeingEngine.showsProjectedGain(null)).isFalse()
        assertThat(WellbeingEngine.showsProjectedGain(-3)).isFalse() // never render "−N"
        assertThat(WellbeingEngine.showsProjectedGain(0)).isFalse()
        assertThat(WellbeingEngine.showsProjectedGain(1)).isFalse() // < 2 = noise
        assertThat(WellbeingEngine.showsProjectedGain(2)).isTrue()
        assertThat(WellbeingEngine.showsProjectedGain(6)).isTrue()
    }

    @Test fun tips_carry_no_projection_before_the_month_is_scored() {
        // First-run: below the scoring floor of receipts → no "+N" pill even on an actionable tip.
        val i = inputs(
            hasAnyBudget = true, budgetedCount = 2, overCount = 1, overspendTotal = "50",
            budgetedTotal = "200", receiptsLogged = 4,
        )
        assertThat(WellbeingEngine.tips(i).all { it.projectedGain == null }).isTrue()
    }

    @Test fun tips_carry_a_projection_once_scored() {
        val i = inputs(
            hasAnyBudget = true, budgetedCount = 2, overCount = 1, overspendTotal = "50",
            budgetedTotal = "200", receiptsLogged = 10,
        )
        assertThat(WellbeingEngine.tips(i).any { it.projectedGain != null }).isTrue()
    }

    @Test fun win_tone_and_unmodelled_tips_have_no_projection() {
        val i = inputs(hasIncome = true, savingsRatePercent = 20)
        fun gainOf(type: TipType, tone: TipTone) = WellbeingEngine.projectedGain(i, WellbeingTip(type, "x", tone))
        assertThat(gainOf(TipType.SAVINGS_WIN, TipTone.WIN)).isNull()
        assertThat(gainOf(TipType.CATEGORY_SPIKE, TipTone.CAUTION)).isNull()
        assertThat(gainOf(TipType.NEGATIVE_CASHFLOW, TipTone.ALERT)).isNull()
    }

    // ── §3.4 rank by projected gain (secondary key, after severity) ──────────────────

    @Test fun rank_breaks_a_tone_tie_by_projected_gain() {
        val tips = listOf(
            WellbeingTip(TipType.MISSING_BUDGET, "small", TipTone.OPPORTUNITY, projectedGain = 2),
            WellbeingTip(TipType.NO_GOAL, "big", TipTone.OPPORTUNITY, projectedGain = 9),
        )
        assertThat(WellbeingEngine.rank(tips, cap = 5).map { it.id }).containsExactly("big", "small").inOrder()
    }

    @Test fun rank_keeps_severity_above_projected_gain() {
        val tips = listOf(
            WellbeingTip(TipType.MISSING_BUDGET, "opp", TipTone.OPPORTUNITY, projectedGain = 50),
            WellbeingTip(TipType.OVER_BUDGET, "alert", TipTone.ALERT, projectedGain = 2),
        )
        assertThat(WellbeingEngine.rank(tips, cap = 5).first().id).isEqualTo("alert")
    }

    // ── §3.5 band-up nudge thresholds ────────────────────────────────────────────────

    @Test fun band_up_shows_within_three_points_of_a_boundary() {
        assertThat(WellbeingEngine.bandUp(57)).isEqualTo(BandUp(3, WellbeingBand.HEALTHY))
        assertThat(WellbeingEngine.bandUp(58)).isEqualTo(BandUp(2, WellbeingBand.HEALTHY))
        assertThat(WellbeingEngine.bandUp(38)).isEqualTo(BandUp(2, WellbeingBand.GETTING_THERE))
        assertThat(WellbeingEngine.bandUp(77)).isEqualTo(BandUp(3, WellbeingBand.THRIVING))
        assertThat(WellbeingEngine.bandUp(37)).isEqualTo(BandUp(3, WellbeingBand.GETTING_THERE))
    }

    @Test fun band_up_suppressed_outside_the_window_or_at_the_top() {
        assertThat(WellbeingEngine.bandUp(56)).isNull() // 4 away from 60
        assertThat(WellbeingEngine.bandUp(60)).isNull() // just entered Healthy; next boundary 20 away
        assertThat(WellbeingEngine.bandUp(72)).isNull() // the healthy state draws no nudge
        assertThat(WellbeingEngine.bandUp(80)).isNull() // no boundary above 80
        assertThat(WellbeingEngine.bandUp(100)).isNull()
    }

    // ── §2.6 budget-streak evidence surfacing ────────────────────────────────────────

    @Test fun budget_streak_evidence_keeps_surfaced_streaks_longest_first_capped_at_two() {
        val streaks = listOf(
            Streak(StreakKind.BUDGET_MONTH, "A", current = 1, best = 3, periodsChecked = 6, liveOnTrack = true),
            Streak(StreakKind.BUDGET_MONTH, "B", current = 4, best = 5, periodsChecked = 6, liveOnTrack = true),
            Streak(StreakKind.BUDGET_MONTH, "C", current = 2, best = 2, periodsChecked = 6, liveOnTrack = false),
            Streak(StreakKind.BUDGET_MONTH, "D", current = 3, best = 4, periodsChecked = 6, liveOnTrack = true),
        )
        val evidence = WellbeingEngine.budgetStreakEvidence(streaks)
        assertThat(evidence.map { it.label }).containsExactly("B", "D").inOrder() // 4, then 3; "C" capped out
    }
}
