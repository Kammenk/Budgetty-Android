package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * [windowAmount] must count a recurring entry only from the month it was added, so a plan is never
 * projected backward onto months it didn't exist for (the "money in vs out" back-projection bug: a
 * half-year view showing 6× a just-added salary).
 */
class RecurringWindowAmountTest {

    private val zone = ZoneId.systemDefault()

    private fun monthStart(y: Int, m: Int): Long =
        YearMonth.of(y, m).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Inclusive end (last millisecond) of month [m], matching how the app builds its windows. */
    private fun monthEnd(y: Int, m: Int): Long =
        YearMonth.of(y, m).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    private fun createdOn(y: Int, m: Int, day: Int): Long =
        YearMonth.of(y, m).atDay(day).atStartOfDay(zone).toInstant().toEpochMilli()

    /** The pay-cycle month containing [y]/[m]/[d] for a given [startDay], as the inclusive epoch-millis
     *  window the app actually feeds windowAmount (mirrors [currentMonthRange]). */
    private fun payCycleWindow(y: Int, m: Int, d: Int, startDay: Int): Pair<Long, Long> {
        val (start, end) = PayCycle.month(LocalDate.of(y, m, d), startDay)
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMs to endMs
    }

    private fun salary(amount: String, createdY: Int, createdM: Int, day: Int = 15) = RecurringEntity(
        label = "Salary",
        amount = BigDecimal(amount),
        isIncome = true,
        cadence = RecurringEntity.Cadence.MONTHLY,
        createdAt = createdOn(createdY, createdM, day),
    )

    @Test
    fun `recurring counts every month when it predates the window`() {
        val s = salary("1000", createdY = 2025, createdM = 1)
        // Jan–Jun 2026, created a year earlier → all 6 months eligible.
        assertEquals(BigDecimal("6000.00"), s.windowAmount(monthStart(2026, 1), monthEnd(2026, 6)))
    }

    @Test
    fun `recurring is zero for a window entirely before it was added`() {
        val s = salary("1000", createdY = 2026, createdM = 7)
        // Jan–Jun 2026 with a salary added in July → nothing to project backward (the reported bug).
        assertEquals(BigDecimal.ZERO, s.windowAmount(monthStart(2026, 1), monthEnd(2026, 6)))
    }

    @Test
    fun `recurring counts only months on or after the added month`() {
        val s = salary("1000", createdY = 2026, createdM = 4)
        // Jan–Jun 2026, added in April → Apr, May, Jun = 3 months.
        assertEquals(BigDecimal("3000.00"), s.windowAmount(monthStart(2026, 1), monthEnd(2026, 6)))
    }

    @Test
    fun `the current single month shows the full monthly rate`() {
        val s = salary("1000", createdY = 2026, createdM = 7, day = 20)
        // Added mid-July, viewing July → the whole month counts (full expected month).
        assertEquals(BigDecimal("1000.00"), s.windowAmount(monthStart(2026, 7), monthEnd(2026, 7)))
    }

    @Test
    fun `a pay-cycle month counts as one whole month, not day-scaled`() {
        // monthStartDay = 10 → the current cycle is the 10th–9th (31 days across Aug/Sep). Before the
        // fix this fell into day-scaling (× 31 / 30.4375 = 1018.48); it must show one full month.
        val s = salary("1000", createdY = 2025, createdM = 1)
        val (start, end) = payCycleWindow(2026, 8, 15, startDay = 10)
        assertEquals(BigDecimal("1000.00"), s.windowAmount(start, end))
    }

    @Test
    fun `a short pay-cycle month still counts as one whole month`() {
        // Feb 10 – Mar 9 is only 28 days; day-scaling under-counted it (× 28 / 30.4375 = 919.90).
        val s = salary("1000", createdY = 2025, createdM = 1)
        val (start, end) = payCycleWindow(2026, 2, 15, startDay = 10)
        assertEquals(BigDecimal("1000.00"), s.windowAmount(start, end))
    }

    @Test
    fun `two pay-cycle months count as two`() {
        val s = salary("1000", createdY = 2025, createdM = 1)
        // Aug 10 – Oct 9: the current cycle's start through two cycles' worth (Aug 10 .. Oct 9 exclusive).
        val start = PayCycle.month(LocalDate.of(2026, 8, 15), 10).first
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val end = PayCycle.month(LocalDate.of(2026, 9, 15), 10).second
            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        assertEquals(BigDecimal("2000.00"), s.windowAmount(start, end))
    }

    @Test
    fun `weekly cadence uses its monthly equivalent scaled by the window`() {
        val weekly = RecurringEntity(
            label = "Groceries",
            amount = BigDecimal("100"),
            isIncome = false,
            cadence = RecurringEntity.Cadence.WEEKLY,
            createdAt = createdOn(2025, 1, 1),
        )
        // Monthly equivalent = 100 × 52 / 12 = 433.33; over a whole single month that is one month of it.
        assertEquals(BigDecimal("433.33"), weekly.windowAmount(monthStart(2026, 3), monthEnd(2026, 3)))
    }

    @Test
    fun `one-time entry counts once inside the window and zero outside`() {
        val bonus = RecurringEntity(
            label = "Bonus",
            amount = BigDecimal("500"),
            isIncome = true,
            cadence = RecurringEntity.Cadence.ONCE,
            createdAt = createdOn(2026, 3, 10),
        )
        assertEquals(BigDecimal("500"), bonus.windowAmount(monthStart(2026, 1), monthEnd(2026, 6)))
        assertEquals(BigDecimal.ZERO, bonus.windowAmount(monthStart(2026, 7), monthEnd(2026, 12)))
    }
}
