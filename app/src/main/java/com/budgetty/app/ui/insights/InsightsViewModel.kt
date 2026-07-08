package com.budgetty.app.ui.insights

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.components.pieColors
import com.budgetty.app.ui.util.currentMonthRange
import com.budgetty.app.ui.util.monthlyAmount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class StoreSpend(val store: String, val amount: BigDecimal)

/** One income source's contribution to the period, for the "Income by source" card. */
data class IncomeSourceUi(
    val entity: RecurringEntity,
    /** The source's amount scaled to the selected period. */
    val amount: BigDecimal,
    /** Share of total income, 0–100. */
    val percent: Int,
)

/** A recurring bill with its next occurrence, for the "Upcoming bills" card. */
data class UpcomingBill(
    val entity: RecurringEntity,
    /** Whole days from today until the next occurrence (0 = today). */
    val daysUntil: Int,
)

/**
 * How the trend chart groups spend, chosen from the width of the selected period: by calendar
 * [MONTHLY] once the window spans three or more months, otherwise by calendar [DAILY] (this month,
 * last month, or any custom range under three months).
 */
enum class TrendBucketing { DAILY, MONTHLY }

/** One bar in the trend chart: total spend in a single day or month of the selected period. */
data class TrendBucket(
    /** Short label under the bar, e.g. "5" (day) or "Jun" (month). */
    val axisLabel: String,
    /** Fuller label for the selected-bar header, e.g. "5 Jun" or "June". */
    val fullLabel: String,
    val total: BigDecimal,
    /** True for today's bar / the current month, so it can be subtly highlighted. */
    val isCurrent: Boolean,
    /** False for a not-yet-elapsed placeholder day added only to pad the chart out to
     *  [MIN_TREND_BARS]; such bars render dimmed and aren't tappable. */
    val enabled: Boolean = true,
)

/** The trend chart never draws fewer than this many bars: a just-begun month or week is padded out
 *  with inactive placeholder days so the chart still fills a full row instead of looking sparse. */
private const val MIN_TREND_BARS = 7

/**
 * The selected period's spend vs the equal-length period immediately before it: the headline
 * percentage and both totals. The composable layer turns these into the period-relative copy
 * ("…than the previous month/week/quarter"); see [InsightsPeriod.previousPeriod].
 */
data class PeriodComparison(
    val deltaPercent: Int,
    val currentTotal: BigDecimal,
    val previousTotal: BigDecimal,
)

/**
 * One row of the "By category vs last month" card: a category's spend change from the previous
 * calendar month to the current one. [delta] is current − previous (positive = spent more).
 */
data class CategoryDelta(
    val category: String,
    val color: Color,
    val delta: BigDecimal,
)

/**
 * A single rule-based "Highlights" callout (no AI). The view model decides which highlights fire;
 * the composable owns the localized wording. Every variant carries its [category] and [color] for
 * the row's emoji tile.
 */
sealed interface Highlight {
    val category: String
    val color: Color

    /** First spend ever in [category] this period ([amount]), with nothing the period before. */
    data class NewCategory(override val category: String, override val color: Color, val amount: BigDecimal) : Highlight

    /** [category] spend moved [percent]% vs the previous period ([up] = rose, else fell). */
    data class CategoryMove(override val category: String, override val color: Color, val percent: Int, val up: Boolean) : Highlight

    /** [category] made up [percent]% of the period's spend. */
    data class TopShare(override val category: String, override val color: Color, val percent: Int) : Highlight
}

/** The trend chart's data for the selected period: one [buckets] entry per day or per month. */
data class TrendData(
    val buckets: List<TrendBucket> = emptyList(),
    val bucketing: TrendBucketing = TrendBucketing.MONTHLY,
) {
    val hasData: Boolean get() = buckets.any { it.total.signum() > 0 }
}

private val DAY_FULL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val MONTH_AXIS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val MONTH_FULL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")

