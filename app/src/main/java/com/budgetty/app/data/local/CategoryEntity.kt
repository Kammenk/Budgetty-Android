package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spending category and how it renders across the app.
 *
 * The [icon]/[isCustom]/[createdAt] columns carry SQL defaults (matching [MIGRATION_12_13]) so the
 * partial seed insert in [seedCategories] — which only writes name + color — stays valid on a fresh
 * install, where Room creates the table from this entity.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val colorArgb: Int,
    /**
     * Emoji icon. Predefined categories are backfilled from
     * [com.budgetty.app.category.Categories]; user-created ones store their chosen emoji here.
     */
    @ColumnInfo(defaultValue = "")
    val icon: String = "",
    /** True for user-created categories — drives the "Your categories" section and the create cap. */
    @ColumnInfo(defaultValue = "0")
    val isCustom: Boolean = false,
    /** Creation time (epoch millis); orders custom categories by when they were added. 0 when seeded. */
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
)
