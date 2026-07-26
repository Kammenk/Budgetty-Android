package com.budgetty.app.data.repository

import com.budgetty.app.data.local.BudgetRolloverEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Persisted carry-over amounts per budget key; rows exist only while rollover is enabled. */
class BudgetRolloverRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.budgetRolloverDao()

    /** budgetKey -> carried amount as of its last-rolled period. Empty when rollover is off. */
    val carried: Flow<Map<String, BigDecimal>> =
        db.flow { d ->
            d.budgetRolloverDao().getAll().map { rows -> rows.associate { it.budgetKey to it.carried } }
        }

    suspend fun get(key: String): BudgetRolloverEntity? = dao.get(key)

    suspend fun upsert(entity: BudgetRolloverEntity) = dao.upsert(entity)

    suspend fun delete(key: String) = dao.delete(key)

    suspend fun clearAll() = dao.clearAll()
}
