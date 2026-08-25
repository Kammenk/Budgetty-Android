package com.budgetty.app.data.repository

import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to the keyword-based buying limits (Account → Buying limits). Mirrors
 * [CategoryRuleRepository] / [SavingsRepository]: a live flow for the management screen plus a
 * one-shot snapshot for the save-time nudge, with all writes routed through here.
 */
class BuyingLimitsRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.buyingLimitDao()

    /** Every saved limit, live — backs the "Buying limits" management screen. */
    val limits: Flow<List<BuyingLimitEntity>> = db.flow { it.buyingLimitDao().getAll() }

    /** Snapshot of every limit, for computing the save-time nudge at receipt finalize. */
    suspend fun getAllOnce(): List<BuyingLimitEntity> = dao.getAllOnce()

    /** Insert or update; returns the limit's row id (the freshly-minted id when inserting). */
    suspend fun upsert(limit: BuyingLimitEntity): Long = dao.upsert(limit)

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        /**
         * Buying limits allowed on the free tier; Premium is unlimited. Raised 1 → 3 (§4.5): one limit
         * is too tight to build a habit portfolio, and the retention value outweighs the conversion
         * value of the 2nd/3rd. Loosening a gate never needs a migration — existing free users silently
         * gain capacity. Every quoted number derives from this constant (count pill, hints, paywall).
         */
        const val FREE_LIMIT = 3
    }
}
