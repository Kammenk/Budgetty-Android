package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {

    @Query("SELECT * FROM warranties ORDER BY purchaseDate DESC")
    fun getAll(): Flow<List<WarrantyEntity>>

    @Query("SELECT * FROM warranties")
    suspend fun getAllOnce(): List<WarrantyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(warranty: WarrantyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(warranties: List<WarrantyEntity>)

    @Query("DELETE FROM warranties WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM warranties")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM warranties")
    suspend fun count(): Int
}
