package com.budgetty.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PayCycleTest {

    @Test
    fun `start day 1 is the ordinary calendar month`() {
        val (start, end) = PayCycle.month(LocalDate.of(2026, 7, 15), startDay = 1)
        assertEquals(LocalDate.of(2026, 7, 1), start)
        assertEquals(LocalDate.of(2026, 7, 31), end)
    }

    @Test
    fun `on the pay day the current cycle starts that day`() {
        val (start, end) = PayCycle.month(LocalDate.of(2026, 7, 25), startDay = 25)
        assertEquals(LocalDate.of(2026, 7, 25), start)
        assertEquals(LocalDate.of(2026, 8, 24), end)
    }

    @Test
    fun `before the pay day today sits in the previous cycle`() {
        val (start, end) = PayCycle.month(LocalDate.of(2026, 7, 10), startDay = 25)
        assertEquals(LocalDate.of(2026, 6, 25), start)
        assertEquals(LocalDate.of(2026, 7, 24), end)
    }

    @Test
    fun `previous offset steps back one whole cycle`() {
        val (start, end) = PayCycle.month(LocalDate.of(2026, 7, 25), startDay = 25, offset = -1)
        assertEquals(LocalDate.of(2026, 6, 25), start)
        assertEquals(LocalDate.of(2026, 7, 24), end)
    }

    @Test
    fun `a 31st pay day clamps to the last day of a short month`() {
        // Viewing mid-March with a 31st pay day: the cycle that contains today starts on Feb's last
        // day (28th in 2026) and ends the day before March's 31st.
        val (start, end) = PayCycle.month(LocalDate.of(2026, 3, 15), startDay = 31)
        assertEquals(LocalDate.of(2026, 2, 28), start)
        assertEquals(LocalDate.of(2026, 3, 30), end)
    }
}
