package com.budgetty.app.ui.recap

import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.data.repository.SubscriptionsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.CountableItem
import com.budgetty.app.ui.util.MerchantCharge
import com.budgetty.app.ui.util.PayCycle
import com.budgetty.app.ui.util.SavingsMath
import com.budgetty.app.ui.streaks.BudgetStreakInput
import com.budgetty.app.ui.streaks.LiveBudgetPeriod
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.streaks.StreakTxn
import com.budgetty.app.ui.util.SubscriptionDetector
import com.budgetty.app.ui.util.windowAmount
import com.budgetty.app.ui.wellbeing.GoalPace
import com.budgetty.app.ui.wellbeing.WellbeingEngine
import com.budgetty.app.ui.wellbeing.WellbeingInputs
import com.budgetty.app.ui.wellbeing.WellbeingScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Builds a [RecapStory] for the just-closed period, reusing the app's existing pure engines rather
 * than introducing a new content model: [PayCycle] windows, [paidAdjustmentOf] spend, [WellbeingEngine]
 * for the 0–100 monthly score/band, [BuyingLimitCounter] for the limits outcome, [SavingsMath] +
 * [SubscriptionDetector] for the score's savings/subscription components. Everything is derived from
 * the user's own local receipts/budgets/goals — nothing leaves the device.
 *
 * The score-input derivation intentionally MIRRORS
 * [com.budgetty.app.ui.wellbeing.WellbeingProvider]'s `inputsFor`, computed for the past month offset,
 * so the recap's report-card score is the same number the wellbeing engine would give that month.
 * Keep the two in step. Registered as a Koin `single` and injected into [RecapViewModel].
 */
