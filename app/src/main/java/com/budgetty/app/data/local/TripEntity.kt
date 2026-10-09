package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A trip ("Travel mode") — metadata wrapped around one [TagEntity]. A trip is nothing more than a
 * tag plus a name, optional dates, an optional budget and an active flag, so everything that already
 * works on tags (the History filter, Insights "By tag", rename/merge) keeps working on a trip's
 * expenses for free. While a trip is [active] the add/review screen pre-applies its [tag] to every
 * new expense (removable per expense), which is the whole point of the mode.
 *
 * [tag] is a normalized [TagEntity.normalize] key that also exists in the `tags` catalog — it is kept
 * here as a plain column (not a foreign key) so a trip survives even if its tag is later deleted from
 * Manage, and so the catalog row can be created independently.
 *
 * Dates and budget are nullable: a trip can run open-ended and without a target. [endedAt] is null
 * while the trip is active and set to the end moment when the user ends it (drives past-trip order).
 * Only one trip is active at a time (the repository deactivates any other when a new one starts).
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tag: String,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budgetAmount: BigDecimal? = null,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val endedAt: Long? = null,
) {
    val hasDates: Boolean get() = startDate != null && endDate != null
    val hasBudget: Boolean get() = budgetAmount != null && budgetAmount.signum() > 0

    companion object {
        /**
         * "lisbon-2026" from "Lisbon" + [year]: the trip's normalized, year-stamped tag. A name that
         * already ends in that year ("Lisbon 2026") isn't stamped twice. Same rule on iOS (`TripOps`).
         */
        fun tagFor(name: String, year: Int): String {
            val base = TagEntity.normalize(name).ifBlank { "trip" }
            return if (base.endsWith("-$year")) base else "$base-$year"
        }
    }
}
