package com.budgetty.app.ui.util

import com.budgetty.app.data.local.BuyingLimitTimeframe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * A single purchased line, reduced to just what a buying-limit needs: the item [name] as printed, its
 * [quantity], and the receipt's made-date [timestamp] (epoch millis). Kept separate from the Room
 * entity so the counting logic stays pure and JVM-testable (and trivially portable to iOS).
 */
data class CountableItem(
    val name: String,
    val quantity: Int,
    val timestamp: Long,
)

/**
 * The keyword purchase-cap matcher and counter. Pure and Android-free so it unit-tests on the JVM and
 * ports 1:1 to iOS.
 *
 * The match is a NEW substring matcher — deliberately not Category rules (whole-name exact match) nor
 * `productStats` (counts rows, ignores quantity): a limit's keywords match anywhere inside an item
 * name (so "coke" catches "Coca-Cola 500ml" and "Coke Zero"), multiple keywords OR into one counter
 * (the same product prints under many names), and the counter sums **quantity** within the current
 * window. All folding is `trim().lowercase()` in Kotlin — Unicode-aware, so Cyrillic folds too; there
 * is no diacritic stripping.
 */
object BuyingLimitCounter {

    /** The canonical match key for [raw]: trimmed + lower-cased (Unicode-aware). */
    fun normalize(raw: String): String = raw.trim().lowercase()

    /**
     * True when [rawName]'s normalized form contains ANY of the [keywords] (which are expected to be
     * pre-normalized, as stored). Substring, case- and Cyrillic-insensitive. Blank keywords are ignored.
     */
    fun matches(rawName: String, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return false
        val name = normalize(rawName)
        return keywords.any { it.isNotEmpty() && name.contains(it) }
    }

    /**
     * Σ [CountableItem.quantity] of [items] whose name matches any of [keywords] AND whose timestamp
     * falls within [startMillis, endMillis] inclusive. This is the count "bought" against a limit.
     */
    fun countInWindow(
        items: List<CountableItem>,
        keywords: List<String>,
        startMillis: Long,
        endMillis: Long,
    ): Int = items.asSequence()
        .filter { it.timestamp in startMillis..endMillis }
        .filter { matches(it.name, keywords) }
        .sumOf { it.quantity }

    /**
     * Inclusive [start, end] epoch-millis window for [timeframe] as of [today]:
     *  - MONTHLY: the user's pay-cycle month ([PayCycle.month] anchored on [monthStartDay]).
     *  - WEEKLY: the week containing [today], starting on the locale's [firstDayOfWeek].
     */
    fun window(
        timeframe: BuyingLimitTimeframe,
        today: LocalDate = LocalDate.now(),
        monthStartDay: Int = 1,
        firstDayOfWeek: DayOfWeek = localeFirstDayOfWeek(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> {
        val (startDate, endInclusive) = when (timeframe) {
            BuyingLimitTimeframe.MONTHLY -> PayCycle.month(today, monthStartDay)
            BuyingLimitTimeframe.WEEKLY -> {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                weekStart to weekStart.plusDays(DAYS_IN_WEEK - 1L)
            }
        }
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Convenience: the count bought against [timeframe]'s current window. */
    fun count(
        items: List<CountableItem>,
        keywords: List<String>,
        timeframe: BuyingLimitTimeframe,
        today: LocalDate = LocalDate.now(),
        monthStartDay: Int = 1,
        firstDayOfWeek: DayOfWeek = localeFirstDayOfWeek(),
    ): Int {
        val (start, end) = window(timeframe, today, monthStartDay, firstDayOfWeek)
        return countInWindow(items, keywords, start, end)
    }

    /**
     * The date the current window rolls over (its count resets to 0):
     *  - MONTHLY: the next pay-cycle month's start day.
     *  - WEEKLY: the start of next week ([firstDayOfWeek]).
     */
    fun nextReset(
        timeframe: BuyingLimitTimeframe,
        today: LocalDate = LocalDate.now(),
        monthStartDay: Int = 1,
        firstDayOfWeek: DayOfWeek = localeFirstDayOfWeek(),
    ): LocalDate = when (timeframe) {
        BuyingLimitTimeframe.MONTHLY -> PayCycle.month(today, monthStartDay, offset = 1).first
        BuyingLimitTimeframe.WEEKLY ->
            today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).plusWeeks(1)
    }

    /** The locale's first day of the week (Monday across most of Europe, Sunday in a few locales). */
    fun localeFirstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
        WeekFields.of(locale).firstDayOfWeek

    private const val DAYS_IN_WEEK = 7
}
