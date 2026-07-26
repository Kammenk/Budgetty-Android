package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Query("SELECT * FROM recurring ORDER BY createdAt ASC, id ASC")
    fun getAll(): Flow<List<RecurringEntity>>

    @Upsert
    suspend fun upsert(item: RecurringEntity)

    /** Bulk insert with fresh ids (pass id = 0) — used by backup restore. */
    @Insert
    suspend fun insertAll(items: List<RecurringEntity>)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Records when a bill was marked paid (0 = not paid). "Paid this cycle" is derived by checking
     *  this timestamp against the bill's current occurrence window (see RecurringMath). */
    @Query("UPDATE recurring SET lastPosted = :millis WHERE id = :id")
    suspend fun setLastPosted(id: Long, millis: Long)

    @Query("DELETE FROM recurring")
    suspend fun clearAll()
}
