package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Query("SELECT * FROM receipts")
    fun getAll(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<ReceiptEntity>)

    @Query("SELECT * FROM receipts WHERE timestamp = :id")
    suspend fun getById(id: Long): ReceiptEntity?

    @Query("DELETE FROM receipts WHERE timestamp = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM receipts")
    suspend fun clearAll()
}
