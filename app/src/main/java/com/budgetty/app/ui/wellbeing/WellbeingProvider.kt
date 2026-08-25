package com.budgetty.app.ui.wellbeing

import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.WellbeingScoreEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.data.repository.SubscriptionsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.repository.WellbeingScoreRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.streaks.BudgetStreakInput
import com.budgetty.app.ui.streaks.LiveBudgetPeriod
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.streaks.StreakTxn
import com.budgetty.app.ui.util.MerchantCharge
import com.budgetty.app.ui.util.PayCycle
import com.budgetty.app.ui.util.SavingsMath
import com.budgetty.app.ui.util.SubscriptionDetector
import com.budgetty.app.ui.util.windowAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
 * Combines the repositories into a single [WellbeingSummary] the score screen, the Home banner and
 * the Insights row all read. Pure derivation on top of [WellbeingEngine]; every figure comes from the
 * user's own local receipts, budgets, goals and recurring plan — nothing leaves the device. Registered
 * as a Koin `single` and injected into the three ViewModels that surface Wellbeing.
 */
class WellbeingProvider(
    private val transactions: TransactionRepository,
    private val receipts: ReceiptRepository,
    private val budgets: BudgetRepository,
    private val recurring: RecurringRepository,
    private val savings: SavingsRepository,
    private val subscriptions: SubscriptionsRepository,
    private val settings: SettingsStore,
    private val history: WellbeingScoreRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
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
    )

    /** The screen-facing summary plus the just-closed month's persistable snapshot (null when unscored). */
    private data class Built(val summary: WellbeingSummary, val closedSnapshot: WellbeingScoreEntity?)

    fun summary(): Flow<WellbeingSummary> {
        val core = combine(
            transactions.getAll(), receipts.getAll(), budgets.budgets, recurring.items, settings.settings,
        ) { txns, rcpts, budg, rec, s -> Core(txns, rcpts, budg, rec, s.monthStartDay) }
        val extra = combine(
            savings.goals, savings.allContributions, subscriptions.ignored,
        ) { goals, contribs, ignored -> Extra(goals, contribs, ignored.map { it.merchant }.toSet()) }
        // Persisting the just-closed month's snapshot is a side effect of scoring it (§3.1). Idempotent
        // (PK = periodId), so the repeated writes from re-emits and multiple collectors converge; the
        // in-flight month is never in [Built.closedSnapshot], so it can never reach history.
        //
        // The trend sparkline (§3.2) reads back the stored history AFTER the just-closed upsert (so it
        // includes the newest closed month) and pairs it with the in-flight score as the ghost point.
        return combine(core, extra) { c, e -> build(c, e) }
            .onEach { built -> built.closedSnapshot?.let { history.upsert(it) } }
            .map { built ->
                val recent = history.getRecent(TREND_MONTHS)
                built.summary.copy(trend = WellbeingHistory.trend(recent, built.summary.score.score))
            }
            .flowOn(Dispatchers.Default)
    }

    private fun build(core: Core, extra: Extra): Built {
        val now = today()
        val receiptsById = core.receipts.associateBy { it.timestamp }
        val monthsTracked = core.txns.map { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) }
            .distinct().size

        fun window(offset: Int): Pair<Long, Long> {
            val (start, end) = PayCycle.month(now, core.monthStartDay, offset)
            return start.atStartOfDay(zone).toInstant().toEpochMilli() to
                end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        }

        fun txnsIn(w: Pair<Long, Long>) = core.txns.filter { it.timestamp in w.first..w.second }
        fun netSpend(list: List<TransactionEntity>) =
            list.fold(BigDecimal.ZERO) { a, t -> a + t.price.multiply(BigDecimal(t.quantity)) }
        fun paidSpend(list: List<TransactionEntity>) = netSpend(list) + paidAdjustmentOf(list, receiptsById)
        fun monthSpend(offset: Int) = paidSpend(txnsIn(window(offset)))

        // Subscriptions (global, from all receipts) — one charge per receipt, minus ignored merchants.
        val charges = buildCharges(core.txns, core.receipts, receiptsById)
        val detected = SubscriptionDetector.detect(charges, now).filterNot { it.merchant in extra.ignoredMerchants }
        val subsMonthly = detected.fold(BigDecimal.ZERO) { a, s -> a + s.monthlyEquivalent }
        val subsCount = detected.size
        val hasSubsData = core.receipts.size >= WellbeingEngine.MIN_RECEIPTS_TO_SCORE && monthsTracked >= 1

        // Trailing monthly-spend average (previous up-to-6 months with spend), for trend + subs share.
        fun avgBefore(offset: Int): BigDecimal? {
            val spends = (1..6).map { monthSpend(offset - it) }.filter { it.signum() > 0 }
            if (spends.isEmpty()) return null
            return spends.fold(BigDecimal.ZERO) { a, b -> a + b }.divide(BigDecimal(spends.size), 2, RoundingMode.HALF_UP)
        }
        fun trendPercentFor(offset: Int, spend: BigDecimal): Int? {
            val avg = avgBefore(offset) ?: return null
            if (avg.signum() <= 0) return null
            return ((spend.toDouble() / avg.toDouble() - 1.0) * 100).roundToInt()
        }

        val catBudgets = core.budgets.filterKeys { it.startsWith(BudgetRepository.CATEGORY_PREFIX) }
            .mapKeys { it.key.removePrefix(BudgetRepository.CATEGORY_PREFIX) }
        val monthlyBudget = core.budgets[BudgetRepository.MONTHLY]
        val avgSpend = avgBefore(0)

        // Per-category trailing monthly average (over prior months that had spend).
        val monthsWithData = (1..6).count { monthSpend(-it).signum() > 0 }.coerceAtLeast(1)
        val catAvg = buildMap<String, BigDecimal> {
            (1..6).forEach { off ->
                txnsIn(window(-off)).groupBy { it.category }.forEach { (cat, list) ->
                    merge(cat, netSpend(list)) { a, b -> a + b }
                }
            }
        }.mapValues { it.value.divide(BigDecimal(monthsWithData), 2, RoundingMode.HALF_UP) }

        fun inputsFor(offset: Int, withDetail: Boolean): WellbeingInputs {
            val w = window(offset)
            val monthTxns = txnsIn(w)
            val spend = paidSpend(monthTxns)
            val income = core.recurring.filter { it.isIncome }
                .fold(BigDecimal.ZERO) { a, r -> a + r.windowAmount(w.first, w.second) }
            val bills = core.recurring.filterNot { it.isIncome }
                .fold(BigDecimal.ZERO) { a, r -> a + r.windowAmount(w.first, w.second) }
            val hasIncome = income.signum() > 0
            val saved = income - bills - spend
            val rate = if (hasIncome) (saved.toDouble() / income.toDouble() * 100).roundToInt() else 0

            val catSpend = monthTxns.groupBy { it.category }.mapValues { netSpend(it.value) }
            val scopes: List<Pair<BigDecimal, BigDecimal>> = when {
                catBudgets.isNotEmpty() -> catBudgets.map { (cat, bud) -> (catSpend[cat] ?: BigDecimal.ZERO) to bud }
                monthlyBudget != null -> listOf(spend to monthlyBudget)
                else -> emptyList()
            }
            val overScopes = scopes.filter { it.first > it.second }
            val overCats = catBudgets.filter { (cat, bud) -> (catSpend[cat] ?: BigDecimal.ZERO) > bud }

            val baseline = avgSpend ?: spend
            val subsShare = when {
                !hasSubsData -> null
                subsCount == 0 -> 0
                baseline.signum() > 0 -> (subsMonthly.toDouble() / baseline.toDouble() * 100).roundToInt()
                else -> null
            }

            val goalPaces = extra.goals.map { g ->
                val prog = SavingsMath.progress(g, extra.contributions.filter { it.goalId == g.id }, now, zone)
                GoalPace(g.name, prog.reached, prog.behind)
            }

            val categories = if (!withDetail) emptyList() else (catSpend.keys + catBudgets.keys + catAvg.keys)
                .distinct().map { cat ->
                    CategorySpend(
                        category = cat,
                        current = catSpend[cat] ?: BigDecimal.ZERO,
                        average = catAvg[cat] ?: BigDecimal.ZERO,
                        monthlyAverage = catAvg[cat] ?: BigDecimal.ZERO,
                        hasBudget = cat in catBudgets,
                        count = monthTxns.count { it.category == cat },
                    )
                }

            return WellbeingInputs(
                hasIncome = hasIncome, savingsRatePercent = rate, income = income, saved = saved, netCashflow = saved,
                hasAnyBudget = scopes.isNotEmpty(), budgetedCount = scopes.size, overCount = overScopes.size,
                overspendTotal = overScopes.fold(BigDecimal.ZERO) { a, s -> a + (s.first - s.second) },
                budgetedTotal = scopes.fold(BigDecimal.ZERO) { a, s -> a + s.second },
                trendPercent = trendPercentFor(offset, spend),
                subsSharePercent = subsShare, subsMonthly = subsMonthly, subsCount = subsCount,
                goals = goalPaces, categories = categories, spend = spend,
                receiptsLogged = core.receipts.size, monthsTracked = monthsTracked,
            )
        }

        // The previous (just-closed) cycle is scored both for the trend chip and for history (§3.1).
        val previousFull = WellbeingEngine.score(inputsFor(-1, withDetail = false))
        val previousScore = previousFull.score
        val current = inputsFor(0, withDetail = true).copy(previousScore = previousScore)
        val score = WellbeingEngine.score(current)
        val monthlyTips = WellbeingEngine.tips(current)

        // Weekly pace card + tactical tips (Monday–Sunday of the current week).
        val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val weekWin = weekStart.atStartOfDay(zone).toInstant().toEpochMilli() to
            weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val weekTxns = txnsIn(weekWin)
        val weekSpent = paidSpend(weekTxns)
        val lastWeekSpent = paidSpend(txnsIn(
            weekStart.minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() to
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli() - 1,
        ))
        val weeklyBudget = core.budgets[BudgetRepository.WEEKLY]
        val daysElapsed = now.dayOfWeek.value
        val paceFraction = daysElapsed / 7f
        val fractionUsed = if (weeklyBudget != null && weeklyBudget.signum() > 0)
            (weekSpent.toDouble() / weeklyBudget.toDouble()).coerceIn(0.0, 1.0).toFloat() else 0f
        val weekDelta = if (lastWeekSpent.signum() > 0)
            ((weekSpent.toDouble() / lastWeekSpent.toDouble() - 1.0) * 100).roundToInt() else null
        val remaining = if (weeklyBudget != null) (weeklyBudget - weekSpent).max(BigDecimal.ZERO) else BigDecimal.ZERO
        val weekly = WeeklyPace(
            spent = weekSpent, weeklyBudget = weeklyBudget, fractionUsed = fractionUsed, paceFraction = paceFraction,
            remaining = remaining, daysLeft = (7 - daysElapsed).coerceAtLeast(0), deltaPercentVsLastWeek = weekDelta,
            onTrack = weeklyBudget == null || fractionUsed <= paceFraction + 0.05f,
        )
        val paced = if (weeklyBudget != null && weeklyBudget.signum() > 0 && fractionUsed >= 0.7f)
            PacedCategory(name = "", percentUsed = (fractionUsed * 100).roundToInt(), remaining = remaining) else null
        val leak = weekTxns.groupBy { it.category }
            .mapValues { it.value.size to netSpend(it.value) }
            .filter { it.value.first >= 6 && it.value.second.signum() > 0 &&
                it.value.second.toDouble() / it.value.first < 10.0 }
            .maxByOrNull { it.value.first }
            ?.let { LeakCategory(it.key, it.value.first, it.value.second) }
        val underPace = if (weeklyBudget != null && weeklyBudget.signum() > 0 &&
            weekSpent.toDouble() < weeklyBudget.toDouble() * paceFraction * 0.8
        ) {
            val expected = weeklyBudget.multiply(BigDecimal(paceFraction.toDouble()))
            UnderPaceCategory(name = "", under = (expected - weekSpent).max(BigDecimal.ZERO))
        } else null
        val weeklyTips = WellbeingEngine.weeklyTips(
            WeeklyInputs(
                spent = weekSpent, weeklyBudget = weeklyBudget, daysElapsed = daysElapsed, daysInWeek = 7,
                deltaPercentVsLastWeek = weekDelta, pacedCategory = paced, leakCategory = leak, underPaceCategory = underPace,
            ),
        )

        val wins = monthlyTips.filter { it.tone == TipTone.WIN }.map {
            WellbeingWin(emoji = winEmoji(it.type), type = it.type, label = it.label, percent = it.percent)
        }

        val onPace = current.goals.count { it.reached || !it.behind }
        val detail = mapOf(
            WellbeingComponentKey.SAVINGS to ComponentDetail(savingsRatePercent = current.savingsRatePercent, saved = current.saved, income = current.income),
            WellbeingComponentKey.BUDGET to ComponentDetail(overCount = current.overCount, budgetedCount = current.budgetedCount, overspend = current.overspendTotal),
            WellbeingComponentKey.TREND to ComponentDetail(trendPercent = current.trendPercent, avgSpend = avgSpend),
            WellbeingComponentKey.SUBSCRIPTIONS to ComponentDetail(subsMonthly = current.subsMonthly, subsSharePercent = current.subsSharePercent),
            WellbeingComponentKey.GOALS to ComponentDetail(goalsOnPace = onPace, goalsTotal = current.goals.size),
        )

        val monthYear = YearMonth.from(PayCycle.month(now, core.monthStartDay, 0).first)
        // §3.1: snapshot the just-closed cycle (offset -1) only — never the in-flight [monthYear].
        val closedSnapshot = WellbeingHistory.closedSnapshot(
            closedPeriodId = WellbeingHistory.periodId(now, core.monthStartDay, offset = -1),
            closedScore = previousFull,
            computedAt = System.currentTimeMillis(),
        )
        // §3.5 band-up nudge + §2.6 budget-adherence streak evidence (both derived, no new persistence).
        val bandUp = score.score?.let { WellbeingEngine.bandUp(it) }
        val budgetStreaks = WellbeingEngine.budgetStreakEvidence(budgetMonthStreaks(core, now, receiptsById))

        val summary = WellbeingSummary(
            score = score, monthlyTips = monthlyTips, weekly = weekly, weeklyTips = weeklyTips, wins = wins,
            detail = detail, receiptsLogged = core.receipts.size, hasBudget = current.hasAnyBudget,
            periodId = monthYear.toString(), monthYear = monthYear, weekStart = weekStart, weekEnd = weekEnd,
            bandUp = bandUp, budgetStreaks = budgetStreaks,
        )
        return Built(summary, closedSnapshot)
    }

    /**
     * Per-scope monthly budget streaks for the Wellbeing evidence (§2.6), computed by [StreakEngine] in
     * one pass. Tags each transaction with the closed pay-cycle month index its date falls in (0 = the
     * just-closed month, positive into the past; the current OPEN month becomes the live period that only
     * feeds [Streak.liveOnTrack]) — mirrors [com.budgetty.app.ui.recap.RecapProvider.tagByPeriod]. The
     * caller surfaces/caps via [WellbeingEngine.budgetStreakEvidence].
     */
    private fun budgetMonthStreaks(core: Core, now: LocalDate, receiptsById: Map<Long, ReceiptEntity>): List<Streak> {
        val endCycle = YearMonth.from(PayCycle.month(now, core.monthStartDay, -1).first)
        val byIndex = core.txns
            .mapNotNull { t ->
                val date = Instant.ofEpochMilli(t.timestamp).atZone(zone).toLocalDate()
                val txnCycle = YearMonth.from(PayCycle.month(date, core.monthStartDay, 0).first)
                val idx = ChronoUnit.MONTHS.between(txnCycle, endCycle).toInt()
                if (idx == LIVE_INDEX || idx in 0 until StreakEngine.MAX_STREAK) idx to t else null
            }
            .groupBy({ it.first }, { it.second })
        val closedGroups = byIndex.filterKeys { it >= 0 }
        val closed = closedGroups.flatMap { (idx, list) ->
            list.map { StreakTxn(idx, it.category, it.price.multiply(BigDecimal(it.quantity))) }
        }
        val adjustment = closedGroups.mapValues { (_, list) -> paidAdjustmentOf(list, receiptsById) }
        val liveList = byIndex[LIVE_INDEX].orEmpty()
        val live = LiveBudgetPeriod(
            transactions = liveList.map { StreakTxn(0, it.category, it.price.multiply(BigDecimal(it.quantity))) },
            monthlyAdjustment = paidAdjustmentOf(liveList, receiptsById),
        )
        val catBudgets = core.budgets.filterKeys { it.startsWith(BudgetRepository.CATEGORY_PREFIX) }
            .mapKeys { it.key.removePrefix(BudgetRepository.CATEGORY_PREFIX) }
        return StreakEngine.budgetStreaks(
            BudgetStreakInput(
                transactions = closed,
                categoryBudgets = catBudgets,
                monthlyBudget = core.budgets[BudgetRepository.MONTHLY],
                kind = StreakKind.BUDGET_MONTH,
                monthlyLabel = "", // blank → the surface shows a localized "Overall budget" label
                monthlyAdjustmentByPeriod = adjustment,
                live = live,
            ),
        )
    }

    private fun winEmoji(type: TipType): String = when (type) {
        // §2.4 no-flames: the saving win used a 🔥; a calm 📈 keeps it a win, not a loss-framed streak.
        TipType.SAVINGS_WIN -> "📈"
        TipType.CATEGORY_IMPROVED -> "📉"
        TipType.UNDER_PACE_WIN -> "🎉"
        else -> "✅"
    }

    /** One [MerchantCharge] per receipt (normalized store, paid total, date) — mirrors SubscriptionsViewModel. */
    private fun buildCharges(
        transactions: List<TransactionEntity>,
        receipts: List<ReceiptEntity>,
        receiptsById: Map<Long, ReceiptEntity>,
    ): List<MerchantCharge> =
        transactions.groupBy { it.receiptId }.mapNotNull { (receiptId, group) ->
            val store = StoreNormalizer.normalize(receiptsById[receiptId]?.store.orEmpty())
            if (store.isBlank()) return@mapNotNull null
            val net = group.fold(BigDecimal.ZERO) { a, t -> a + t.price.multiply(BigDecimal(t.quantity)) }
            val total = net + paidAdjustmentOf(group, receiptsById)
            if (total.signum() <= 0) return@mapNotNull null
            val category = group.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: group.first().category
            MerchantCharge(
                merchant = store,
                emoji = Categories.emojiOf(category),
                amount = total,
                dateMillis = group.firstOrNull()?.timestamp ?: receiptId,
            )
        }

    private companion object {
        /** Period index of the current OPEN pay-cycle month in the streak tagging (never counted; §2.3). */
        const val LIVE_INDEX = -1

        /** How many stored closed months the trend sparkline reads back (§3.2). */
        const val TREND_MONTHS = 6
    }
}
