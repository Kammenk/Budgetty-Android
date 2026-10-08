package com.budgetty.app.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

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

    /** Length of a fortnight — a fixed 14-day block, unlike the 28–31-day pay-cycle month. */
    const val FORTNIGHT_DAYS = 14L

    /**
     * The 14-day "fortnight" window [offset] fortnights from the one containing [today] (0 = current,
     * −1 = previous), as an inclusive [start, end] pair. Fortnights run continuously every 14 days from
     * [anchorEpochDay] (a reference pay day as an epoch day), so — unlike [month] — they keep the same
     * length across month boundaries and never re-anchor. The anchor may sit in the future or the past;
     * the block containing today is found by snapping down to the nearest 14-day boundary (floor
     * division, so a today before the anchor still resolves to the block that contains it).
     */
    fun fortnight(today: LocalDate, anchorEpochDay: Long, offset: Int = 0): Pair<LocalDate, LocalDate> {
        val anchor = LocalDate.ofEpochDay(anchorEpochDay)
        val blocks = Math.floorDiv(ChronoUnit.DAYS.between(anchor, today), FORTNIGHT_DAYS)
        val start = anchor.plusDays((blocks + offset) * FORTNIGHT_DAYS)
        return start to start.plusDays(FORTNIGHT_DAYS - 1)
    }

    /**
     * A stable default fortnight anchor (epoch day) when the user has none persisted: the start of the
     * pay-cycle month containing [today]. Deriving it once — then persisting it — keeps fortnights from
     * drifting, since re-deriving each month would jump the anchor by a non-multiple of 14 days.
     */
    fun defaultFortnightAnchor(today: LocalDate, monthStartDay: Int): Long =
        month(today, monthStartDay).first.toEpochDay()
}
