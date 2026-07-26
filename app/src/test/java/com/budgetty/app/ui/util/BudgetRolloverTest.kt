package com.budgetty.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetRolloverTest {

    @Test
    fun `current period key is the pay-cycle start month`() {
        assertEquals("2026-07", BudgetRollover.currentPeriodKey(LocalDate.of(2026, 7, 25), monthStartDay = 1))
        // Pay day 25: Jul 25 opens the Jul cycle; Jul 10 still sits in the Jun 25 - Jul 24 cycle.
        assertEquals("2026-07", BudgetRollover.currentPeriodKey(LocalDate.of(2026, 7, 25), monthStartDay = 25))
        assertEquals("2026-06", BudgetRollover.currentPeriodKey(LocalDate.of(2026, 7, 10), monthStartDay = 25))
    }

    @Test
    fun `next period key rolls the year over`() {
        assertEquals("2027-01", BudgetRollover.nextPeriodKey("2026-12"))
    }

    @Test
    fun `unspent budget accumulates across elapsed periods`() {
        // Budget 400. May spent 350 -> 50 left; June spent 300 -> 400+50-300 = 150 left. Roll to July.
        val spent = mapOf("2026-05" to BigDecimal("350"), "2026-06" to BigDecimal("300"))
        val carried = BudgetRollover.rollForward(
            storedCarried = BigDecimal.ZERO,
            storedPeriodKey = "2026-05",
            currentPeriodKey = "2026-07",
            budget = BigDecimal("400"),
        ) { spent[it] ?: BigDecimal.ZERO }
        assertEquals(BigDecimal("150"), carried)
    }

    @Test
    fun `overspend is forgiven, never rolls negative`() {
        val carried = BudgetRollover.rollForward(
            storedCarried = BigDecimal.ZERO,
            storedPeriodKey = "2026-05",
            currentPeriodKey = "2026-06",
            budget = BigDecimal("400"),
        ) { BigDecimal("500") } // spent 500 on a 400 budget
        assertEquals(BigDecimal.ZERO, carried)
    }

    @Test
    fun `the carried buffer absorbs overspend but never goes negative`() {
        // Available = budget + carried = 400 + 120 = 520; spending 500 draws the buffer down to 20.
        val carried = BudgetRollover.rollForward(
            storedCarried = BigDecimal("120"),
            storedPeriodKey = "2026-05",
            currentPeriodKey = "2026-06",
            budget = BigDecimal("400"),
        ) { BigDecimal("500") }
        assertEquals(BigDecimal("20"), carried)
    }

    @Test
    fun `already caught up is a no-op`() {
        val carried = BudgetRollover.rollForward(
            storedCarried = BigDecimal("50"),
            storedPeriodKey = "2026-07",
            currentPeriodKey = "2026-07",
            budget = BigDecimal("400"),
        ) { error("should not query spend when already current") }
        assertEquals(BigDecimal("50"), carried)
    }
}
