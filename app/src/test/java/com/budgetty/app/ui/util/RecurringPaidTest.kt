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

    // ── Autopay: derived "effectively paid" once the due day passes (no lastPosted stamp) ──

    private fun autoBill(cadence: String, dueDay: Int, autoPay: Boolean = true) = RecurringEntity(
        label = "Rent",
        amount = BigDecimal("800"),
        isIncome = false,
        cadence = cadence,
        dueDay = dueDay,
        autoPay = autoPay,
    )

    @Test
    fun `monthly due day passed this cycle, upcoming day not yet`() {
        // today = Jul 25; the Jul 10 due day has passed, the Jul 28 one has not.
        assertTrue(autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 10).isDuePassedThisCycle(today, 1))
        assertFalse(autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 28).isDuePassedThisCycle(today, 1))
    }

    @Test
    fun `weekly monday has passed by any later day of its week`() {
        assertTrue(autoBill(RecurringEntity.Cadence.WEEKLY, dueDay = 1).isDuePassedThisCycle(today, 1))
    }

    @Test
    fun `yearly and one-off never auto-mark (no computable due date)`() {
        assertFalse(autoBill(RecurringEntity.Cadence.YEARLY, dueDay = 1).isDuePassedThisCycle(today, 1))
        assertFalse(autoBill(RecurringEntity.Cadence.ONCE, dueDay = 1).isDuePassedThisCycle(today, 1))
    }

    @Test
    fun `autopay fills paid once the due day passes`() {
        assertTrue(autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 10).isEffectivelyPaidThisCycle(today, 1))
        assertFalse(autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 28).isEffectivelyPaidThisCycle(today, 1))
    }

    @Test
    fun `autopay off leaves it to the manual stamp`() {
        val b = autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 10, autoPay = false)
        assertFalse(b.isEffectivelyPaidThisCycle(today, 1))
    }

    @Test
    fun `a manual payment counts even before the autopay due day`() {
        val b = autoBill(RecurringEntity.Cadence.MONTHLY, dueDay = 28)
            .copy(lastPosted = millis(LocalDate.of(2026, 7, 10)))
        assertTrue(b.isEffectivelyPaidThisCycle(today, 1))
    }
}
