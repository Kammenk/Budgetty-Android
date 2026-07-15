package com.budgetty.app.data.repository

import com.budgetty.app.data.local.BudgetEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Reads/writes budget limits. Keys: [MONTHLY], [WEEKLY], or [categoryKey] for a category. */
class BudgetRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.budgetDao()

    /** All budgets as key -> amount. */
    val budgets: Flow<Map<String, BigDecimal>> =
        db.flow { d -> d.budgetDao().getAll().map { rows -> rows.associate { it.budgetKey to it.amount } } }

    /** Sets (or, for null / non-positive amounts, clears) the budget for [key]. */
    suspend fun setBudget(key: String, amount: BigDecimal?) {
        if (amount == null || amount <= BigDecimal.ZERO) {
            dao.delete(key)
        } else {
            dao.upsert(BudgetEntity(key, amount))
        }
    }

    /** Clears any per-category budget for [category] (custom-category delete). */
    suspend fun clearCategoryBudget(category: String) = dao.delete(categoryKey(category))

    /** Moves a per-category budget from [from] to [to] (custom-category rename). */
    suspend fun renameCategoryBudget(from: String, to: String) =
        dao.renameKey(categoryKey(from), categoryKey(to))

    companion object {
        const val MONTHLY = "MONTHLY"
        const val WEEKLY = "WEEKLY"
        const val CATEGORY_PREFIX = "CAT:"
        fun categoryKey(category: String) = "$CATEGORY_PREFIX$category"
    }
}
