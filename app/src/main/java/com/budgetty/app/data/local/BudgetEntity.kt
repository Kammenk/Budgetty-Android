package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A single budget limit. [budgetKey] is "MONTHLY", "WEEKLY", or "CAT:<category>"
 * (see BudgetRepository for the key helpers).
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val budgetKey: String,
    val amount: BigDecimal,
)
