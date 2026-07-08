package com.budgetty.app.data.repository

import com.budgetty.app.data.local.TransactionDao
import com.budgetty.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Single point of access to transaction data for the ViewModels. */
class TransactionRepository(
    private val dao: TransactionDao,
) {
    fun getAll(): Flow<List<TransactionEntity>> = dao.getAll()

    fun getBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.getBetween(start, end)

    /** Earliest recorded transaction timestamp, or null when there are none. */
    fun earliestTimestamp(): Flow<Long?> = dao.earliestTimestamp()

    suspend fun getByReceiptId(receiptId: Long): List<TransactionEntity> =
        dao.getByReceiptId(receiptId)

    suspend fun getAllOnce(): List<TransactionEntity> = dao.getAllOnce()

    suspend fun updateCategoryForIds(ids: List<Long>, category: String) =
        dao.updateCategoryForIds(ids, category)

    suspend fun reassignCategory(from: String, to: String) = dao.reassignCategory(from, to)

    suspend fun countByCategory(category: String): Int = dao.countByCategory(category)

    suspend fun insertAll(transactions: List<TransactionEntity>) =
        dao.insertAll(transactions)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteByReceiptId(receiptId: Long) = dao.deleteByReceiptId(receiptId)
}
