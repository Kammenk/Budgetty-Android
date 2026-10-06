package com.budgetty.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TagEntityTest {

    @Test
    fun `lowercases and trims`() {
        assertEquals("work", TagEntity.normalize("  Work "))
    }

    @Test
    fun `strips a leading hash`() {
        assertEquals("reimbursable", TagEntity.normalize("#reimbursable"))
        assertEquals("work", TagEntity.normalize("##Work"))
    }

    @Test
    fun `turns spaces into hyphens`() {
        assertEquals("work-trip", TagEntity.normalize("Work Trip"))
        assertEquals("groceries-run", TagEntity.normalize("  Groceries   Run  "))
    }

    @Test
    fun `is Cyrillic-safe`() {
        assertEquals("дача", TagEntity.normalize("#Дача"))
        assertEquals("лисабон-2026", TagEntity.normalize("Лисабон 2026"))
    }

    @Test
    fun `drops punctuation and symbols but keeps letters, numbers and hyphens`() {
        assertEquals("tax-deductible", TagEntity.normalize("tax-deductible!"))
        assertEquals("lisbon2026", TagEntity.normalize("lisbon@2026"))
        // Spaces become hyphens before the slash is dropped, so each space leaves its own hyphen.
        assertEquals("a--b", TagEntity.normalize("a / b"))
    }

    @Test
    fun `returns empty for input with no usable characters`() {
        assertEquals("", TagEntity.normalize("   "))
        assertEquals("", TagEntity.normalize("#"))
        assertEquals("", TagEntity.normalize("!!!"))
    }
}
