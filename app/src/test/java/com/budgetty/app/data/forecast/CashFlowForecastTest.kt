package com.budgetty.app.data.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class CashFlowForecastTest {

    private fun bd(v: String) = BigDecimal(v)
    private fun eq(expected: String, actual: BigDecimal) = assertEquals(0, bd(expected).compareTo(actual))

    @Test
    fun `single month projects income, bills and spread discretionary`() {
        val result = CashFlowForecast.project(
            startBalance = bd("1000"),
            today = LocalDate.of(2026, 10, 1),
            horizonMonths = 1,
            events = listOf(
                CashEvent(LocalDate.of(2026, 10, 5), bd("-500")), // a bill
                CashEvent(LocalDate.of(2026, 10, 25), bd("2000")), // income
            ),
            monthlyDiscretionary = bd("300"),
            comfortThreshold = BigDecimal.ZERO,
        )
        assertEquals(1, result.months.size)
        val oct = result.months.single()
        assertEquals(YearMonth.of(2026, 10), oct.month)
        eq("2000", oct.income)
        eq("500", oct.bills)
        // 300 × 12 ÷ 365 = 9.86/day × 30 days (2nd–31st)
        eq("295.80", oct.discretionary)
        eq("1204.20", oct.net)
        eq("2204.20", oct.endBalance)
        eq("2204.20", result.endBalance)
        assertFalse(result.dipsBelowComfort) // trough ~273 on 24 Oct stays above 0
    }

    @Test
    fun `a bill that overdraws flags the trough below the comfort line`() {
        val result = CashFlowForecast.project(
            startBalance = bd("100"),
            today = LocalDate.of(2026, 10, 1),
            horizonMonths = 1,
            events = listOf(CashEvent(LocalDate.of(2026, 10, 5), bd("-500"))),
            monthlyDiscretionary = BigDecimal.ZERO,
            comfortThreshold = BigDecimal.ZERO,
        )
        assertTrue(result.dipsBelowComfort)
        eq("-400", result.trough)
        assertEquals(LocalDate.of(2026, 10, 5), result.troughDate)
    }

    @Test
    fun `horizon spans whole months from today`() {
        val result = CashFlowForecast.project(
            startBalance = bd("1000"),
            today = LocalDate.of(2026, 10, 1),
            horizonMonths = 2,
            events = emptyList(),
            monthlyDiscretionary = BigDecimal.ZERO,
            comfortThreshold = BigDecimal.ZERO,
        )
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11)), result.months.map { it.month })
        assertEquals(LocalDate.of(2026, 11, 30), result.endDate)
        eq("1000", result.endBalance)
    }

    @Test
    fun `a mid-month start only spans the remaining days of the first month`() {
        val result = CashFlowForecast.project(
            startBalance = bd("500"),
            today = LocalDate.of(2026, 10, 20),
            horizonMonths = 1,
            events = emptyList(),
            monthlyDiscretionary = bd("310"),
            comfortThreshold = BigDecimal.ZERO,
        )
        assertEquals(LocalDate.of(2026, 10, 20), result.points.first().date)
        assertEquals(LocalDate.of(2026, 10, 31), result.points.last().date)
        assertEquals(1, result.months.size)
        // 11 days of spread spending (21st–31st) at 310 × 12 ÷ 365 = 10.19/day.
        eq("112.09", result.months.single().discretionary)
    }
}
