package com.budgetty.app.ui.util

import com.budgetty.app.data.local.BuyingLimitTimeframe
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The editor's live "CURRENTLY MATCHES" preview, computed purely from the saved items so the user sees
 * exactly what a limit will catch before saving — this is how substring matching is kept honest (e.g.
 * warning that "ice" would also catch "juice"). Pure/JVM-testable; ports 1:1 to iOS.
 *
 * The distinct item NAMES and the too-broad signal are drawn from all saved receipts (what the keyword
 * catches overall); the [windowQuantity] is scoped to the selected timeframe's current window (what
 * would count toward the limit right now).
 */
data class BuyingLimitPreview(
    val state: State,
    /** Up to [MAX_NAMES] representative item names (original casing), most-bought first. */
    val names: List<String>,
    /** Distinct matching item names beyond the [names] shown ("+n more"); 0 when all fit. */
    val moreCount: Int,
    /** Σ quantity matching in the selected timeframe's current window. */
    val windowQuantity: Int,
    /** True when a short keyword catches a lot of distinct items — a "catching a lot" warning (never blocks save). */
    val tooBroad: Boolean,
    /** A more specific keyword to suggest instead (already lower-cased), or null when none is sensible. */
    val suggestion: String?,
) {
    enum class State { HIDDEN, NO_MATCH, MATCH }

    companion object {
        const val MAX_NAMES = 3
        private const val BROAD_MIN_DISTINCT = 6
        private const val BROAD_SHORT_LEN = 4

        fun compute(
            items: List<CountableItem>,
            rawKeywords: List<String>,
            timeframe: BuyingLimitTimeframe,
            label: String = "",
            today: LocalDate = LocalDate.now(),
            monthStartDay: Int = 1,
            firstDayOfWeek: DayOfWeek = BuyingLimitCounter.localeFirstDayOfWeek(),
        ): BuyingLimitPreview {
            val keywords = rawKeywords.map(BuyingLimitCounter::normalize).filter { it.isNotEmpty() }.distinct()
            if (keywords.isEmpty()) return hidden()

            val matching = items.filter { BuyingLimitCounter.matches(it.name, keywords) }
            if (matching.isEmpty()) {
                return BuyingLimitPreview(State.NO_MATCH, emptyList(), 0, 0, tooBroad = false, suggestion = null)
            }

            // Distinct items by normalized name, each with a representative display name + total qty ever.
            val groups = matching
                .groupBy { BuyingLimitCounter.normalize(it.name) }
                .map { (_, rows) -> rows.first().name to rows.sumOf { it.quantity } }
                .sortedByDescending { it.second }
            val names = groups.take(MAX_NAMES).map { it.first }
            val moreCount = (groups.size - names.size).coerceAtLeast(0)

            val (start, end) = BuyingLimitCounter.window(timeframe, today, monthStartDay, firstDayOfWeek)
            val windowQuantity = BuyingLimitCounter.countInWindow(matching, keywords, start, end)

            val tooBroad = groups.size >= BROAD_MIN_DISTINCT && keywords.any { it.length <= BROAD_SHORT_LEN }
            val suggestion = if (tooBroad) suggestFor(label, keywords, groups.first().first) else null

            return BuyingLimitPreview(State.MATCH, names, moreCount, windowQuantity, tooBroad, suggestion)
        }

        private fun hidden() = BuyingLimitPreview(State.HIDDEN, emptyList(), 0, 0, tooBroad = false, suggestion = null)

        /**
         * A more specific keyword to offer: the user's own [label] if it's a longer term they haven't
         * already added, else the most-bought matching product's name — either way only when it's
         * genuinely narrower (longer) than the shortest current keyword.
         */
        private fun suggestFor(label: String, keywords: List<String>, topName: String): String? {
            val shortest = keywords.minOf { it.length }
            val fromLabel = BuyingLimitCounter.normalize(label)
                .takeIf { it.isNotEmpty() && it !in keywords && it.length > shortest }
            val fromTop = BuyingLimitCounter.normalize(topName)
                .takeIf { it.isNotEmpty() && it !in keywords && it.length > shortest }
            return fromLabel ?: fromTop
        }
    }
}
