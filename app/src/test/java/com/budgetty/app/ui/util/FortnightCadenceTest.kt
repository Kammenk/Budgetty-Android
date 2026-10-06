package com.budgetty.app.ui.util

import com.budgetty.app.data.repository.BudgetRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Fortnightly budget cadence math: the continuous 14-day [PayCycle.fortnight] window, the ×12÷26
 * proration conversions, and [BudgetCadence] resolution / window selection.
 */
class FortnightCadenceTest {

    // Wed 14 Oct 2026 — the anchor used throughout (matches the design mock's pay day).
    private val anchor = LocalDate.of(2026, 10, 14).toEpochDay()

    @Test
    fun `fortnight containing today runs the 14-day block from the anchor`() {
        val (start, end) = PayCycle.fortnight(LocalDate.of(2026, 10, 17), anchor)
        assertEquals(LocalDate.of(2026, 10, 14), start)
        assertEquals(LocalDate.of(2026, 10, 27), end)
    }

    @Test
    fun `next fortnight steps forward exactly 14 days`() {
        val (start, end) = PayCycle.fortnight(LocalDate.of(2026, 10, 17), anchor, offset = 1)
        assertEquals(LocalDate.of(2026, 10, 28), start)
        assertEquals(LocalDate.of(2026, 11, 10), end)
    }

    @Test
    fun `previous fortnight steps back exactly 14 days`() {
        val (start, end) = PayCycle.fortnight(LocalDate.of(2026, 10, 17), anchor, offset = -1)
        assertEquals(LocalDate.of(2026, 9, 30), start)
        assertEquals(LocalDate.of(2026, 10, 13), end)
    }

    @Test
    fun `fortnights stay continuous across a month boundary (no re-anchoring)`() {
        // 11 Nov is the third block from 14 Oct (14 Oct, 28 Oct, 11 Nov) — not re-anchored to November.
        val (start, end) = PayCycle.fortnight(LocalDate.of(2026, 11, 11), anchor)
        assertEquals(LocalDate.of(2026, 11, 11), start)
        assertEquals(LocalDate.of(2026, 11, 24), end)
    }

    @Test
    fun `a today before the anchor resolves to the block that contains it`() {
        val (start, end) = PayCycle.fortnight(LocalDate.of(2026, 10, 10), anchor)
        assertEquals(LocalDate.of(2026, 9, 30), start)
        assertEquals(LocalDate.of(2026, 10, 13), end)
    }

    @Test
    fun `default anchor is the pay-cycle month start containing today`() {
        val derived = PayCycle.defaultFortnightAnchor(LocalDate.of(2026, 10, 17), monthStartDay = 14)
        assertEquals(LocalDate.of(2026, 10, 14).toEpochDay(), derived)
    }

    @Test
    fun `monthly prorates to a fortnight by times 12 over 26`() {
        assertEquals(BigDecimal("600.00"), monthlyToFortnightly(BigDecimal("1300")))
        assertEquals(BigDecimal("448.62"), monthlyToFortnightly(BigDecimal("972")))
    }

    @Test
    fun `fortnightly scales back up to monthly by times 26 over 12`() {
        assertEquals(BigDecimal("1300.00"), fortnightlyToMonthly(BigDecimal("600")))
    }

    @Test
    fun `blank cadence pref derives Weekly only when weekly alone is set`() {
        val weekly = mapOf(BudgetRepository.WEEKLY to BigDecimal("300"))
        val monthly = mapOf(BudgetRepository.MONTHLY to BigDecimal("1200"))
        assertEquals(BudgetCadence.WEEKLY, BudgetCadence.resolve("", weekly))
        assertEquals(BudgetCadence.MONTHLY, BudgetCadence.resolve("", monthly))
        assertEquals(BudgetCadence.MONTHLY, BudgetCadence.resolve("", weekly + monthly))
        assertEquals(BudgetCadence.MONTHLY, BudgetCadence.resolve("", emptyMap()))
    }

    @Test
    fun `an explicit cadence pref wins over which key is set`() {
        val monthly = mapOf(BudgetRepository.MONTHLY to BigDecimal("1200"))
        assertEquals(BudgetCadence.FORTNIGHTLY, BudgetCadence.resolve("FORTNIGHTLY", monthly))
        assertEquals(BudgetCadence.WEEKLY, BudgetCadence.resolve("WEEKLY", monthly))
        // An unrecognized stored value falls back to deriving from the keys.
        assertEquals(BudgetCadence.MONTHLY, BudgetCadence.resolve("bogus", monthly))
    }

    @Test
    fun `windowDates selects the right window per cadence`() {
        val today = LocalDate.of(2026, 10, 17)
        assertEquals(
            PayCycle.month(today, 1),
            BudgetCadence.MONTHLY.windowDates(today, monthStartDay = 1, fortnightAnchorEpochDay = anchor),
        )
        assertEquals(
            LocalDate.of(2026, 10, 14) to LocalDate.of(2026, 10, 27),
            BudgetCadence.FORTNIGHTLY.windowDates(today, monthStartDay = 1, fortnightAnchorEpochDay = anchor),
        )
        // Weekly is the Mon–Sun week; 17 Oct 2026 is a Saturday, so the week runs 12–18 Oct.
        assertEquals(
            LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 10, 18),
            BudgetCadence.WEEKLY.windowDates(today, monthStartDay = 1, fortnightAnchorEpochDay = 0L),
        )
    }

    @Test
    fun `fortnightly windowDates falls back to a derived anchor when none is pinned`() {
        val today = LocalDate.of(2026, 10, 17)
        // anchor 0 ⇒ derive from monthStartDay 14 ⇒ same 14–27 Oct window as an explicit anchor.
        assertEquals(
            LocalDate.of(2026, 10, 14) to LocalDate.of(2026, 10, 27),
            BudgetCadence.FORTNIGHTLY.windowDates(today, monthStartDay = 14, fortnightAnchorEpochDay = 0L),
        )
    }
}
