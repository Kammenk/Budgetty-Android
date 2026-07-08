package com.budgetty.app.store

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreNormalizerTest {

    @Test
    fun `merges Kaufland branch and legal-entity variants`() {
        assertEquals("Kaufland", StoreNormalizer.normalize("Kaufland Lyulin"))
        assertEquals("Kaufland", StoreNormalizer.normalize("КАУФЛАНД БЪЛГАРИЯ ЕООД ЕНД КО КД"))
        assertEquals("Kaufland", StoreNormalizer.normalize("kaufland"))
        assertEquals("Kaufland", StoreNormalizer.normalize("каулланд"))
    }

    @Test
    fun `matches Cyrillic and Latin spellings`() {
        assertEquals("Lidl", StoreNormalizer.normalize("ЛИДЛ БЪЛГАРИЯ"))
        assertEquals("Billa", StoreNormalizer.normalize("BILLA Sofia Mladost"))
    }

    @Test
    fun `matches multi-word brand`() {
        assertEquals("T-Market", StoreNormalizer.normalize("T MARKET Plovdiv"))
    }

    @Test
    fun `short alias only matches a whole word, not inside another`() {
        assertEquals("dm", StoreNormalizer.normalize("dm drogerie markt"))
        // "dm" appears inside "Goodman" but must not trigger a brand match.
        assertEquals("Goodman Store", StoreNormalizer.normalize("Goodman Store"))
    }

    @Test
    fun `unknown store falls through, trimmed and space-collapsed`() {
        assertEquals("Corner Shop", StoreNormalizer.normalize("  Corner   Shop  "))
    }

    @Test
    fun `blank stays blank`() {
        assertEquals("", StoreNormalizer.normalize("   "))
    }
}
