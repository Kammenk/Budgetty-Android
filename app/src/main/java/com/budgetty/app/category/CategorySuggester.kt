package com.budgetty.app.category

import kotlin.math.pow

/**
 * Ranks a user's categories for the picker's "Suggested for you" row — frequency weighted toward
 * recent use, so the categories they reach for lately float to the top. Pure Kotlin (no Android or
 * database dependency) so it stays unit-testable and can run off a cheap `category, timestamp`
 * snapshot; the UI layer resolves emoji/colour/parent for whatever names come back.
 *
 * It needs no new data model — ranking is a query over the transactions the user already has, and
 * the learned name → category rules (applied separately, by the picker, for the context-aware lead)
 * power the "because you usually…" match. See the habit-suggestions design brief.
 */
object CategorySuggester {

    /** How many chips the Suggested row shows at most. */
    const val LIMIT = 5

    /**
     * Below this many categorised transactions there isn't a meaningful habit yet, so the row shows
     * generic [COMMON_PICKS] labelled honestly as "Common picks" rather than personalised ones.
     */
    const val MIN_FOR_PERSONALIZED = 5

    /** Recency half-life: a transaction this many days old counts half as much as a brand-new one. */
    private const val HALF_LIFE_DAYS = 30.0
    private const val DAY_MS = 86_400_000.0

    /**
     * Generic starter suggestions for users without enough history — everyday essentials, ordered to
     * match the design's "Common picks" row. Filtered to names that still exist in the taxonomy so a
     * future rename/removal can never surface a dead chip.
     */
    val COMMON_PICKS: List<String> =
        listOf("Groceries", "Restaurant & Dining", "Coffee & Cafés", "Public Transport", "Fuel")
            .filter { Categories.isPredefined(it) }

    /** [categories] best-first; [personalized] is false when these are the generic [COMMON_PICKS]. */
    data class Ranked(val categories: List<String>, val personalized: Boolean)

    /**
     * Ranks [stamps] (each a `category to timestampMillis`) by frequency weighted toward recent use:
     * every transaction adds `0.5^(ageDays / HALF_LIFE_DAYS)` to its category's score, so recent
     * spend counts most and a long-dormant category sinks. Returns the top [limit] once the user has
     * at least [MIN_FOR_PERSONALIZED] transactions; otherwise the generic [COMMON_PICKS]. The list is
     * empty only when there's no history and no defaults apply (then the picker omits the row).
     */
    fun rank(stamps: List<Pair<String, Long>>, now: Long, limit: Int = LIMIT): Ranked {
        val usable = stamps.filter { it.first.isNotBlank() }
        if (usable.size < MIN_FOR_PERSONALIZED) {
            return Ranked(COMMON_PICKS.take(limit), personalized = false)
        }
        val scores = HashMap<String, Double>()
        for ((category, ts) in usable) {
            val ageDays = (now - ts).coerceAtLeast(0L) / DAY_MS
            val weight = 0.5.pow(ageDays / HALF_LIFE_DAYS)
            scores[category] = (scores[category] ?: 0.0) + weight
        }
        val ranked = scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .map { it.key }
            .take(limit)
        return Ranked(ranked, personalized = true)
    }
}
