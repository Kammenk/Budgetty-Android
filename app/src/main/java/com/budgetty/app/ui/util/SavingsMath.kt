package com.budgetty.app.ui.util

import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Derived progress for one savings goal, from its target and the sum of its dated contributions.
 * [monthlyRate] and [behind] are only meaningful when the goal has a target date and isn't reached.
 */
data class SavingsProgress(
    val saved: BigDecimal,
    val remaining: BigDecimal,
    val fraction: Float,
    val reached: Boolean,
    /** remaining ÷ whole months to the target date; null when there's no date or the goal is reached. */
    val monthlyRate: BigDecimal?,
    /** True when the required [monthlyRate] exceeds the goal's recent average monthly deposit. */
    val behind: Boolean,
)

/** Pure math for savings goals — a goal's saved total is the signed sum of its contributions. */
object SavingsMath {

    fun savedOf(contributions: List<SavingsContributionEntity>): BigDecimal =
        contributions.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }

    fun progress(
        goal: SavingsGoalEntity,
        contributions: List<SavingsContributionEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): SavingsProgress {
        val saved = savedOf(contributions)
        val target = goal.targetAmount
        val remaining = (target - saved).max(BigDecimal.ZERO)
        val fraction = if (target.signum() <= 0) 0f
        else (saved.toDouble() / target.toDouble()).coerceIn(0.0, 1.0).toFloat()
        val reached = target.signum() > 0 && saved >= target

        if (reached || goal.targetDate == null || remaining.signum() <= 0) {
            return SavingsProgress(saved, remaining, fraction, reached, null, false)
        }
        val targetDate = Instant.ofEpochMilli(goal.targetDate).atZone(zone).toLocalDate()
        val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate))
            .toInt().coerceAtLeast(1)
        val rate = remaining.divide(BigDecimal(months), 2, RoundingMode.HALF_UP)
        val avg = averageMonthlyDeposit(contributions, zone)
        return SavingsProgress(saved, remaining, fraction, reached, rate, avg != null && rate > avg)
    }

    /** Average monthly deposit across the distinct months that have a deposit (withdrawals ignored). */
    private fun averageMonthlyDeposit(
        contributions: List<SavingsContributionEntity>,
        zone: ZoneId,
    ): BigDecimal? {
        val deposits = contributions.filter { it.amount.signum() > 0 }
        if (deposits.isEmpty()) return null
        val months = deposits
            .map { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(zone)) }
            .distinct().size.coerceAtLeast(1)
        val total = deposits.fold(BigDecimal.ZERO) { a, c -> a + c.amount }
        return total.divide(BigDecimal(months), 2, RoundingMode.HALF_UP)
    }
}
