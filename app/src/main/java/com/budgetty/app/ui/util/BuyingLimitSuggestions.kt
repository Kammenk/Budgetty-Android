package com.budgetty.app.ui.util

import com.budgetty.app.data.local.BuyingLimitTimeframe
import java.time.LocalDate
import java.time.ZoneId

/**
 * A frequency-derived buying-limit suggestion (§4.4): an item the user buys a lot but hasn't capped.
 * Pure data so the ranking is JVM-testable and ports 1:1 to iOS.
 */
data class LimitSuggestion(
    /** Representative display name in its original casing, e.g. "Coca-Cola". */
    val name: String,
    /** The normalized keyword to seed the editor with ([BuyingLimitCounter.normalize]); also the dismissal key. */
    val keyword: String,
    /** Quantity bought in the last month — the "N×" shown in the prompt. */
    val monthCount: Int,
    /** Suggested weekly cap = the current weekly rate rounded down, floored at 1. */
    val suggestedCap: Int,
    /** The timeframe the [suggestedCap] is expressed in (always weekly for now). */
    val timeframe: BuyingLimitTimeframe = BuyingLimitTimeframe.WEEKLY,
)

/**
 * Ranks up to [MAX_SUGGESTIONS] buying-limit suggestions from the user's own purchase history —
 * **frequency-only**, no attempt to guess which items are "staples" (that would misfire across 16
 * locales; letting the user reject is better, §4.4). Pure and Android-free so it unit-tests on the JVM
 * and mirrors 1:1 on iOS.
 *
 * A name qualifies when its total quantity over the last [LOOKBACK_DAYS] days is at least
 * [MIN_QUANTITY] AND it was bought at least once in the last [RECENT_DAYS] days (so a stale item isn't
 * offered as "most bought lately"). Anything already caught by an existing limit's keyword, or whose
 * key the user has dismissed, is dropped and never returns. Candidates are ranked by the last-month
 * count (the number the prompt shows), so the visible list is sorted by its visible figure.
 */
object BuyingLimitSuggestions {

    /** Qualification window: total quantity is summed over this many days back. */
    const val LOOKBACK_DAYS = 60L

    /** "Most bought lately": a candidate must also have been bought within this many days back. */
    const val RECENT_DAYS = 30L

    /** Minimum total quantity in the [LOOKBACK_DAYS] window to be worth suggesting a cap for. */
    const val MIN_QUANTITY = 6

    /** At most this many suggestions are ever offered at once. */
    const val MAX_SUGGESTIONS = 3

    private const val WEEK_DAYS = 7

    /**
     * The suggestions to offer, best (most-bought-lately) first.
     *
     * @param items every saved purchased line (name / quantity / made-date millis).
     * @param existingKeywords all normalized keywords across the user's existing limits — a candidate
     *   already matched by any of them is skipped (no duplicate-limit suggestions).
     * @param dismissed normalized keys the user has dismissed; each is skipped for good.
     */
    fun suggest(
        items: List<CountableItem>,
        existingKeywords: List<String>,
        dismissed: Set<String>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LimitSuggestion> {
        val lookbackStart = today.minusDays(LOOKBACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val recentStart = today.minusDays(RECENT_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        return items.asSequence()
            .filter { it.timestamp in lookbackStart..end && it.name.isNotBlank() }
            .groupBy { BuyingLimitCounter.normalize(it.name) }
            .mapNotNull { (key, rows) -> candidate(key, rows, recentStart, end, existingKeywords, dismissed) }
            .sortedWith(compareByDescending<LimitSuggestion> { it.monthCount }.thenBy { it.name })
            .take(MAX_SUGGESTIONS)
            .toList()
    }

    private fun candidate(
        key: String,
        rows: List<CountableItem>,
        recentStart: Long,
        end: Long,
        existingKeywords: List<String>,
        dismissed: Set<String>,
    ): LimitSuggestion? {
        if (key.isEmpty() || key in dismissed) return null
        // Already covered by an existing limit (any of its keywords matches this name) → don't re-suggest.
        if (BuyingLimitCounter.matches(rows.first().name, existingKeywords)) return null
        val total = rows.sumOf { it.quantity }
        if (total < MIN_QUANTITY) return null
        val monthCount = rows.filter { it.timestamp in recentStart..end }.sumOf { it.quantity }
        if (monthCount <= 0) return null
        // The most-recently-bought row lends its original casing, so the name reads current.
        val displayName = rows.maxByOrNull { it.timestamp }?.name ?: rows.first().name
        return LimitSuggestion(
            name = displayName,
            keyword = key,
            monthCount = monthCount,
            // Weekly rate implied by last month's purchases, rounded down, floored at 1.
            suggestedCap = (monthCount * WEEK_DAYS / RECENT_DAYS).toInt().coerceAtLeast(1),
        )
    }
}
