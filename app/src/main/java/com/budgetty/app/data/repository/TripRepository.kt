package com.budgetty.app.data.repository

import androidx.room.withTransaction
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to trips. Trips are the metadata layer over a tag (see [TripEntity]); the
 * tag catalog and the transaction↔tag links stay the concern of [TagRepository], so starting a trip
 * is: ensure its tag exists (caller), then [start] it here, then backfill links (caller). This keeps
 * the trips table and the tag tables independently backed up and migrated.
 */
class TripRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.tripDao()

    /** Every trip, active-first then newest — the Trips screen. */
    val trips: Flow<List<TripEntity>> = db.flow { it.tripDao().getAll() }

    /** The one active trip (or null), live — the add-screen auto-tag and the active summary. */
    val activeTrip: Flow<TripEntity?> = db.flow { it.tripDao().getActive() }

    suspend fun getActiveOnce(): TripEntity? = dao.getActiveOnce()

    suspend fun getById(id: Long): TripEntity? = dao.getById(id)

    /**
     * Starts [trip] as the active one: ends any other active trip first, then inserts it — atomically,
     * so there is never more than one active trip. Returns the new row id.
     */
    suspend fun start(trip: TripEntity): Long = db.database.withTransaction {
        val tripDao = db.database.tripDao()
        tripDao.deactivateAll(trip.createdAt)
        tripDao.upsert(trip.copy(active = true, endedAt = null))
    }

    /** Ends the active trip (nothing is untagged or deleted — the tag and its links stay). */
    suspend fun endActive(now: Long) = dao.deactivateAll(now)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
