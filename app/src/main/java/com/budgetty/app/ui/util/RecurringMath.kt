package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Shared money-flow math for income & recurring payments, so the Budget screen and the History
 * "Budgets" tab compute identical monthly totals (and treat one-time entries the same way).
 */

/** Inclusive [start, end] epoch-millis window for [today]'s calendar month. */
fun currentMonthRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val month = YearMonth.from(today)
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
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
 * The entry's contribution to a whole [monthSpan]-month window running [windowStart]..[windowEnd].
 * Recurring cadences repeat, so they scale to their monthly-equivalent × [monthSpan]; a one-time
 * entry instead counts its full amount exactly once (only when it was added inside the window),
 * rather than being multiplied across every month of it. Used to scale the History Budgets snapshot
 * to the selected date range the same way the rest of the History filters narrow by period.
 */
fun RecurringEntity.periodAmount(windowStart: Long, windowEnd: Long, monthSpan: Int): BigDecimal =
    if (cadence == RecurringEntity.Cadence.ONCE) {
        if (createdAt in windowStart..windowEnd) amount else BigDecimal.ZERO
    } else {
        monthlyAmount(windowStart, windowEnd).multiply(BigDecimal(monthSpan))
    }
