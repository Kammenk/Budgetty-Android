package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetEnvelopeDao {

    @Query("SELECT * FROM budget_envelopes ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<BudgetEnvelopeEntity>>

    @Query("SELECT * FROM budget_envelopes")
    suspend fun getAllOnce(): List<BudgetEnvelopeEntity>

    @Upsert
    suspend fun upsert(envelope: BudgetEnvelopeEntity): Long

    @Insert
    suspend fun insertAll(envelopes: List<BudgetEnvelopeEntity>)

    @Query("DELETE FROM budget_envelopes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budget_envelopes")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM budget_envelopes")
    suspend fun count(): Int
}
