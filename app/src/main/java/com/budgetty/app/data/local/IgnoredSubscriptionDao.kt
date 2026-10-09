package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredSubscriptionDao {

    @Query("SELECT * FROM ignored_subscriptions")
    fun getAll(): Flow<List<IgnoredSubscriptionEntity>>

    @Upsert
    suspend fun ignore(item: IgnoredSubscriptionEntity)

    /** Backup restore: a merchant already dismissed on this device keeps its own row (IGNORE). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<IgnoredSubscriptionEntity>)

    @Query("DELETE FROM ignored_subscriptions WHERE merchant = :merchant")
    suspend fun restore(merchant: String)

    @Query("DELETE FROM ignored_subscriptions")
    suspend fun clearAll()
}
