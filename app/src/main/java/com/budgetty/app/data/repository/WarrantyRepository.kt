package com.budgetty.app.data.repository

import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.local.WarrantyEntity
import kotlinx.coroutines.flow.Flow

/** Reads/writes tracked warranties. Ordered newest-purchase-first; the UI regroups by status. */
class WarrantyRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.warrantyDao()

    val warranties: Flow<List<WarrantyEntity>> = db.flow { it.warrantyDao().getAll() }

    suspend fun upsert(warranty: WarrantyEntity): Long = dao.upsert(warranty)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun count(): Int = dao.count()
}