@Suppress("LongParameterList") // Mirrors WellbeingProvider: one repository per data source, by design.
class RecapProvider(
    private val transactions: TransactionRepository,
    private val receipts: ReceiptRepository,
    private val budgets: BudgetRepository,
    private val recurring: RecurringRepository,
    private val savings: SavingsRepository,
    private val subscriptions: SubscriptionsRepository,
    private val categories: CategoryRepository,
    private val buyingLimits: BuyingLimitsRepository,
    private val settings: SettingsStore,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val firstDayOfWeek: () -> DayOfWeek = { BuyingLimitCounter.localeFirstDayOfWeek() },
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    private data class Core(
        val txns: List<TransactionEntity>,
        val receipts: List<ReceiptEntity>,
        val budgets: Map<String, BigDecimal>,
        val recurring: List<RecurringEntity>,
        val monthStartDay: Int,
    )

    private data class Extra(
        val goals: List<SavingsGoalEntity>,
        val contributions: List<SavingsContributionEntity>,
        val ignoredMerchants: Set<String>,
        val categoryColors: Map<String, Int>,
        val limits: List<BuyingLimitEntity>,
    )

    private class Loaded(core: Core, extra: Extra) {
        val txns = core.txns
        val receipts = core.receipts
        val budgets = core.budgets
        val recurring = core.recurring
        val monthStartDay = core.monthStartDay
        val goals = extra.goals
        val contributions = extra.contributions
        val ignoredMerchants = extra.ignoredMerchants
        val categoryColors = extra.categoryColors
        val limits = extra.limits
        val receiptsById: Map<Long, ReceiptEntity> = core.receipts.associateBy { it.timestamp }
    }

    /**
     * A built recap for the [kind] period at [offset] (−1 = the just-closed period), or null when the
     * data guard says there's nothing worth showing. Only the collected story runs (WhileSubscribed).
     */
    fun story(kind: RecapKind, offset: Int = -1): Flow<RecapStory?> {
        val core = combine(
            transactions.getAll(), receipts.getAll(), budgets.budgets, recurring.items, settings.settings,
        ) { txns, rcpts, budg, rec, s -> Core(txns, rcpts, budg, rec, s.monthStartDay) }
        val extra = combine(
            savings.goals, savings.allContributions, subscriptions.ignored, categories.categories, buyingLimits.limits,
        ) { goals, contribs, ignored, cats, limits ->
            Extra(
                goals = goals,
                contributions = contribs,
                ignoredMerchants = ignored.map { it.merchant }.toSet(),
                categoryColors = cats.associate { it.name to it.colorArgb },
                limits = limits,
            )
        }
        return combine(core, extra) { c, e ->
            val loaded = Loaded(c, e)
            when (kind) {
                RecapKind.MONTHLY -> buildMonthly(loaded, offset)
                RecapKind.WEEKLY -> buildWeekly(loaded, offset)
            }
        }.flowOn(Dispatchers.Default)
    }

    // ── Window + spend helpers ────────────────────────────────────────────────────

    private fun monthWindow(now: LocalDate, monthStartDay: Int, offset: Int): LongRange {
        val (start, end) = PayCycle.month(now, monthStartDay, offset)
        return dayRangeMillis(start, end)
    }

    private fun weekWindow(now: LocalDate, offset: Int): LongRange {
        val start = now.with(TemporalAdjusters.previousOrSame(firstDayOfWeek())).plusWeeks(offset.toLong())
        return dayRangeMillis(start, start.plusDays(6))
    }

    /** Inclusive epoch-millis range covering [startDate]'s start of day through [endDate]'s end of day. */
    private fun dayRangeMillis(startDate: LocalDate, endDate: LocalDate): LongRange {
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start..end
    }

    private fun txnsIn(loaded: Loaded, w: LongRange) = loaded.txns.filter { it.timestamp in w }

    /** Receipts logged within a period window — the period-scoped count for the weekly data floor (§1.2). */
    private fun receiptsInWindow(loaded: Loaded, w: LongRange): Int = loaded.receipts.count { it.timestamp in w }

    private fun netSpend(list: List<TransactionEntity>): BigDecimal =
        list.fold(BigDecimal.ZERO) { a, t -> a + t.price.multiply(BigDecimal(t.quantity)) }

    private fun paidSpend(loaded: Loaded, list: List<TransactionEntity>): BigDecimal =
        netSpend(list) + paidAdjustmentOf(list, loaded.receiptsById)

    private fun monthSpend(loaded: Loaded, now: LocalDate, offset: Int): BigDecimal =
        paidSpend(loaded, txnsIn(loaded, monthWindow(now, loaded.monthStartDay, offset)))

    private fun deltaPercent(current: BigDecimal, previous: BigDecimal): Int? =
        if (previous.signum() > 0) ((current.toDouble() / previous.toDouble() - 1.0) * 100).roundToInt() else null

    // ── Monthly ─────────────────────────────────────────────────────────────────

    @Suppress("ReturnCount")
    private fun buildMonthly(loaded: Loaded, offset: Int): RecapStory? {
        val now = today()
        val (monthStart, monthEndDate) = PayCycle.month(now, loaded.monthStartDay, offset)
        val monthYear = YearMonth.from(monthStart)
        val window = monthWindow(now, loaded.monthStartDay, offset)
        val prevWindow = monthWindow(now, loaded.monthStartDay, offset - 1)
        val monthTxns = txnsIn(loaded, window)
        val prevTxns = txnsIn(loaded, prevWindow)
        val spend = paidSpend(loaded, monthTxns)
        val prevSpend = paidSpend(loaded, prevTxns)

        val guard = RecapDataGuard.evaluate(
            kind = RecapKind.MONTHLY,
            totalReceipts = loaded.receipts.size,
            periodReceipts = receiptsInWindow(loaded, window),
            periodHasSpend = spend.signum() > 0,
            priorPeriodHasSpend = prevSpend.signum() > 0,
        )
        if (guard is RecapGuard.Skip) return null
        val withComparison = (guard as RecapGuard.Show).withComparison

        val delta = if (withComparison) deltaPercent(spend, prevSpend) else null
        val score = scoreForMonth(loaded, now, offset)
        val prevScore = if (withComparison) scoreForMonth(loaded, now, offset - 1).score else null
        val scoreDelta = if (score.score != null && prevScore != null) score.score!! - prevScore else null
        val movers = if (withComparison) topMovers(loaded, monthTxns, prevTxns) else emptyList()
        val budget = budgetOutcome(loaded, monthTxns, spend)

        val tough = isTough(delta, scoreDelta)
        val nextMonth = YearMonth.from(PayCycle.month(now, loaded.monthStartDay, offset + 1).first)
        val cards = if (tough) {
            toughMonthlyCards(spend, delta, monthYear, prevSpend, movers, budget)
        } else {
            goodMonthlyCards(
                loaded, now, offset, spend, delta, prevSpend, monthYear,
                score, scoreDelta, movers, budget, window,
            )
        }
        return RecapStory(
            kind = RecapKind.MONTHLY, monthYear = monthYear, nextMonth = nextMonth,
            weekStart = monthStart, weekEnd = monthEndDate, cards = cards,
        )
    }

    /** A month reads "tougher" when spend rose meaningfully OR the score fell — never framed as red. */
    private fun isTough(deltaPercent: Int?, scoreDelta: Int?): Boolean =
        (deltaPercent != null && deltaPercent >= TOUGH_SPEND_UP) ||
            (scoreDelta != null && scoreDelta <= TOUGH_SCORE_DROP)

    @Suppress("LongParameterList", "LongMethod")
    private fun goodMonthlyCards(
        loaded: Loaded,
        now: LocalDate,
        offset: Int,
        spend: BigDecimal,
        delta: Int?,
        prevSpend: BigDecimal,
        monthYear: YearMonth,
        score: WellbeingScore,
        scoreDelta: Int?,
        movers: List<Mover>,
        budget: BudgetOutcome,
        window: LongRange,
    ): List<RecapCard> {
        val improved = delta == null || delta <= FLAT_TOLERANCE
        val cards = mutableListOf<RecapCard>()
        cards += RecapCard.Cover(RecapBand.PRIMARY)
        cards += RecapCard.Total(
            band = if (improved) RecapBand.GOOD else RecapBand.WARN,
            spent = spend, deltaPercent = delta,
            prevMonth = if (delta != null) monthYear.minusMonths(1) else null,
            prevTotal = if (delta != null) prevSpend else null, improved = improved,
        )
        if (score.score != null && score.band != null) {
            cards += RecapCard.Score(
                band = RecapBand.SECONDARY, score = score.score!!, scoreBand = score.band!!,
                delta = scoreDelta, prevMonth = if (scoreDelta != null) monthYear.minusMonths(1) else null,
            )
        }
        movers.firstOrNull()?.let { primary ->
            cards += RecapCard.Mover(
                band = RecapBand.NEUTRAL, category = primary.category, dotColorArgb = primary.colorArgb,
                delta = primary.delta, previousAmount = primary.previous, currentAmount = primary.current,
                second = movers.getOrNull(1)?.let { RecapSecondMover(it.category, it.delta) },
            )
        }
        if (budget.hasBudget) {
            val streak = monthStreak(loaded, now, offset)
            cards += RecapCard.BudgetStreak(
                band = RecapBand.GREAT, streakMonths = streak.current, best = streak.best,
                liveOnTrack = streak.liveOnTrack,
                underCount = budget.underCount, scopeCount = budget.scopeCount,
                segments = budget.segments, safeToSpend = budget.safeToSpend,
            )
        }
        if (loaded.limits.isNotEmpty()) {
            val outcome = limitOutcomes(loaded, window)
            cards += RecapCard.Limits(
                band = RecapBand.WARN, underCount = outcome.count { it.under },
                totalCount = outcome.size, chips = outcome,
            )
        }
        cards += RecapCard.Focus(RecapBand.PRIMARY, deriveMonthlyFocus(budget, movers), isWeekly = false)
        return cards
    }

    @Suppress("LongParameterList")
    private fun toughMonthlyCards(
        spend: BigDecimal,
        delta: Int?,
        monthYear: YearMonth,
        prevSpend: BigDecimal,
        movers: List<Mover>,
        budget: BudgetOutcome,
    ): List<RecapCard> {
        val cards = mutableListOf<RecapCard>()
        cards += RecapCard.Total(
            band = RecapBand.WARN, spent = spend, deltaPercent = delta,
            prevMonth = if (delta != null) monthYear.minusMonths(1) else null,
            prevTotal = if (delta != null) prevSpend else null, improved = false,
        )
        val rise = movers.firstOrNull { it.delta.signum() > 0 } ?: movers.firstOrNull()
        rise?.let {
            cards += RecapCard.Mover(
                band = RecapBand.NEUTRAL, category = it.category, dotColorArgb = it.colorArgb,
                delta = it.delta, previousAmount = it.previous, currentAmount = it.current, second = null,
            )
        }
        cards += RecapCard.Focus(RecapBand.PRIMARY, deriveToughFocus(rise, budget), isWeekly = false)
        return cards
    }

    // ── Weekly ──────────────────────────────────────────────────────────────────

    @Suppress("ReturnCount")
    private fun buildWeekly(loaded: Loaded, offset: Int): RecapStory? {
        val now = today()
        val window = weekWindow(now, offset)
        val prevWindow = weekWindow(now, offset - 1)
        val weekStart = Instant.ofEpochMilli(window.first).atZone(zone).toLocalDate()
        val weekEnd = weekStart.plusDays(6)
        val weekTxns = txnsIn(loaded, window)
        val spend = paidSpend(loaded, weekTxns)
        val prevSpend = paidSpend(loaded, txnsIn(loaded, prevWindow))

        val guard = RecapDataGuard.evaluate(
            kind = RecapKind.WEEKLY,
            totalReceipts = loaded.receipts.size,
            periodReceipts = receiptsInWindow(loaded, window),
            periodHasSpend = spend.signum() > 0,
            priorPeriodHasSpend = prevSpend.signum() > 0,
        )
        if (guard is RecapGuard.Skip) return null
        val withComparison = (guard as RecapGuard.Show).withComparison

        val weeklyBudget = loaded.budgets[BudgetRepository.WEEKLY]
        val fractionUsed = if (weeklyBudget != null && weeklyBudget.signum() > 0) {
            (spend.toDouble() / weeklyBudget.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
        val remaining = if (weeklyBudget != null) (weeklyBudget - spend).max(BigDecimal.ZERO) else BigDecimal.ZERO
        val delta = if (withComparison) deltaPercent(spend, prevSpend) else null
        val onTrack = weeklyBudget == null || fractionUsed <= 1f
        val movers = if (withComparison) topMovers(loaded, weekTxns, txnsIn(loaded, prevWindow)) else emptyList()

        // §1.3: a fuller weekly story — Cover → Pace → Limits (if any) → Streak (if any) → Focus.
        // Both the Limits and Streak cards drop out entirely when there's nothing to show, so a bare
        // week stays Cover → Pace → Focus (3 cards) and never pads.
        val cards = buildList {
            add(RecapCard.Cover(RecapBand.PRIMARY))
            add(
                RecapCard.Pace(
                    band = if (onTrack) RecapBand.GOOD else RecapBand.WARN, spent = spend,
                    weeklyBudget = weeklyBudget, fractionUsed = fractionUsed, paceFraction = 1f,
                    remaining = remaining, deltaPercent = delta,
                ),
            )
            if (loaded.limits.isNotEmpty()) {
                val outcome = limitOutcomes(loaded, window, weekly = true)
                add(
                    RecapCard.Limits(
                        band = RecapBand.WARN, underCount = outcome.count { it.under },
                        totalCount = outcome.size, chips = outcome,
                    ),
                )
            }
            weekStreak(loaded, now, offset)?.let { add(streakCard(it)) }
            add(RecapCard.Focus(RecapBand.PRIMARY, deriveWeeklyFocus(movers), isWeekly = true))
        }
        return RecapStory(
            kind = RecapKind.WEEKLY, monthYear = YearMonth.from(weekEnd),
            nextMonth = YearMonth.from(weekEnd), weekStart = weekStart, weekEnd = weekEnd, cards = cards,
        )
    }

    // ── Movers ──────────────────────────────────────────────────────────────────

    private data class Mover(
        val category: String,
        val colorArgb: Int,
        val delta: BigDecimal,
        val previous: BigDecimal,
        val current: BigDecimal,
    )

    private fun topMovers(
        loaded: Loaded,
        current: List<TransactionEntity>,
        previous: List<TransactionEntity>,
    ): List<Mover> {
        val currByCat = current.groupBy { it.category }.mapValues { netSpend(it.value) }
        val prevByCat = previous.groupBy { it.category }.mapValues { netSpend(it.value) }
        return (currByCat.keys + prevByCat.keys).map { cat ->
            val cur = currByCat[cat] ?: BigDecimal.ZERO
            val prev = prevByCat[cat] ?: BigDecimal.ZERO
            Mover(cat, catColor(loaded, cat), cur - prev, prev, cur)
        }.filter { it.delta.signum() != 0 }.sortedByDescending { it.delta.abs() }.take(2)
    }

    private fun catColor(loaded: Loaded, category: String): Int =
        loaded.categoryColors[category] ?: Categories.colorOf(category)

    // ── Budget outcome + streak ───────────────────────────────────────────────────

    private data class BudgetOutcome(
        val hasBudget: Boolean,
        val underCount: Int,
        val scopeCount: Int,
        val segments: List<RecapSegStatus>,
        val safeToSpend: BigDecimal,
        val overCategory: String?,
        val overBudget: BigDecimal?,
    )

    /** Each budgeted scope as (label, spend, budget): per-category budgets if any, else the monthly one. */
    private fun budgetScopes(
        loaded: Loaded,
        monthTxns: List<TransactionEntity>,
        spend: BigDecimal,
    ): List<Triple<String, BigDecimal, BigDecimal>> {
        val catBudgets = categoryBudgetsOf(loaded)
        val monthlyBudget = loaded.budgets[BudgetRepository.MONTHLY]
        val catSpend = monthTxns.groupBy { it.category }.mapValues { netSpend(it.value) }
        return when {
            catBudgets.isNotEmpty() ->
                catBudgets.map { (cat, bud) -> Triple(cat, catSpend[cat] ?: BigDecimal.ZERO, bud) }
            monthlyBudget != null -> listOf(Triple(BudgetRepository.MONTHLY, spend, monthlyBudget))
            else -> emptyList()
        }
    }

    private fun budgetOutcome(loaded: Loaded, monthTxns: List<TransactionEntity>, spend: BigDecimal): BudgetOutcome {
        val scopes = budgetScopes(loaded, monthTxns, spend)
        if (scopes.isEmpty()) {
            return BudgetOutcome(false, 0, 0, emptyList(), BigDecimal.ZERO, null, null)
        }
        val segments = scopes.map { (_, sp, bud) -> segStatus(sp, bud) }
        val over = scopes.filter { (_, sp, bud) -> sp > bud }
        val worstOver = over.maxByOrNull { (_, sp, bud) -> (sp - bud).toDouble() }
        val budgetedTotal = scopes.fold(BigDecimal.ZERO) { a, s -> a + s.third }
        return BudgetOutcome(
            hasBudget = true,
            underCount = scopes.count { (_, sp, bud) -> sp <= bud },
            scopeCount = scopes.size,
            segments = segments,
            safeToSpend = (budgetedTotal - spend),
            overCategory = worstOver?.first,
            overBudget = worstOver?.third,
        )
    }

    private fun segStatus(spend: BigDecimal, budget: BigDecimal): RecapSegStatus = when {
        budget.signum() <= 0 -> RecapSegStatus.WARN
        spend > budget -> RecapSegStatus.BAD
        spend.toDouble() >= budget.toDouble() * WARN_FRACTION -> RecapSegStatus.WARN
        else -> RecapSegStatus.GOOD
    }

    private data class PeriodTagged(
        val closed: List<StreakTxn>,
        val adjustmentByPeriod: Map<Int, BigDecimal>,
        val live: LiveBudgetPeriod,
    )

    /**
     * Tags every transaction with the streak period index its date falls in, via [periodIndexOf]:
     * 0 = the just-closed period, positive into the past, and [LIVE_INDEX] (−1) = the current OPEN
     * period. Closed periods (0 until [StreakEngine.MAX_STREAK]) become [StreakTxn]s carrying their
     * per-period whole-budget paid adjustment; the open period becomes the [LiveBudgetPeriod] that only
     * ever feeds [Streak.liveOnTrack] and is never counted in [Streak.current] (§2.3). Grouping happens
     * exactly once so both month and week streaks are one-pass (§2.8).
     */
    private fun tagByPeriod(loaded: Loaded, periodIndexOf: (LocalDate) -> Int): PeriodTagged {
        val byIndex = loaded.txns
            .mapNotNull { t ->
                val date = Instant.ofEpochMilli(t.timestamp).atZone(zone).toLocalDate()
                val idx = periodIndexOf(date)
                if (idx == LIVE_INDEX || idx in 0 until StreakEngine.MAX_STREAK) idx to t else null
            }
            .groupBy({ it.first }, { it.second })
        val closedGroups = byIndex.filterKeys { it >= 0 }
        val closed = closedGroups.flatMap { (idx, list) ->
            list.map { StreakTxn(idx, it.category, it.price.multiply(BigDecimal(it.quantity))) }
        }
        val adjustment = closedGroups.mapValues { (_, list) -> paidAdjustmentOf(list, loaded.receiptsById) }
        val liveList = byIndex[LIVE_INDEX].orEmpty()
        val live = LiveBudgetPeriod(
            transactions = liveList.map { StreakTxn(0, it.category, it.price.multiply(BigDecimal(it.quantity))) },
            monthlyAdjustment = paidAdjustmentOf(liveList, loaded.receiptsById),
        )
        return PeriodTagged(closed, adjustment, live)
    }

    private fun categoryBudgetsOf(loaded: Loaded): Map<String, BigDecimal> =
        loaded.budgets.filterKeys { it.startsWith(BudgetRepository.CATEGORY_PREFIX) }
            .mapKeys { it.key.removePrefix(BudgetRepository.CATEGORY_PREFIX) }

    /**
     * The all-scopes month streak (§2.1): consecutive CLOSED pay-cycle months where EVERY budgeted
     * scope stayed under, ending with the month at [endOffset], plus its personal best and whether the
     * current OPEN month is on track. Re-sourced from [StreakEngine.allScopesStreak] so there is a
     * single streak implementation. Feeds the de-flamed monthly [RecapCard.BudgetStreak] (§2.4/§2.6).
     */
    private fun monthStreak(loaded: Loaded, now: LocalDate, endOffset: Int): Streak {
        val endCycle = YearMonth.from(PayCycle.month(now, loaded.monthStartDay, endOffset).first)
        val tagged = tagByPeriod(loaded) { date ->
            val txnCycle = YearMonth.from(PayCycle.month(date, loaded.monthStartDay).first)
            ChronoUnit.MONTHS.between(txnCycle, endCycle).toInt()
        }
        return StreakEngine.allScopesStreak(
            BudgetStreakInput(
                transactions = tagged.closed,
                categoryBudgets = categoryBudgetsOf(loaded),
                monthlyBudget = loaded.budgets[BudgetRepository.MONTHLY],
                kind = StreakKind.BUDGET_MONTH,
                monthlyLabel = BudgetRepository.MONTHLY,
                monthlyAdjustmentByPeriod = tagged.adjustmentByPeriod,
                live = tagged.live,
            ),
        )
    }

    /**
     * The best per-scope WEEK streak to surface on the weekly Streak card (§1.3), or null when there's
     * nothing worth showing. Per-category when any category budget is set — each monthly category budget
     * sliced to a week via [weeklyShareOf] — else the single whole-budget scope (an explicit WEEKLY
     * budget as-is, or the monthly budget sliced). [pickWeekStreak] chooses one scope: a live
     * current run first, else a best-run fallback, both gated at [StreakEngine.MIN_TO_SURFACE].
     */
    private fun weekStreak(loaded: Loaded, now: LocalDate, endOffset: Int): Streak? {
        val catBudgets = categoryBudgetsOf(loaded)
        val weeklyBudget = loaded.budgets[BudgetRepository.WEEKLY]
        val monthlyBudget = loaded.budgets[BudgetRepository.MONTHLY]
        val categoryBudgets: Map<String, BigDecimal>
        val wholeBudget: BigDecimal?
        when {
            catBudgets.isNotEmpty() -> {
                categoryBudgets = catBudgets.mapValues { weeklyShareOf(it.value) }
                wholeBudget = null
            }
            weeklyBudget != null -> {
                categoryBudgets = emptyMap()
                wholeBudget = weeklyBudget
            }
            monthlyBudget != null -> {
                categoryBudgets = emptyMap()
                wholeBudget = weeklyShareOf(monthlyBudget)
            }
            else -> return null
        }
        val fdow = firstDayOfWeek()
        val endWeekStart = now.with(TemporalAdjusters.previousOrSame(fdow)).plusWeeks(endOffset.toLong())
        val tagged = tagByPeriod(loaded) { date ->
            ChronoUnit.WEEKS.between(date.with(TemporalAdjusters.previousOrSame(fdow)), endWeekStart).toInt()
        }
        return pickWeekStreak(
            StreakEngine.budgetStreaks(
                BudgetStreakInput(
                    transactions = tagged.closed,
                    categoryBudgets = categoryBudgets,
                    monthlyBudget = wholeBudget,
                    kind = StreakKind.BUDGET_WEEK,
                    monthlyLabel = "",
                    monthlyAdjustmentByPeriod = tagged.adjustmentByPeriod,
                    live = tagged.live,
                ),
            ),
        )
    }

    /** Maps a chosen [Streak] to the calm secondary-band [RecapCard.Streak] (best-run when current < 2). */
    private fun streakCard(streak: Streak): RecapCard.Streak = RecapCard.Streak(
        band = RecapBand.SECONDARY,
        kind = streak.kind,
        scope = streak.label.ifBlank { null },
        current = streak.current,
        best = streak.best,
        liveOnTrack = streak.liveOnTrack,
        isBestRun = streak.current < StreakEngine.MIN_TO_SURFACE,
    )

    // ── Buying-limits outcome ──────────────────────────────────────────────────────

    /**
     * The limit chips for a recap window. In a [weekly] story the window IS a single week, so every
     * limit is simply counted within it against its cap (§1.3 — weekly limits pair naturally with the
     * weekly recap). In the monthly story a monthly cap counts over the whole month, while a weekly cap
     * asks "did you keep every week under it" via the worst week's count.
     */
    private fun limitOutcomes(loaded: Loaded, window: LongRange, weekly: Boolean = false): List<RecapLimitChip> {
        val items = loaded.txns.map { CountableItem(it.name, it.quantity, it.timestamp) }
        return loaded.limits.map { limit ->
            val bought = when {
                weekly || limit.timeframe == BuyingLimitTimeframe.MONTHLY ->
                    BuyingLimitCounter.countInWindow(items, limit.keywordList, window.first, window.last)
                // A weekly cap over a month is "did you keep every week under it": the worst week's count.
                else -> worstWeekCount(items, limit.keywordList, window)
            }
            RecapLimitChip(
                emoji = limit.emoji.ifBlank { "🏷️" },
                label = limit.displayTitle,
                bought = bought,
                cap = limit.count,
            )
        }
    }

    private fun worstWeekCount(items: List<CountableItem>, keywords: List<String>, monthWindow: LongRange): Int {
        val monthStart = Instant.ofEpochMilli(monthWindow.first).atZone(zone).toLocalDate()
        val monthEnd = Instant.ofEpochMilli(monthWindow.last).atZone(zone).toLocalDate()
        var weekStart = monthStart.with(TemporalAdjusters.previousOrSame(firstDayOfWeek()))
        var worst = 0
        while (!weekStart.isAfter(monthEnd)) {
            val wStart = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val wEnd = weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            worst = maxOf(worst, BuyingLimitCounter.countInWindow(items, keywords, wStart, wEnd))
            weekStart = weekStart.plusWeeks(1)
        }
        return worst
    }

    // ── Focus derivation ───────────────────────────────────────────────────────────

    /** The over-budget category as a cap-to-try, when one exists. */
    private fun overBudgetCap(budget: BudgetOutcome): RecapFocus.CapCategory? {
        val cat = budget.overCategory ?: return null
        return RecapFocus.CapCategory(cat, roundToNice(budget.overBudget ?: BigDecimal.ZERO))
    }

    private fun deriveMonthlyFocus(budget: BudgetOutcome, movers: List<Mover>): RecapFocus {
        overBudgetCap(budget)?.let { return it }
        movers.firstOrNull { it.delta.signum() > 0 }?.let { return RecapFocus.WatchCategory(it.category) }
        return RecapFocus.KeepItUp
    }

    private fun deriveToughFocus(rise: Mover?, budget: BudgetOutcome): RecapFocus {
        overBudgetCap(budget)?.let { return it }
        rise?.let { return RecapFocus.CapCategory(it.category, roundToNice(it.previous)) }
        return RecapFocus.KeepItUp
    }

    private fun deriveWeeklyFocus(movers: List<Mover>): RecapFocus {
        movers.firstOrNull { it.delta.signum() > 0 }?.let { return RecapFocus.WatchCategory(it.category) }
        return RecapFocus.KeepItUp
    }

    /** Rounds a suggested cap to the nearest 10 so it reads as an intentional target, not a raw figure. */
    private fun roundToNice(value: BigDecimal): BigDecimal {
        if (value.signum() <= 0) return value
        val step = BigDecimal(NICE_STEP)
        return value.divide(step, 0, RoundingMode.HALF_UP).multiply(step).max(step)
    }

    // ── Wellbeing score for a given month (mirrors WellbeingProvider.inputsFor) ──────

    private fun scoreForMonth(loaded: Loaded, now: LocalDate, offset: Int): WellbeingScore =
        WellbeingEngine.score(wellbeingInputs(loaded, now, offset))

    @Suppress("LongMethod")
    private fun wellbeingInputs(loaded: Loaded, now: LocalDate, offset: Int): WellbeingInputs {
        val monthsTracked = loaded.txns
            .map { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) }.distinct().size

        val charges = buildCharges(loaded)
        val detected = SubscriptionDetector.detect(charges, now).filterNot { it.merchant in loaded.ignoredMerchants }
        val subsMonthly = detected.fold(BigDecimal.ZERO) { a, s -> a + s.monthlyEquivalent }
        val subsCount = detected.size
        val hasSubsData = loaded.receipts.size >= WellbeingEngine.MIN_RECEIPTS_TO_SCORE && monthsTracked >= 1

        fun avgBefore(off: Int): BigDecimal? {
            val spends = (1..TRAILING_MONTHS).map { monthSpend(loaded, now, off - it) }.filter { it.signum() > 0 }
            if (spends.isEmpty()) return null
            return spends.fold(BigDecimal.ZERO) { a, b -> a + b }
                .divide(BigDecimal(spends.size), 2, RoundingMode.HALF_UP)
        }

        val window = monthWindow(now, loaded.monthStartDay, offset)
        val monthTxns = txnsIn(loaded, window)
        val spend = paidSpend(loaded, monthTxns)
        val income = loaded.recurring.filter { it.isIncome }
            .fold(BigDecimal.ZERO) { a, r -> a + r.windowAmount(window.first, window.last) }
        val bills = loaded.recurring.filterNot { it.isIncome }
            .fold(BigDecimal.ZERO) { a, r -> a + r.windowAmount(window.first, window.last) }
        val hasIncome = income.signum() > 0
        val saved = income - bills - spend
        val rate = if (hasIncome) (saved.toDouble() / income.toDouble() * 100).roundToInt() else 0

        val scopes = budgetScopes(loaded, monthTxns, spend)
        val overScopes = scopes.filter { it.second > it.third }
        val baseline = avgBefore(0) ?: spend
        val subsShare = when {
            !hasSubsData -> null
            subsCount == 0 -> 0
            baseline.signum() > 0 -> (subsMonthly.toDouble() / baseline.toDouble() * 100).roundToInt()
            else -> null
        }
        val trendPercent = avgBefore(offset)?.takeIf { it.signum() > 0 }
            ?.let { ((spend.toDouble() / it.toDouble() - 1.0) * 100).roundToInt() }
        val goalPaces = loaded.goals.map { g ->
            val prog = SavingsMath.progress(g, loaded.contributions.filter { it.goalId == g.id }, now, zone)
            GoalPace(g.name, prog.reached, prog.behind)
        }

        return WellbeingInputs(
            hasIncome = hasIncome, savingsRatePercent = rate, income = income, saved = saved, netCashflow = saved,
            hasAnyBudget = scopes.isNotEmpty(), budgetedCount = scopes.size, overCount = overScopes.size,
            overspendTotal = overScopes.fold(BigDecimal.ZERO) { a, s -> a + (s.second - s.third) },
            budgetedTotal = scopes.fold(BigDecimal.ZERO) { a, s -> a + s.third },
            trendPercent = trendPercent, subsSharePercent = subsShare, subsMonthly = subsMonthly, subsCount = subsCount,
            goals = goalPaces, categories = emptyList(), spend = spend,
            receiptsLogged = loaded.receipts.size, monthsTracked = monthsTracked,
        )
    }

    /** One [MerchantCharge] per receipt (normalized store, paid total) — mirrors WellbeingProvider. */
    private fun buildCharges(loaded: Loaded): List<MerchantCharge> =
        loaded.txns.groupBy { it.receiptId }.mapNotNull { (receiptId, group) ->
            val store = StoreNormalizer.normalize(loaded.receiptsById[receiptId]?.store.orEmpty())
            if (store.isBlank()) return@mapNotNull null
            val total = netSpend(group) + paidAdjustmentOf(group, loaded.receiptsById)
            if (total.signum() <= 0) return@mapNotNull null
            val category = group.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key
                ?: group.first().category
            MerchantCharge(
                merchant = store, emoji = Categories.emojiOf(category), amount = total,
                dateMillis = group.firstOrNull()?.timestamp ?: receiptId,
            )
        }

    private companion object {
        const val TOUGH_SPEND_UP = 8
        const val TOUGH_SCORE_DROP = -5
        const val FLAT_TOLERANCE = 2
        const val WARN_FRACTION = 0.9
        const val TRAILING_MONTHS = 6
        const val NICE_STEP = 10

        /** Period index of the current OPEN period in [tagByPeriod] — feeds liveOnTrack only, never counted. */
        const val LIVE_INDEX = -1
    }
}

private const val MONTHS_PER_YEAR = 12
private const val WEEKS_PER_YEAR = 52
private const val WEEK_SHARE_SCALE = 2

/**
 * A monthly budget amount sliced to one week for the weekly streak comparison (§1.3). Category and
 * whole-budget amounts in Budgetty are monthly; a month is `52 ⁄ 12` weeks, so a week's allowance is
 * `monthly × 12 ⁄ 52`. An explicitly-set WEEKLY budget is already per-week and is used as-is (never
 * passed here). Kept a pure top-level function so it is JVM-unit-testable and ports 1:1 to iOS.
 */
internal fun weeklyShareOf(monthly: BigDecimal): BigDecimal =
    monthly.multiply(BigDecimal(MONTHS_PER_YEAR))
        .divide(BigDecimal(WEEKS_PER_YEAR), WEEK_SHARE_SCALE, RoundingMode.HALF_UP)

/**
 * Picks the single scope for the weekly Streak card (§1.3): the strongest live current run (highest
 * [Streak.current], then [Streak.best]) among those clearing the [StreakEngine.MIN_TO_SURFACE] ≥2 floor;
 * failing that, the strongest best-run fallback (highest [Streak.best] ≥ 2). Ties break on label for
 * determinism. Null when nothing qualifies, so the card drops out. Pure + top-level for JVM testing.
 */
internal fun pickWeekStreak(streaks: List<Streak>): Streak? {
    StreakEngine.surfaced(streaks)
        .maxWithOrNull(compareBy({ it.current }, { it.best }, { it.label }))
        ?.let { return it }
    return streaks
        .filter { it.best >= StreakEngine.MIN_TO_SURFACE }
        .maxWithOrNull(compareBy({ it.best }, { it.label }))
}
