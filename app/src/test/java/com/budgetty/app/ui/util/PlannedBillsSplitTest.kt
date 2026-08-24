package com.budgetty.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * [splitPlannedBills] is the only new logic behind the Insights planned-bills overlay: it de-duplicates
 * a recurring bill against the period's receipts so a bill the user both planned and logged/scanned is
 * counted once (in spend), not twice. A bill matches a charge only when the names align AND the amount
 * is close; it is otherwise kept visible (conservative — a false hide understates the planned story).
 */
class PlannedBillsSplitTest {

    private fun bill(label: String, amount: String, match: String = amount) =
        PlannedBillLine(label = label, category = "", amount = BigDecimal(amount), matchAmount = BigDecimal(match))

    private fun charge(merchant: String, amount: String, date: Long = 1_000L) =
        ReceiptCharge(merchant = merchant, amount = BigDecimal(amount), dateMillis = date)

    @Test
    fun `unmatched bill stays visible`() {
        val split = splitPlannedBills(listOf(bill("Rent", "780")), charges = emptyList())
        assertEquals(listOf("Rent"), split.visible.map { it.label })
        assertTrue(split.matched.isEmpty())
    }

    @Test
    fun `bill matched by name and close amount is hidden and carries the receipt figure`() {
        val split = splitPlannedBills(
            bills = listOf(bill("Water", "18.40")),
            charges = listOf(charge("Water", "18.40", date = 42L)),
        )
        assertTrue(split.visible.isEmpty())
        assertEquals(listOf("Water"), split.matched.map { it.label })
        assertEquals(BigDecimal("18.40"), split.matched.first().amount)
        assertEquals(42L, split.matched.first().dateMillis)
    }

    @Test
    fun `amount outside tolerance is not matched`() {
        // €50 bill vs €80 charge: diff €30 > max(€2, 15% = €7.50) → no match.
        val split = splitPlannedBills(listOf(bill("Gym", "50")), listOf(charge("Gym", "80")))
        assertEquals(listOf("Gym"), split.visible.map { it.label })
        assertTrue(split.matched.isEmpty())
    }

    @Test
    fun `different merchant is not matched`() {
        val split = splitPlannedBills(listOf(bill("Netflix", "13")), listOf(charge("Lidl", "13")))
        assertEquals(listOf("Netflix"), split.visible.map { it.label })
        assertTrue(split.matched.isEmpty())
    }

    @Test
    fun `merchant name match is case insensitive`() {
        val split = splitPlannedBills(listOf(bill("Netflix", "13")), listOf(charge("NETFLIX", "13")))
        assertTrue(split.visible.isEmpty())
        assertEquals(1, split.matched.size)
    }

    @Test
    fun `one charge matches only one of two same-named bills`() {
        val split = splitPlannedBills(
            bills = listOf(bill("Spotify", "10.99"), bill("Spotify", "10.99")),
            charges = listOf(charge("Spotify", "10.99")),
        )
        assertEquals(1, split.visible.size)
        assertEquals(1, split.matched.size)
    }

    @Test
    fun `bill with zero window amount is dropped entirely`() {
        val split = splitPlannedBills(listOf(bill("Rent", "0", match = "780")), emptyList())
        assertTrue(split.visible.isEmpty())
        assertTrue(split.matched.isEmpty())
    }

    @Test
    fun `dedup matches the per-occurrence amount, not the multi-month window sum`() {
        // A quarter view projects 3× rent (2340) but a single receipt is one month (780): still matches.
        val split = splitPlannedBills(
            bills = listOf(bill("Rent", "2340", match = "780")),
            charges = listOf(charge("Rent", "780")),
        )
        assertTrue(split.visible.isEmpty())
        assertEquals(1, split.matched.size)
    }

    @Test
    fun `variable bill within 15 percent tolerance matches`() {
        // €200 bill, €215 charge: diff €15 <= 15% (€30) → match (a variable utility).
        val split = splitPlannedBills(listOf(bill("Electric", "200")), listOf(charge("Electric", "215")))
        assertTrue(split.visible.isEmpty())
        assertEquals(1, split.matched.size)
    }

    @Test
    fun `the closest eligible charge by amount wins`() {
        val split = splitPlannedBills(
            bills = listOf(bill("Rent", "780")),
            charges = listOf(charge("Rent", "800", date = 1L), charge("Rent", "775", date = 2L)),
        )
        assertEquals(1, split.matched.size)
        assertEquals(BigDecimal("775"), split.matched.first().amount)
    }
}
