package com.budgetty.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A finalized Wellbeing score for one CLOSED pay-cycle month (§3.1) — the only new persistence in the
 * retention spec. [com.budgetty.app.ui.wellbeing.WellbeingProvider] upserts a row when it scores a
 * closed month; the in-flight month is never stored, so history stays a fixed, non-revisionist record.
 * Recomputing an old month against today's budgets/goals would silently rewrite the past and make the
 * future trend line lie, so the snapshot — not a recompute — is the source of truth.
 *
 * [periodId] is the pay-cycle month id "yyyy-MM" (see [com.budgetty.app.ui.util.PayCycle]) and the
 * primary key, so re-scoring the same month REPLACEs its row rather than duplicating. [componentsJson]
 * holds the per-component sub-scores (see
 * [com.budgetty.app.ui.wellbeing.WellbeingHistory.encodeComponents]) for the future breakdown-over-time
 * view; the saved total is never re-derived from it — the row is the snapshot.
 */
@Entity(tableName = "wellbeing_scores")
data class WellbeingScoreEntity(
    @PrimaryKey val periodId: String,
    val score: Int,
    /** [com.budgetty.app.ui.wellbeing.WellbeingBand] name at snapshot time. */
    val band: String,
    /** Per-component sub-scores as stable JSON (componentKey -> sub-score, null when not counted). */
    val componentsJson: String,
    /** When this snapshot was written (epoch millis) — audit only, never part of the score. */
    val computedAt: Long,
)
