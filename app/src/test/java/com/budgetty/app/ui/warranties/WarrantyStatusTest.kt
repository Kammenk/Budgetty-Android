package com.budgetty.app.ui.warranties

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WarrantyStatusTest {

    @Test
    fun `active warranty is half elapsed at its midpoint`() {
        val s = Warranties.status(
            purchaseDate = LocalDate.of(2026, 1, 1),
            durationMonths = 24,
            today = LocalDate.of(2027, 1, 1),
        )
        assertEquals(WarrantyState.ACTIVE, s.state)
        assertEquals(LocalDate.of(2028, 1, 1), s.expiryDate)
        assertEquals(365L, s.daysLeft)
        assertTrue("fraction ~0.5 but was ${s.elapsedFraction}", s.elapsedFraction in 0.45f..0.55f)
    }

    @Test
    fun `within 30 days of expiry is expiring soon`() {
        val s = Warranties.status(LocalDate.of(2026, 1, 1), 12, LocalDate.of(2026, 12, 20))
        assertEquals(WarrantyState.EXPIRING_SOON, s.state)
        assertEquals(12L, s.daysLeft)
    }

    @Test
    fun `past expiry is expired and fully elapsed`() {
        val s = Warranties.status(LocalDate.of(2020, 1, 1), 12, LocalDate.of(2026, 1, 1))
        assertEquals(WarrantyState.EXPIRED, s.state)
        assertTrue(s.daysLeft < 0)
        assertEquals(1f, s.elapsedFraction, 0.0001f)
    }

    @Test
    fun `the expiring-soon boundary is exactly 30 days`() {
        val today = LocalDate.of(2026, 6, 1)
        // Expiry exactly 30 days out → expiring soon; 31 days out → still active.
        assertEquals(WarrantyState.EXPIRING_SOON, statusExpiringIn(30, today).state)
        assertEquals(WarrantyState.ACTIVE, statusExpiringIn(31, today).state)
    }

    /** A warranty whose expiry is [days] after [today] (1-day coverage window shifted to land there). */
    private fun statusExpiringIn(days: Long, today: LocalDate): WarrantyStatus {
        val expiry = today.plusDays(days)
        // 1-month coverage ending on `expiry`, so purchase is expiry − 1 month.
        return Warranties.status(expiry.minusMonths(1), 1, today)
    }
}
