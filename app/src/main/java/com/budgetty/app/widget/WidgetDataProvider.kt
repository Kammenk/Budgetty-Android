package com.budgetty.app.widget

import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.monthlyToWeekly
import com.budgetty.app.ui.util.weeklyToMonthly
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Computes the [WidgetData] snapshot the Glance widgets render from, via one-shot reads of the
 * existing repositories — no new tables, no cached prefs. Mirrors the range/spend math used by
 * BudgetViewModel and InsightsViewModel so the widgets always agree with the in-app screens.
 */
class WidgetDataProvider(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsStore: SettingsStore,
    private val scanQuota: ScanQuota,
    private val billingManager: BillingManager,
    private val receiptRepository: ReceiptRepository,
) {
    suspend fun load(today: LocalDate = LocalDate.now()): WidgetData {
        val budgets = budgetRepository.budgets.first()
        val monthlySet = budgets[BudgetRepository.MONTHLY]
        val weeklySet = budgets[BudgetRepository.WEEKLY]
        // Single-budget model: only one of MONTHLY/WEEKLY is ever set, so derive the other period
        // from it (weekly⇄monthly) — both budget widgets then always show a meaningful number.
        val monthlyBudget = monthlySet ?: weeklySet?.let { weeklyToMonthly(it) } ?: BigDecimal.ZERO
        val weeklyBudget = weeklySet ?: monthlySet?.let { monthlyToWeekly(it) } ?: BigDecimal.ZERO

        val (monthStart, monthEnd) = monthRange(today)
        val (weekStart, weekEnd) = weekRange(today)
        val (prevStart, prevEnd) = monthRange(today.minusMonths(1))
        val (lastWeekStart, lastWeekEnd) = weekRange(today.minusWeeks(1))

        val monthTxns = transactionRepository.getBetween(monthStart, monthEnd).first()
        val weekTxns = transactionRepository.getBetween(weekStart, weekEnd).first()
        val prevMonthTxns = transactionRepository.getBetween(prevStart, prevEnd).first()
        val lastWeekTxns = transactionRepository.getBetween(lastWeekStart, lastWeekEnd).first()

        // Period spend is adjusted to what was paid — on-top tax (tax-exclusive receipts) and extra
        // charges added, order discounts subtracted — so the widgets match the in-app screens; the
        // top-category breakdown below stays on the net line prices.
        val receiptsById = receiptRepository.getAll().first().associateBy { it.timestamp }
        val monthlySpent = monthTxns.spend() + paidAdjustmentOf(monthTxns, receiptsById)
        val lastMonthTotal = prevMonthTxns.spend() + paidAdjustmentOf(prevMonthTxns, receiptsById)
        val weeklySpent = weekTxns.spend() + paidAdjustmentOf(weekTxns, receiptsById)
        val lastWeekTotal = lastWeekTxns.spend() + paidAdjustmentOf(lastWeekTxns, receiptsById)

        val colorByName = categoryRepository.categories.first().associate { it.name to it.colorArgb }
        val topCategories = monthTxns
            .groupBy { it.category }
            .mapValues { (_, list) -> list.spend() }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (name, amount) ->
                WidgetCategory(name, amount, colorByName[name] ?: Categories.colorOf(name))
            }

        val vsLastMonthPercent = if (lastMonthTotal.signum() > 0) {
            ((monthlySpent.toDouble() - lastMonthTotal.toDouble()) / lastMonthTotal.toDouble() * 100)
                .roundToInt()
        } else {
            null
        }
        val vsLastWeekPercent = if (lastWeekTotal.signum() > 0) {
            ((weeklySpent.toDouble() - lastWeekTotal.toDouble()) / lastWeekTotal.toDouble() * 100)
                .roundToInt()
        } else {
            null
        }

        return WidgetData(
            currencySymbol = settingsStore.settings.value.currency.symbol,
            monthlyBudget = monthlyBudget,
            monthlySpent = monthlySpent,
            weeklyBudget = weeklyBudget,
            weeklySpent = weeklySpent,
            // Monthly wins if both or neither is set; weekly only when it alone is set.
            budgetIsMonthly = monthlySet != null || weeklySet == null,
            lastMonthTotal = lastMonthTotal,
            vsLastMonthPercent = vsLastMonthPercent,
            lastWeekTotal = lastWeekTotal,
            vsLastWeekPercent = vsLastWeekPercent,
            weekLabel = weekLabel(today),
            monthReceiptCount = monthTxns.map { it.receiptId }.toSet().size,
            topCategories = topCategories,
            monthLabel = MONTH_YEAR.format(YearMonth.from(today)),
            isPremium = billingManager.isPremium.value,
            scansRemaining = scanQuota.remaining(),
        )
    }

    /** Summed price × quantity across the transactions (matches the app's spend math). */
    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /** Inclusive [start, end] epoch-millis window for [date]'s calendar month. */
    private fun monthRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val month = YearMonth.from(date)
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Inclusive [start, end] epoch-millis window for [date]'s Mon–Sun week. */
    private fun weekRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Short label for [date]'s Mon–Sun week, e.g. "Jun 24–30" (or "Jun 28 – Jul 4" across months). */
    private fun weekLabel(date: LocalDate): String {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return if (start.month == end.month) {
            "${MONTH.format(start)} ${start.dayOfMonth}–${end.dayOfMonth}"
        } else {
            "${MONTH.format(start)} ${start.dayOfMonth} – ${MONTH.format(end)} ${end.dayOfMonth}"
        }
    }

    private companion object {
        val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    }
}
