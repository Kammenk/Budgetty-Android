package com.budgetty.app.ui.recap

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

/** One buying-limit outcome chip: emoji + label + how many were bought against the [cap]. */
data class RecapLimitChip(val emoji: String, val label: String, val bought: Int, val cap: Int) {
    val under: Boolean get() = bought <= cap
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
        /** Consecutive closed months under budget, ending with this one (≥ 1 on this card). */
        val streakMonths: Int,
        val underCount: Int,
        val scopeCount: Int,
        val segments: List<RecapSegStatus>,
        val safeToSpend: BigDecimal,
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
