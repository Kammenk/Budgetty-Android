package com.budgetty.app.ui.streaks

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Pins the §2 outcome-streak contract that the iOS port must mirror exactly: per-scope (not
 * all-or-nothing) streaks, closed periods only with a separate live-on-track signal, strict reset on a
 * miss with an always-computed best inside the [StreakEngine.MAX_STREAK] window, and — critically — a
 * period with no receipts at all treated as "no data" rather than conflated with a budgeted scope that
 * simply spent nothing that period.
 */
class StreakEngineTest {

    private fun txn(period: Int, category: String, amount: String) =
        StreakTxn(period, category, BigDecimal(amount))

    private fun budgetInput(
        txns: List<StreakTxn>,
        categoryBudgets: Map<String, String> = emptyMap(),
        monthlyBudget: String? = null,
        monthlyAdjustment: Map<Int, String> = emptyMap(),
        live: LiveBudgetPeriod? = null,
    ) = BudgetStreakInput(
        transactions = txns,
        categoryBudgets = categoryBudgets.mapValues { BigDecimal(it.value) },
        monthlyBudget = monthlyBudget?.let { BigDecimal(it) },
        kind = StreakKind.BUDGET_MONTH,
        monthlyAdjustmentByPeriod = monthlyAdjustment.mapValues { BigDecimal(it.value) },
        live = live,
    )

    private fun onlyStreak(input: BudgetStreakInput): Streak {
        val streaks = StreakEngine.budgetStreaks(input)
        assertThat(streaks).hasSize(1)
        return streaks.first()
    }

    // ── Consecutive periods ─────────────────────────────────────────────────────────

