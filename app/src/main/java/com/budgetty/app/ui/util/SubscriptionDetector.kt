package com.budgetty.app.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** How often a detected subscription recurs. */
enum class SubscriptionCadence { MONTHLY, YEARLY }

/** One charge from a merchant, fed into [SubscriptionDetector] (one per receipt). */
data class MerchantCharge(
    val merchant: String,
    val emoji: String,
    val amount: BigDecimal,
    val dateMillis: Long,
)

/** A single charge inside a detected subscription's history. */
data class SubCharge(val amount: BigDecimal, val dateMillis: Long)

/** A detected price increase: [oldAmount] → [newAmount] starting on [sinceMillis]. */
data class PriceHike(val oldAmount: BigDecimal, val newAmount: BigDecimal, val sinceMillis: Long)

/** A recurring merchant found by clustering its charges — never a bank feed, only the app's receipts. */
data class DetectedSubscription(
    val merchant: String,
    val emoji: String,
    val cadence: SubscriptionCadence,
    /** The current (most recent) charge amount. */
    val amount: BigDecimal,
    /** Amount normalised to a month (yearly ÷ 12), for the per-month total. */
    val monthlyEquivalent: BigDecimal,
    val lastChargeMillis: Long,
    val nextExpectedMillis: Long,
    /** Day-of-month the charge tends to land, for prefilling a recurring bill. */
    val dueDay: Int,
    val charges: List<SubCharge>,
    val priceHike: PriceHike?,
) {
    val chargeCount: Int get() = charges.size
}

/**
 * On-device recurring-charge detection: groups charges by merchant and keeps those that repeat at a
 * regular monthly or yearly cadence with a stable amount (≥ [MIN_CHARGES] occurrences). No network, no
 * bank — it only ever sees the receipts already in the app. Pure Kotlin so it unit-tests on the host.
 */
object SubscriptionDetector {

    const val MIN_CHARGES = 3

    // Gap bands (days) for each cadence. Every consecutive gap must fall in the band, so an irregular
    // merchant (a grocery run) is rejected even if its average happens to look monthly.
    private val MONTHLY_GAP = 20..40
    private val YEARLY_GAP = 300..430

    fun detect(charges: List<MerchantCharge>, today: LocalDate = LocalDate.now()): List<DetectedSubscription> =
        charges.groupBy { it.merchant }
            .mapNotNull { (_, group) -> detectOne(group, today) }
            .sortedByDescending { it.monthlyEquivalent }

    private fun detectOne(group: List<MerchantCharge>, today: LocalDate): DetectedSubscription? {
        if (group.size < MIN_CHARGES) return null
        val sorted = group.sortedBy { it.dateMillis }
        val dates = sorted.map { it.localDate() }
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
        val cadence = when {
            gaps.all { it in MONTHLY_GAP } -> SubscriptionCadence.MONTHLY
            gaps.all { it in YEARLY_GAP } -> SubscriptionCadence.YEARLY
            else -> return null
        }
        // A subscription's price is stable (≤ 2 distinct amounts allows a single hike/drop); a merchant
        // whose amount varies every time (groceries) is not one.
        val distinct = sorted.map { it.amount.setScale(2, RoundingMode.HALF_UP) }.distinct()
        if (distinct.size > 2) return null

        val latest = sorted.last()
        val amount = latest.amount.setScale(2, RoundingMode.HALF_UP)
        val monthly = if (cadence == SubscriptionCadence.YEARLY)
            amount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP) else amount
        val lastDate = latest.localDate()
        val nextDate = if (cadence == SubscriptionCadence.YEARLY) lastDate.plusYears(1) else lastDate.plusMonths(1)

        // Price hike: an earlier, lower amount that steps up to the current one.
        val hike = if (distinct.size == 2) {
            val lower = distinct.min()
            val higher = distinct.max()
            if (amount == higher) {
                val sinceMillis = sorted.first { it.amount.setScale(2, RoundingMode.HALF_UP) == higher }.dateMillis
                PriceHike(oldAmount = lower, newAmount = higher, sinceMillis = sinceMillis)
            } else null
        } else null

        return DetectedSubscription(
            merchant = latest.merchant,
            emoji = group.groupingBy { it.emoji }.eachCount().maxByOrNull { it.value }?.key ?: latest.emoji,
            cadence = cadence,
            amount = amount,
            monthlyEquivalent = monthly,
            lastChargeMillis = latest.dateMillis,
            nextExpectedMillis = nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dueDay = lastDate.dayOfMonth,
            charges = sorted.reversed().map { SubCharge(it.amount.setScale(2, RoundingMode.HALF_UP), it.dateMillis) },
            priceHike = hike,
        )
    }

    private fun MerchantCharge.localDate(): LocalDate =
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
