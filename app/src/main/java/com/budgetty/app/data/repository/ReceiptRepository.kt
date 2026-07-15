package com.budgetty.app.data.repository

import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Access to receipt-level details (store/date/discount), keyed by upload timestamp. */
class ReceiptRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.receiptDao()

    fun getAll(): Flow<List<ReceiptEntity>> = db.flow { it.receiptDao().getAll() }

    suspend fun insert(receipt: ReceiptEntity) = dao.insert(receipt)

    suspend fun getById(id: Long): ReceiptEntity? = dao.getById(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
