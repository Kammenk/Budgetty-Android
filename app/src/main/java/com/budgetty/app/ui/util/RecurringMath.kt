package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Shared money-flow math for income & recurring payments, so the Budget screen and the History
 * "Budgets" tab compute identical monthly totals (and treat one-time entries the same way).
 */

/**
 * Inclusive [start, end] epoch-millis window for [today]'s pay-cycle month, which starts on
 * [monthStartDay] (1 = the ordinary calendar month). Shifting it moves the monthly budget and the
 * recurring plan's "current month" onto the user's pay day. See [PayCycle].
 */
fun currentMonthRange(today: LocalDate = LocalDate.now(), monthStartDay: Int = 1): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val (start, end) = PayCycle.month(today, monthStartDay)
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return startMillis to endMillis
}

/**
 * The entry's amount expressed per month (weekly ×52/12, yearly ÷12), for the totals/breakdown.
 * A one-time ([RecurringEntity.Cadence.ONCE]) entry counts its full amount only in the calendar
 * month it was added ([monthStart]..[monthEnd]) and zero afterwards, so a variable monthly wage or
 * a one-off bonus lands in that month's plan without carrying over into future months.
 */
fun RecurringEntity.monthlyAmount(monthStart: Long, monthEnd: Long): BigDecimal = when (cadence) {
    RecurringEntity.Cadence.WEEKLY ->
        amount.multiply(BigDecimal(52)).divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
    RecurringEntity.Cadence.YEARLY -> amount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
    RecurringEntity.Cadence.ONCE -> if (createdAt in monthStart..monthEnd) amount else BigDecimal.ZERO
    else -> amount
}

/**
 * The entry's contribution to the inclusive [windowStart]..[windowEnd] epoch-millis window, counting
 * a recurring cadence only from the calendar month it was added ([RecurringEntity.createdAt]) onward.
 * A salary or bill is therefore never projected backward onto months it didn't yet exist for — a
 * half-year view no longer shows 6× a salary that was only just added — while a one-time entry still
 * counts its full amount exactly once, and only when it was added inside the window. Used by the
 * Insights money-flow cards and the History Budgets snapshot so both scale a plan to the selected
 * range using only what's actually known, rather than predicting the past.
 */
fun RecurringEntity.windowAmount(
    windowStart: Long,
    windowEnd: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): BigDecimal {
    if (cadence == RecurringEntity.Cadence.ONCE) {
        return if (createdAt in windowStart..windowEnd) amount else BigDecimal.ZERO
    }
    // Recurring cadences ignore the month bounds passed to monthlyAmount — it returns the per-month rate.
    val rate = monthlyAmount(windowStart, windowEnd)
    if (rate.signum() == 0) return BigDecimal.ZERO

    val startDate = Instant.ofEpochMilli(windowStart).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(windowEnd).atZone(zone).toLocalDate()
    val createdMonth = YearMonth.from(Instant.ofEpochMilli(createdAt).atZone(zone))
    // Clip the window to the part on/after the month the entry was added; nothing left ⇒ no contribution.
    val activeStart = maxOf(startDate, createdMonth.atDay(1))
    if (activeStart.isAfter(endDate)) return BigDecimal.ZERO

    val startMonth = YearMonth.from(startDate)
    val endMonth = YearMonth.from(endDate)
    val wholeMonths = startDate == startMonth.atDay(1) && endDate == endMonth.atEndOfMonth()
    return if (wholeMonths) {
        // Whole calendar-month window (every Home/History preset and the Insights month/quarter/half
        // steps): count the eligible months exactly for clean integer scaling of the monthly rate.
        val eligibleMonths = generateSequence(startMonth) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(endMonth) }
            .count { !it.isBefore(createdMonth) }
        rate.multiply(BigDecimal(eligibleMonths)).setScale(2, RoundingMode.HALF_UP)
    } else {
        // Partial window (a week step or a custom range): scale by active days over an average month,
        // matching the Insights custom-range factor.
        val activeDays = ChronoUnit.DAYS.between(activeStart, endDate) + 1
        rate.multiply(BigDecimal(activeDays)).divide(BigDecimal("30.4375"), 2, RoundingMode.HALF_UP)
    }
}
