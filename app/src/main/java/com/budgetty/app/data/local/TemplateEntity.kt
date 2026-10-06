package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A saved "regular" the user can log in one tap from the Add sheet. A template never posts by itself
 * (that's a recurring bill) — tapping it pre-fills the manual-entry review screen, which the user
 * confirms. [askAmount] templates leave the amount blank so a variable regular (e.g. groceries) prompts
 * for the price each time. [amount] is TEXT (BigDecimal via Converters), matching the other money columns.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "")
    val emoji: String = "",
    @ColumnInfo(defaultValue = "")
    val name: String = "",
    val amount: BigDecimal,
    @ColumnInfo(defaultValue = "")
    val category: String = "",
    @ColumnInfo(defaultValue = "")
    val store: String = "",
    @ColumnInfo(defaultValue = "0")
    val askAmount: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
)
