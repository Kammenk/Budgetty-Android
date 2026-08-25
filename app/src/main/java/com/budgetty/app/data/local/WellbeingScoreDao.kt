package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes the stored Wellbeing score history (one row per closed pay-cycle month, §3.1). Mirrors
 * [BuyingLimitDao] / [SavingsDao]: a live flow plus a bounded read for the future trend view, an
 * idempotent per-period [upsert], and the bulk restore/clear the backup path needs.
 */
@Dao
interface WellbeingScoreDao {

    /** Every stored month, oldest→newest (periodId is "yyyy-MM", so lexical order == chronological). */
    @Query("SELECT * FROM wellbeing_scores ORDER BY periodId ASC")
    fun getAll(): Flow<List<WellbeingScoreEntity>>

    /**
     * The most-recent [limit] months, returned oldest→newest so the future sparkline renders
     * left-to-right. The inner query picks the newest N by descending periodId; the outer flips them
     * back to ascending.
     */
    @Query(
        "SELECT * FROM (SELECT * FROM wellbeing_scores ORDER BY periodId DESC LIMIT :limit) " +
            "ORDER BY periodId ASC",
    )
    suspend fun getRecent(limit: Int): List<WellbeingScoreEntity>

    /**
     * Insert or update keyed by [WellbeingScoreEntity.periodId]. Idempotent: re-scoring the same
     * closed month REPLACEs its row, so a month can never appear twice in the trend.
     */
    @Upsert
    suspend fun upsert(entity: WellbeingScoreEntity)

    /**
     * Bulk restore from a backup. IGNORE (not REPLACE) so importing onto an account that already has
     * history keeps the on-device snapshot — the honest, first-computed record — instead of letting a
     * backup overwrite it. On a full replace the table is cleared first, so IGNORE is moot there.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<WellbeingScoreEntity>)

    @Query("DELETE FROM wellbeing_scores")
    suspend fun clearAll()
}
