package com.budgetty.app.ui.recap

import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.wellbeing.WellbeingBand
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * The tonal band backdrop of a card, mapped to the app's theme tokens in the composable layer (never
 * a hard-coded colour) so every card themes correctly in dark mode. Mirrors the mockup CSS vars:
 * PRIMARY = `--pc`, GOOD = `--goodc`, WARN = `--warnc`, GREAT = `--greatc`, SECONDARY = `--secc`,
 * NEUTRAL = `--sch`.
 */
enum class RecapBand { PRIMARY, GOOD, WARN, GREAT, SECONDARY, NEUTRAL }

/** Accent tone of a small pill (e.g. the "↓ 12% less" chip): good = the improvement green, warn = amber. */
enum class RecapPillTone { GOOD, WARN, NEUTRAL }

/** Traffic-light state of one budget scope, for the budget card's segment bar. */
enum class RecapSegStatus { GOOD, WARN, BAD }

/**
 * One buying-limit outcome chip: emoji + label + how many were bought against the [cap]. [under] is
 * STRICTLY under the cap (green + counted in "stayed under N of M"); at or over the cap is the warm
 * "reached" state (never red, never counted) — matching the mockup, where 4-of-4 reads as warn, not
 * as a kept limit (§1.3 / no-loss-framing).
 */
data class RecapLimitChip(val emoji: String, val label: String, val bought: Int, val cap: Int) {
    val under: Boolean get() = bought < cap
}

/** The second-biggest category mover, shown as a trailing clause on the mover card. */
data class RecapSecondMover(val category: String, val delta: BigDecimal)

/**
 * The one focus/tip that closes the story. Raw payload only; the composable owns the localized copy.
 */
sealed interface RecapFocus {
    /** A concrete cap to try next period (the tougher-month close, or an over-budget category). */
    data class CapCategory(val category: String, val amount: BigDecimal) : RecapFocus

    /** Keep an eye on a category that crept up. */
    data class WatchCategory(val category: String) : RecapFocus

    /** A material category has no budget yet. */
    data class SetBudget(val category: String) : RecapFocus

    /** Nothing to fix — a genuine "keep it up". */
    data object KeepItUp : RecapFocus
}

/**
 * One card of the story. Each carries RAW figures (BigDecimal/Int/enum) — the stateless composable
 * formats money, dates and copy via string resources so it localizes and dark-themes correctly, and
 * so the model ports 1:1 to iOS. Rendered by a single composable switching on the concrete type.
 */
sealed interface RecapCard {
    val band: RecapBand

    /** Opening card: title/sub come from the story's kind + dates in the composable. */
    data class Cover(override val band: RecapBand) : RecapCard

    /** Total spent (monthly) or spent-this-period, with an optional vs-previous comparison. */
    data class Total(
        override val band: RecapBand,
        val spent: BigDecimal,
        /** Signed % vs the previous period (negative = spent less); null = no comparison (partial). */
        val deltaPercent: Int?,
        val prevMonth: YearMonth?,
        val prevTotal: BigDecimal?,
        /** True when this is an improvement (spent less / flat), driving the good vs amber treatment. */
        val improved: Boolean,
    ) : RecapCard

    /** Monthly-only wellbeing score ring + its change vs the previous month. */
    data class Score(
        override val band: RecapBand,
        val score: Int,
        val scoreBand: WellbeingBand,
        /** Signed change vs the previous month; null when there's no prior score. */
        val delta: Int?,
        val prevMonth: YearMonth?,
    ) : RecapCard

    /** The single biggest category move vs the previous period. */
    data class Mover(
        override val band: RecapBand,
        val category: String,
        val dotColorArgb: Int,
        /** current − previous (negative = a drop, the good direction). */
        val delta: BigDecimal,
        val previousAmount: BigDecimal,
        val currentAmount: BigDecimal,
        val second: RecapSecondMover?,
    ) : RecapCard

