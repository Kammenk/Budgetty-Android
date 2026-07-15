package com.budgetty.app.data.repository

import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to the learned name → category rules for the ViewModels.
 *
 * Keys are normalized through [CategoryRuleEntity.key] so lookups, writes and deletes all agree on
 * the same match key.
 */
class CategoryRuleRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.categoryRuleDao()

    /** Every saved rule, live — backs the "Category rules" management screen. */
    val rules: Flow<List<CategoryRuleEntity>> = db.flow { it.categoryRuleDao().getAll() }

    /** Snapshot of every rule as a normalized-name → category map, for applying to a new scan. */
    suspend fun rulesByName(): Map<String, String> =
        dao.getAllOnce().associate { it.name to it.category }

    suspend fun setRule(name: String, category: String) =
        dao.upsert(CategoryRuleEntity(CategoryRuleEntity.key(name), category))

    suspend fun removeRule(name: String) = dao.delete(CategoryRuleEntity.key(name))

    /** Re-points every rule from category [from] to [to] (custom-category rename). */
    suspend fun reassignCategory(from: String, to: String) = dao.reassignCategory(from, to)

    /** Drops every rule mapping to [category] (custom-category delete). */
    suspend fun removeRulesForCategory(category: String) = dao.deleteByCategory(category)
}
