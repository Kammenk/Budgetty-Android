package com.budgetty.app.ui.wellbeing

import com.budgetty.app.data.local.WellbeingScoreEntity
import com.budgetty.app.ui.util.PayCycle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure history-snapshot logic for the Wellbeing score (§3.1). Android-free (plain `java.time` + Gson,
 * which is already a project dependency), so it ports 1:1 to iOS and is unit-testable without Room.
 * [WellbeingProvider] uses it to build the row it persists for a CLOSED pay-cycle month; the in-flight
 * month is never snapshotted — its score is still moving, and §3.1's whole point is a final record.
 *
 * The saved snapshot is deliberately NOT a recompute: scoring an old month against today's budgets and
 * goals would silently rewrite the user's past and make the trend line lie, so once written a month's
 * row is left alone (it is only re-touched while it is still the *just-closed* month, PK-idempotently).
 */
object WellbeingHistory {

    // serializeNulls so an un-scored (null) component stays in the JSON rather than vanishing — the
    // future breakdown view needs to tell "not counted" apart from "absent key".
    private val encoder: Gson = GsonBuilder().serializeNulls().create()
    private val decoder: Gson = Gson()
    private val mapType = object : TypeToken<Map<String, Int?>>() {}.type

    /** Below this many STORED closed months the sparkline renders nothing at all — no placeholder (§3.2). */
    const val MIN_TREND_MONTHS = 2

    /**
     * The trend-sparkline model (§3.2) from the stored closed months [recentClosed] (oldest→newest, as
     * [com.budgetty.app.data.local.WellbeingScoreDao.getRecent] returns them) plus the in-flight
     * [liveScore], which becomes the dashed ghost / hollow dot at the end.
     *
     * Returns null below [MIN_TREND_MONTHS] stored months — one point isn't a trend, so the caller
     * renders NOTHING (no placeholder, no "not enough data yet" card; the sparkline simply grows in over
     * time as months close). The caption delta is "now" (the live score, or the last closed month when
     * there is no live score) minus the first shown month, so "Up 8 since March." reads honestly.
     */
    fun trend(recentClosed: List<WellbeingScoreEntity>, liveScore: Int?): WellbeingTrend? {
        val points = recentClosed.mapNotNull { e ->
            runCatching { WellbeingTrendPoint(YearMonth.parse(e.periodId), e.score) }.getOrNull()
        }
        if (points.size < MIN_TREND_MONTHS) return null
        val first = points.first()
        val now = liveScore ?: points.last().score
        return WellbeingTrend(
            closed = points,
            liveScore = liveScore,
            deltaSinceFirst = now - first.score,
            firstMonth = first.yearMonth,
        )
    }

    /**
     * The "yyyy-MM" id of the pay-cycle month at [offset] from [today] (0 = current, -1 = just-closed).
     * Uses the same [PayCycle] anchoring as the rest of the app, so it matches
     * [WellbeingSummary.periodId] exactly.
     */
    fun periodId(today: LocalDate, monthStartDay: Int, offset: Int): String =
        YearMonth.from(PayCycle.month(today, monthStartDay, offset).first).toString()

    /**
     * The snapshot row to persist for the just-closed cycle, or null when that cycle can't be scored
     * yet (too little data — [WellbeingScore.score] is null). [closedScore] MUST be the previous,
     * completed cycle's score and [closedPeriodId] its id (offset -1); the caller never passes the
     * in-flight cycle here, so the current month has no path into history.
     */
    fun closedSnapshot(
        closedPeriodId: String,
        closedScore: WellbeingScore,
        computedAt: Long,
    ): WellbeingScoreEntity? {
        val score = closedScore.score ?: return null
        val band = closedScore.band ?: return null
        return WellbeingScoreEntity(
            periodId = closedPeriodId,
            score = score,
            band = band.name,
            componentsJson = encodeComponents(closedScore.components),
            computedAt = computedAt,
        )
    }

    /** Stable JSON object of componentKey -> sub-score (null when the component wasn't counted). */
    fun encodeComponents(components: List<WellbeingComponent>): String {
        val map = LinkedHashMap<String, Int?>()
        components.forEach { map[it.key.name] = it.score }
        return encoder.toJson(map)
    }

    /** Reads [encodeComponents] back — for the future breakdown-over-time view. Never throws. */
    @Suppress("UNCHECKED_CAST")
    fun decodeComponents(json: String): Map<String, Int?> =
        runCatching { decoder.fromJson(json, mapType) as Map<String, Int?> }.getOrDefault(emptyMap())
}
