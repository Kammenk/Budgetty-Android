package com.budgetty.app.data.ingest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for [itemlessTotalFallback] — the itemless-slip stand-in that fixes the card-slip
 * double-count. A receipt with no line items but a printed total used to park the whole amount in the
 * invisible extraCharges residual, so typing the one obvious item doubled the finalized total
 * (a 36.92 slip finalizing at 73.84). The fallback surfaces the total as a real, editable line and the
 * caller zeroes the residual, so it is counted exactly once.
 */
class ItemlessTotalFallbackTest {

    private fun product(price: String) = ParsedTransaction(name = "Item", price = BigDecimal(price))

    @Test
    fun `itemless slip yields one stand-in line worth the whole total`() {
        val result = itemlessTotalFallback(total = 36.92, products = emptyList(), chargeItems = emptyList())

        assertEquals(1, result.size)
        // The stand-in equals the total exactly — so with extraCharges zeroed, the finalize total is
        // 36.92, never 2 x 36.92.
        assertEquals(0, result.single().price.compareTo(BigDecimal("36.92")))
        // Blank name/category so the user names and categorizes it, like a fresh manual item.
        assertEquals("", result.single().name)
        assertEquals("", result.single().category)
    }

    @Test
    fun `a receipt with product lines is left untouched`() {
        val result = itemlessTotalFallback(
            total = 40.30,
            products = listOf(product("12.90"), product("14.50")),
            chargeItems = emptyList(),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `a slip with only a fee or tip line is left untouched`() {
        val result = itemlessTotalFallback(
            total = 5.00,
            products = emptyList(),
            chargeItems = listOf(product("5.00")),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no positive total means no stand-in line`() {
        assertTrue(itemlessTotalFallback(total = null, products = emptyList(), chargeItems = emptyList()).isEmpty())
        assertTrue(itemlessTotalFallback(total = 0.0, products = emptyList(), chargeItems = emptyList()).isEmpty())
    }
}
