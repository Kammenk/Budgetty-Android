package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * The many-to-many link between a line-item [TransactionEntity] and a [TagEntity], keyed by
 * (transactionId, tagName).
 *
 * Deleting a transaction cascades its links away — so editing a receipt, which deletes and
 * re-inserts its line items, drops the old links; the review screen then re-creates them from the
 * tags it carried on each row. Deleting a tag from the catalog cascades its links away too, but never
 * touches the transactions themselves (see TagRepository). Rename/merge is done by re-pointing links
 * and dropping the old catalog row, not by updating this key in place.
 */
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagName"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["name"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagName")],
)
data class TransactionTagEntity(
    val transactionId: Long,
    val tagName: String,
)
