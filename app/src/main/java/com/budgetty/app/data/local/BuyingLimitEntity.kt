package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** How often a [BuyingLimitEntity]'s count resets. */
enum class BuyingLimitTimeframe { WEEKLY, MONTHLY }

/**
 * A keyword-based item purchase cap — a sibling to [CategoryRuleEntity]. The user caps how many of
 * something they buy in a [timeframe] (e.g. "no more than 1 Coke a week"); Budgetty counts matching
 * items off saved receipts and nudges when the count reaches/exceeds [count].
 *
 * [keywords] is a newline-joined list of **normalized** match keys (see [normalizeKeyword]): each is
 * trimmed and lower-cased. Lower-casing is done in Kotlin (Unicode-aware) rather than via SQLite
 * `NOCASE`/`LOWER()` (ASCII-only) so Cyrillic keywords — Bulgarian receipts — fold correctly too.
 * Newline is the delimiter (not comma) so a committed multi-word keyword can still contain a comma.
 *
 * The count itself is never stored: it's derived on demand by summing the quantity of matching
 * transactions in the current window (see the counter/window helpers), so it always reflects the
 * live receipt data — Budgetty holds no running totals.
 */
@Entity(tableName = "buying_limits")
data class BuyingLimitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Optional per-limit emoji; empty = none, so the card/editor fall back to a generic tag glyph. */
    @ColumnInfo(defaultValue = "")
    val emoji: String = "",
    /** Optional user label; empty = none, so the display title falls back to the first keyword. */
    @ColumnInfo(defaultValue = "")
    val label: String = "",
    /** Newline-joined normalized keywords; any one matching (substring) counts toward this limit. */
    @ColumnInfo(defaultValue = "")
    val keywords: String = "",
    @ColumnInfo(defaultValue = "MONTHLY")
    val timeframe: BuyingLimitTimeframe = BuyingLimitTimeframe.MONTHLY,
    /** The cap, per [timeframe] window. Floored at 1 by the editor. */
    @ColumnInfo(defaultValue = "1")
    val count: Int = 1,
    /** Creation time (epoch millis); orders the limits list by when each was added. */
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
) {
    /** The stored keywords as a list (newline-split, blanks dropped). Already normalized. */
    val keywordList: List<String>
        get() = keywords.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * The card/nudge title: the user's [label] if set, otherwise the first keyword with its initial
     * letter capitalized ("coke" → "Coke"; Cyrillic folds too). Empty only for a limit with no label
     * and no keywords, which the editor never saves.
     */
    val displayTitle: String
        get() = label.ifBlank {
            keywordList.firstOrNull()?.replaceFirstChar { it.uppercase() }.orEmpty()
        }

    companion object {
        /** The canonical match key for [raw]: trimmed + lower-cased (Unicode-aware), like the rules key. */
        fun normalizeKeyword(raw: String): String = raw.trim().lowercase()

        /** Joins [raw] keywords into the stored form: normalized, de-duplicated, newline-joined. */
        fun joinKeywords(raw: List<String>): String =
            raw.map(::normalizeKeyword).filter { it.isNotEmpty() }.distinct().joinToString("\n")
    }
}
