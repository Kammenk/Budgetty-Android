package com.budgetty.app.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [CategoryRuleEntity.key] is the match key that makes a learned "name → category" rule stick across
 * receipts. Lower-casing is done in Kotlin rather than SQLite `NOCASE`/`LOWER()` specifically so
 * non-ASCII names fold — Bulgarian receipts are the reason. These cases guard that contract, since a
 * key that folds ASCII but not Cyrillic would silently stop matching real receipts.
 */
class CategoryRuleEntityTest {

    @Test
    fun `trims surrounding whitespace`() {
        assertThat(CategoryRuleEntity.key("  Milk  ")).isEqualTo("milk")
    }

    @Test
    fun `lower-cases ASCII`() {
        assertThat(CategoryRuleEntity.key("COCA-COLA")).isEqualTo("coca-cola")
    }

    @Test
    fun `folds Cyrillic case — the reason lower-casing is not left to SQLite`() {
        // МЛЯКО (Bulgarian for "milk"), upper-case, must fold to its lower-case form. SQLite's LOWER()
        // would leave this untouched and the rule would never match the next receipt.
        assertThat(CategoryRuleEntity.key("МЛЯКО")).isEqualTo("мляко")
    }

    @Test
    fun `two spellings that differ only by case and padding share one key`() {
        assertThat(CategoryRuleEntity.key(" Хляб ")).isEqualTo(CategoryRuleEntity.key("хляб"))
    }

    @Test
    fun `an all-whitespace name collapses to empty rather than throwing`() {
        assertThat(CategoryRuleEntity.key("   ")).isEqualTo("")
    }
}
