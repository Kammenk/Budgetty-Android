package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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
 * Whether this bill has been marked paid for its CURRENT occurrence — i.e. [RecurringEntity.lastPosted]
 * (set to "now" when the user taps Paid) falls inside the window of the occurrence that contains
 * [today]: the pay-cycle month for a monthly bill, the Mon–Sun week for a weekly one, the calendar
 * year for a yearly one. It therefore resets on its own when the next occurrence begins — last cycle's
 * timestamp lands outside the new window — with no scheduled job. A one-time entry stays paid once set.
 */
fun RecurringEntity.isPaidThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    if (lastPosted <= 0L) return false
    val (start, end) = when (cadence) {
        RecurringEntity.Cadence.ONCE -> return true
        RecurringEntity.Cadence.WEEKLY -> {
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            weekStart to weekStart.plusDays(6)
        }
        RecurringEntity.Cadence.YEARLY -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        else -> PayCycle.month(today, monthStartDay) // MONTHLY
    }
    val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return lastPosted in startMs..endMs
}

/** The date within [today]'s pay-cycle month whose day-of-month is [day] (clamped to the month). */
private fun dueDateThisCycle(today: LocalDate, monthStartDay: Int, day: Int): LocalDate {
    val (start, end) = PayCycle.month(today, monthStartDay)
    val d = day.coerceIn(1, 31)
    val candidate = clampDay(YearMonth.from(start), d)
    return if (!candidate.isBefore(start) && !candidate.isAfter(end)) candidate
    else clampDay(YearMonth.from(end), d)
}

/**
 * Whether this bill's due date within its current occurrence has already arrived (today on/after it):
 * the [RecurringEntity.dueDay]-th of the pay-cycle month for a monthly bill, the [dueDay] weekday of
 * this Mon–Sun week for a weekly one. Yearly (stores no month) and one-offs return false — they have
 * no computable due date to auto-mark against.
 */
fun RecurringEntity.isDuePassedThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): Boolean = when (cadence) {
    RecurringEntity.Cadence.MONTHLY -> !today.isBefore(dueDateThisCycle(today, monthStartDay, dueDay))
    RecurringEntity.Cadence.WEEKLY -> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        !today.isBefore(weekStart.plusDays((dueDay.coerceIn(1, 7) - 1).toLong()))
    }
    else -> false
}

/**
 * Whether a bill counts as paid for this cycle: either manually marked ([isPaidThisCycle]) or, when
 * [RecurringEntity.autoPay] is on, its due date has already passed ([isDuePassedThisCycle]). Auto-pay
 * only fills paid in once the day arrives; it never clears a manual payment.
 */
fun RecurringEntity.isEffectivelyPaidThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): Boolean = isPaidThisCycle(today, monthStartDay) ||
    (autoPay && isDuePassedThisCycle(today, monthStartDay))

/**
 * Whether autopay can apply to this entry at all — independent of the [RecurringEntity.autoPay]
 * switch. Bills only (never income), and only monthly or weekly cadences: yearly stores no month and
 * one-offs don't recur, so neither has a due date to auto-mark against.
 */
fun RecurringEntity.autoPayEligible(): Boolean =
    !isIncome &&
        (cadence == RecurringEntity.Cadence.MONTHLY || cadence == RecurringEntity.Cadence.WEEKLY)

/**
 * True when autopay is both switched on and [autoPayEligible] for this entry's cadence — the single
 * source of truth for "this bill is auto-managed". Used when persisting (so a yearly/one-off entry
 * can never keep a stuck autopay flag) and when displaying (so the Budget row shows a manual paid
 * toggle, not the "Auto" chip, for any ineligible entry — including rows saved before the rule was
 * enforced).
 */
fun RecurringEntity.isAutoPayActive(): Boolean = autoPay && autoPayEligible()

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

/** A recurring bill paired with the whole days until its next occurrence (0 = today), for the Home
 *  "Upcoming bills" card. */
data class UpcomingBill(
    val entity: RecurringEntity,
    val daysUntil: Int,
)

/**
 * The recurring bills (non-income) due soon, soonest first: each bill not already marked paid for
 * this cycle, paired with the whole days until its next occurrence. Only monthly and weekly bills
 * have a computable next date; yearly bills (which store no month) and one-offs are omitted.
 */
fun List<RecurringEntity>.upcomingBills(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): List<UpcomingBill> =
    filterNot { it.isIncome }
        // A bill paid for this cycle (manually or via autopay) drops off "upcoming" until next time.
        .filterNot { it.isEffectivelyPaidThisCycle(today, monthStartDay) }
        .mapNotNull { bill -> bill.nextOccurrenceDays(today)?.let { UpcomingBill(bill, it) } }
        .sortedBy { it.daysUntil }

/** Days from [today] to this bill's next occurrence, or null when it has no monthly/weekly schedule
 *  (yearly bills store no month, one-offs don't recur). */
fun RecurringEntity.nextOccurrenceDays(today: LocalDate = LocalDate.now()): Int? = when (cadence) {
    RecurringEntity.Cadence.MONTHLY -> {
        val day = dueDay.coerceIn(1, 31)
        var date = clampDay(YearMonth.from(today), day)
        if (date.isBefore(today)) date = clampDay(YearMonth.from(today).plusMonths(1), day)
        ChronoUnit.DAYS.between(today, date).toInt()
    }
    RecurringEntity.Cadence.WEEKLY -> {
        val target = dueDay.coerceIn(1, 7) // 1=Mon … 7=Sun
        var date = today
        while (date.dayOfWeek.value != target) date = date.plusDays(1)
        ChronoUnit.DAYS.between(today, date).toInt()
    }
    else -> null
}

private fun clampDay(month: YearMonth, day: Int): LocalDate =
    month.atDay(day.coerceAtMost(month.lengthOfMonth()))
