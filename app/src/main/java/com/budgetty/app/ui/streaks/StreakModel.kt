package com.budgetty.app.ui.streaks

/**
 * The kind of scope a [Streak] tracks. The value carries the period cadence (a monthly or weekly
 * budget) or that it is a buying-limit window; the scope itself is named by [Streak.label].
 */
enum class StreakKind { BUDGET_MONTH, BUDGET_WEEK, LIMIT }

/**
 * One outcome streak for a single scope, computed by [StreakEngine] over CLOSED periods only. Pure
 * data — no Android, no Compose — so it ports 1:1 to iOS and the surfaces (Budget row, Recap card,
 * Wellbeing evidence) render it however they like. Callers filter to [StreakEngine.MIN_TO_SURFACE]
 * before showing anything (§2.7); the engine itself always reports the honest numbers.
 */
data class Streak(
    val kind: StreakKind,
    /** Scope: a category name, the monthly-budget scope, or a limit's displayTitle. */
    val label: String,
    /** Consecutive CLOSED periods met, ending with the most recent closed one. */
    val current: Int,
    /** Personal best within the history window (see 2.5). */
    val best: Int,
    /** How many closed periods actually backed this — honesty for the label. */
    val periodsChecked: Int,
    /** Is the OPEN period currently on track to extend it. Never counted in [current]. */
    val liveOnTrack: Boolean,
)
