package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A merchant the user dismissed from subscription detection. Keyed by the normalized merchant name —
 * dismissed, never deleted, because detection would just surface it again from the same receipts.
 */
@Entity(tableName = "ignored_subscriptions")
data class IgnoredSubscriptionEntity(
    @PrimaryKey
    val merchant: String,
    @ColumnInfo(defaultValue = "0")
    val ignoredAt: Long = 0,
)
