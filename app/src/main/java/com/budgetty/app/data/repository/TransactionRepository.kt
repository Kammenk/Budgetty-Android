package com.budgetty.app.data.repository

import com.budgetty.app.data.local.CategoryStamp
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Single point of access to transaction data for the ViewModels. */
class TransactionRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.transactionDao()

    fun getAll(): Flow<List<TransactionEntity>> = db.flow { it.transactionDao().getAll() }

    fun getBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        db.flow { it.transactionDao().getBetween(start, end) }

    /** Earliest recorded transaction timestamp, or null when there are none. */
    fun earliestTimestamp(): Flow<Long?> = db.flow { it.transactionDao().earliestTimestamp() }

    /** Live category + timestamp of recent transactions — ranked into the picker's habit suggestions. */
    fun recentCategoryStamps(): Flow<List<CategoryStamp>> =
        db.flow { it.transactionDao().recentCategoryStamps() }

    suspend fun getByReceiptId(receiptId: Long): List<TransactionEntity> =
        dao.getByReceiptId(receiptId)

    suspend fun getAllOnce(): List<TransactionEntity> = dao.getAllOnce()

    suspend fun updateCategoryForIds(ids: List<Long>, category: String) =
        dao.updateCategoryForIds(ids, category)

    suspend fun reassignCategory(from: String, to: String) = dao.reassignCategory(from, to)

    suspend fun countByCategory(category: String): Int = dao.countByCategory(category)

    /** Inserts the rows and returns their new ids, in input order (so callers can link per-row tags). */
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> =
        dao.insertAll(transactions)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteByReceiptId(receiptId: Long) = dao.deleteByReceiptId(receiptId)
}
