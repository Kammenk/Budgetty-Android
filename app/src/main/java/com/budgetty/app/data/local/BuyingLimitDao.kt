package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BuyingLimitDao {

    @Query("SELECT * FROM buying_limits ORDER BY createdAt ASC, id ASC")
    fun getAll(): Flow<List<BuyingLimitEntity>>

    /** One-shot snapshot, used to compute the save-time nudge in the receipt upload/save flow. */
    @Query("SELECT * FROM buying_limits")
    suspend fun getAllOnce(): List<BuyingLimitEntity>

    /** Insert or update; returns the row id (the freshly-minted id when inserting). */
    @Upsert
    suspend fun upsert(limit: BuyingLimitEntity): Long

    @Query("DELETE FROM buying_limits WHERE id = :id")
    suspend fun delete(id: Long)

    /** Bulk insert with fresh ids (backup restore); limits carry no child rows, so no id remap. */
    @Insert
    suspend fun insertAll(limits: List<BuyingLimitEntity>)

    @Query("DELETE FROM buying_limits")
    suspend fun clearAll()
}
