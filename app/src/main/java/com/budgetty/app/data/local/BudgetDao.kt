package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE budgetKey = :key")
    suspend fun delete(key: String)

    /** Moves a budget from key [from] to [to] (custom-category rename). No-op if [from] has none. */
    @Query("UPDATE budgets SET budgetKey = :to WHERE budgetKey = :from")
    suspend fun renameKey(from: String, to: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
