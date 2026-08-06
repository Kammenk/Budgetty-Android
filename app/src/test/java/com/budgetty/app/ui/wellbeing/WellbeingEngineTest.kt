package com.budgetty.app.ui.wellbeing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class WellbeingEngineTest {

    private fun bd(v: String) = BigDecimal(v)

    private fun inputs(
        hasIncome: Boolean = true,
        savingsRatePercent: Int = 18,
        income: String = "2400",
        saved: String = "432",
        netCashflow: String = "120",
        hasAnyBudget: Boolean = true,
        budgetedCount: Int = 6,
        overCount: Int = 0,
        overspendTotal: String = "0",
        budgetedTotal: String = "1200",
        trendPercent: Int? = -3,
        subsSharePercent: Int? = 9,
        subsMonthly: String = "67",
        subsCount: Int = 3,
        goals: List<GoalPace> = listOf(GoalPace("Vacation", reached = false, behind = false)),
        categories: List<CategorySpend> = emptyList(),
        spend: String = "712",
        receiptsLogged: Int = 18,
        monthsTracked: Int = 6,
        previousScore: Int? = null,
    ) = WellbeingInputs(
        hasIncome = hasIncome, savingsRatePercent = savingsRatePercent, income = bd(income),
        saved = bd(saved), netCashflow = bd(netCashflow), hasAnyBudget = hasAnyBudget,
        budgetedCount = budgetedCount, overCount = overCount, overspendTotal = bd(overspendTotal),
        budgetedTotal = bd(budgetedTotal), trendPercent = trendPercent, subsSharePercent = subsSharePercent,
        subsMonthly = bd(subsMonthly), subsCount = subsCount, goals = goals, categories = categories,
        spend = bd(spend), receiptsLogged = receiptsLogged, monthsTracked = monthsTracked,
        previousScore = previousScore,
    )

    // ── Bands & tiers ─────────────────────────────────────────────────────────────
    @Test fun bands_map_at_the_documented_thresholds() {
        assertThat(WellbeingEngine.band(0)).isEqualTo(WellbeingBand.NEEDS_WORK)
        assertThat(WellbeingEngine.band(39)).isEqualTo(WellbeingBand.NEEDS_WORK)
        assertThat(WellbeingEngine.band(40)).isEqualTo(WellbeingBand.GETTING_THERE)
        assertThat(WellbeingEngine.band(59)).isEqualTo(WellbeingBand.GETTING_THERE)
        assertThat(WellbeingEngine.band(60)).isEqualTo(WellbeingBand.HEALTHY)
        assertThat(WellbeingEngine.band(79)).isEqualTo(WellbeingBand.HEALTHY)
        assertThat(WellbeingEngine.band(80)).isEqualTo(WellbeingBand.THRIVING)
        assertThat(WellbeingEngine.band(100)).isEqualTo(WellbeingBand.THRIVING)
    }

    @Test fun tiers_map_at_70_and_40() {
        assertThat(WellbeingEngine.tier(70)).isEqualTo(WellbeingTier.GOOD)
        assertThat(WellbeingEngine.tier(69)).isEqualTo(WellbeingTier.WARN)
        assertThat(WellbeingEngine.tier(40)).isEqualTo(WellbeingTier.WARN)
        assertThat(WellbeingEngine.tier(39)).isEqualTo(WellbeingTier.BAD)
    }

    // ── Component sub-scores ────────────────────────────────────────────────────────
    @Test fun savings_score_is_full_at_20_zero_at_minus_10_and_monotonic() {
        assertThat(WellbeingEngine.savingsScore(20)).isEqualTo(100)
        assertThat(WellbeingEngine.savingsScore(30)).isEqualTo(100)
        assertThat(WellbeingEngine.savingsScore(-10)).isEqualTo(0)
        assertThat(WellbeingEngine.savingsScore(-50)).isEqualTo(0)
        assertThat(WellbeingEngine.savingsScore(5)).isEqualTo(50)
        assertThat(WellbeingEngine.savingsScore(2)).isLessThan(WellbeingEngine.savingsScore(18))
    }

    @Test fun subscriptions_score_full_under_5_percent_then_falls() {
        assertThat(WellbeingEngine.subscriptionsScore(0)).isEqualTo(100)
        assertThat(WellbeingEngine.subscriptionsScore(5)).isEqualTo(100)
        assertThat(WellbeingEngine.subscriptionsScore(9)).isEqualTo(56)
        assertThat(WellbeingEngine.subscriptionsScore(9)).isGreaterThan(WellbeingEngine.subscriptionsScore(15))
    }

    @Test fun trend_score_full_when_flat_or_down_and_penalises_rises() {
        assertThat(WellbeingEngine.trendScore(-5)).isEqualTo(100)
        assertThat(WellbeingEngine.trendScore(0)).isEqualTo(100)
        assertThat(WellbeingEngine.trendScore(22)).isEqualTo(34)
        assertThat(WellbeingEngine.trendScore(40)).isEqualTo(0)
    }

    @Test fun budget_score_is_full_with_no_overspend() {
        assertThat(WellbeingEngine.budgetScore(6, 0, bd("0"), bd("1200"))).isEqualTo(100)
        // Some overspend must score below full and above zero.
        val partial = WellbeingEngine.budgetScore(6, 3, bd("214"), bd("1200"))
        assertThat(partial).isLessThan(100)
        assertThat(partial).isGreaterThan(0)
    }

    @Test fun goals_score_null_when_none_and_blends_on_pace_with_behind() {
        assertThat(WellbeingEngine.goalsScore(emptyList())).isNull()
        assertThat(WellbeingEngine.goalsScore(listOf(GoalPace("A", false, false)))).isEqualTo(100)
        assertThat(WellbeingEngine.goalsScore(listOf(GoalPace("A", false, true)))).isEqualTo(40)
        assertThat(
            WellbeingEngine.goalsScore(listOf(GoalPace("A", false, false), GoalPace("B", false, true)))
        ).isEqualTo(70)
    }

    // ── Aggregate + renormalisation ─────────────────────────────────────────────────
    @Test fun aggregate_renormalises_over_available_components() {
        val comps = listOf(
            WellbeingComponent(WellbeingComponentKey.SAVINGS, 25, 80),
            WellbeingComponent(WellbeingComponentKey.BUDGET, 25, 60),
            WellbeingComponent(WellbeingComponentKey.TREND, 15, 100),
            WellbeingComponent(WellbeingComponentKey.SUBSCRIPTIONS, 15, 40),
            WellbeingComponent(WellbeingComponentKey.GOALS, 20, null), // excluded
        )
        // (80*25 + 60*25 + 100*15 + 40*15) / (25+25+15+15) = 5600/80 = 70
        assertThat(WellbeingEngine.aggregate(comps)).isEqualTo(70)
    }

    @Test fun aggregate_null_when_no_component_has_data() {
        val comps = WellbeingComponentKey.entries.map { WellbeingComponent(it, 20, null) }
        assertThat(WellbeingEngine.aggregate(comps)).isNull()
    }

    @Test fun score_is_first_run_until_enough_receipts() {
        assertThat(WellbeingEngine.score(inputs(receiptsLogged = 4)).hasScore).isFalse()
        assertThat(WellbeingEngine.score(inputs(receiptsLogged = 5)).hasScore).isTrue()
    }

    @Test fun score_reports_trend_delta_against_previous() {
        val s = WellbeingEngine.score(inputs(previousScore = 68))
        assertThat(s.score).isNotNull()
        assertThat(s.trendDeltaVsPrevious).isEqualTo(s.score!! - 68)
    }

    @Test fun excluded_goal_is_marked_not_counted() {
        val s = WellbeingEngine.score(inputs(goals = emptyList()))
        val goals = s.components.first { it.key == WellbeingComponentKey.GOALS }
        assertThat(goals.score).isNull()
        assertThat(s.hasExcludedComponent).isTrue()
    }

    // ── Tip ranking ─────────────────────────────────────────────────────────────────
    @Test fun rank_orders_by_severity_and_always_keeps_one_win() {
        val tips = listOf(
            WellbeingTip(TipType.SAVINGS_WIN, "w", TipTone.WIN),
            WellbeingTip(TipType.CATEGORY_SPIKE, "c1", TipTone.CAUTION),
            WellbeingTip(TipType.CATEGORY_SPIKE, "c2", TipTone.CAUTION),
            WellbeingTip(TipType.NEGATIVE_CASHFLOW, "a", TipTone.ALERT),
            WellbeingTip(TipType.NO_GOAL, "o", TipTone.OPPORTUNITY),
        )
        val ranked = WellbeingEngine.rank(tips, cap = 3)
        assertThat(ranked).hasSize(3)
        assertThat(ranked.first().tone).isEqualTo(TipTone.ALERT)
        assertThat(ranked.count { it.tone == TipTone.WIN }).isEqualTo(1)
    }

    @Test fun rank_without_wins_just_takes_top_severity() {
        val tips = listOf(
            WellbeingTip(TipType.OVER_BUDGET, "a1", TipTone.ALERT),
            WellbeingTip(TipType.NEGATIVE_CASHFLOW, "a2", TipTone.ALERT),
            WellbeingTip(TipType.NO_GOAL, "o", TipTone.OPPORTUNITY),
        )
        val ranked = WellbeingEngine.rank(tips, cap = 2)
        assertThat(ranked).hasSize(2)
        assertThat(ranked.all { it.tone == TipTone.ALERT }).isTrue()
    }

    // ── End-to-end tips ─────────────────────────────────────────────────────────────
    @Test fun healthy_inputs_yield_a_win_and_no_alert() {
        val tips = WellbeingEngine.tips(
            inputs(
                overCount = 0, overspendTotal = "0", netCashflow = "300",
                categories = listOf(
                    CategorySpend(
                        "Dining", current = bd("182"), average = bd("136"),
                        monthlyAverage = bd("150"), hasBudget = false,
                    ),
                ),
            )
        )
        assertThat(tips.any { it.tone == TipTone.ALERT }).isFalse()
        assertThat(tips.any { it.tone == TipTone.WIN }).isTrue()
        assertThat(tips.any { it.type == TipType.CATEGORY_SPIKE }).isTrue()
    }

    @Test fun overspent_negative_cashflow_and_no_goal_produce_alerts_and_opportunity() {
        val tips = WellbeingEngine.tips(
            inputs(
                savingsRatePercent = 2, saved = "48", netCashflow = "-120",
                overCount = 3, overspendTotal = "214", goals = emptyList(),
            )
        )
        assertThat(tips.any { it.type == TipType.NEGATIVE_CASHFLOW && it.tone == TipTone.ALERT }).isTrue()
        assertThat(tips.any { it.type == TipType.OVER_BUDGET && it.tone == TipTone.ALERT }).isTrue()
        assertThat(tips.any { it.type == TipType.NO_GOAL && it.tone == TipTone.OPPORTUNITY }).isTrue()
        assertThat(tips.first().tone).isEqualTo(TipTone.ALERT)
    }

    @Test fun weekly_tips_flag_pace_leak_and_a_win() {
        val week = WeeklyInputs(
            spent = bd("210"), weeklyBudget = bd("300"), daysElapsed = 5, daysInWeek = 7, deltaPercentVsLastWeek = -12,
            pacedCategory = PacedCategory("Dining", percentUsed = 78, remaining = bd("22")),
            leakCategory = LeakCategory("Coffee", count = 9, total = bd("26")),
            underPaceCategory = UnderPaceCategory("Groceries", under = bd("18")),
        )
        val tips = WellbeingEngine.weeklyTips(week)
        assertThat(tips.any { it.type == TipType.BUDGET_PACE }).isTrue()
        assertThat(tips.any { it.type == TipType.UNDER_PACE_WIN && it.tone == TipTone.WIN }).isTrue()
        assertThat(tips.size).isAtMost(WellbeingEngine.WEEKLY_TIP_CAP)
    }
}
