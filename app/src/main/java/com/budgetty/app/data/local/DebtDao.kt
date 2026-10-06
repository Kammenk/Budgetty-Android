package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    /** Every debt, oldest-added first — the planner list and the live simulation read this. */
    @Query("SELECT * FROM debts ORDER BY createdAt ASC, id ASC")
    fun getAll(): Flow<List<DebtEntity>>

    /** Insert a new debt or update an existing one (by id); returns the row id. */
    @Upsert
    suspend fun upsert(debt: DebtEntity): Long

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun delete(id: Long)

    /** Bulk insert with fresh ids — backup restore. */
    @Insert
    suspend fun insertAll(debts: List<DebtEntity>)

    @Query("DELETE FROM debts")
    suspend fun clearAll()
}
