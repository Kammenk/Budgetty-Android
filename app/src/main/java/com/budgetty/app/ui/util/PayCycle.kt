package com.budgetty.app.ui.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * Resolves the user's "financial month" — a month that begins on their pay day ([startDay]) instead
 * of the calendar 1st. This shifts the whole monthly cycle: a [startDay] of 25 makes each month run
 * the 25th → the 24th of the next month, so "this month", "last month" and the monthly budget all
 * follow the salary rather than the calendar.
 *
 * A [startDay] of 1 is the ordinary calendar month, so the default preserves the previous behaviour.
 * A [startDay] past the length of a short month clamps to that month's last day (a 31st pay day
 * starts February on the 28th/29th), matching how a recurring bill's due-day is clamped.
 */
object PayCycle {

    /**
     * The pay-cycle month [offset] cycles from the one containing [today] (0 = current, −1 = previous,
     * +1 = next), as an inclusive [start, end] pair of calendar dates. With [startDay] = 1 this is the
     * plain calendar month; otherwise the cycle is anchored on [startDay], clamped per month length.
     */
    fun month(today: LocalDate, startDay: Int, offset: Int = 0): Pair<LocalDate, LocalDate> {
        // The month whose anchored start day opens the cycle that contains today: this calendar month
        // once its start day has arrived, otherwise the previous one (today sits in its tail end).
        val thisMonth = YearMonth.from(today)
        val currentCycleMonth =
            if (!today.isBefore(anchor(thisMonth, startDay))) thisMonth else thisMonth.minusMonths(1)
        val cycleMonth = currentCycleMonth.plusMonths(offset.toLong())
        val start = anchor(cycleMonth, startDay)
        val end = anchor(cycleMonth.plusMonths(1), startDay).minusDays(1)
        return start to end
    }

    /** The [startDay] of [month], clamped to the month's length so short months stay valid. */
    private fun anchor(month: YearMonth, startDay: Int): LocalDate =
        month.atDay(startDay.coerceIn(1, month.lengthOfMonth()))
}
