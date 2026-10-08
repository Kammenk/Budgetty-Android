package com.budgetty.app.data.repository

import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Access to the user's debt-payoff records (the Debt payoff planner). Planning-only, per-user. */
class DebtRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.debtDao()

    /** Every debt, oldest-added first — live. */
    val debts: Flow<List<DebtEntity>> = db.flow { it.debtDao().getAll() }

    suspend fun upsert(debt: DebtEntity): Long = dao.upsert(debt)

    suspend fun delete(id: Long) = dao.delete(id)
}
