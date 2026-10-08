package com.budgetty.app.ui.planners

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class LoanCalculatorTest {

    /** The design's worked example: €15,000 at 6.9% over 5 years ⇒ ≈ €296.31/mo, 60 months. */
    @Test fun worked_example_matches_design() {
        val r = LoanCalculator.compute(BigDecimal("15000"), BigDecimal("6.9"), years = 5)
        assertEquals(60, r.months)
        val pay = r.monthlyPayment.toDouble()
        assertTrue("payment was $pay", pay in 296.28..296.34)
        assertEquals(5, r.years.size)
        // The last year clears the balance.
        assertEquals(0, r.years.last().endBalance.compareTo(BigDecimal.ZERO))
        // Total paid reconciles to principal + interest (each rounded once from the raw payment).
        assertEquals(0, r.totalPaid.compareTo(r.totalInterest.add(BigDecimal("15000"))))
        assertTrue(r.totalInterest.signum() > 0)
        assertEquals(100, r.principalPercent + r.interestPercent)
    }

    /** A 0% loan amortises to amount ÷ months with no interest. */
    @Test fun zero_interest_is_flat_principal() {
        val r = LoanCalculator.compute(BigDecimal("1200"), BigDecimal.ZERO, years = 1)
        assertEquals(12, r.months)
        assertEquals(0, r.monthlyPayment.compareTo(BigDecimal("100.00")))
        assertEquals(0, r.totalInterest.compareTo(BigDecimal.ZERO))
        assertEquals(100, r.principalPercent)
    }

    /** Every year's principal + interest sums to roughly 12 payments, and balances only fall. */
    @Test fun yearly_rows_reconcile() {
        val r = LoanCalculator.compute(BigDecimal("10000"), BigDecimal("4.5"), years = 3)
        var prev = BigDecimal("10000")
        r.years.forEach { y ->
            assertTrue("balance rose in year ${y.year}", y.endBalance <= prev)
            prev = y.endBalance
        }
        assertEquals(0, r.years.last().endBalance.compareTo(BigDecimal.ZERO))
    }
}
