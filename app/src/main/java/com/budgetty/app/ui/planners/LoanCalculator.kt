package com.budgetty.app.ui.planners

import java.math.BigDecimal
import java.math.RoundingMode

/** One year of a loan's amortisation: how much of the year's payments went to principal vs interest,
 *  and the balance still owed at the end of that year. */
data class LoanYear(
    val year: Int,
    val principalPaid: BigDecimal,
    val interestPaid: BigDecimal,
    val endBalance: BigDecimal,
)

/**
 * The amortised result of a fixed-rate loan: the level monthly payment and where the money goes over
 * the whole term. Everything is an estimate (fees and lender rounding aside) — see the screen's note.
 */
data class LoanResult(
    val monthlyPayment: BigDecimal,
    val months: Int,
    val totalInterest: BigDecimal,
    val totalPaid: BigDecimal,
    /** Share of total paid that is principal, 0–100 (the rest is interest). */
    val principalPercent: Int,
    val years: List<LoanYear>,
) {
    val interestPercent: Int get() = 100 - principalPercent
}

/**
 * Closed-form loan amortisation. The level monthly payment is
 * `P·r ÷ (1 − (1 + r)^−n)` with `r = APR ÷ 12` (monthly rate) and `n` months, degrading to `P ÷ n`
 * at 0% so a zero-interest loan still returns a sensible payment. The per-year breakdown re-runs the
 * month-by-month schedule so principal/interest/balance always reconcile with the payment shown.
 */
object LoanCalculator {

    fun compute(amount: BigDecimal, aprPercent: BigDecimal, years: Int): LoanResult {
        val principal = amount.toDouble().coerceAtLeast(0.0)
        val termYears = years.coerceAtLeast(1)
        val n = termYears * 12
        val monthlyRate = aprPercent.toDouble() / 1200.0

        val payment = if (monthlyRate <= 0.0) {
            principal / n
        } else {
            principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -n.toDouble()))
        }

        var balance = principal
        val yearRows = ArrayList<LoanYear>(termYears)
        for (y in 1..termYears) {
            var principalThisYear = 0.0
            var interestThisYear = 0.0
            repeat(12) {
                if (balance > 0.0) {
                    val interest = balance * monthlyRate
                    val principalPart = (payment - interest).coerceAtMost(balance)
                    interestThisYear += interest
                    principalThisYear += principalPart
                    balance -= principalPart
                }
            }
            yearRows += LoanYear(
                year = y,
                principalPaid = principalThisYear.toMoney(),
                interestPaid = interestThisYear.toMoney(),
                endBalance = balance.coerceAtLeast(0.0).toMoney(),
            )
        }

        val totalPaid = payment * n
        val totalInterest = (totalPaid - principal).coerceAtLeast(0.0)
        val principalPercent = if (totalPaid > 0.0) {
            (principal / totalPaid * 100).roundToIntSafe()
        } else {
            100
        }

        return LoanResult(
            monthlyPayment = payment.toMoney(),
            months = n,
            totalInterest = totalInterest.toMoney(),
            totalPaid = totalPaid.toMoney(),
            principalPercent = principalPercent.coerceIn(0, 100),
            years = yearRows,
        )
    }

    private fun Double.toMoney(): BigDecimal = BigDecimal(this).setScale(2, RoundingMode.HALF_UP)

    private fun Double.roundToIntSafe(): Int =
        if (isNaN() || isInfinite()) 0 else Math.round(this).toInt()
}
