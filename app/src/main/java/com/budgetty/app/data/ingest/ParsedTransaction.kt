package com.budgetty.app.data.ingest

import com.budgetty.app.category.Categories
import java.math.BigDecimal
import java.util.UUID

/**
 * An editable, pre-save transaction produced by receipt extraction.
 *
 * Has no database id yet; [clientId] is a stable key for Compose list rendering
 * while the user reviews and edits before finalizing.
 */
data class ParsedTransaction(
    val clientId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val quantity: Int = 1,
    /** Left blank in the UI (shows a "Groceries" hint); resolved on save. */
    val category: String = "",
    /** ARGB color for [category]; resolved from saved categories or picked in the upload UI. */
    val categoryColor: Int = Categories.defaultColor,
    /**
     * True when [category] was filled in automatically from a saved category rule during a scan
     * (drives the "✦ from your rules" badge on the review screen). Cleared once the user picks a
     * category by hand, since it's then their explicit choice rather than the rule's.
     */
    val fromRule: Boolean = false,
)
