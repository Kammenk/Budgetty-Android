package com.budgetty.app.ui.wellbeing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Guards the wellbeing-score floor. A brand-new user — 5+ receipts logged but no income, budget,
 * goals, or prior-month trend, and no subscriptions — must NOT score a bogus 100/Thriving. The bug:
 * Subscriptions scored a perfect 100 from a 0% share (absence of data, not a real 0%) and could be
 * the only scored component. Pure engine, plain JUnit.
 */
class WellbeingEngineScoreTest {

    private fun inputs(
        hasIncome: Boolean = false,
        savingsRatePercent: Int = 0,
        hasAnyBudget: Boolean = false,
        trendPercent: Int? = null,
        subsSharePercent: Int? = null,
        subsCount: Int = 0,
        monthsTracked: Int = 1,
        receiptsLogged: Int = WellbeingEngine.MIN_RECEIPTS_TO_SCORE,
    ) = WellbeingInputs(
        hasIncome = hasIncome,
        savingsRatePercent = savingsRatePercent,
        income = BigDecimal.ZERO,
        saved = BigDecimal.ZERO,
        netCashflow = BigDecimal.ZERO,
        hasAnyBudget = hasAnyBudget,
        budgetedCount = if (hasAnyBudget) 1 else 0,
        overCount = 0,
        overspendTotal = BigDecimal.ZERO,
        budgetedTotal = if (hasAnyBudget) BigDecimal("100") else BigDecimal.ZERO,
        trendPercent = trendPercent,
        subsSharePercent = subsSharePercent,
        subsMonthly = BigDecimal.ZERO,
        subsCount = subsCount,
        goals = emptyList(),
        categories = emptyList(),
        spend = BigDecimal("100"),
        receiptsLogged = receiptsLogged,
        monthsTracked = monthsTracked,
    )

    @Test
    fun `new user with no signal and no subscriptions scores no total, not 100`() {
        // The Provider passes subsSharePercent = 0 for zero subscriptions; before the fix that made
        // Subscriptions the only scored component at a perfect 100 → 100/THRIVING for a fresh account.
        val result = WellbeingEngine.score(inputs(subsSharePercent = 0, subsCount = 0, monthsTracked = 1))
        assertThat(result.score).isNull()
        assertThat(result.band).isNull()
    }

    @Test
    fun `zero subscriptions only scores once there is enough history`() {
        assertThat(
            WellbeingEngine.subscriptionsComponentScore(inputs(subsSharePercent = 0, subsCount = 0, monthsTracked = 1)),
        ).isNull()
        // After MIN_MONTHS_FOR_ZERO_SUBS months, a real 0% share is a genuine win (full marks).
        assertThat(
            WellbeingEngine.subscriptionsComponentScore(inputs(subsSharePercent = 0, subsCount = 0, monthsTracked = 2)),
        ).isEqualTo(100)
    }

    @Test
    fun `real signal still scores even in the first month`() {
        // A user who set up income has a genuine savings component — that should still score.
        val result = WellbeingEngine.score(
            inputs(hasIncome = true, savingsRatePercent = 20, subsSharePercent = 0, subsCount = 0, monthsTracked = 1),
        )
        assertThat(result.score).isNotNull()
    }

    @Test
    fun `actual subscriptions score regardless of history`() {
        // subsCount > 0 means there IS data to score, from month one: 100 - (10-5)*11 = 45.
        assertThat(
            WellbeingEngine.subscriptionsComponentScore(
                inputs(subsSharePercent = 10, subsCount = 2, monthsTracked = 1),
            ),
        ).isEqualTo(45)
    }
}
