package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * The unspent budget carried into the current period for one budget key ("MONTHLY" or "CAT:<name>",
 * mirroring [BudgetEntity.budgetKey]). Persisted rather than recomputed, because budget amounts change
 * without history: [carried] is the leftover accumulated as of [periodKey] (the pay-cycle month, as
 * "yyyy-MM" of the cycle's start). Rows exist only while rollover is enabled and are wiped when it's
 * turned off. See `ui/util/BudgetRollover.kt` for the roll-forward math.
 */
@Entity(tableName = "budget_rollover")
data class BudgetRolloverEntity(
    @PrimaryKey val budgetKey: String,
    val carried: BigDecimal,
    val periodKey: String,
)
