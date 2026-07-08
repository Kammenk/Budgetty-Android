package com.budgetty.app.ui.insights

import androidx.annotation.StringRes
import com.budgetty.app.R
import com.budgetty.app.ui.home.dateRangeToEpochMillis
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The granularity the Insights period stepper moves by. Each unit is a calendar-aligned block the
 * back/forward arrows walk through one step at a time: a locale week, a calendar month, a calendar
 * quarter (Jan–Mar …) or a half-year (Jan–Jun / Jul–Dec). Persisted by [name], so values must stay
 * stable; [labelRes] names the unit in the stepper's dropdown.
 */
enum class PeriodUnit(@param:StringRes val labelRes: Int) {
    WEEK(R.string.period_unit_week),
    MONTH(R.string.period_unit_month),
    QUARTER(R.string.period_unit_quarter),
    HALF_YEAR(R.string.period_unit_half_year),
}

/**
 * The time window the Insights screen is showing: a calendar-aligned [Stepped] block the period
 * stepper walks through, or a user-picked [Custom] start/end range. Both resolve to an inclusive
 * epoch-millis window via [toRange] so the rest of the screen treats them the same.
 */
sealed interface InsightsPeriod {

    fun toRange(today: LocalDate = LocalDate.now()): Pair<Long, Long>

    /**
     * A calendar-aligned block of [unit], [offset] units back from the one containing today
     * (0 = current, −1 = previous, …). Weeks honor the locale's first day of week; quarters are
     * calendar quarters and halves are Jan–Jun / Jul–Dec. Forward stepping is capped at offset 0 by
     * the view model — [toRange] itself imposes no cap, so a future offset is representable.
     */
    data class Stepped(val unit: PeriodUnit, val offset: Int = 0) : InsightsPeriod {
        override fun toRange(today: LocalDate): Pair<Long, Long> =
            bounds(today).let { (start, end) -> dateRangeToEpochMillis(start, end) }

        /** The inclusive [start, end] calendar dates of this block. */
        fun bounds(
            today: LocalDate = LocalDate.now(),
            locale: Locale = Locale.getDefault(),
        ): Pair<LocalDate, LocalDate> = when (unit) {
            PeriodUnit.WEEK -> {
                val first = today.with(WeekFields.of(locale).dayOfWeek(), 1L).plusWeeks(offset.toLong())
                first to first.plusDays(6)
            }
            PeriodUnit.MONTH -> YearMonth.from(today).plusMonths(offset.toLong())
                .let { it.atDay(1) to it.atEndOfMonth() }
            PeriodUnit.QUARTER -> YearMonth.of(today.year, firstMonthOfQuarter(today.monthValue))
                .plusMonths(offset * 3L)
                .let { it.atDay(1) to it.plusMonths(2).atEndOfMonth() }
            PeriodUnit.HALF_YEAR -> YearMonth.of(today.year, if (today.monthValue <= 6) 1 else 7)
                .plusMonths(offset * 6L)
                .let { it.atDay(1) to it.plusMonths(5).atEndOfMonth() }
        }

        /** The equal-length block immediately before this one — powers period-over-period compares. */
        fun previous(): Stepped = copy(offset = offset - 1)

        /** First calendar month (1–12) of the quarter containing [month]: 1, 4, 7 or 10. */
        private fun firstMonthOfQuarter(month: Int): Int = ((month - 1) / 3) * 3 + 1
    }

    data class Custom(val start: LocalDate, val end: LocalDate) : InsightsPeriod {
        override fun toRange(today: LocalDate): Pair<Long, Long> = dateRangeToEpochMillis(start, end)
    }
}

/**
 * The equal-length window immediately before [this] period, for period-over-period comparison: one
 * [PeriodUnit] earlier for a [InsightsPeriod.Stepped] block, or the preceding same-length span for a
 * [InsightsPeriod.Custom] range.
 */
fun InsightsPeriod.previousPeriod(): InsightsPeriod = when (this) {
    is InsightsPeriod.Stepped -> previous()
    is InsightsPeriod.Custom -> {
        val length = ChronoUnit.DAYS.between(start, end)
        val prevEnd = start.minusDays(1)
        InsightsPeriod.Custom(prevEnd.minusDays(length), prevEnd)
    }
}
