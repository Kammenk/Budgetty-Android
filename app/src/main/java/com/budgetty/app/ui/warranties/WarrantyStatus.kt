package com.budgetty.app.ui.warranties

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Where a warranty sits in its life: still covered, ending within [Warranties.EXPIRING_SOON_DAYS], or past. */
enum class WarrantyState { ACTIVE, EXPIRING_SOON, EXPIRED }

/** The derived state of one warranty on a given day: its expiry, how far through it is, and days left. */
data class WarrantyStatus(
    val expiryDate: LocalDate,
    /** 0..1 fraction of the coverage period elapsed — drives the ring. */
    val elapsedFraction: Float,
    /** Whole days until expiry; negative once expired. */
    val daysLeft: Long,
    val state: WarrantyState,
)

/** Pure warranty math + the free-tier cap. No Android deps, so it's unit-testable on the host. */
object Warranties {

    /** Free users can track this many warranties; adding past it routes to the paywall. */
    const val FREE_LIMIT = 5

    /** A warranty within this many days of expiry counts as "expiring soon". */
    const val EXPIRING_SOON_DAYS = 30L

    /** Derives the [WarrantyStatus] for a warranty bought on [purchaseDate] for [durationMonths]. */
    fun status(purchaseDate: LocalDate, durationMonths: Int, today: LocalDate): WarrantyStatus {
        val expiry = purchaseDate.plusMonths(durationMonths.toLong())
        val totalDays = ChronoUnit.DAYS.between(purchaseDate, expiry).coerceAtLeast(1)
        val elapsedDays = ChronoUnit.DAYS.between(purchaseDate, today).coerceIn(0, totalDays)
        val fraction = (elapsedDays.toDouble() / totalDays).toFloat().coerceIn(0f, 1f)
        val daysLeft = ChronoUnit.DAYS.between(today, expiry)
        val state = when {
            daysLeft < 0 -> WarrantyState.EXPIRED
            daysLeft <= EXPIRING_SOON_DAYS -> WarrantyState.EXPIRING_SOON
            else -> WarrantyState.ACTIVE
        }
        return WarrantyStatus(expiry, fraction, daysLeft, state)
    }
}
