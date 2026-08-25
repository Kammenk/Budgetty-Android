package com.budgetty.app.ui.recap

import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Pins the two pure pieces of the weekly outcome-streak sourcing (§1.3) the iOS port must mirror:
 * [weeklyShareOf] (a monthly budget sliced to one week) and [pickWeekStreak] (which single scope the
 * weekly Streak card surfaces — a live current run first, else a best-run fallback, both at the ≥2 floor).
 */
class RecapWeekStreakTest {

    private fun weekStreak(label: String, current: Int, best: Int, live: Boolean = false) =
        Streak(
            kind = StreakKind.BUDGET_WEEK, label = label, current = current, best = best,
            periodsChecked = maxOf(current, best), liveOnTrack = live,
        )

    // ── weeklyShareOf: monthly budget → weekly allowance (× 12 ⁄ 52) ──────────────

    @Test fun weekly_share_slices_a_monthly_budget_to_a_week() {
        // 400 × 12 ⁄ 52 = 92.3076… → 92.31 (2dp, HALF_UP).
        assertThat(weeklyShareOf(BigDecimal("400"))).isEqualToIgnoringScale(BigDecimal("92.31"))
    }

    @Test fun weekly_share_is_exact_when_it_divides_evenly() {
        // 130 × 12 ⁄ 52 = 30 exactly.
        assertThat(weeklyShareOf(BigDecimal("130"))).isEqualToIgnoringScale(BigDecimal("30"))
    }

    @Test fun weekly_share_of_zero_is_zero() {
        assertThat(weeklyShareOf(BigDecimal.ZERO)).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    // ── pickWeekStreak: which single scope surfaces ───────────────────────────────

    @Test fun prefers_a_live_current_run_over_a_longer_best_run() {
        // A has a huge best but 0 current; B/C are live current runs — a current run always wins.
        val picked = pickWeekStreak(
            listOf(weekStreak("A", current = 0, best = 12), weekStreak("B", current = 3, best = 3)),
        )
        assertThat(picked?.label).isEqualTo("B")
        assertThat(picked?.current).isEqualTo(3)
    }

    @Test fun among_current_runs_picks_the_longest_then_the_higher_best() {
        val picked = pickWeekStreak(
            listOf(
                weekStreak("A", current = 2, best = 9),
                weekStreak("B", current = 3, best = 3),
                weekStreak("C", current = 3, best = 8),
            ),
        )
        // B and C tie on current 3; C's higher best breaks it.
        assertThat(picked?.label).isEqualTo("C")
    }

    @Test fun the_one_period_floor_is_respected_a_current_of_one_never_surfaces() {
        // current 1 < MIN_TO_SURFACE and best 1 < the floor too → nothing to show.
        val picked = pickWeekStreak(
            listOf(weekStreak("A", current = 1, best = 1), weekStreak("B", current = 0, best = 1)),
        )
        assertThat(picked).isNull()
    }

    @Test fun falls_back_to_the_strongest_best_run_when_no_current_run_qualifies() {
        val picked = pickWeekStreak(
            listOf(weekStreak("A", current = 0, best = 6), weekStreak("B", current = 1, best = 4)),
        )
        assertThat(picked?.label).isEqualTo("A")
        assertThat(picked?.current).isEqualTo(0) // rendered as the best-run fallback
        assertThat(picked?.best).isEqualTo(6)
    }

    @Test fun returns_null_when_the_list_is_empty() {
        assertThat(pickWeekStreak(emptyList())).isNull()
    }
}
