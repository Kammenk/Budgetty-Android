package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A tracked product warranty: what was bought, when, and for how long it's covered. The expiry is
 * derived ([purchaseDate] + [durationMonths]) rather than stored, so a length edit never leaves a
 * stale date. [receiptId] optionally links the warranty to the receipt it was created from (0 = none);
 * no photo is kept, matching the app's no-image-storage rule. Added in Room v28.
 */
@Entity(tableName = "warranties")
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // No @ColumnInfo default: a multi-byte emoji in a hand-written migration DEFAULT risks a Room
    // schema mismatch. The emoji always comes from the entity (Kotlin default below), so none is needed.
    val emoji: String = "🛡️",
    @ColumnInfo(defaultValue = "")
    val store: String = "",
    /** Spending category (for the emoji/eligibility heuristic); empty when unknown. */
    @ColumnInfo(defaultValue = "")
    val category: String = "",
    /** Purchase date as epoch millis at local midnight. */
    val purchaseDate: Long,
    /** Warranty length in whole months. */
    val durationMonths: Int,
    /** Free-text coverage detail (e.g. "Battery covered 1 yr"); optional. */
    @ColumnInfo(defaultValue = "")
    val coverageNote: String = "",
    /** The receipt this warranty was created from ([ReceiptEntity.timestamp]); 0 when added manually. */
    @ColumnInfo(defaultValue = "0")
    val receiptId: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0,
)
