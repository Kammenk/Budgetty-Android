package com.budgetty.app.data.repository

import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Reads/writes recurring money entries — income sources and recurring payments (bills). */
class RecurringRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.recurringDao()

    val items: Flow<List<RecurringEntity>> = db.flow { it.recurringDao().getAll() }

    suspend fun upsert(item: RecurringEntity) = dao.upsert(item)

    suspend fun delete(id: Long) = dao.deleteById(id)

    companion object {
        /** Recurring payments (bills) allowed on the free tier; Premium is unlimited. Income is uncapped. */
        const val FREE_RECURRING_LIMIT = 3
    }
}
