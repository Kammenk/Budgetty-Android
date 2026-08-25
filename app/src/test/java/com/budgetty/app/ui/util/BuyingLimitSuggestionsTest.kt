package com.budgetty.app.ui.util

import com.budgetty.app.data.local.BuyingLimitTimeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pins the §4.4 buying-limit suggestion ranking + dismissal: frequency-only, a ≥ 6 total over 60 days,
 * recent activity required, existing-limit and dismissed exclusions, weekly-rate cap, ranked by the
 * last-month figure the prompt shows. These are the rules the iOS port must mirror.
 */
class BuyingLimitSuggestionsTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 4, 15)

    private fun at(date: LocalDate): Long = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun item(name: String, quantity: Int, date: LocalDate) = CountableItem(name, quantity, at(date))

    private fun suggest(
        items: List<CountableItem>,
        existing: List<String> = emptyList(),
        dismissed: Set<String> = emptySet(),
    ) = BuyingLimitSuggestions.suggest(items, existing, dismissed, today = today, zone = zone)

    @Test
    fun ranksByLastMonthCount_withWeeklyCap() {
        val out = suggest(
            listOf(
                item("Coca-Cola", 14, LocalDate.of(2026, 4, 1)),
                item("Crisps", 11, LocalDate.of(2026, 4, 5)),
            ),
        )
        assertEquals(2, out.size)
        assertEquals("coca-cola", out[0].keyword)
        assertEquals(14, out[0].monthCount)
        assertEquals("14×/month → floor(14·7/30) weekly", 3, out[0].suggestedCap)
        assertEquals(BuyingLimitTimeframe.WEEKLY, out[0].timeframe)
        assertEquals("Coca-Cola", out[0].name)
        assertEquals("crisps", out[1].keyword)
        assertEquals(2, out[1].suggestedCap)
    }

    @Test
    fun dropsBelowMinimumQuantity() {
        // 5 total over the window is under the ≥ 6 bar — no suggestion.
        val out = suggest(listOf(item("Rare treat", 5, LocalDate.of(2026, 4, 2))))
        assertTrue(out.isEmpty())
    }

    @Test
    fun dropsStaleItemsNotBoughtLately() {
        // 8 total qualifies on 60 days, but all of it is older than 30 days → not "most bought lately".
        val out = suggest(listOf(item("Old staple", 8, LocalDate.of(2026, 2, 20))))
        assertTrue(out.isEmpty())
    }

    @Test
    fun excludesItemsAlreadyCoveredByAnExistingLimit() {
        val out = suggest(
            listOf(item("Coffee beans", 9, LocalDate.of(2026, 4, 3))),
            existing = listOf("coffee"),
        )
        assertTrue("an existing 'coffee' keyword already caps this", out.isEmpty())
    }

    @Test
    fun aDismissedSuggestionNeverReturns() {
        val items = listOf(
            item("Coca-Cola", 14, LocalDate.of(2026, 4, 1)),
            item("Crisps", 11, LocalDate.of(2026, 4, 5)),
        )
        val out = suggest(items, dismissed = setOf("coca-cola"))
        assertEquals(1, out.size)
        assertEquals("dismissed coke is gone; crisps remains", "crisps", out[0].keyword)
    }

    @Test
    fun capsAtThreeSuggestions() {
        val out = suggest(
            listOf(
                item("Coke", 20, LocalDate.of(2026, 4, 1)),
                item("Crisps", 15, LocalDate.of(2026, 4, 1)),
                item("Chocolate", 12, LocalDate.of(2026, 4, 1)),
                item("Beer", 10, LocalDate.of(2026, 4, 1)),
                item("Energy drink", 8, LocalDate.of(2026, 4, 1)),
            ),
        )
        assertEquals(3, out.size)
        assertEquals(listOf("coke", "crisps", "chocolate"), out.map { it.keyword })
    }
}
