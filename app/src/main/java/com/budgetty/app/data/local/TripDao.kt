package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Trips are ordered active-first, then most-recently-ended/created first, so the active trip (if any)
 * always sits at the top of the Trips screen and past trips read newest-first beneath it.
 */
@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY active DESC, COALESCE(endedAt, createdAt) DESC, id DESC")
    fun getAll(): Flow<List<TripEntity>>

    /** The one active trip, live — drives the add-screen auto-tag and the active summary. */
    @Query("SELECT * FROM trips WHERE active = 1 ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun getActive(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE active = 1 ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getActiveOnce(): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Query("SELECT * FROM trips")
    suspend fun getAllOnce(): List<TripEntity>

    @Upsert
    suspend fun upsert(trip: TripEntity): Long

    @Insert
    suspend fun insertAll(trips: List<TripEntity>)

    /** Ends every currently-active trip (used when a new trip starts and when the user ends one). */
    @Query("UPDATE trips SET active = 0, endedAt = :endedAt WHERE active = 1")
    suspend fun deactivateAll(endedAt: Long)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun clearAll()
}
