package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(categories: List<CategoryEntity>)

    /** Re-homes [name] under [parent] (null = top-level). Inserts a row first if the category — e.g. a
     *  built-in with no stored row yet — isn't present, so the override sticks. */
    @Query("UPDATE categories SET parent = :parent WHERE name = :name")
    suspend fun setParent(name: String, parent: String?)

    /** On deleting a parent, promote its children to top-level rather than orphaning or deleting them. */
    @Query("UPDATE categories SET parent = NULL WHERE parent = :parent")
    suspend fun promoteChildrenOf(parent: String)

    /** On renaming a parent, keep its children attached by repointing their parent name. */
    @Query("UPDATE categories SET parent = :newParent WHERE parent = :oldParent")
    suspend fun reparentChildren(oldParent: String, newParent: String)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
