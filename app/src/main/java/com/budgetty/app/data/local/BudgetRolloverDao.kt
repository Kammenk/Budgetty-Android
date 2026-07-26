package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetRolloverDao {

    @Query("SELECT * FROM budget_rollover")
    fun getAll(): Flow<List<BudgetRolloverEntity>>

    @Query("SELECT * FROM budget_rollover WHERE budgetKey = :key")
    suspend fun get(key: String): BudgetRolloverEntity?

    @Upsert
    suspend fun upsert(entity: BudgetRolloverEntity)

    @Query("DELETE FROM budget_rollover WHERE budgetKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM budget_rollover")
    suspend fun clearAll()
}
