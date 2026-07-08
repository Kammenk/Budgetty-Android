package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A learned "this item name → this category" preference. Applied automatically to matching line
 * items when a receipt is scanned, so a user's category choice sticks across future receipts.
 *
 * [name] is the normalized match key (see [key]): trimmed and lower-cased. Lower-casing is done in
 * Kotlin rather than via SQLite `NOCASE`/`LOWER()` (which only fold ASCII) so Cyrillic names —
 * Bulgarian receipts — fold correctly too.
 */
@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val name: String,
    val category: String,
) {
    companion object {
        /** The canonical match key for [raw]: trimmed + lower-cased (Unicode-aware). */
        fun key(raw: String): String = raw.trim().lowercase()
    }
}