data class InsightsUiState(
    // False only for the initial placeholder emitted before the first DB load; the screen gates its
    // empty state on this so the breakdown's "no spending" view doesn't flash on cold start.
    val isLoaded: Boolean = false,
    val period: InsightsPeriod = InsightsPeriod.Stepped(PeriodUnit.MONTH),
    /** Date of the earliest recorded transaction, or null when there's none; bounds the stepper's
     *  back arrow so it can't page into periods before any data exists. */
    val earliestDate: LocalDate? = null,
    val slices: List<PieSlice> = emptyList(),
    val total: BigDecimal = BigDecimal.ZERO,
    val receiptCount: Int = 0,
    val totalSaved: BigDecimal = BigDecimal.ZERO,
    val avgPerReceipt: BigDecimal = BigDecimal.ZERO,
    /** Average spend per elapsed day of the selected period (past periods use their full length). */
    val avgPerDay: BigDecimal = BigDecimal.ZERO,
    /** Projected end-of-period spend at the current pace, for the in-progress current period only;
     *  null for past/complete periods and custom ranges. */
    val projectedTotal: BigDecimal? = null,
    val topStores: List<StoreSpend> = emptyList(),
    /** The period's largest single line-item purchases (price × quantity), priciest first. */
    val biggestPurchases: List<TransactionEntity> = emptyList(),
    /** Every transaction in the selected period, newest first. The Insights screen filters this
     *  by category to populate the per-category transactions sheet. */
    val transactions: List<TransactionEntity> = emptyList(),
    /** Receipt id (a receipt's timestamp) → store name, used to label each row in the
     *  per-category sheet with the store it was bought from. */
    val storeByReceiptId: Map<Long, String> = emptyMap(),
    /** Per-day or per-month spend for the trend chart, bucketed to fit the selected period. */
    val trend: TrendData = TrendData(),
    /** The selected period vs the equal period before it; null until that previous period has spend
     *  to compare against. Follows the [period] filter (e.g. week-over-week in week mode). */
    val periodComparison: PeriodComparison? = null,
    /** Biggest per-category changes from the previous calendar month to the current one. */
    val categoryDeltas: List<CategoryDelta> = emptyList(),
    /** Up to three rule-based narrative callouts about the period (movers, new/dominant categories). */
    val highlights: List<Highlight> = emptyList(),
    /** Saved budget limits (keys from BudgetRepository: MONTHLY/WEEKLY/CAT:…), for the budget card. */
    val budgets: Map<String, BigDecimal> = emptyMap(),
    // ── Income & recurring payments (money-flow cards), scaled to the selected period ──
    /** Total income for the selected period (monthly-equivalent × period length). */
    val periodIncome: BigDecimal = BigDecimal.ZERO,
    /** Total recurring bills for the selected period. */
    val periodBills: BigDecimal = BigDecimal.ZERO,
    /** Per-source income breakdown (share of total), largest first. */
    val incomeSources: List<IncomeSourceUi> = emptyList(),
    /** Monthly/weekly bills with their next occurrence, soonest first (date-based, not period-scaled). */
    val upcomingBills: List<UpcomingBill> = emptyList(),
    /** Whether any income source exists (gates the income cards). */
    val hasIncome: Boolean = false,
    /** Whether any recurring bill exists (gates the upcoming-bills card). */
    val hasBills: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    receiptRepository: ReceiptRepository,
    budgetRepository: BudgetRepository,
    recurringRepository: RecurringRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow<InsightsPeriod>(
        // Seed from the remembered stepper unit (offset 0 = current period); offset is never persisted.
        InsightsPeriod.Stepped(
            runCatching { PeriodUnit.valueOf(settingsStore.settings.value.insightsPeriodUnit) }
                .getOrDefault(PeriodUnit.MONTH),
        ),
    )

    private val transactions = selectedPeriod.flatMapLatest { period ->
        val (start, end) = period.toRange()
        repository.getBetween(start, end)
    }

    /** Transactions in the period immediately before the selected one, feeding the period-over-period
     *  comparison cards. Re-queries with the filter, so the comparison tracks whatever's on screen. */
    private val previousPeriodTxns: Flow<List<TransactionEntity>> = selectedPeriod.flatMapLatest { period ->
        val (start, end) = period.previousPeriod().toRange()
        repository.getBetween(start, end)
    }

    val uiState: StateFlow<InsightsUiState> =
        combine(
            selectedPeriod,
            transactions,
            categoryRepository.categories,
            receiptRepository.getAll(),
            previousPeriodTxns,
        ) { period, txns, categories, receipts, prevTxns ->
            val receiptsById = receipts.associateBy { it.timestamp }
            // Adjust the headline spend to what was actually paid: add on-top tax (tax-exclusive
            // receipts) and extra charges, and subtract order discounts. The per-category slices,
            // trend and comparison below stay on the net line prices.
            val total = txns.sumOfSpend() + paidAdjustmentOf(txns, receiptsById)
            val colorByCategory = categories.associate { it.name to it.colorArgb }

            // Normalize here so the same chain (e.g. two Kaufland branches) merges into one
            // entry everywhere downstream: Top stores grouping and the per-category sheet.
            val storeByReceiptId = receipts.associate { it.timestamp to StoreNormalizer.normalize(it.store) }
            val receiptIdsInPeriod = txns.map { it.receiptId }.toSet()
            val receiptCount = receiptIdsInPeriod.size
            val totalSaved = receipts
                .filter { it.timestamp in receiptIdsInPeriod }
                .fold(BigDecimal.ZERO) { acc, r -> acc + r.discount }
            val avgPerReceipt = if (receiptCount > 0) {
                total.divide(BigDecimal(receiptCount), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            val avgPerDay = if (total.signum() > 0) {
                total.divide(BigDecimal(periodElapsedDays(period)), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            val projectedTotal = projectPeriodTotal(period, total)
            val topStores = txns
                .groupBy { storeByReceiptId[it.receiptId].orEmpty() }
                .filterKeys { it.isNotBlank() }
                .map { (store, list) -> StoreSpend(store, list.sumOfSpend()) }
                .sortedByDescending { it.amount }
                .take(5)
            val biggestPurchases = txns
                .sortedByDescending { it.price.multiply(BigDecimal(it.quantity)) }
                .take(5)

            val (periodComparison, categoryDeltas) = computePeriodComparison(txns, prevTxns, colorByCategory)
            val highlights = computeHighlights(txns, prevTxns, colorByCategory)

            InsightsUiState(
                isLoaded = true,
                period = period,
                slices = txns.toSlices(colorByCategory),
                total = total,
                receiptCount = receiptCount,
                totalSaved = totalSaved,
                avgPerReceipt = avgPerReceipt,
                avgPerDay = avgPerDay,
                projectedTotal = projectedTotal,
                topStores = topStores,
                biggestPurchases = biggestPurchases,
                transactions = txns,
                storeByReceiptId = storeByReceiptId,
                trend = computeTrend(period, txns),
                periodComparison = periodComparison,
                categoryDeltas = categoryDeltas,
                highlights = highlights,
            )
        }
            .combine(budgetRepository.budgets) { state, budgets -> state.copy(budgets = budgets) }
            .combine(recurringRepository.items) { state, recurring ->
                val ri = recurringInsights(state.period, recurring)
                state.copy(
                    periodIncome = ri.periodIncome,
                    periodBills = ri.periodBills,
                    incomeSources = ri.incomeSources,
                    upcomingBills = ri.upcomingBills,
                    hasIncome = ri.hasIncome,
                    hasBills = ri.hasBills,
                )
            }
            .combine(repository.earliestTimestamp()) { state, earliest ->
                state.copy(
                    earliestDate = earliest?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InsightsUiState(),
            )

    /**
     * Buckets the selected period's spend for the trend chart. The window is taken straight from
     * the active filter (same range the rest of the screen uses), then grouped by month when it
     * spans three or more calendar months and by day otherwise.
     */
    private fun computeTrend(period: InsightsPeriod, txns: List<TransactionEntity>): TrendData {
        val zone = ZoneId.systemDefault()
        val (startMillis, endMillis) = period.toRange()
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val monthSpan = ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(endDate)) + 1
        return if (monthSpan >= 3) {
            monthlyTrend(txns, startDate, endDate, zone)
        } else {
            dailyTrend(txns, startDate, endDate, zone)
        }
    }

    /** One bar per calendar day from [startDate] through [endDate] (clamped to today, so the
     *  current month doesn't trail off into empty future days), then padded out to at least
     *  [MIN_TREND_BARS] with the following, not-yet-elapsed days as inactive placeholders — so a
     *  just-begun month or week still fills a full row instead of showing two or three lonely bars. */
    private fun dailyTrend(
        txns: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        zone: ZoneId,
    ): TrendData {
        val byDay = txns
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .mapValues { (_, list) -> list.sumOfSpend() }
        val today = LocalDate.now()
        val lastDay = if (endDate.isAfter(today)) today else endDate
        // Elapsed days (real bars), then enough following days to reach MIN_TREND_BARS (inactive).
        val realDays = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastDay) }
            .toList()
        val padCount = (MIN_TREND_BARS - realDays.size).coerceAtLeast(0)
        val padDays = if (padCount > 0 && realDays.isNotEmpty()) {
            generateSequence(realDays.last().plusDays(1)) { it.plusDays(1) }.take(padCount).toList()
        } else {
            emptyList()
        }
        val buckets = realDays.map { day ->
            TrendBucket(
                axisLabel = day.dayOfMonth.toString(),
                fullLabel = DAY_FULL_FORMAT.format(day),
                total = byDay[day] ?: BigDecimal.ZERO,
                isCurrent = day == today,
            )
        } + padDays.map { day ->
            TrendBucket(
                axisLabel = day.dayOfMonth.toString(),
                fullLabel = DAY_FULL_FORMAT.format(day),
                total = BigDecimal.ZERO,
                isCurrent = false,
                enabled = false,
            )
        }
        return TrendData(buckets, TrendBucketing.DAILY)
    }

    /** One bar per calendar month spanned by the window, each summing only its in-range days. */
    private fun monthlyTrend(
        txns: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        zone: ZoneId,
    ): TrendData {
        val byMonth = txns
            .groupBy { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) }
            .mapValues { (_, list) -> list.sumOfSpend() }
        val currentMonth = YearMonth.now()
        // Clamp the last bar to the current month so a stepped quarter/half viewed mid-period (e.g.
        // the current quarter in its first month) doesn't trail off into empty future months.
        val lastMonth = minOf(YearMonth.from(endDate), currentMonth)
        val buckets = generateSequence(YearMonth.from(startDate)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(lastMonth) }
            .map { month ->
                TrendBucket(
                    axisLabel = MONTH_AXIS_FORMAT.format(month),
                    fullLabel = MONTH_FULL_FORMAT.format(month),
                    total = byMonth[month] ?: BigDecimal.ZERO,
                    isCurrent = month == currentMonth,
                )
            }
            .toList()
        return TrendData(buckets, TrendBucketing.MONTHLY)
    }

    /**
     * Builds the period-over-period comparison and the per-category deltas from the current period's
     * [currentTxns] and the previous period's [previousTxns] (each already windowed by its own flow).
     * The comparison is null unless the previous period had spend, so the headline percentage stays
     * meaningful. Category deltas are the biggest absolute movers (top five).
     */
    private fun computePeriodComparison(
        currentTxns: List<TransactionEntity>,
        previousTxns: List<TransactionEntity>,
        colorByCategory: Map<String, Int>,
    ): Pair<PeriodComparison?, List<CategoryDelta>> {
        val currentTotal = currentTxns.sumOfSpend()
        val previousTotal = previousTxns.sumOfSpend()

        val comparison = if (previousTotal.signum() > 0) {
            val delta = ((currentTotal.toDouble() - previousTotal.toDouble()) /
                previousTotal.toDouble() * 100).roundToInt()
            PeriodComparison(
                deltaPercent = delta,
                currentTotal = currentTotal,
                previousTotal = previousTotal,
            )
        } else {
            null
        }

        val currentByCategory = currentTxns.groupBy { it.category }.mapValues { (_, l) -> l.sumOfSpend() }
        val previousByCategory = previousTxns.groupBy { it.category }.mapValues { (_, l) -> l.sumOfSpend() }
        val categoryDeltas = (currentByCategory.keys + previousByCategory.keys)
            .map { category ->
                val change = (currentByCategory[category] ?: BigDecimal.ZERO) -
                    (previousByCategory[category] ?: BigDecimal.ZERO)
                CategoryDelta(
                    category = category,
                    color = colorByCategory[category]?.let { Color(it) }
                        ?: pieColors[(category.hashCode() and 0x7FFFFFFF) % pieColors.size],
                    delta = change,
                )
            }
            .filter { it.delta.signum() != 0 }
            .sortedByDescending { it.delta.abs() }
            .take(5)

        return comparison to categoryDeltas
    }

    /**
     * Up to three rule-based highlights for the period (no AI): a brand-new category, the biggest
     * percentage rise and the biggest fall vs the previous period, then a dominant category — each
     * category used at most once. Empty when nothing stands out, which hides the card.
     */
    private fun computeHighlights(
        currentTxns: List<TransactionEntity>,
        previousTxns: List<TransactionEntity>,
        colorByCategory: Map<String, Int>,
    ): List<Highlight> {
        val curr = currentTxns.groupBy { it.category }
            .mapValues { (_, l) -> l.sumOfSpend() }
            .filterValues { it.signum() > 0 }
        if (curr.isEmpty()) return emptyList()
        val prev = previousTxns.groupBy { it.category }.mapValues { (_, l) -> l.sumOfSpend() }
        val netTotal = curr.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
        fun colorOf(category: String): Color = colorByCategory[category]?.let { Color(it) }
            ?: pieColors[(category.hashCode() and 0x7FFFFFFF) % pieColors.size]
        fun pct(part: Double, whole: Double): Int = if (whole > 0) (part / whole * 100).roundToInt() else 0

        val used = mutableSetOf<String>()
        val out = mutableListOf<Highlight>()

        // 1. A brand-new category — spend now, none in the previous period — biggest first.
        curr.entries
            .filter { (c, _) -> (prev[c]?.signum() ?: 0) == 0 }
            .maxByOrNull { it.value }
            ?.let { (c, v) -> out += Highlight.NewCategory(c, colorOf(c), v); used += c }

        // 2. Biggest percentage increase over a category that also had spend before.
        curr.entries
            .filter { (c, v) -> c !in used && (prev[c]?.signum() ?: 0) > 0 && v > prev.getValue(c) }
            .maxByOrNull { (c, v) -> (v.toDouble() - prev.getValue(c).toDouble()) / prev.getValue(c).toDouble() }
            ?.let { (c, v) ->
                val p = pct(v.toDouble() - prev.getValue(c).toDouble(), prev.getValue(c).toDouble())
                if (p >= 5) { out += Highlight.CategoryMove(c, colorOf(c), p, up = true); used += c }
            }

        // 3. Biggest percentage decrease.
        curr.entries
            .filter { (c, v) -> c !in used && (prev[c]?.signum() ?: 0) > 0 && v < prev.getValue(c) }
            .minByOrNull { (c, v) -> (v.toDouble() - prev.getValue(c).toDouble()) / prev.getValue(c).toDouble() }
            ?.let { (c, v) ->
                val p = pct(prev.getValue(c).toDouble() - v.toDouble(), prev.getValue(c).toDouble())
                if (p >= 5) { out += Highlight.CategoryMove(c, colorOf(c), p, up = false); used += c }
            }

        // 4. A category that dominated the period (>= 40% of net spend).
        if (out.size < 3) {
            curr.entries.maxByOrNull { it.value }?.let { (c, v) ->
                val p = pct(v.toDouble(), netTotal.toDouble())
                if (c !in used && p >= 40) { out += Highlight.TopShare(c, colorOf(c), p); used += c }
            }
        }
        return out.take(3)
    }

    /** Switches to the current block of [unit] (offset 0) and remembers the unit for next launch. */
    fun onUnitSelected(unit: PeriodUnit) {
        selectedPeriod.value = InsightsPeriod.Stepped(unit)
        settingsStore.setInsightsPeriodUnit(unit.name)
    }

    /** Steps one unit further into the past; no-op unless a [InsightsPeriod.Stepped] period is active. */
    fun onStepBackward() {
        val current = selectedPeriod.value as? InsightsPeriod.Stepped ?: return
        selectedPeriod.value = current.previous()
    }

    /** Steps one unit toward the present, capped at the current period (offset 0). */
    fun onStepForward() {
        val current = selectedPeriod.value as? InsightsPeriod.Stepped ?: return
        if (current.offset < 0) {
            selectedPeriod.value = current.copy(offset = current.offset + 1)
        }
    }

    fun onCustomRangeSelected(start: LocalDate, end: LocalDate) {
        selectedPeriod.value = InsightsPeriod.Custom(start, end)
    }

    private data class RecurringInsights(
        val periodIncome: BigDecimal,
        val periodBills: BigDecimal,
        val incomeSources: List<IncomeSourceUi>,
        val upcomingBills: List<UpcomingBill>,
        val hasIncome: Boolean,
        val hasBills: Boolean,
    )

    /**
     * Derives the money-flow card data for [period] from the recurring rows: income and bills summed
     * to a monthly-equivalent (one-offs count only in their added month) then scaled to the period's
     * length, plus the per-source income split and the next-occurrence list for upcoming bills.
     */
    private fun recurringInsights(period: InsightsPeriod, recurring: List<RecurringEntity>): RecurringInsights {
        val (monthStart, monthEnd) = currentMonthRange()
        val factor = periodMonthFactor(period)
        val incomeEntities = recurring.filter { it.isIncome }
        val billEntities = recurring.filterNot { it.isIncome }

        val perSourceMonthly = incomeEntities.map { it to it.monthlyAmount(monthStart, monthEnd) }
        val monthlyIncome = perSourceMonthly.fold(BigDecimal.ZERO) { acc, (_, m) -> acc + m }
        val monthlyBills = billEntities.fold(BigDecimal.ZERO) { acc, b -> acc + b.monthlyAmount(monthStart, monthEnd) }

        val incomeSources = perSourceMonthly
            .filter { (_, m) -> m.signum() > 0 }
            .sortedByDescending { (_, m) -> m }
            .map { (entity, m) ->
                val percent = if (monthlyIncome.signum() > 0) {
                    (m.toDouble() / monthlyIncome.toDouble() * 100).roundToInt()
                } else {
                    0
                }
                IncomeSourceUi(entity, (m * factor).setScale(2, RoundingMode.HALF_UP), percent)
            }

        val today = LocalDate.now()
        val upcomingBills = billEntities
            .mapNotNull { bill -> nextOccurrenceDays(bill, today)?.let { UpcomingBill(bill, it) } }
            .sortedBy { it.daysUntil }

        return RecurringInsights(
            periodIncome = (monthlyIncome * factor).setScale(2, RoundingMode.HALF_UP),
            periodBills = (monthlyBills * factor).setScale(2, RoundingMode.HALF_UP),
            incomeSources = incomeSources,
            upcomingBills = upcomingBills,
            hasIncome = incomeEntities.isNotEmpty(),
            hasBills = billEntities.isNotEmpty(),
        )
    }

    /** How many "months" the selected period spans, for scaling monthly income/bills to it. */
    private fun periodMonthFactor(period: InsightsPeriod): BigDecimal = when (period) {
        is InsightsPeriod.Stepped -> when (period.unit) {
            PeriodUnit.WEEK -> BigDecimal(12).divide(BigDecimal(52), 6, RoundingMode.HALF_UP)
            PeriodUnit.MONTH -> BigDecimal.ONE
            PeriodUnit.QUARTER -> BigDecimal(3)
            PeriodUnit.HALF_YEAR -> BigDecimal(6)
        }
        is InsightsPeriod.Custom -> {
            val days = ChronoUnit.DAYS.between(period.start, period.end) + 1
            BigDecimal(days).divide(BigDecimal("30.4375"), 6, RoundingMode.HALF_UP)
        }
    }

    /** Days from [today] to the bill's next occurrence, or null when it has no monthly/weekly schedule
     *  (yearly bills store no month, one-offs don't recur). */
    private fun nextOccurrenceDays(bill: RecurringEntity, today: LocalDate): Int? = when (bill.cadence) {
        RecurringEntity.Cadence.MONTHLY -> {
            val day = bill.dueDay.coerceIn(1, 31)
            var date = clampDay(YearMonth.from(today), day)
            if (date.isBefore(today)) date = clampDay(YearMonth.from(today).plusMonths(1), day)
            ChronoUnit.DAYS.between(today, date).toInt()
        }
        RecurringEntity.Cadence.WEEKLY -> {
            val target = bill.dueDay.coerceIn(1, 7) // 1=Mon … 7=Sun
            var date = today
            while (date.dayOfWeek.value != target) date = date.plusDays(1)
            ChronoUnit.DAYS.between(today, date).toInt()
        }
        else -> null
    }

    private fun clampDay(month: YearMonth, day: Int): LocalDate =
        month.atDay(day.coerceAtMost(month.lengthOfMonth()))

    private fun List<TransactionEntity>.sumOfSpend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /** One slice per category, value = summed price × quantity, colored by the saved category color. */
    private fun List<TransactionEntity>.toSlices(colorByCategory: Map<String, Int>): List<PieSlice> =
        groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOfSpend() }
            .entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (category, value) ->
                PieSlice(
                    label = category,
                    value = value,
                    color = colorByCategory[category]?.let { Color(it) } ?: pieColors[index % pieColors.size],
                )
            }

    /** Days elapsed in [period] up to today (a past period uses its full length), at least 1, for
     *  daily-average figures. */
    private fun periodElapsedDays(period: InsightsPeriod, today: LocalDate = LocalDate.now()): Long {
        val zone = ZoneId.systemDefault()
        val (startMillis, endMillis) = period.toRange()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val last = if (end.isAfter(today)) today else end
        return (ChronoUnit.DAYS.between(start, last) + 1).coerceAtLeast(1)
    }

    /**
     * Projects the period's spend to its end at the current pace — total scaled by
     * full-length / elapsed-days — but only for the in-progress current period (a [Stepped] block at
     * offset 0, not yet over, with at least two elapsed days). Null for past/complete periods and
     * custom ranges, where a projection is meaningless.
     */
    private fun projectPeriodTotal(period: InsightsPeriod, total: BigDecimal): BigDecimal? {
        val stepped = period as? InsightsPeriod.Stepped ?: return null
        if (stepped.offset != 0 || total.signum() <= 0) return null
        val (start, end) = stepped.bounds()
        if (!LocalDate.now().isBefore(end)) return null
        val elapsed = periodElapsedDays(period)
        val totalDays = ChronoUnit.DAYS.between(start, end) + 1
        if (elapsed < 2 || elapsed >= totalDays) return null
        return total.multiply(BigDecimal(totalDays)).divide(BigDecimal(elapsed), 2, RoundingMode.HALF_UP)
    }
}
