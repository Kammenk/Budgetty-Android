package com.budgetty.app.ui.trips

import com.budgetty.app.data.local.TripEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pace maths for a trip budget, checked against the Travel-mode mockup's worked example: €512 of a
 * €900 budget on day 3 of a 9-day trip reads "ahead of pace" with €65/day to stay on budget.
 */
class TripStatsTest {

    private val utc: ZoneId = ZoneId.of("UTC")
    private fun millis(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(utc).toInstant().toEpochMilli()

    private fun trip(start: Long?, end: Long?, budget: String?) = TripEntity(
        id = 1, name = "Lisbon", tag = "lisbon-2026",
        startDate = start, endDate = end,
        budgetAmount = budget?.let(::BigDecimal),
        createdAt = start ?: 0,
    )

    @Test
    fun `mockup example — day 3 of 9, over pace`() {
        val t = trip(millis(2026, 10, 12), millis(2026, 10, 20), "900")
        val r = TripStats.compute(t, BigDecimal("512"), LocalDate.of(2026, 10, 14), utc)

        assertEquals(9, r.totalDays)
        assertEquals(3, r.daysElapsed)
        assertEquals(6, r.daysLeft)
        assertEquals(BigDecimal("170.67"), r.perDay)
        assertEquals(listOf(true, true, true, false, false, false, false, false, false), r.dayStrip)

        val pace = requireNotNull(r.pace)
        assertEquals(TripPaceState.OVER_PACE, pace.state)
        assertEquals(0.569f, pace.fill, 0.01f)
        assertEquals(0.333f, pace.tickFraction, 0.01f)
        assertEquals(BigDecimal("388"), pace.remaining)
        assertEquals(BigDecimal("64.67"), pace.suggestedDaily)
    }

    @Test
    fun `spending below the time line is under pace`() {
        val t = trip(millis(2026, 10, 12), millis(2026, 10, 20), "900")
        val r = TripStats.compute(t, BigDecimal("100"), LocalDate.of(2026, 10, 14), utc)
        assertEquals(TripPaceState.UNDER_PACE, requireNotNull(r.pace).state)
    }

    @Test
    fun `over the budget is over budget with no daily allowance`() {
        val t = trip(millis(2026, 10, 12), millis(2026, 10, 20), "900")
        val r = TripStats.compute(t, BigDecimal("950"), LocalDate.of(2026, 10, 14), utc)
        val pace = requireNotNull(r.pace)
        assertEquals(TripPaceState.OVER_BUDGET, pace.state)
        assertEquals(BigDecimal.ZERO, pace.suggestedDaily)
        assertEquals(1f, pace.fill, 0.001f)
        assertTrue(pace.remaining.signum() < 0)
    }

    @Test
    fun `an open-ended trip has a per-day but no span, strip or pace`() {
        val t = trip(start = null, end = null, budget = null).copy(createdAt = millis(2026, 10, 12))
        val r = TripStats.compute(t, BigDecimal("300"), LocalDate.of(2026, 10, 14), utc)
        assertNull(r.totalDays)
        assertNull(r.daysLeft)
        assertNull(r.pace)
        assertTrue(r.dayStrip.isEmpty())
        // 12th → 14th inclusive = 3 days: 300 / 3 = 100.00
        assertEquals(3, r.daysElapsed)
        assertEquals(BigDecimal("100.00"), r.perDay)
    }

    @Test
    fun `dates without a budget still give a day strip but no pace`() {
        val t = trip(millis(2026, 10, 12), millis(2026, 10, 20), budget = null)
        val r = TripStats.compute(t, BigDecimal("200"), LocalDate.of(2026, 10, 14), utc)
        assertNull(r.pace)
        assertEquals(9, r.dayStrip.size)
        assertEquals(3, r.dayStrip.count { it })
    }
}
