package com.budgetty.app.ui.budgets

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How a budget is tracking against its time window. */
enum class PaceState { ON_PACE, AHEAD, OVER_PACE, OVER_BUDGET }

/** The derived pace of a budget on a given day: bar fill, the "today" tick, and the derived figures. */
data class PaceResult(
    /** 0..1 fraction of the limit spent (capped at 1 for the bar). */
    val fill: Float,
    /** 0..1 fraction of the window elapsed — the "Today" tick position. */
    val todayFraction: Float,
    val state: PaceState,
    /** limit − spent (can be negative when over budget). */
    val remaining: BigDecimal,
    val daysLeft: Long,
    /** remaining ÷ days left — the daily allowance to stay on budget. */
    val dailyAllowance: BigDecimal,
    /** spent ÷ elapsed days — the actual average so far. */
    val avgPerDay: BigDecimal,
    /** Projected end-of-window total at the current pace (spent ÷ elapsed × total). */
    val projected: BigDecimal,
)

/** Pure pace math for a time-boxed budget. No Android deps, so it's unit-testable on the host. */
object BudgetPace {

    /** Above this spent-vs-elapsed ratio a budget is "over pace"; between 1.0 and here it's "ahead". */
    const val AHEAD_RATIO = 1.15

    /**
     * Pace for a budget of [limit] that has [spent] over an inclusive window [start]..[end], as of
     * [today]. On or below a 1.0 spent/elapsed ratio is on pace; up to [AHEAD_RATIO] is slightly ahead;
     * beyond that, or past 100% spent, is over.
     */
    fun compute(
        spent: BigDecimal,
        limit: BigDecimal,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
    ): PaceResult {
        val totalDays = ChronoUnit.DAYS.between(start, end.plusDays(1)).coerceAtLeast(1)
        val clampedToday = today.coerceIn(start, end)
        val elapsedDays = ChronoUnit.DAYS.between(start, clampedToday.plusDays(1)).coerceIn(1, totalDays)
        val daysLeft = (totalDays - elapsedDays).coerceAtLeast(0)

        val limitD = limit.toDouble().coerceAtLeast(0.01)
        val spentFraction = spent.toDouble() / limitD
        val elapsedFraction = elapsedDays.toDouble() / totalDays
        val ratio = if (elapsedFraction > 0) spentFraction / elapsedFraction else 0.0
        val state = when {
            spent > limit -> PaceState.OVER_BUDGET
            ratio > AHEAD_RATIO -> PaceState.OVER_PACE
            ratio > 1.0 -> PaceState.AHEAD
            else -> PaceState.ON_PACE
        }

        val remaining = limit.subtract(spent)
        val dailyAllowance =
            if (daysLeft > 0) remaining.divide(BigDecimal(daysLeft), 2, RoundingMode.HALF_UP) else remaining
        val avgPerDay = spent.divide(BigDecimal(elapsedDays), 2, RoundingMode.HALF_UP)
        val projected = spent.multiply(BigDecimal(totalDays))
            .divide(BigDecimal(elapsedDays), 2, RoundingMode.HALF_UP)

        return PaceResult(
            fill = spentFraction.coerceIn(0.0, 1.0).toFloat(),
            todayFraction = elapsedFraction.coerceIn(0.0, 1.0).toFloat(),
            state = state,
            remaining = remaining,
            daysLeft = daysLeft,
            dailyAllowance = dailyAllowance,
            avgPerDay = avgPerDay,
            projected = projected,
        )
    }
}
