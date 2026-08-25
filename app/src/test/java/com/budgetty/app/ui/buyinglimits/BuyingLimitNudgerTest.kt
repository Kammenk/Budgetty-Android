package com.budgetty.app.ui.buyinglimits

import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.ui.util.CountableItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pins §4.6 nudge restraint on the pure [BuyingLimitNudger.selectNudge]: exactly one nudge per save
 * (the most-over limit), only on the receipt that actually CROSSES the cap, and never a re-nudge for a
 * limit that was already at/over its cap earlier in the same window.
 */
class BuyingLimitNudgerTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    // Wed 2026-04-15, Monday weeks → the open weekly window is Apr 13–19.
    private val today = LocalDate.of(2026, 4, 15)

    private fun at(date: LocalDate): Long = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun item(name: String, quantity: Int, date: LocalDate) = CountableItem(name, quantity, at(date))

    private fun coke(cap: Int, id: Long = 1) = BuyingLimitEntity(
        id = id, keywords = "coke", timeframe = BuyingLimitTimeframe.WEEKLY, count = cap,
    )

    private fun select(
        limits: List<BuyingLimitEntity>,
        all: List<CountableItem>,
        saved: List<CountableItem>,
    ) = BuyingLimitNudger.selectNudge(limits, all, saved, today, monthStartDay = 1, firstDayOfWeek = DayOfWeek.MONDAY)

    @Test
    fun nudgesOnTheReceiptThatCrossesTheCap() {
        val saved = listOf(item("Coke", 1, today))
        val all = listOf(item("Coke", 1, LocalDate.of(2026, 4, 13))) + saved // before 1 → after 2, cap 2
        val nudge = select(listOf(coke(cap = 2)), all, saved)
        assertEquals(2, nudge?.countAfter)
        assertEquals(1L, nudge?.limitId)
    }

    @Test
    fun doesNotReNudgeWhenAlreadyOverBeforeThisReceipt() {
        // Already 2 (at cap) before this save; this receipt pushes to 3 → they know, no re-nudge.
        val saved = listOf(item("Coke", 1, today))
        val all = listOf(item("Coke", 2, LocalDate.of(2026, 4, 13))) + saved
        assertNull(select(listOf(coke(cap = 2)), all, saved))
    }

    @Test
    fun doesNotNudgeWhenThisReceiptDidNotContribute() {
        val saved = listOf(item("Bread", 1, today))
        val all = listOf(item("Coke", 2, LocalDate.of(2026, 4, 13))) + saved
        assertNull(select(listOf(coke(cap = 2)), all, saved))
    }

    @Test
    fun picksTheMostOverAmongCrossingLimits_atMostOne() {
        val saved = listOf(item("Coke", 1, today), item("Crisps", 1, today))
        val all = listOf(
            item("Coke", 1, LocalDate.of(2026, 4, 13)), // coke before 1 → after 2 (cap 2): over by 0
            item("Crisps", 3, LocalDate.of(2026, 4, 13)), // crisps before 3 → after 4 (cap 1): over by 3
        ) + saved
        val crisps = BuyingLimitEntity(id = 2, keywords = "crisps", timeframe = BuyingLimitTimeframe.WEEKLY, count = 1)
        // crisps was already over (before 3 ≥ cap 1) → excluded; coke crosses → coke wins (the only one).
        val nudge = select(listOf(coke(cap = 2, id = 1), crisps), all, saved)
        assertEquals(1L, nudge?.limitId)
        assertEquals(2, nudge?.countAfter)
    }
}
