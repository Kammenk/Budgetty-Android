package com.budgetty.app.data.repository

import com.budgetty.app.data.local.ReceiptDao
import com.budgetty.app.data.local.ReceiptEntity
import kotlinx.coroutines.flow.Flow

/** Access to receipt-level details (store/date/discount), keyed by upload timestamp. */
class ReceiptRepository(private val dao: ReceiptDao) {

    fun getAll(): Flow<List<ReceiptEntity>> = dao.getAll()

    suspend fun insert(receipt: ReceiptEntity) = dao.insert(receipt)

    suspend fun getById(id: Long): ReceiptEntity? = dao.getById(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
