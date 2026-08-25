package com.budgetty.app.ui.wellbeing

import com.budgetty.app.data.local.WellbeingScoreEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pins the pure history-snapshot logic (§3.1): the closed-vs-in-flight month decision, the "yyyy-MM"
 * id math (calendar and pay-cycle), and the componentsJson round-trip the future breakdown view reads.
 *
 * The provider-level guarantee that the in-flight month can never be written to history is structural:
 * [WellbeingProvider] only ever passes offset -1's id + score to [WellbeingHistory.closedSnapshot], and
 * never the current cycle's. That seam is proved here by showing the persisted periodId is always the
 * previous (closed) cycle and is distinct from the current one. A full provider integration test is
 * disproportionate — its repository dependencies are final classes with a Firebase-coupled constructor
 * that can't be faked without opening the architecture.
 */
class WellbeingHistoryTest {

    private fun scoreOf(total: Int?, band: WellbeingBand?) = WellbeingScore(
        score = total,
        band = band,
        components = listOf(
            WellbeingComponent(WellbeingComponentKey.SAVINGS, WellbeingEngine.W_SAVINGS, 80),
            WellbeingComponent(WellbeingComponentKey.BUDGET, WellbeingEngine.W_BUDGET, 60),
            WellbeingComponent(WellbeingComponentKey.TREND, WellbeingEngine.W_TREND, null),
            WellbeingComponent(WellbeingComponentKey.SUBSCRIPTIONS, WellbeingEngine.W_SUBSCRIPTIONS, 100),
            WellbeingComponent(WellbeingComponentKey.GOALS, WellbeingEngine.W_GOALS, null),
        ),
        trendDeltaVsPrevious = null,
    )

    // ── periodId identity ────────────────────────────────────────────────────────

    @Test
    fun `calendar month closed id is the previous month and differs from the current one`() {
        val today = LocalDate.of(2026, 3, 15)
        assertThat(WellbeingHistory.periodId(today, monthStartDay = 1, offset = 0)).isEqualTo("2026-03")
        assertThat(WellbeingHistory.periodId(today, monthStartDay = 1, offset = -1)).isEqualTo("2026-02")
    }

    @Test
    fun `pay-cycle month uses the salary-anchored cycle, not the calendar month`() {
        // Pay day 25th: on the 15th, today sits in the tail of the cycle that opened on Feb 25 → the
        // current cycle is "2026-02" and the just-closed one is "2026-01".
        val today = LocalDate.of(2026, 3, 15)
        assertThat(WellbeingHistory.periodId(today, monthStartDay = 25, offset = 0)).isEqualTo("2026-02")
        assertThat(WellbeingHistory.periodId(today, monthStartDay = 25, offset = -1)).isEqualTo("2026-01")
    }

    @Test
    fun `closed id rolls over the year boundary`() {
        val today = LocalDate.of(2026, 1, 10)
        assertThat(WellbeingHistory.periodId(today, monthStartDay = 1, offset = -1)).isEqualTo("2025-12")
    }

    // ── closed-month snapshot (the guard) ─────────────────────────────────────────

    @Test
    fun `snapshot stores the closed month and never the in-flight one`() {
        val today = LocalDate.of(2026, 3, 15)
        val closedId = WellbeingHistory.periodId(today, monthStartDay = 1, offset = -1)
        val currentId = WellbeingHistory.periodId(today, monthStartDay = 1, offset = 0)

        val entity = WellbeingHistory.closedSnapshot(
            closedPeriodId = closedId,
            closedScore = scoreOf(72, WellbeingBand.HEALTHY),
            computedAt = 1_700_000_000_000L,
        )

        requireNotNull(entity)
        assertThat(entity.periodId).isEqualTo("2026-02")
        assertThat(entity.periodId).isNotEqualTo(currentId) // the in-flight month is never the target
        assertThat(entity.score).isEqualTo(72)
        assertThat(entity.band).isEqualTo("HEALTHY")
        assertThat(entity.computedAt).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun `snapshot is null when the closed month cannot be scored yet`() {
        // A brand-new account (too few receipts) scores null → no junk row is ever written.
        val entity = WellbeingHistory.closedSnapshot(
            closedPeriodId = "2026-02",
            closedScore = scoreOf(null, null),
            computedAt = 1L,
        )
        assertThat(entity).isNull()
    }

    // ── componentsJson round-trip ─────────────────────────────────────────────────

    @Test
    fun `componentsJson preserves every component including the un-scored nulls`() {
        val json = WellbeingHistory.encodeComponents(scoreOf(72, WellbeingBand.HEALTHY).components)
        val decoded = WellbeingHistory.decodeComponents(json)

        assertThat(decoded.keys).containsExactly(
            "SAVINGS", "BUDGET", "TREND", "SUBSCRIPTIONS", "GOALS",
        )
        assertThat(decoded["SAVINGS"]).isEqualTo(80)
        assertThat(decoded["BUDGET"]).isEqualTo(60)
        assertThat(decoded["SUBSCRIPTIONS"]).isEqualTo(100)
        // A component with no data is kept as an explicit null, not dropped.
        assertThat(decoded.containsKey("TREND")).isTrue()
        assertThat(decoded["TREND"]).isNull()
        assertThat(decoded["GOALS"]).isNull()
    }

    @Test
    fun `decodeComponents tolerates a malformed payload`() {
        assertThat(WellbeingHistory.decodeComponents("not json")).isEmpty()
    }

    // ── §3.2 trend sparkline model ────────────────────────────────────────────────

    private fun entity(periodId: String, score: Int) = WellbeingScoreEntity(
        periodId = periodId, score = score, band = WellbeingEngine.band(score).name,
        componentsJson = "{}", computedAt = 0L,
    )

    @Test
    fun `trend renders nothing below two stored months`() {
        // The whole point of the thin-data state: one point (or none) is not a trend → null (no card).
        assertThat(WellbeingHistory.trend(emptyList(), liveScore = 57)).isNull()
        assertThat(WellbeingHistory.trend(listOf(entity("2026-03", 49)), liveScore = 57)).isNull()
    }

    @Test
    fun `trend builds from stored months and anchors the delta on the live ghost`() {
        val stored = listOf(entity("2026-03", 49), entity("2026-04", 52), entity("2026-05", 55))
        val trend = requireNotNull(WellbeingHistory.trend(stored, liveScore = 57))
        assertThat(trend.closed.map { it.score }).containsExactly(49, 52, 55).inOrder()
        assertThat(trend.closed.first().yearMonth).isEqualTo(YearMonth.of(2026, 3))
        assertThat(trend.firstMonth).isEqualTo(YearMonth.of(2026, 3))
        assertThat(trend.liveScore).isEqualTo(57)
        assertThat(trend.deltaSinceFirst).isEqualTo(8) // "now" (live 57) − first shown (49)
    }

    @Test
    fun `trend delta falls back to the last closed month when there is no live score`() {
        val stored = listOf(entity("2026-03", 49), entity("2026-04", 60))
        val trend = requireNotNull(WellbeingHistory.trend(stored, liveScore = null))
        assertThat(trend.liveScore).isNull()
        assertThat(trend.deltaSinceFirst).isEqualTo(11) // last closed (60) − first (49)
    }

    @Test
    fun `trend drops a malformed periodId and still requires two valid months`() {
        val mixed = listOf(entity("2026-03", 49), entity("bogus", 50))
        assertThat(WellbeingHistory.trend(mixed, liveScore = 57)).isNull() // only one usable point left
    }
}
