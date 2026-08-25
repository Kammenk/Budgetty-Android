package com.budgetty.app.data.repository

import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.local.WellbeingScoreEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to the stored Wellbeing score history (one row per closed pay-cycle month,
 * §3.1). Mirrors [BuyingLimitsRepository] / [SavingsRepository]: live reads for the future trend view
 * plus the idempotent [upsert] that [com.budgetty.app.ui.wellbeing.WellbeingProvider] calls when it
 * scores a closed month, with every write routed through here. Backup export/restore talks to the DAO
 * directly (like the other collections), so this exposes only what the app itself needs.
 */
class WellbeingScoreRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.wellbeingScoreDao()

    /** Every stored month, oldest→newest — backs the future trend sparkline / breakdown view. */
    val scores: Flow<List<WellbeingScoreEntity>> = db.flow { it.wellbeingScoreDao().getAll() }

    /** The most-recent [limit] closed months, oldest→newest (ready for a left-to-right sparkline). */
    suspend fun getRecent(limit: Int): List<WellbeingScoreEntity> = dao.getRecent(limit)

    /** Idempotent per-period upsert (PK = periodId); re-scoring a closed month REPLACEs its row. */
    suspend fun upsert(entity: WellbeingScoreEntity) = dao.upsert(entity)
}
