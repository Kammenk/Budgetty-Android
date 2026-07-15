package com.budgetty.app.ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.Receipt
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.components.pieColors
import com.budgetty.app.ui.util.monthlyAmount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    // False only for the initial placeholder the StateFlow emits before the first DB load lands.
    // The UI gates the "no receipts" empty state on this so it doesn't flash during cold start.
    val isLoaded: Boolean = false,
    val filter: DateRangeFilter = DateRangeFilter.CURRENT_MONTH,
    val transactions: List<TransactionEntity> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val slices: List<PieSlice> = emptyList(),
    val total: BigDecimal = BigDecimal.ZERO,
    val monthlySpent: BigDecimal = BigDecimal.ZERO,
    // Recurring bills (isIncome=false) summed to their current-month equivalent. Planning-only: the
    // summary card shows these alongside the receipt-backed spend as "planned", never merged into it.
    val monthlyBills: BigDecimal = BigDecimal.ZERO,
    val monthlyBudget: BigDecimal? = null,
    val weeklySpent: BigDecimal = BigDecimal.ZERO,
    val weeklyBudget: BigDecimal? = null,
    val hasCategoryBudgets: Boolean = false,
    // Quick-stats strip: this week vs last week, and the top category this month.
    val lastWeekSpent: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryAmount: BigDecimal = BigDecimal.ZERO,
    // Tablet summary header: spend in the equal-length period before the selected one (null when
    // there's nothing to compare against) and the average spend per elapsed day of the period.
    val previousPeriodSpent: BigDecimal? = null,
    val dailyAvg: BigDecimal = BigDecimal.ZERO,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    private val receiptRepository: ReceiptRepository,
    private val scanQuota: ScanQuota,
    private val billingManager: BillingManager,
    recurringRepository: RecurringRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(DateRangeFilter.CURRENT_MONTH)
    val filter: StateFlow<DateRangeFilter> = selectedFilter.asStateFlow()

    private val transactions = selectedFilter.flatMapLatest { filter ->
        val (start, end) = filter.toRange()
        repository.getBetween(start, end)
    }

    // The equal-length period before the selected one, for the "vs last month" comparison.
    private val previousTransactions = selectedFilter.flatMapLatest { filter ->
        val (start, end) = filter.previousRange()
        repository.getBetween(start, end)
    }

    // Current calendar month's spend, independent of the selected filter (for the budget box).
    private val monthRange = DateRangeFilter.CURRENT_MONTH.toRange()
    private val monthlyTransactions = repository.getBetween(monthRange.first, monthRange.second)

    // Recurring bills expressed as a current-month total (weekly ×52/12, yearly ÷12, one-time only in
    // its own month). Income rows are excluded — only bills count toward the "with bills" figure.
    private val monthlyBills: Flow<BigDecimal> = recurringRepository.items.map { items ->
        items.filterNot { it.isIncome }
            .fold(BigDecimal.ZERO) { acc, r -> acc + r.monthlyAmount(monthRange.first, monthRange.second) }
    }

    // Current calendar week's spend (Mon–Sun), independent of the filter (for the weekly box).
    private val weekRange = currentWeekRange()
    private val weeklyTransactions = repository.getBetween(weekRange.first, weekRange.second)

    // Previous Mon–Sun week, for the "vs last week" quick stat.
    private val lastWeekRange = currentWeekRange(LocalDate.now().minusWeeks(1))
    private val lastWeeklyTransactions = repository.getBetween(lastWeekRange.first, lastWeekRange.second)

    val uiState: StateFlow<HomeUiState> =
        combine(
            selectedFilter,
            transactions,
            categoryRepository.categories,
            combine(monthlyTransactions, monthlyBills) { t, b -> MonthlyData(t, b) },
            combine(
                budgetRepository.budgets,
                receiptRepository.getAll(),
                weeklyTransactions,
                lastWeeklyTransactions,
                previousTransactions,
            ) { b, r, w, lw, pt -> BudgetsReceiptsWeeks(b, r, w, lw, pt) },
        ) { filter, txns, categories, monthlyData, brw ->
            val receiptsById = brw.receipts.associateBy { it.timestamp }
            // Adjust each period's summed line prices to what was actually paid: add on-top tax
            // (tax-exclusive receipts) and extra charges, and subtract order discounts — so the spend
            // figures match the receipt totals. Per-category slices below stay on the net line prices.
            val total = txns.spend() + paidAdjustmentOf(txns, receiptsById)
            val monthlySpent = monthlyData.txns.spend() + paidAdjustmentOf(monthlyData.txns, receiptsById)
            val weeklySpent = brw.weekly.spend() + paidAdjustmentOf(brw.weekly, receiptsById)
            val lastWeekSpent = brw.lastWeekly.spend() + paidAdjustmentOf(brw.lastWeekly, receiptsById)
            val previousSpent = brw.previous.spend() + paidAdjustmentOf(brw.previous, receiptsById)
            val colorByCategory = categories.associate { it.name to it.colorArgb }
            val topByCategory = monthlyData.txns.groupBy { it.category }
                .mapValues { (_, list) -> list.spend() }
                .maxByOrNull { it.value }
            HomeUiState(
                isLoaded = true,
                filter = filter,
                transactions = txns,
                receipts = txns.toReceipts(receiptsById),
                slices = txns.toSlices(colorByCategory),
                total = total,
                monthlySpent = monthlySpent,
                monthlyBills = monthlyData.bills,
                monthlyBudget = brw.budgets[BudgetRepository.MONTHLY],
                weeklySpent = weeklySpent,
                weeklyBudget = brw.budgets[BudgetRepository.WEEKLY],
                hasCategoryBudgets = brw.budgets.keys.any { it.startsWith(BudgetRepository.CATEGORY_PREFIX) },
                lastWeekSpent = lastWeekSpent,
                topCategory = topByCategory?.key,
                topCategoryAmount = topByCategory?.value ?: BigDecimal.ZERO,
                previousPeriodSpent = previousSpent.takeIf { it.signum() > 0 },
                dailyAvg = dailyAverage(total, filter),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /** The 5 most recent receipts across all time (independent of the period filter), for the Home
     *  "Recent receipts" section — so it isn't empty just because a new month started. */
    val recentReceipts: StateFlow<List<Receipt>> =
        combine(repository.getAll(), receiptRepository.getAll()) { txns, receipts ->
            txns.toReceipts(receipts.associateBy { it.timestamp }).take(5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onFilterSelected(filter: DateRangeFilter) {
        selectedFilter.value = filter
    }

    /** Inclusive [start, end] epoch-millis window for the current Mon–Sun week. */
    private fun currentWeekRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Deletes a whole receipt: all its line items plus the receipt metadata. Undoable. */
    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            val meta = receiptRepository.getById(receipt.id)
            lastDeleted = DeletedSnapshot(receipt.transactions, listOfNotNull(meta))
            repository.deleteByReceiptId(receipt.id)
            receiptRepository.deleteById(receipt.id)
        }
    }

    /** Deletes one line item; if it was the receipt's last item, drops the receipt too. Undoable. */
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val receipt = uiState.value.receipts.find { it.id == transaction.receiptId }
            val wasLast = receipt != null && receipt.transactions.size <= 1
            val meta = if (wasLast) receiptRepository.getById(transaction.receiptId) else null
            lastDeleted = DeletedSnapshot(listOf(transaction), listOfNotNull(meta))
            repository.deleteById(transaction.id)
            if (wasLast) receiptRepository.deleteById(transaction.receiptId)
        }
    }

    /**
     * Restores the most recently deleted receipt/line item with its original ids.
     * Does not touch the scan quota — a delete never refunds a used scan.
     */
    fun undoLastDelete() {
        val snapshot = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            repository.insertAll(snapshot.transactions)
            snapshot.receipts.forEach { receiptRepository.insert(it) }
        }
    }

    private var lastDeleted: DeletedSnapshot? = null

    private data class DeletedSnapshot(
        val transactions: List<TransactionEntity>,
        val receipts: List<ReceiptEntity>,
    )

    fun isPremium(): Boolean = billingManager.isPremium.value

    /** Premium users get unlimited scans; otherwise the free quota applies. */
    fun canScan(): Boolean = isPremium() || scanQuota.canScan()

    fun scanRemaining(): Int = scanQuota.remaining()

    /** Summed price × quantity across the transactions. */
    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /** Pairs the current-month transactions with the recurring-bills total so the outer combine keeps
     *  one slot for both current-month sources. */
    private data class MonthlyData(
        val txns: List<TransactionEntity>,
        val bills: BigDecimal,
    )

    /** Bundles the nested-combine sources so the outer combine stays within arity limits. */
    private data class BudgetsReceiptsWeeks(
        val budgets: Map<String, BigDecimal>,
        val receipts: List<ReceiptEntity>,
        val weekly: List<TransactionEntity>,
        val lastWeekly: List<TransactionEntity>,
        val previous: List<TransactionEntity>,
    )

    /**
     * Average spend per elapsed day of the selected period: total divided by the number of days
     * from the period's start through today (or the period end, whichever is sooner), so the
     * current month isn't diluted by days that haven't happened yet.
     */
    private fun dailyAverage(total: BigDecimal, filter: DateRangeFilter): BigDecimal {
        val zone = ZoneId.systemDefault()
        val (startMillis, endMillis) = filter.toRange()
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now()
        val lastDay = if (endDate.isAfter(today)) today else endDate
        val days = (ChronoUnit.DAYS.between(startDate, lastDay) + 1).coerceAtLeast(1)
        return total.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
    }

    /** One slice per category, value = summed price × quantity, colored by the saved category color. */
    private fun List<TransactionEntity>.toSlices(colorByCategory: Map<String, Int>): List<PieSlice> =
        groupBy { it.category }
            .mapValues { (_, txns) ->
                txns.fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }
            }
            .entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (category, value) ->
                PieSlice(
                    label = category,
                    value = value,
                    color = colorByCategory[category]?.let { Color(it) }
                        ?: pieColors[index % pieColors.size],
                )
            }

    /** One [Receipt] per upload, grouping the transactions that share a timestamp. */
    private fun List<TransactionEntity>.toReceipts(receiptsById: Map<Long, ReceiptEntity>): List<Receipt> =
        groupBy { it.receiptId }
            .map { (receiptId, txns) ->
                val meta = receiptsById[receiptId]
                val netSum = txns.fold(BigDecimal.ZERO) { acc, t ->
                    acc + t.price.multiply(BigDecimal(t.quantity))
                }
                // Anchor on the printed total: add on-top tax (tax-exclusive receipts) plus any extra
                // charges (delivery/service fees, a courier tip) so the receipt total equals what was
                // paid; the item rows still show the printed net prices.
                val addedCharges = (if (meta?.taxOnTop == true) meta.tax else BigDecimal.ZERO) +
                    (meta?.extraCharges ?: BigDecimal.ZERO)
                Receipt(
                    id = receiptId,
                    // Normalize on read so receipts saved before this (or with a raw name) still
                    // group and display under the canonical brand. Idempotent for new clean rows.
                    store = StoreNormalizer.normalize(meta?.store.orEmpty()),
                    transactions = txns,
                    // All items of one receipt share the made-date; fall back to the id (legacy rows).
                    timestamp = txns.firstOrNull()?.timestamp ?: receiptId,
                    price = netSum + addedCharges,
                    discount = meta?.discount ?: BigDecimal.ZERO,
                    tax = meta?.tax ?: BigDecimal.ZERO,
                )
            }
            // Newest first by the receipt's printed date. That date is day-granular (local midnight),
            // so receipts from the same day tie; break the tie by id — the upload timestamp minted at
            // save — so the most recently added receipt of a day sorts to the top instead of landing in
            // an arbitrary database order.
            .sortedWith(compareByDescending<Receipt> { it.timestamp }.thenByDescending { it.id })
}
