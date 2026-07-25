package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * [isPaidThisCycle] derives "paid" from the [RecurringEntity.lastPosted] timestamp falling inside the
 * bill's current occurrence window, so it resets on its own when the next occurrence begins.
 */
class RecurringPaidTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 7, 25)

    private fun millis(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun bill(cadence: String, lastPosted: Long) = RecurringEntity(
        label = "Rent",
        amount = BigDecimal("800"),
        isIncome = false,
        cadence = cadence,
        lastPosted = lastPosted,
    )

    @Test
    fun `never posted is not paid`() {
        assertFalse(bill(RecurringEntity.Cadence.MONTHLY, lastPosted = 0L).isPaidThisCycle(today, monthStartDay = 1))
    }

    @Test
    fun `monthly paid inside the current pay-cycle month is paid`() {
        val b = bill(RecurringEntity.Cadence.MONTHLY, lastPosted = millis(LocalDate.of(2026, 7, 10)))
        assertTrue(b.isPaidThisCycle(today, monthStartDay = 1))
    }

    @Test
    fun `monthly paid in a previous month has auto-reset to unpaid`() {
        val b = bill(RecurringEntity.Cadence.MONTHLY, lastPosted = millis(LocalDate.of(2026, 6, 10)))
        assertFalse(b.isPaidThisCycle(today, monthStartDay = 1))
    }

    @Test
    fun `pay-cycle month shifts the paid window`() {
        // With monthStartDay = 15, today (Jul 25) sits in the Jul 15 - Aug 14 cycle, so a Jul 5 payment
        // belongs to the PREVIOUS cycle and no longer counts as paid.
        val b = bill(RecurringEntity.Cadence.MONTHLY, lastPosted = millis(LocalDate.of(2026, 7, 5)))
        assertTrue(b.isPaidThisCycle(today, monthStartDay = 1))
        assertFalse(b.isPaidThisCycle(today, monthStartDay = 15))
    }

    @Test
    fun `weekly paid this week is paid, last week is not`() {
        assertTrue(bill(RecurringEntity.Cadence.WEEKLY, millis(today)).isPaidThisCycle(today, monthStartDay = 1))
        val lastWeek = bill(RecurringEntity.Cadence.WEEKLY, millis(today.minusWeeks(1)))
        assertFalse(lastWeek.isPaidThisCycle(today, monthStartDay = 1))
    }

    @Test
    fun `one-time entry stays paid once marked`() {
        val b = bill(RecurringEntity.Cadence.ONCE, lastPosted = millis(LocalDate.of(2020, 1, 1)))
        assertTrue(b.isPaidThisCycle(today, monthStartDay = 1))
    }
}
