package com.budgetty.app.data.repository

import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Reads/writes the named spending budgets ("envelopes"), ordered by the user's sort then id. */
class BudgetEnvelopeRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.budgetEnvelopeDao()

    val envelopes: Flow<List<BudgetEnvelopeEntity>> = db.flow { it.budgetEnvelopeDao().getAll() }

    suspend fun upsert(envelope: BudgetEnvelopeEntity): Long = dao.upsert(envelope)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun count(): Int = dao.count()

    companion object {
        /** Extra named budgets allowed on the free tier (beyond the main budget); Premium is unlimited. */
        const val FREE_LIMIT = 1
    }
}
