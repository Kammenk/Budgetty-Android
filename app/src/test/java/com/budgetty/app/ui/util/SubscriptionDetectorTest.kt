package com.budgetty.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class SubscriptionDetectorTest {

    private val today = LocalDate.of(2026, 7, 20)
    private fun millis(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun charge(name: String, amount: String, y: Int, m: Int, d: Int) =
        MerchantCharge(name, "🎬", BigDecimal(amount), millis(y, m, d))

    @Test fun `monthly identical charges are detected`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Netflix", "13.99", 2026, 5, 14),
                charge("Netflix", "13.99", 2026, 6, 14),
                charge("Netflix", "13.99", 2026, 7, 14),
            ),
            today,
        )
        assertEquals(1, subs.size)
        val s = subs.single()
        assertEquals(SubscriptionCadence.MONTHLY, s.cadence)
        assertEquals(BigDecimal("13.99"), s.amount)
        assertEquals(BigDecimal("13.99"), s.monthlyEquivalent)
        assertEquals(14, s.dueDay)
        assertEquals(3, s.chargeCount)
        assertNull(s.priceHike)
        // next expected ≈ 14 Aug
        assertEquals(
            LocalDate.of(2026, 8, 14),
            java.time.Instant.ofEpochMilli(s.nextExpectedMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
    }

    @Test fun `a price hike is flagged old-to-new`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Netflix", "11.99", 2026, 3, 14),
                charge("Netflix", "13.99", 2026, 4, 14),
                charge("Netflix", "13.99", 2026, 5, 14),
            ),
            today,
        )
        val hike = subs.single().priceHike
        assertNotNull(hike)
        assertEquals(BigDecimal("11.99"), hike!!.oldAmount)
        assertEquals(BigDecimal("13.99"), hike.newAmount)
        assertEquals(
            LocalDate.of(2026, 4, 14),
            java.time.Instant.ofEpochMilli(hike.sinceMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        assertEquals(BigDecimal("13.99"), subs.single().amount) // current amount is the new one
    }

    @Test fun `yearly cadence uses a one-twelfth monthly equivalent`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Allianz", "59.00", 2024, 3, 12),
                charge("Allianz", "59.00", 2025, 3, 12),
                charge("Allianz", "59.00", 2026, 3, 12),
            ),
            today,
        )
        val s = subs.single()
        assertEquals(SubscriptionCadence.YEARLY, s.cadence)
        assertEquals(BigDecimal("4.92"), s.monthlyEquivalent) // 59 / 12
    }

    @Test fun `varying amounts (groceries) are not a subscription`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Kaufland", "47.86", 2026, 5, 3),
                charge("Kaufland", "31.40", 2026, 6, 5),
                charge("Kaufland", "52.10", 2026, 7, 2),
            ),
            today,
        )
        assertTrue(subs.isEmpty())
    }

    @Test fun `irregular gaps are rejected even at a stable amount`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Cafe", "3.50", 2026, 5, 1),
                charge("Cafe", "3.50", 2026, 5, 6),  // 5-day gap
                charge("Cafe", "3.50", 2026, 7, 5),  // ~60-day gap
            ),
            today,
        )
        assertTrue(subs.isEmpty())
    }

    @Test fun `fewer than three charges is not enough`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Spotify", "10.99", 2026, 6, 3),
                charge("Spotify", "10.99", 2026, 7, 3),
            ),
            today,
        )
        assertTrue(subs.isEmpty())
    }

    @Test fun `subscriptions sort by monthly cost descending`() {
        val subs = SubscriptionDetector.detect(
            listOf(
                charge("Spotify", "10.99", 2026, 5, 3),
                charge("Spotify", "10.99", 2026, 6, 3),
                charge("Spotify", "10.99", 2026, 7, 3),
                charge("Gym", "24.90", 2026, 5, 1),
                charge("Gym", "24.90", 2026, 6, 1),
                charge("Gym", "24.90", 2026, 7, 1),
            ),
            today,
        )
        assertEquals(listOf("Gym", "Spotify"), subs.map { it.merchant })
    }
}
