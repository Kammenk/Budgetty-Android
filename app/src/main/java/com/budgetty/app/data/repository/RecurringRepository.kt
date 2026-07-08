package com.budgetty.app.data.repository

import com.budgetty.app.data.local.RecurringDao
import com.budgetty.app.data.local.RecurringEntity
import kotlinx.coroutines.flow.Flow

/** Reads/writes recurring money entries — income sources and recurring payments (bills). */
class RecurringRepository(private val dao: RecurringDao) {

    val items: Flow<List<RecurringEntity>> = dao.getAll()

    suspend fun upsert(item: RecurringEntity) = dao.upsert(item)

    suspend fun delete(id: Long) = dao.deleteById(id)

    companion object {
        /** Recurring payments (bills) allowed on the free tier; Premium is unlimited. Income is uncapped. */
        const val FREE_RECURRING_LIMIT = 3
    }
}
