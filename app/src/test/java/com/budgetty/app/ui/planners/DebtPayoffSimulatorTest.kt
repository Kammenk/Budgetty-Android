package com.budgetty.app.ui.planners

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DebtPayoffSimulatorTest {

    private fun debt(id: Long, bal: String, apr: String, min: String) =
        DebtInput(id, BigDecimal(bal), BigDecimal(apr), BigDecimal(min))

    @Test fun no_debts_clears_immediately() {
        val r = DebtPayoffSimulator.simulate(emptyList(), BigDecimal.ZERO, PayoffStrategy.AVALANCHE)
        assertEquals(0, r.months)
        assertTrue(r.clearedAll)
    }

    @Test fun single_zero_interest_debt_pays_off_by_minimum() {
        val r = DebtPayoffSimulator.simulate(
            listOf(debt(1, "1000", "0", "100")),
            extraPerMonth = BigDecimal.ZERO,
            strategy = PayoffStrategy.SNOWBALL,
        )
        assertEquals(10, r.months)
        assertEquals(0, r.totalInterest.compareTo(BigDecimal.ZERO))
        assertEquals(10, r.payoffMonthById[1])
        // Series starts at the full balance and ends at zero.
        assertEquals(0, r.balanceSeries.first().compareTo(BigDecimal("1000.00")))
        assertEquals(0, r.balanceSeries.last().compareTo(BigDecimal.ZERO))
        assertTrue(r.clearedAll)
    }

    @Test fun extra_payment_clears_sooner() {
        val debts = listOf(debt(1, "2000", "12", "50"))
        val slow = DebtPayoffSimulator.simulate(debts, BigDecimal.ZERO, PayoffStrategy.AVALANCHE)
        val fast = DebtPayoffSimulator.simulate(debts, BigDecimal("150"), PayoffStrategy.AVALANCHE)
        assertTrue("extra should be faster", fast.months < slow.months)
        assertTrue("extra should cost less interest", fast.totalInterest < slow.totalInterest)
    }

    /** Avalanche (highest APR first) never costs more interest than snowball on the same debts. */
    @Test fun avalanche_is_no_costlier_than_snowball() {
        val debts = listOf(
            debt(1, "2400", "19.9", "60"),
            debt(2, "6800", "6.5", "210"),
            debt(3, "650", "24.9", "25"),
            debt(4, "4200", "3.2", "90"),
        )
        val extra = BigDecimal("150")
        val snow = DebtPayoffSimulator.simulate(debts, extra, PayoffStrategy.SNOWBALL)
        val aval = DebtPayoffSimulator.simulate(debts, extra, PayoffStrategy.AVALANCHE)
        assertTrue(snow.clearedAll && aval.clearedAll)
        assertTrue("avalanche interest ${aval.totalInterest} > snowball ${snow.totalInterest}",
            aval.totalInterest <= snow.totalInterest)
        // Every debt gets a payoff month under a plan that clears.
        assertEquals(4, aval.payoffMonthById.size)
    }

    /** A minimum that doesn't cover its interest never clears — flagged, not looped forever. */
    @Test fun unpayable_minimum_is_flagged() {
        val r = DebtPayoffSimulator.simulate(
            listOf(debt(1, "1000", "120", "1")),
            extraPerMonth = BigDecimal.ZERO,
            strategy = null,
        )
        assertFalse(r.clearedAll)
        assertEquals(DebtPayoffSimulator.MAX_MONTHS, r.months)
    }
}