    /** Budget adherence + streak, with the per-scope segment bar and safe-to-spend at the close. */
    data class BudgetStreak(
        override val band: RecapBand,
        /**
         * Consecutive CLOSED months where every budgeted scope stayed under, ending with the just-closed
         * one; 0 when the just-closed month wasn't all-under. The hero de-flames per §2.4 (no 🔥) and
         * shows the streak [motif][com.budgetty.app.ui.streaks.StreakMotif] once this is ≥ 2 (§2.7).
         */
        val streakMonths: Int,
        /** Personal best all-under run within the 24-month window; drives the best-run fallback (§2.5). */
        val best: Int,
        /** True when the current open month is on track to extend the run — the motif's ghost segment. */
        val liveOnTrack: Boolean,
        val underCount: Int,
        val scopeCount: Int,
        val segments: List<RecapSegStatus>,
        val safeToSpend: BigDecimal,
    ) : RecapCard

    /**
     * Outcome-streak card (§1.3 / §2): one scope's run of consecutive CLOSED periods met, on the calm
     * secondary band. Sourced from [com.budgetty.app.ui.streaks.StreakEngine]; only ever built when
     * there is something worth showing — a current run ([current] ≥ 2, [isBestRun] = false) or, when the
     * current run is 0, the personal-best fallback ([best] ≥ 2, [isBestRun] = true). Never padded: when
     * neither holds the card is dropped entirely (a bare week stays Cover → Pace → Focus).
     */
    data class Streak(
        override val band: RecapBand,
        /** Cadence of the run — drives the weeks/months copy and the analytics kind. */
        val kind: StreakKind,
        /** Category name for a per-category run, or null for the whole-budget scope ("under budget"). */
        val scope: String?,
        /** Consecutive closed periods met (≥ 2 for a current run; 0 when showing the best-run fallback). */
        val current: Int,
        /** Personal best within the 24-period window (labelled "best in the last 24 …"). */
        val best: Int,
        /** Whether the open period is on track to extend the run — the motif's dotted ghost segment. */
        val liveOnTrack: Boolean,
        /** True = render the "Best run: N weeks" fallback instead of a live current run. */
        val isBestRun: Boolean,
    ) : RecapCard

    /** Buying-limits outcome: how many limits stayed under, plus a chip per limit. Dropped when the
     *  user has no buying limits. */
    data class Limits(
        override val band: RecapBand,
        val underCount: Int,
        val totalCount: Int,
        val chips: List<RecapLimitChip>,
    ) : RecapCard

    /** Weekly pace: spend vs last week + a pace bar with a day-of-week marker. */
    data class Pace(
        override val band: RecapBand,
        val spent: BigDecimal,
        val weeklyBudget: BigDecimal?,
        /** spent ÷ budget, clamped 0..1 (0 when no weekly budget). */
        val fractionUsed: Float,
        /** where the week "should" be by its close (always full for a closed week). */
        val paceFraction: Float,
        val remaining: BigDecimal,
        val deltaPercent: Int?,
    ) : RecapCard

    /** Closing card: the one focus for next period + the Done / See details actions (rendered by the
     *  frame, since they exit the story). */
    data class Focus(
        override val band: RecapBand,
        val focus: RecapFocus,
        val isWeekly: Boolean,
    ) : RecapCard
}

/**
 * A fully-built recap ready to render: the ordered [cards] plus the labels the frame's header needs.
 * Built by [RecapProvider] for the just-closed period; the composable is a dumb renderer of it.
 */
data class RecapStory(
    val kind: RecapKind,
    /** The just-closed pay-cycle month (monthly). For weekly this is the month of [weekEnd]. */
    val monthYear: YearMonth,
    /** The next pay-cycle month, for the monthly focus card's "FOCUS FOR {month}" kicker. */
    val nextMonth: YearMonth,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val cards: List<RecapCard>,
)
