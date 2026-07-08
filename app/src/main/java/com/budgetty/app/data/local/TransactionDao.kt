package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    /** Earliest recorded transaction timestamp, or null when there are none. Anchors the Insights
     *  stepper so it can't page back past the first recorded spend. */
    @Query("SELECT MIN(timestamp) FROM transactions")
    fun earliestTimestamp(): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE receiptId = :receiptId ORDER BY id ASC")
    suspend fun getByReceiptId(receiptId: Long): List<TransactionEntity>

    /** One-shot snapshot of every transaction, for name-matching category rules in Kotlin. */
    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<TransactionEntity>

    /** Bulk-recategorizes the given rows (used when propagating a category change by item name). */
    @Query("UPDATE transactions SET category = :category WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<Long>, category: String)

    /** Moves every transaction in category [from] to [to] (custom-category rename / delete). */
    @Query("UPDATE transactions SET category = :to WHERE category = :from")
    suspend fun reassignCategory(from: String, to: String)

    /** How many saved transactions are in [category] (drives the delete-category confirm copy). */
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :category")
    suspend fun countByCategory(category: String): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE receiptId = :receiptId")
    suspend fun deleteByReceiptId(receiptId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
