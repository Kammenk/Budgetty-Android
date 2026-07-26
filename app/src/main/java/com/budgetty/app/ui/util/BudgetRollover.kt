package com.budgetty.app.ui.util

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure math for budget rollover (unspent-only). A budget's period is the user's pay-cycle month; a
 * period is identified by its cycle-start [YearMonth] as "yyyy-MM" — which sorts lexicographically in
 * chronological order, so period keys can be compared as plain strings.
 */
object BudgetRollover {

    /** "yyyy-MM" of the pay-cycle month containing [today] (the cycle's start month). */
    fun currentPeriodKey(today: LocalDate, monthStartDay: Int): String =
        YearMonth.from(PayCycle.month(today, monthStartDay).first).toString()

    /** Inclusive [start, end] calendar dates of the pay-cycle whose start month is [periodKey]. */
    fun periodWindow(periodKey: String, monthStartDay: Int): Pair<LocalDate, LocalDate> {
        val month = YearMonth.parse(periodKey)
        fun anchor(m: YearMonth) = m.atDay(monthStartDay.coerceIn(1, m.lengthOfMonth()))
        return anchor(month) to anchor(month.plusMonths(1)).minusDays(1)
    }

    /** The period-key immediately after [periodKey]. */
    fun nextPeriodKey(periodKey: String): String = YearMonth.parse(periodKey).plusMonths(1).toString()

    /**
     * Rolls the carried balance forward from [storedPeriodKey] up to [currentPeriodKey]. Each elapsed
     * period's leftover — max(0, budget + carried − spent) — accumulates into the next; overspend is
     * forgiven (never rolls negative). [spentIn] returns the spend during a given period key. Returns
     * the carried amount as of [currentPeriodKey], or [storedCarried] unchanged once already caught up.
     */
    fun rollForward(
        storedCarried: BigDecimal,
        storedPeriodKey: String,
        currentPeriodKey: String,
        budget: BigDecimal,
        spentIn: (periodKey: String) -> BigDecimal,
    ): BigDecimal {
        var carried = storedCarried
        var period = storedPeriodKey
        var guard = 0
        while (period < currentPeriodKey && guard++ < MAX_PERIODS) {
            carried = (budget + carried - spentIn(period)).coerceAtLeast(BigDecimal.ZERO)
            period = nextPeriodKey(period)
        }
        return carried
    }

    /** Safety bound on the roll-forward loop (≈50 years) against a corrupt/ancient stored period key. */
    private const val MAX_PERIODS = 600
}
