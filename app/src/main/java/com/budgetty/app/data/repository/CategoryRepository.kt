package com.budgetty.app.data.repository

import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Single point of access to the saved categories (name → color) for the ViewModels. */
class CategoryRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.categoryDao()

    val categories: Flow<List<CategoryEntity>> = db.flow { it.categoryDao().getAll() }

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

    /**
     * Re-homes [name] under [parent] (null = top-level). A built-in may have no stored row yet, so
     * this inserts one (carrying its predefined color/emoji) before setting the parent, keeping the
     * override even for a category that was previously code-only.
     */
    suspend fun setParent(name: String, parent: String?) {
        if (!Categories.isPredefined(name)) {
            dao.setParent(name, parent)
            return
        }
        // Ensure a row exists for the built-in so the parent override has somewhere to live.
        dao.insertOrIgnore(
            listOf(CategoryEntity(name = name, colorArgb = Categories.colorOf(name), icon = Categories.emojiOf(name))),
        )
        dao.setParent(name, parent)
    }

    /** Promotes every child of [parent] to top-level — used when a parent category is deleted. */
    suspend fun promoteChildrenOf(parent: String) = dao.promoteChildrenOf(parent)

    /** Repoints every child of [oldParent] to [newParent] — used when a parent is renamed. */
    suspend fun reparentChildren(oldParent: String, newParent: String) =
        dao.reparentChildren(oldParent, newParent)
}