    @Test fun consecutive_closed_periods_under_budget_count_up() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "80"),
                    txn(1, "Groceries", "90"),
                    txn(2, "Groceries", "70"),
                ),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.kind).isEqualTo(StreakKind.BUDGET_MONTH)
        assertThat(s.label).isEqualTo("Groceries")
        assertThat(s.current).isEqualTo(3)
        assertThat(s.best).isEqualTo(3)
        assertThat(s.periodsChecked).isEqualTo(3)
    }

    @Test fun spend_exactly_at_budget_is_met() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "100"), txn(1, "Groceries", "100")),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.current).isEqualTo(2)
    }

    // ── Strict reset + current==0 while best>0 ──────────────────────────────────────

    @Test fun a_miss_in_the_most_recent_closed_period_resets_current_to_zero_but_best_survives() {
        val s = onlyStreak(
            budgetInput(
                // period 0 over budget; the two before it were clean.
                txns = listOf(
                    txn(0, "Groceries", "120"),
                    txn(1, "Groceries", "80"),
                    txn(2, "Groceries", "70"),
                ),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.current).isEqualTo(0)
        assertThat(s.best).isEqualTo(2)
        assertThat(s.periodsChecked).isEqualTo(3)
    }

    @Test fun a_mid_run_miss_resets_current_but_best_is_the_longer_earlier_run() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "80"), // met
                    txn(1, "Groceries", "90"), // met
                    txn(2, "Groceries", "150"), // miss
                    txn(3, "Groceries", "70"), // met
                    txn(4, "Groceries", "60"), // met
                    txn(5, "Groceries", "50"), // met
                ),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.current).isEqualTo(2)
        assertThat(s.best).isEqualTo(3)
    }

    // ── best within the 24-period window ────────────────────────────────────────────

    @Test fun best_is_computed_within_the_max_streak_window_and_ignores_periods_beyond_it() {
        val txns = buildList {
            (0..5).forEach { add(txn(it, "Groceries", "80")) } // 6 met
            add(txn(6, "Groceries", "150")) // miss
            (7..40).forEach { add(txn(it, "Groceries", "80")) } // met, but 24..40 fall outside the window
        }
        val s = onlyStreak(budgetInput(txns, categoryBudgets = mapOf("Groceries" to "100")))
        assertThat(s.current).isEqualTo(6)
        assertThat(s.best).isEqualTo(17) // periods 7..23 only — 24..40 are outside MAX_STREAK
        assertThat(s.periodsChecked).isEqualTo(StreakEngine.MAX_STREAK)
    }

    @Test fun current_and_best_are_bounded_by_max_streak() {
        val txns = (0..29).map { txn(it, "Groceries", "80") } // 30 consecutive met periods
        val s = onlyStreak(budgetInput(txns, categoryBudgets = mapOf("Groceries" to "100")))
        assertThat(s.current).isEqualTo(StreakEngine.MAX_STREAK)
        assertThat(s.best).isEqualTo(StreakEngine.MAX_STREAK)
        assertThat(s.periodsChecked).isEqualTo(StreakEngine.MAX_STREAK)
    }

    // ── liveOnTrack — the open period, never counted in current ──────────────────────

    @Test fun live_on_track_extends_nothing_into_current() {
        val input = budgetInput(
            txns = listOf(txn(0, "Groceries", "80"), txn(1, "Groceries", "90")),
            categoryBudgets = mapOf("Groceries" to "100"),
            live = LiveBudgetPeriod(listOf(txn(0, "Groceries", "40"))),
        )
        val s = onlyStreak(input)
        assertThat(s.current).isEqualTo(2) // NOT 3 — the open period is never counted
        assertThat(s.liveOnTrack).isTrue()
    }

    @Test fun live_over_budget_is_not_on_track() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80")),
                categoryBudgets = mapOf("Groceries" to "100"),
                live = LiveBudgetPeriod(listOf(txn(0, "Groceries", "150"))),
            ),
        )
        assertThat(s.current).isEqualTo(1)
        assertThat(s.liveOnTrack).isFalse()
    }

    @Test fun live_with_no_spend_yet_is_on_track() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80")),
                categoryBudgets = mapOf("Groceries" to "100"),
                live = LiveBudgetPeriod(emptyList()),
            ),
        )
        assertThat(s.liveOnTrack).isTrue()
    }

    @Test fun no_live_period_reads_as_not_on_track() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80")),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.liveOnTrack).isFalse()
    }

    // ── No-budget scope ─────────────────────────────────────────────────────────────

    @Test fun no_budget_at_all_produces_no_streaks() {
        val streaks = StreakEngine.budgetStreaks(
            budgetInput(txns = listOf(txn(0, "Groceries", "80"))),
        )
        assertThat(streaks).isEmpty()
    }

    @Test fun a_category_without_a_budget_gets_no_streak_of_its_own() {
        val streaks = StreakEngine.budgetStreaks(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80"), txn(0, "Dining", "40")),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(streaks.map { it.label }).containsExactly("Groceries")
    }

    // ── Empty period vs zero-spend-with-data must NOT be conflated ───────────────────

    @Test fun a_closed_period_with_no_receipts_at_all_breaks_the_streak() {
        val s = onlyStreak(
            budgetInput(
                // period 1 has NO transactions anywhere → no data → breaks the run.
                txns = listOf(txn(0, "Groceries", "80"), txn(2, "Groceries", "70")),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        )
        assertThat(s.current).isEqualTo(1)
    }

    @Test fun a_budgeted_scope_that_simply_spent_nothing_in_a_period_with_data_is_met() {
        // period 0 has receipts (Dining) but Groceries spent nothing → Groceries is MET, not "no data".
        // period 2 has NO receipts at all → that IS "no data" and breaks the run. The two are distinct.
        val s = StreakEngine.budgetStreaks(
            budgetInput(
                txns = listOf(
                    txn(0, "Dining", "50"), // data present in period 0; Groceries spend = 0
                    txn(1, "Groceries", "80"),
                    // period 2 intentionally empty (no data)
                    txn(3, "Groceries", "70"),
                ),
                categoryBudgets = mapOf("Groceries" to "100"),
            ),
        ).first { it.label == "Groceries" }
        assertThat(s.current).isEqualTo(2) // periods 0 (zero-spend, met) + 1 (met); period 2 no-data breaks
        assertThat(s.best).isEqualTo(2)
        assertThat(s.periodsChecked).isEqualTo(3) // periods 0,1,3 have data; period 2 does not
    }

    // ── Per-scope independence ──────────────────────────────────────────────────────

    @Test fun one_category_clean_while_another_overspends_yields_independent_streaks() {
        val streaks = StreakEngine.budgetStreaks(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "80"), txn(0, "Dining", "70"), // Dining over (>50) in period 0
                    txn(1, "Groceries", "90"), txn(1, "Dining", "40"),
                    txn(2, "Groceries", "70"), txn(2, "Dining", "30"),
                ),
                categoryBudgets = mapOf("Groceries" to "100", "Dining" to "50"),
            ),
        ).associateBy { it.label }

        assertThat(streaks.getValue("Groceries").current).isEqualTo(3)
        assertThat(streaks.getValue("Dining").current).isEqualTo(0) // most recent closed period was over
        assertThat(streaks.getValue("Dining").best).isEqualTo(2) // the two clean periods before it
    }

    @Test fun category_budgets_take_precedence_over_the_monthly_budget() {
        // Groceries 80 is under its own 100 cap but over the tiny 50 monthly budget → the category cap wins.
        val s = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80")),
                categoryBudgets = mapOf("Groceries" to "100"),
                monthlyBudget = "50",
            ),
        )
        assertThat(s.label).isEqualTo("Groceries")
        assertThat(s.current).isEqualTo(1)
    }

    // ── Monthly (whole-budget) scope + paid adjustment ──────────────────────────────

    @Test fun monthly_scope_uses_the_whole_budget_when_no_category_budgets_are_set() {
        val s = onlyStreak(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "40"), txn(0, "Dining", "40"), // 80 total <= 100
                    txn(1, "Groceries", "95"),
                ),
                monthlyBudget = "100",
            ),
        )
        assertThat(s.label).isEqualTo("MONTHLY")
        assertThat(s.current).isEqualTo(2)
    }

    @Test fun the_paid_adjustment_can_tip_the_monthly_scope_over_budget() {
        val withoutAdjustment = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "90"), txn(1, "Groceries", "90")),
                monthlyBudget = "100",
            ),
        )
        assertThat(withoutAdjustment.current).isEqualTo(2)

        val withAdjustment = onlyStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "90"), txn(1, "Groceries", "90")),
                monthlyBudget = "100",
                monthlyAdjustment = mapOf(0 to "20"), // 90 + 20 = 110 > 100 → period 0 misses
            ),
        )
        assertThat(withAdjustment.current).isEqualTo(0)
        assertThat(withAdjustment.best).isEqualTo(1)
    }

    // ── allScopesStreak — the legacy every-scope aggregate ──────────────────────────

    @Test fun all_scopes_streak_counts_only_periods_where_every_scope_stayed_under() {
        val s = StreakEngine.allScopesStreak(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "80"), txn(0, "Dining", "70"), // Dining over → aggregate misses
                    txn(1, "Groceries", "90"), txn(1, "Dining", "40"),
                    txn(2, "Groceries", "70"), txn(2, "Dining", "30"),
                ),
                categoryBudgets = mapOf("Groceries" to "100", "Dining" to "50"),
            ),
        )
        assertThat(s.current).isEqualTo(0) // one category over in the most recent period zeroes the aggregate
        assertThat(s.best).isEqualTo(2)
    }

    @Test fun all_scopes_streak_counts_up_when_every_scope_is_under() {
        val s = StreakEngine.allScopesStreak(
            budgetInput(
                txns = listOf(
                    txn(0, "Groceries", "80"), txn(0, "Dining", "40"),
                    txn(1, "Groceries", "90"), txn(1, "Dining", "45"),
                    txn(2, "Groceries", "70"), txn(2, "Dining", "30"),
                ),
                categoryBudgets = mapOf("Groceries" to "100", "Dining" to "50"),
            ),
        )
        assertThat(s.current).isEqualTo(3)
    }

    @Test fun all_scopes_streak_breaks_on_a_period_with_no_data() {
        val s = StreakEngine.allScopesStreak(
            budgetInput(
                txns = listOf(txn(0, "Groceries", "80"), txn(2, "Groceries", "70")),
                monthlyBudget = "100",
            ),
        )
        assertThat(s.current).isEqualTo(1)
    }

    @Test fun all_scopes_streak_is_zero_when_there_is_no_budget() {
        val s = StreakEngine.allScopesStreak(budgetInput(txns = listOf(txn(0, "Groceries", "80"))))
        assertThat(s.current).isEqualTo(0)
        assertThat(s.best).isEqualTo(0)
        assertThat(s.periodsChecked).isEqualTo(0)
    }

    // ── Limit streaks ───────────────────────────────────────────────────────────────

    @Test fun limit_streak_counts_consecutive_closed_windows_within_cap() {
        val s = StreakEngine.limitStreak(
            LimitStreakInput(
                label = "Coke",
                cap = 2,
                closedWindows = listOf(
                    LimitWindow(1, hasData = true), // met
                    LimitWindow(2, hasData = true), // met (at cap)
                    LimitWindow(3, hasData = true), // over cap → miss
                    LimitWindow(0, hasData = true), // met
                ),
            ),
        )
        assertThat(s.kind).isEqualTo(StreakKind.LIMIT)
        assertThat(s.current).isEqualTo(2)
        assertThat(s.best).isEqualTo(2)
        assertThat(s.periodsChecked).isEqualTo(4)
    }

    @Test fun limit_window_with_no_receipts_is_no_data_but_zero_purchases_with_data_is_met() {
        val s = StreakEngine.limitStreak(
            LimitStreakInput(
                label = "Coke",
                cap = 2,
                closedWindows = listOf(
                    LimitWindow(0, hasData = true), // bought nothing but did log receipts → met
                    LimitWindow(0, hasData = false), // no receipts at all → no data → breaks
                    LimitWindow(1, hasData = true),
                ),
            ),
        )
        assertThat(s.current).isEqualTo(1)
        assertThat(s.periodsChecked).isEqualTo(2)
    }

    @Test fun limit_live_window_within_cap_is_on_track_and_never_counted() {
        val s = StreakEngine.limitStreak(
            LimitStreakInput(
                label = "Coke",
                cap = 2,
                closedWindows = listOf(LimitWindow(1, hasData = true), LimitWindow(0, hasData = true)),
                live = LimitWindow(1, hasData = true),
            ),
        )
        assertThat(s.current).isEqualTo(2)
        assertThat(s.liveOnTrack).isTrue()
    }

    @Test fun limit_live_window_over_cap_is_not_on_track() {
        val s = StreakEngine.limitStreak(
            LimitStreakInput(
                label = "Coke",
                cap = 2,
                closedWindows = listOf(LimitWindow(1, hasData = true)),
                live = LimitWindow(5, hasData = true),
            ),
        )
        assertThat(s.liveOnTrack).isFalse()
    }

    // ── Minimum bar (§2.7) ──────────────────────────────────────────────────────────

    @Test fun surfaced_keeps_only_streaks_of_at_least_two() {
        val one = Streak(StreakKind.BUDGET_MONTH, "A", current = 1, best = 4, periodsChecked = 6, liveOnTrack = true)
        val two = Streak(StreakKind.BUDGET_MONTH, "B", current = 2, best = 2, periodsChecked = 2, liveOnTrack = false)
        val six = Streak(StreakKind.LIMIT, "C", current = 6, best = 6, periodsChecked = 6, liveOnTrack = true)
        assertThat(StreakEngine.surfaced(listOf(one, two, six)).map { it.label }).containsExactly("B", "C")
    }
}
