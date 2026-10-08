package com.budgetty.app.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySuggesterTest {

    private val now = 1_000_000_000_000L
    private val dayMs = 86_400_000L
    private fun daysAgo(d: Long) = now - d * dayMs

    @Test
    fun `too little history falls back to common picks, not personalized`() {
        val stamps = List(3) { "Groceries" to now }
        val result = CategorySuggester.rank(stamps, now)
        assertFalse(result.personalized)
        assertEquals(CategorySuggester.COMMON_PICKS.take(CategorySuggester.LIMIT), result.categories)
    }

    @Test
    fun `enough history is personalized`() {
        val stamps = List(6) { "Fuel" to now }
        val result = CategorySuggester.rank(stamps, now)
        assertTrue(result.personalized)
        assertEquals(listOf("Fuel"), result.categories)
    }

    @Test
    fun `recent use outranks stale but more frequent use`() {
        val stamps = List(10) { "Stale" to daysAgo(365) } + List(5) { "Fresh" to now }
        val result = CategorySuggester.rank(stamps, now)
        assertTrue(result.personalized)
        assertEquals(listOf("Fresh", "Stale"), result.categories)
    }

    @Test
    fun `higher frequency at equal recency ranks first`() {
        val stamps = List(6) { "A" to now } +
            List(5) { "B" to now } +
            List(4) { "C" to now } +
            List(3) { "D" to now } +
            List(2) { "E" to now } +
            List(1) { "F" to now }
        val result = CategorySuggester.rank(stamps, now)
        // Capped at LIMIT (5), so the single-use "F" is dropped.
        assertEquals(listOf("A", "B", "C", "D", "E"), result.categories)
    }

    @Test
    fun `ties break alphabetically for stable ordering`() {
        val stamps = listOf("Zebra", "Apple", "Milk", "Nuts", "Oats").map { it to now }
        val result = CategorySuggester.rank(stamps, now)
        assertEquals(listOf("Apple", "Milk", "Nuts", "Oats", "Zebra"), result.categories)
    }

    @Test
    fun `blank categories are ignored when ranking and counting`() {
        // Four blanks + four real rows: only 4 usable rows, so still below the personalized threshold.
        val stamps = List(4) { "Food" to now } + List(4) { "" to now }
        val result = CategorySuggester.rank(stamps, now)
        assertFalse(result.personalized)
        assertFalse(result.categories.contains(""))
    }

    @Test
    fun `common picks are all real categories`() {
        assertTrue(CategorySuggester.COMMON_PICKS.isNotEmpty())
        assertTrue(CategorySuggester.COMMON_PICKS.all { Categories.isPredefined(it) })
    }
}
