package com.budgetty.app.ui.util

import com.budgetty.app.data.local.BuyingLimitTimeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pins the buying-limit substring counter: it sums QUANTITY (not rows), ORs across keywords, folds
 * case + Cyrillic, and windows on the pay-cycle month / locale week. These are the rules the card
 * count and the save-time nudge both rely on, and the contract the iOS port must mirror.
 */
class BuyingLimitCounterTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Epoch millis for [date] at noon in the counter's zone, well inside any day-bounded window. */
    private fun at(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(name: String, quantity: Int, date: LocalDate) =
        CountableItem(name, quantity, at(date))

    // matches(): substring, case, Cyrillic, OR.

    @Test
    fun matches_isSubstringCaseInsensitive() {
        assertTrue(BuyingLimitCounter.matches("Coke Zero 500ml", listOf("coke")))
        assertTrue(BuyingLimitCounter.matches("COCA-COLA", listOf("cola")))
        // "coke" is not a substring of "coca-cola" — only "cola"/"coca" would catch it (honest matcher).
        assertFalse(BuyingLimitCounter.matches("Coca-Cola 500ml", listOf("coke")))
    }

    @Test
    fun matches_foldsCyrillic() {
        assertTrue(BuyingLimitCounter.matches("Кока-Кола 500мл", listOf("кока")))
        // Uppercase Cyrillic folds to the lowercase keyword (Unicode-aware, no ASCII-only shortcut).
        assertTrue(BuyingLimitCounter.matches("ЛЮТЕНИЦА 500Г", listOf("лютеница")))
    }

    @Test
    fun matches_orsAcrossKeywords() {
        val keywords = listOf("coke", "cola", "fanta")
        assertTrue(BuyingLimitCounter.matches("Fanta Orange", keywords))
        assertTrue(BuyingLimitCounter.matches("Coke Zero", keywords))
        assertFalse(BuyingLimitCounter.matches("Sparkling water", keywords))
    }

    @Test
    fun matches_emptyKeywordsNeverMatch() {
        assertFalse(BuyingLimitCounter.matches("anything", emptyList()))
    }

    // countInWindow(): sums quantity, ORs, respects the window.

    @Test
    fun count_sumsQuantityAcrossKeywordsInWindow() {
        val today = LocalDate.of(2026, 2, 15)
        val items = listOf(
            item("Coke Zero", 2, LocalDate.of(2026, 2, 10)),
            item("Coca-Cola 1L", 3, LocalDate.of(2026, 2, 12)),
            item("Fanta", 1, LocalDate.of(2026, 2, 14)),
        )
        val n = BuyingLimitCounter.count(
            items, listOf("coke", "cola"), BuyingLimitTimeframe.MONTHLY, today, monthStartDay = 1,
        )
        assertEquals("sums quantity (2 via coke + 3 via cola), not rows", 5, n)
    }

    // Monthly window: calendar month and pay-cycle month.

    @Test
    fun monthly_calendarMonthBoundaries() {
        val today = LocalDate.of(2026, 2, 15)
        val items = listOf(
            item("Coke", 1, LocalDate.of(2026, 1, 31)),
            item("Coke", 1, LocalDate.of(2026, 2, 1)),
            item("Coke", 1, LocalDate.of(2026, 2, 28)),
            item("Coke", 1, LocalDate.of(2026, 3, 1)),
        )
        val n = BuyingLimitCounter.count(items, listOf("coke"), BuyingLimitTimeframe.MONTHLY, today, monthStartDay = 1)
        assertEquals("only the two rows dated within February count", 2, n)
    }

    @Test
    fun monthly_payCycleShiftsTheWindow() {
        // Pay day 25: the cycle containing Feb 15 runs Jan 25 to Feb 24.
        val today = LocalDate.of(2026, 2, 15)
        val items = listOf(
            item("Coke", 1, LocalDate.of(2026, 1, 24)),
            item("Coke", 1, LocalDate.of(2026, 1, 25)),
            item("Coke", 1, LocalDate.of(2026, 2, 24)),
            item("Coke", 1, LocalDate.of(2026, 2, 26)),
        )
        val n = BuyingLimitCounter.count(items, listOf("coke"), BuyingLimitTimeframe.MONTHLY, today, monthStartDay = 25)
        assertEquals("Jan 25 and Feb 24 are inside the cycle; Jan 24 and Feb 26 are not", 2, n)
    }

    // Weekly window: locale first-day-of-week.

    @Test
    fun weekly_boundariesOnFirstDayOfWeek() {
        // Wed 2026-02-11 with a Monday week start: the window is Feb 9 to Feb 15.
        val today = LocalDate.of(2026, 2, 11)
        val items = listOf(
            item("Coke", 1, LocalDate.of(2026, 2, 8)),
            item("Coke", 2, LocalDate.of(2026, 2, 9)),
            item("Coke", 1, LocalDate.of(2026, 2, 15)),
            item("Coke", 1, LocalDate.of(2026, 2, 16)),
        )
        val n = BuyingLimitCounter.count(
            items, listOf("coke"), BuyingLimitTimeframe.WEEKLY, today, firstDayOfWeek = DayOfWeek.MONDAY,
        )
        assertEquals("2 on Monday + 1 on Sunday, both inside the week", 3, n)
    }

    @Test
    fun weekly_sundayFirstDayShiftsWindow() {
        // With Sunday as the week start, Wed Feb 11's week is Feb 8 (Sun) to Feb 14 (Sat).
        val today = LocalDate.of(2026, 2, 11)
        val items = listOf(
            item("Coke", 1, LocalDate.of(2026, 2, 8)),
            item("Coke", 1, LocalDate.of(2026, 2, 15)),
        )
        val n = BuyingLimitCounter.count(
            items, listOf("coke"), BuyingLimitTimeframe.WEEKLY, today, firstDayOfWeek = DayOfWeek.SUNDAY,
        )
        assertEquals("Feb 8 falls in this Sunday-week; Feb 15 opens the next one", 1, n)
    }

    // nextReset(): the window roll-over date.

    @Test
    fun nextReset_weeklyIsStartOfNextWeek() {
        val reset = BuyingLimitCounter.nextReset(
            BuyingLimitTimeframe.WEEKLY, LocalDate.of(2026, 2, 11), firstDayOfWeek = DayOfWeek.MONDAY,
        )
        assertEquals(LocalDate.of(2026, 2, 16), reset)
    }

    @Test
    fun nextReset_monthlyIsNextCycleStart() {
        val reset = BuyingLimitCounter.nextReset(
            BuyingLimitTimeframe.MONTHLY, LocalDate.of(2026, 2, 15), monthStartDay = 25,
        )
        assertEquals("cycle Jan 25 to Feb 24 rolls over on Feb 25", LocalDate.of(2026, 2, 25), reset)
    }
}
