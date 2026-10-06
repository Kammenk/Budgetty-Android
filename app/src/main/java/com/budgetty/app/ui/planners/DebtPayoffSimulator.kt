package com.budgetty.app.ui.planners

import java.math.BigDecimal
import java.math.RoundingMode

/** The order debts are attacked in once every minimum is covered. */
enum class PayoffStrategy {
    /** Smallest balance first — quick wins that keep motivation up. */
    SNOWBALL,

    /** Highest APR first — the least interest paid overall. */
    AVALANCHE,
}

/** One debt's figures fed into the simulation (names/emoji live on the entity, not here). */
data class DebtInput(
    val id: Long,
    val balance: BigDecimal,
    val aprPercent: BigDecimal,
    val minPayment: BigDecimal,
)

/**
 * The outcome of simulating a payoff plan: how long it takes, the interest paid, the total balance
 * after each month (for the chart), and the month each debt cleared. [clearedAll] is false when the
 * plan never reaches zero within the cap (e.g. a minimum that doesn't cover its own interest).
 */
data class DebtPayoffResult(
    val months: Int,
    val totalInterest: BigDecimal,
    /** Total balance owed at month 0, 1, 2, … down to 0 — length is [months] + 1. */
    val balanceSeries: List<BigDecimal>,
    /** Debt id → the month index (1-based) it was cleared; absent for a debt still owing at the cap. */
    val payoffMonthById: Map<Long, Int>,
    val clearedAll: Boolean,
)

/**
 * Month-by-month debt payoff simulation, matching the planner's design. Each month every debt accrues
 * interest, then pays its minimum; whatever is left of the fixed budget (the sum of all original
 * minimums plus the user's extra — so a cleared debt's minimum frees up and cascades) attacks one debt
 * at a time in the strategy's order. A null [strategy] is the minimums-only baseline the plan is
 * compared against. Pure Double math (an estimate), capped at [MAX_MONTHS] so a self-growing balance
 * can't loop forever.
 */
object DebtPayoffSimulator {

    /** 50 years — well past any realistic payoff; a plan still owing here never clears at this rate. */
    const val MAX_MONTHS = 600

    fun simulate(
        debts: List<DebtInput>,
        extraPerMonth: BigDecimal,
        strategy: PayoffStrategy?,
    ): DebtPayoffResult {
        val active = debts
            .filter { it.balance.signum() > 0 }
            .map {
                MutableDebt(it.id, it.balance.toDouble(), it.aprPercent.toDouble() / 1200.0, it.minPayment.toDouble())
            }
        if (active.isEmpty()) {
            return DebtPayoffResult(0, BigDecimal.ZERO, listOf(BigDecimal.ZERO), emptyMap(), clearedAll = true)
        }

        val budget = active.sumOf { it.min } + extraPerMonth.toDouble().coerceAtLeast(0.0)
        val payoffMonth = HashMap<Long, Int>()
        val series = ArrayList<Double>()
        series += active.sumOf { it.balance }
        var totalInterest = 0.0
        var month = 0

        while (active.any { it.balance > CENT } && month < MAX_MONTHS) {
            month++
            // Accrue interest on everything still owing.
            active.forEach { d ->
                if (d.balance > 0) {
                    val interest = d.balance * d.monthlyRate
                    d.balance += interest
                    totalInterest += interest
                }
            }
            // Every debt pays its minimum first.
            var available = budget
            active.forEach { d ->
                if (d.balance > 0) {
                    val pay = minOf(d.min, d.balance)
                    d.balance -= pay
                    available -= pay
                }
            }
            // The remainder cascades onto the target debt(s) in strategy order.
            if (strategy != null && available > CENT) {
                val order = active.filter { it.balance > 0 }.sortedWith(strategy.comparator())
                for (d in order) {
                    if (available <= CENT) break
                    val pay = minOf(available, d.balance)
                    d.balance -= pay
                    available -= pay
                }
            }
            // Record any debt that just cleared.
            active.forEach { d ->
                if (d.balance <= CENT && d.id !in payoffMonth) {
                    d.balance = 0.0
                    payoffMonth[d.id] = month
                }
            }
            series += active.sumOf { it.balance }
        }

        val cleared = active.none { it.balance > CENT }
        return DebtPayoffResult(
            months = month,
            totalInterest = BigDecimal(totalInterest).setScale(2, RoundingMode.HALF_UP),
            balanceSeries = series.map { BigDecimal(it).setScale(2, RoundingMode.HALF_UP) },
            payoffMonthById = payoffMonth,
            clearedAll = cleared,
        )
    }

    private fun PayoffStrategy.comparator(): Comparator<MutableDebt> = when (this) {
        PayoffStrategy.SNOWBALL -> compareBy { it.balance }
        PayoffStrategy.AVALANCHE -> compareByDescending { it.monthlyRate }
    }

    /** Half a cent — the "effectively zero" threshold so float dust doesn't keep a debt alive. */
    private const val CENT = 0.005

    private class MutableDebt(val id: Long, var balance: Double, val monthlyRate: Double, val min: Double)
}
