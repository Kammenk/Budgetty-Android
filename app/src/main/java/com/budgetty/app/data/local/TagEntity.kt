package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A free-form tag — orthogonal to the category taxonomy. A transaction has exactly one category but
 * any number of tags (the link lives in [TransactionTagEntity]); tags never change categorisation or
 * the learned category rules.
 *
 * [name] is the normalized key (see [normalize]): trimmed, lower-cased, a leading '#' dropped, spaces
 * turned to hyphens, and anything that isn't a letter, number or hyphen removed. Lower-casing is done
 * in Kotlin (not SQLite `NOCASE`, which only folds ASCII) so Cyrillic — "#дача" — folds correctly,
 * exactly like the learned category-rule keys ([CategoryRuleEntity.key]).
 *
 * The catalog is its own table rather than being derived from the join so a tag can exist before (or
 * after) anything carries it — e.g. a Travel-mode trip tag created before its first expense.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = 0,
) {
    companion object {
        private val SPACES = Regex("\\s+")
        private val INVALID = Regex("[^\\p{L}\\p{N}-]")

        /**
         * The canonical key for [raw]: trimmed, lower-cased, leading '#'s stripped, internal
         * whitespace turned to single hyphens, and any character that isn't a Unicode letter/number
         * or a hyphen removed. Matches the tag mockup's normalisation and is Unicode-aware, so
         * "Work Trip" → "work-trip" and "#Дача" → "дача". Returns "" for input with no usable chars.
         */
        fun normalize(raw: String): String =
            raw.trim().lowercase().trimStart('#').replace(SPACES, "-").replace(INVALID, "")
    }
}
