package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A named spending budget ("envelope") beyond the single main budget: a [limitAmount] over an
 * inclusive date window ([startDate]..[endDate], epoch millis at local midnight), scoped either to all
 * spending ([categories] blank) or to a set of category names (newline-joined). Spend is summed live
 * from transactions in the window matching the scope; nothing is denormalised. Added in Room v28.
 */
@Entity(tableName = "budget_envelopes")
data class BudgetEnvelopeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // No @ColumnInfo default: a multi-byte emoji in a hand-written migration DEFAULT risks a schema
    // mismatch. The emoji always comes from the entity (Kotlin default below), so none is needed.
    val emoji: String = "🧾",
    val limitAmount: BigDecimal,
    val startDate: Long,
    val endDate: Long,
    /** Newline-joined category names the budget counts; blank = all spending. */
    @ColumnInfo(defaultValue = "")
    val categories: String = "",
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0,
) {
    /** True when the budget counts every category (its scope is all spending). */
    val isAllSpending: Boolean get() = categories.isBlank()

    /** The scoped category names, or empty for an all-spending budget. */
    fun categoryList(): List<String> = categories.split("\n").filter { it.isNotBlank() }
}
