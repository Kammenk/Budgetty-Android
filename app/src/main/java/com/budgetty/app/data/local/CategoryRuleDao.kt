package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules")
    fun getAll(): Flow<List<CategoryRuleEntity>>

    /** One-shot snapshot, used to apply rules to a freshly scanned receipt. */
    @Query("SELECT * FROM category_rules")
    suspend fun getAllOnce(): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(rules: List<CategoryRuleEntity>)

    @Query("DELETE FROM category_rules WHERE name = :key")
    suspend fun delete(key: String)

    /** Re-points every rule mapping to category [from] onto [to] (custom-category rename). */
    @Query("UPDATE category_rules SET category = :to WHERE category = :from")
    suspend fun reassignCategory(from: String, to: String)

    /** Drops every rule that maps to [category] (custom-category delete). */
    @Query("DELETE FROM category_rules WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM category_rules")
    suspend fun clearAll()
}
