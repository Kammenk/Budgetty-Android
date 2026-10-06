package com.budgetty.app.ui.budgets

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetPaceTest {

    private fun bd(v: String) = BigDecimal(v)
    private fun eq(expected: String, actual: BigDecimal) = assertEquals(0, bd(expected).compareTo(actual))

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun `an on-pace monthly budget reports its daily allowance`() {
        val p = BudgetPace.compute(
            spent = bd("842"), limit = bd("1200"),
            start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31), today = today,
        )
        assertEquals(PaceState.ON_PACE, p.state)
        assertEquals(9L, p.daysLeft)
        eq("39.78", p.dailyAllowance) // (1200 − 842) ÷ 9
        eq("38.27", p.avgPerDay) // 842 ÷ 22 elapsed
        eq("1186.45", p.projected) // 842 × 31 ÷ 22
        assertEquals(0.70f, p.fill, 0.01f)
        assertEquals(0.71f, p.todayFraction, 0.01f)
    }

    @Test
    fun `a trip spending faster than its days is over pace`() {
        val p = BudgetPace.compute(
            spent = bd("512"), limit = bd("600"),
            start = LocalDate.of(2026, 10, 14), end = LocalDate.of(2026, 10, 27), today = today,
        )
        assertEquals(PaceState.OVER_PACE, p.state)
        assertEquals(5L, p.daysLeft)
        eq("88", p.remaining)
        eq("796.44", p.projected) // 512 × 14 ÷ 9
    }

    @Test
    fun `slightly ahead within 15 percent is a warning, not over`() {
        val p = BudgetPace.compute(
            spent = bd("262"), limit = bd("350"),
            start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31), today = today,
        )
        assertEquals(PaceState.AHEAD, p.state)
    }

    @Test
    fun `past the limit is over budget and the bar caps at full`() {
        val p = BudgetPace.compute(
            spent = bd("130"), limit = bd("100"),
            start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 31), today = today,
        )
        assertEquals(PaceState.OVER_BUDGET, p.state)
        assertEquals(1f, p.fill, 0.0001f)
        eq("-30", p.remaining)
    }
}
