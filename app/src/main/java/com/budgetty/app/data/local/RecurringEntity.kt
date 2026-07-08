package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A recurring money entry on the Budget screen: either an income source ([isIncome] = true, e.g.
 * "Salary") or a recurring payment / bill ([isIncome] = false, e.g. "Rent", "Netflix"). Income and
 * bills are the same primitive with opposite signs, so they share one table and one edit sheet.
 *
 * These are planning-only for now — they feed the Budget screen's monthly breakdown but never post
 * transactions. The scheduling columns ([nextDue] / [lastPosted] / [active]) are stored ahead of the
 * later auto-posting phase so it can be added without another schema change.
 */
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val amount: BigDecimal,
    /** True for income sources, false for recurring payments (bills). */
    val isIncome: Boolean,
    /** Spending category for a bill; empty for income (income sits outside the spend categories). */
    @ColumnInfo(defaultValue = "")
    val category: String = "",
    /** How often it repeats — one of [Cadence]. [Cadence.ONCE] is a non-repeating, one-time entry. */
    @ColumnInfo(defaultValue = Cadence.MONTHLY)
    val cadence: String = Cadence.MONTHLY,
    /**
     * When it lands: day-of-month (1..31) for monthly/yearly, or day-of-week (1=Mon..7=Sun) weekly.
     * Unused for [Cadence.ONCE], which is anchored to [createdAt] instead of a recurring day.
     */
    @ColumnInfo(defaultValue = "1")
    val dueDay: Int = 1,
    /** Creation time (epoch millis); orders each list by when the entry was added. */
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    // ── Reserved for the later auto-posting phase (unused while planning-only) ──
    @ColumnInfo(defaultValue = "0")
    val nextDue: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val lastPosted: Long = 0L,
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
) {
    object Cadence {
        const val MONTHLY = "MONTHLY"
        const val WEEKLY = "WEEKLY"
        const val YEARLY = "YEARLY"

        /**
         * A single, non-repeating entry — e.g. a one-off bonus or a shift worker's variable monthly
         * wage. It counts toward the budget only for the calendar month it was added ([createdAt]),
         * so it never inflates later months' plans the way a recurring amount would.
         */
        const val ONCE = "ONCE"
    }
}
