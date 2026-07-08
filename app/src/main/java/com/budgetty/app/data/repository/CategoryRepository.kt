package com.budgetty.app.data.repository

import com.budgetty.app.data.local.CategoryDao
import com.budgetty.app.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** Single point of access to the saved categories (name → color) for the ViewModels. */
class CategoryRepository(
    private val dao: CategoryDao,
) {
    val categories: Flow<List<CategoryEntity>> = dao.getAll()

    suspend fun upsertAll(categories: List<CategoryEntity>) = dao.upsertAll(categories)

    /**
     * Inserts only categories whose names don't exist yet, leaving existing rows untouched. Used when
     * finalizing a receipt to persist any brand-new category name (so its color resolves later)
     * without clobbering a predefined row's emoji or downgrading a user-created custom category
     * (which would strip its `isCustom`/icon and hide it from the picker).
     */
    suspend fun insertMissing(categories: List<CategoryEntity>) = dao.insertOrIgnore(categories)

    /** Creates or updates a single category (used for user-created custom categories). */
    suspend fun upsert(category: CategoryEntity) = dao.upsert(category)

    suspend fun deleteByName(name: String) = dao.deleteByName(name)
}
