package com.budgetty.app.ui.trips

import com.budgetty.app.data.local.TripEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Where spending sits against the even day-by-day pace of a trip's budget. */
enum class TripPaceState { UNDER_PACE, OVER_PACE, OVER_BUDGET }

/**
 * A trip budget's pace — the same spent-vs-time idea the Multiple-budgets pace bar uses. [fill] is the
 * fraction of the budget spent and [tickFraction] the fraction of the trip's days elapsed, both
 * clamped to 0..1 for drawing; [state] compares them. [suggestedDaily] is what you can spend each
 * remaining day and still land on budget (0 once you're over).
 */
data class TripPace(
    val fill: Float,
    val tickFraction: Float,
    val state: TripPaceState,
    val remaining: BigDecimal,
    val suggestedDaily: BigDecimal,
)

/**
 * The time-and-pace maths for a trip — pure so it can be unit-tested against the design's worked
 * example. [spent] and [expenseCount] are summed by the ViewModel from the transactions carrying the
 * trip's tag; everything here derives from the trip's dates/budget and [today].
 *
 * With no dates a trip still has a [perDay] (counted from when it was created), but no [totalDays],
 * [daysLeft], [dayStrip] or [pace] — those need a known span.
 */
data class TripStatsResult(
    val daysElapsed: Int,
    val totalDays: Int?,
    val daysLeft: Int?,
    val perDay: BigDecimal,
    val dayStrip: List<Boolean>,
    val pace: TripPace?,
)

object TripStats {

    fun compute(
        trip: TripEntity,
        spent: BigDecimal,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TripStatsResult {
        fun day(epoch: Long): LocalDate = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()

        val startDay = day(trip.startDate ?: trip.createdAt)
        val endDay = trip.endDate?.let(::day)
        val totalDays = endDay?.let { (ChronoUnit.DAYS.between(startDay, it) + 1).toInt().coerceAtLeast(1) }

        // Day 1 is the start day itself; clamp into [1, totalDays] so a future start or an overrun
        // still reads sensibly (day 1, or the final day).
        val rawElapsed = (ChronoUnit.DAYS.between(startDay, today) + 1).toInt()
        val daysElapsed = rawElapsed.coerceIn(1, totalDays ?: Int.MAX_VALUE)
        val daysLeft = totalDays?.let { (it - daysElapsed).coerceAtLeast(0) }

        val perDay = spent.divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP)
        val dayStrip = totalDays?.let { total -> List(total) { it < daysElapsed } }.orEmpty()

        val pace = trip.budgetAmount
            ?.takeIf { it.signum() > 0 && totalDays != null }
            ?.let { budget -> pace(spent, budget, daysElapsed, totalDays!!, daysLeft ?: 0) }

        return TripStatsResult(daysElapsed, totalDays, daysLeft, perDay, dayStrip, pace)
    }

    private fun pace(
        spent: BigDecimal,
        budget: BigDecimal,
        daysElapsed: Int,
        totalDays: Int,
        daysLeft: Int,
    ): TripPace {
        val spentFraction = spent.toDouble() / budget.toDouble()
        val timeFraction = daysElapsed.toDouble() / totalDays.toDouble()
        val remaining = budget.subtract(spent)
        val state = when {
            spent > budget -> TripPaceState.OVER_BUDGET
            spentFraction > timeFraction -> TripPaceState.OVER_PACE
            else -> TripPaceState.UNDER_PACE
        }
        // What's left, spread over the days still to come (today counts as a day you can still spend).
        val suggestedDaily = if (remaining.signum() <= 0) {
            BigDecimal.ZERO
        } else {
            remaining.divide(BigDecimal(daysLeft.coerceAtLeast(1)), 2, RoundingMode.HALF_UP)
        }
        return TripPace(
            fill = spentFraction.toFloat().coerceIn(0f, 1f),
            tickFraction = timeFraction.toFloat().coerceIn(0f, 1f),
            state = state,
            remaining = remaining,
            suggestedDaily = suggestedDaily,
        )
    }
}
