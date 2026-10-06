package com.budgetty.app.data.forecast

import com.budgetty.app.data.local.RecurringEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Expands recurring income & bills into the dated [CashEvent]s the forecast projects over
 * `[today, endDate]`: a monthly entry lands on its due-day each month; a weekly entry on each matching
 * weekday. Yearly entries (which store no month) and one-offs don't project forward and are skipped.
 * Income is positive, a bill negative — matching [CashFlowForecast.project]'s sign convention.
 */
fun List<RecurringEntity>.toForecastEvents(today: LocalDate, endDate: LocalDate): List<CashEvent> {
    val events = ArrayList<CashEvent>()
    for (r in this) {
        val signed = if (r.isIncome) r.amount else r.amount.negate()
        when (r.cadence) {
            RecurringEntity.Cadence.MONTHLY -> {
                var month = YearMonth.from(today)
                val lastMonth = YearMonth.from(endDate)
                while (!month.isAfter(lastMonth)) {
                    val date = month.atDay(r.dueDay.coerceIn(1, month.lengthOfMonth()))
                    if (!date.isBefore(today) && !date.isAfter(endDate)) events.add(CashEvent(date, signed))
                    month = month.plusMonths(1)
                }
            }
            RecurringEntity.Cadence.WEEKLY -> {
                val targetWeekday = r.dueDay.coerceIn(1, 7) // 1 = Monday … 7 = Sunday
                var date = today
                while (!date.isAfter(endDate)) {
                    if (date.dayOfWeek.value == targetWeekday) events.add(CashEvent(date, signed))
                    date = date.plusDays(1)
                }
            }
            else -> Unit // YEARLY stores no month; ONCE is a past one-off — neither projects forward.
        }
    }
    return events
}
