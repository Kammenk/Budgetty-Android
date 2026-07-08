package com.budgetty.app.ui.home

import androidx.annotation.StringRes
import com.budgetty.app.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Selectable time windows for the Home screen. Default is [CURRENT_MONTH]. */
enum class DateRangeFilter(val label: String, @param:StringRes val labelRes: Int) {
    CURRENT_MONTH("This month", R.string.period_this_month),
    LAST_MONTH("Last month", R.string.period_last_month),
    LAST_3_MONTHS("Last 3 months", R.string.period_last_3_months),
    LAST_6_MONTHS("Last 6 months", R.string.period_last_6_months);

    /** How many calendar months this window spans, used to find the preceding equal-length period. */
    val monthSpan: Int
        get() = when (this) {
            CURRENT_MONTH, LAST_MONTH -> 1
            LAST_3_MONTHS -> 3
            LAST_6_MONTHS -> 6
        }

    /** Inclusive [start, end] epoch-millis window for this filter, anchored to [today]. */
    fun toRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val currentMonth = YearMonth.from(today)
        val (startDate, endExclusive) = when (this) {
            CURRENT_MONTH -> currentMonth.atDay(1) to currentMonth.plusMonths(1).atDay(1)
            LAST_MONTH -> {
                val prev = currentMonth.minusMonths(1)
                prev.atDay(1) to currentMonth.atDay(1)
            }
            LAST_3_MONTHS -> currentMonth.minusMonths(2).atDay(1) to currentMonth.plusMonths(1).atDay(1)
            LAST_6_MONTHS -> currentMonth.minusMonths(5).atDay(1) to currentMonth.plusMonths(1).atDay(1)
        }
        return dateRangeToEpochMillis(startDate, endExclusive.minusDays(1))
    }

    /**
     * Inclusive [start, end] epoch-millis window for the equal-length period immediately preceding
     * this one — e.g. for [CURRENT_MONTH] it's last month. Since every preset is month-aligned, the
     * preceding window is just this filter re-anchored [monthSpan] months earlier.
     */
    fun previousRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> =
        toRange(today.minusMonths(monthSpan.toLong()))
}

/**
 * Inclusive [start, end] epoch-millis window spanning [startDate] 00:00 through the final
 * millisecond of [endInclusive], both interpreted in [zone]. Shared by the preset [DateRangeFilter]
 * windows and the Insights custom date range so they bucket transactions identically.
 */
fun dateRangeToEpochMillis(
    startDate: LocalDate,
    endInclusive: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<Long, Long> {
    val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}
