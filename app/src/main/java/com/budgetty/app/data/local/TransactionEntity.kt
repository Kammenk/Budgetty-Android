package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A single purchased product line saved to the database.
 *
 * [timestamp] is the moment the transaction was uploaded (epoch millis), which is
 * what Home/History filter and group by.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val timestamp: Long,
    val price: BigDecimal,
    val quantity: Int,
    /** Spending category. Defaults to "Groceries" when an upload leaves it blank. */
    @ColumnInfo(defaultValue = "Groceries")
    val category: String = "Groceries",
    /** Groups the items from one upload into a single receipt (the upload id). [timestamp] is the
     *  receipt's made-date, so this separate id keeps grouping unique even when dates collide. */
    @ColumnInfo(defaultValue = "0")
    val receiptId: Long = 0,
)
