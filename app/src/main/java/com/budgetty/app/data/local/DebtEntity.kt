package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * One debt the user is paying down, for the Debt payoff planner. Purely a planning record — nothing
 * here is linked to a real account or posts transactions; the payoff schedule is simulated live from
 * these figures (see [com.budgetty.app.ui.planners.DebtPayoffSimulator]).
 *
 * [balance], [aprPercent] and [minPayment] are stored as TEXT (BigDecimal via Converters), matching
 * the other money columns; the [emoji]/[name]/[createdAt] columns carry schema defaults so the table
 * mirrors the entity exactly on migration.
 */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "")
    val emoji: String = "",
    @ColumnInfo(defaultValue = "")
    val name: String = "",
    /** Current balance owed. */
    val balance: BigDecimal,
    /** Annual interest rate as a percentage (e.g. 19.9 for a 19.9% card). */
    val aprPercent: BigDecimal,
    /** The lender's required minimum payment per month. */
    val minPayment: BigDecimal,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
)
