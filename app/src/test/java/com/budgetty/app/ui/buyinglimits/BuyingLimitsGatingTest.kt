package com.budgetty.app.ui.buyinglimits

import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.repository.BuyingLimitsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins §4.5: the free tier is 3 limits, and the Add row only locks for a free user at that cap. */
class BuyingLimitsGatingTest {

    @Test
    fun freeLimitIsThree() {
        assertEquals(3, BuyingLimitsRepository.FREE_LIMIT)
    }

    private fun state(count: Int, premium: Boolean) = BuyingLimitsUiState(
        limits = (1..count).map {
            BuyingLimitCardUi(
                BuyingLimitEntity(it.toLong(), keywords = "k$it", timeframe = BuyingLimitTimeframe.WEEKLY, count = 1),
                bought = 0,
            )
        },
        isPremium = premium,
        isLoaded = true,
    )

    @Test
    fun freeUserLocksOnlyAtThree() {
        assertFalse("two of three used → still open", state(2, premium = false).atCap)
        assertTrue("three of three used → locked", state(3, premium = false).atCap)
    }

    @Test
    fun premiumNeverLocks() {
        assertFalse(state(5, premium = true).atCap)
    }
}
