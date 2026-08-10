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
import com.budgetty.app.data.repository.BudgetRolloverRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.components.pieColors
import com.budgetty.app.ui.util.PayCycle
import com.budgetty.app.ui.util.UpcomingBill
import com.budgetty.app.ui.util.currentMonthRange
import com.budgetty.app.ui.util.isEffectivelyPaidThisCycle
import com.budgetty.app.ui.util.monthlyAmount
import com.budgetty.app.ui.util.upcomingBills
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.budgetty.app.ui.wellbeing.WellbeingProvider
import com.budgetty.app.ui.wellbeing.WellbeingSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** One month's spend within a multi-month Home window, for the summary card's mini trend bars. */
data class MonthlySpend(val month: YearMonth, val amount: BigDecimal)

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
    // ── Safe to spend (current pay-cycle cash-flow) ──
    // Recurring income summed to the pay-cycle month; 0 ⇒ the card shows its "add income" setup state.
    val cycleIncome: BigDecimal = BigDecimal.ZERO,
    // Recurring bills split into what's still owed this cycle vs already marked paid (planning-only).
    val billsStillDue: BigDecimal = BigDecimal.ZERO,
    val billsPaid: BigDecimal = BigDecimal.ZERO,
    // Recurring bills (monthly/weekly) due soon, soonest first — the Home "Upcoming bills" card
    // (date-based, independent of the period filter). [hasBills] gates the card on any bill existing.
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val hasBills: Boolean = false,
    // Left to spend freely before the next payday: income − spent so far − every recurring bill this
    // cycle (still due + already paid). Paying a bill is net-neutral here — it was already reserved.
    val safeToSpend: BigDecimal = BigDecimal.ZERO,
    // Whole days from today to the next pay-cycle start (min 1) and that date, for the per-day line.
    val daysUntilPayday: Int = 1,
    val nextPayday: LocalDate? = null,
    val monthlyBudget: BigDecimal? = null,
    // Unspent budget carried into this pay-cycle month (0 unless rollover is on); added on top of the
    // overall monthly budget for the budget card's effective limit + a "+X carried over" line.
    val monthlyCarried: BigDecimal = BigDecimal.ZERO,
    val weeklySpent: BigDecimal = BigDecimal.ZERO,
    val weeklyBudget: BigDecimal? = null,
    val hasCategoryBudgets: Boolean = false,
    // Tablet summary header: spend in the equal-length period before the selected one (null when
    // there's nothing to compare against) and the average spend per elapsed day of the period.
    val previousPeriodSpent: BigDecimal? = null,
    val dailyAvg: BigDecimal = BigDecimal.ZERO,
    // Multi-month period extras (empty/zero for single-month windows): per-pay-cycle-month spend for
    // the summary card's mini trend bars (Last 3 / Last 6 months), and the average monthly spend across
    // the window (Last 3 / Last 6 / All time), shown once the monthly budget + bills cards drop out.
    val monthlyBreakdown: List<MonthlySpend> = emptyList(),
    val monthlyAverage: BigDecimal = BigDecimal.ZERO,
    // Wellbeing score + top tip for the Home banner (null until the first summary lands).
    val wellbeing: WellbeingSummary? = null,
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
    settingsStore: SettingsStore,
    rolloverRepository: BudgetRolloverRepository,
    wellbeingProvider: WellbeingProvider,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(DateRangeFilter.CURRENT_MONTH)
    val filter: StateFlow<DateRangeFilter> = selectedFilter.asStateFlow()

    // The pay-day the financial month starts on; every month window below recomputes when it changes.
    private val monthStartDay: Flow<Int> =
        settingsStore.settings.map { it.monthStartDay }.distinctUntilChanged()

    // The selected preset paired with the pay-cycle start day, so the window reacts to either.
    private val filterCtx: Flow<FilterCtx> =
        combine(selectedFilter, monthStartDay) { filter, day -> FilterCtx(filter, day) }

    private val transactions = filterCtx.flatMapLatest { (filter, day) ->
        val (start, end) = filter.toRange(monthStartDay = day)
        repository.getBetween(start, end)
    }

    // The equal-length period before the selected one, for the "vs last month" comparison.
    private val previousTransactions = filterCtx.flatMapLatest { (filter, day) ->
        val (start, end) = filter.previousRange(monthStartDay = day)
        repository.getBetween(start, end)
    }

    // Current pay-cycle month's spend, independent of the selected filter (for the budget box).
    private val monthlyTransactions = monthStartDay.flatMapLatest { day ->
        val (start, end) = DateRangeFilter.CURRENT_MONTH.toRange(monthStartDay = day)
        repository.getBetween(start, end)
    }

    // Current pay-cycle cash-flow from the recurring plan: income summed to the cycle, and bills split
    // into still-due vs already-paid (mark-as-paid is planning-only). Drives the Safe-to-spend card;
    // the two bill parts also sum to the planned-bills total the summary/tablet cards show.
    private val cashFlow: Flow<CashFlow> =
        combine(recurringRepository.items, monthStartDay) { items, day ->
            val (start, end) = currentMonthRange(monthStartDay = day)
            val today = LocalDate.now()
            var income = BigDecimal.ZERO
            var billsStillDue = BigDecimal.ZERO
            var billsPaid = BigDecimal.ZERO
            items.forEach { r ->
                val amount = r.monthlyAmount(start, end)
                when {
                    r.isIncome -> income += amount
                    r.isEffectivelyPaidThisCycle(today, day) -> billsPaid += amount
                    else -> billsStillDue += amount
                }
            }
            CashFlow(
                income = income,
                billsStillDue = billsStillDue,
                billsPaid = billsPaid,
                upcomingBills = items.upcomingBills(today, day),
                hasBills = items.any { !it.isIncome },
            )
        }

    // Current calendar week's spend (Mon–Sun), independent of the filter (for the weekly box).
    private val weekRange = currentWeekRange()
    private val weeklyTransactions = repository.getBetween(weekRange.first, weekRange.second)

    // Unspent budget carried into the current pay-cycle month for the overall monthly budget; 0 when
    // rollover is off (the stored rows only accrue while it's enabled). Weekly budgets never roll.
    private val monthlyCarried: Flow<BigDecimal> =
        combine(rolloverRepository.carried, settingsStore.settings) { carried, settings ->
            if (settings.budgetRolloverEnabled) carried[BudgetRepository.MONTHLY] ?: BigDecimal.ZERO
            else BigDecimal.ZERO
        }

    val uiState: StateFlow<HomeUiState> =
        combine(
            filterCtx,
            transactions,
            categoryRepository.categories,
            combine(monthlyTransactions, cashFlow) { t, cf -> MonthlyData(t, cf) },
            combine(
                combine(budgetRepository.budgets, monthlyCarried) { b, mc -> b to mc },
                receiptRepository.getAll(),
                weeklyTransactions,
                previousTransactions,
            ) { (b, mc), r, w, pt -> BudgetsReceiptsWeeks(b, mc, r, w, pt) },
        ) { ctx, txns, categories, monthlyData, brw ->
            val receiptsById = brw.receipts.associateBy { it.timestamp }
            // Adjust each period's summed line prices to what was actually paid: add on-top tax
            // (tax-exclusive receipts) and extra charges, and subtract order discounts — so the spend
            // figures match the receipt totals. Per-category slices below stay on the net line prices.
            val total = txns.spend() + paidAdjustmentOf(txns, receiptsById)
            val monthlySpent = monthlyData.txns.spend() + paidAdjustmentOf(monthlyData.txns, receiptsById)
            val weeklySpent = brw.weekly.spend() + paidAdjustmentOf(brw.weekly, receiptsById)
            val previousSpent = brw.previous.spend() + paidAdjustmentOf(brw.previous, receiptsById)
            val colorByCategory = categories.associate { it.name to it.colorArgb }
            val cashFlow = monthlyData.cashFlow
            // Safe to spend before the next payday = cycle income − spent so far − every recurring bill
            // this cycle (still due + already paid). Paid bills stay subtracted, so marking one paid just
            // shifts it from the still-due bucket to the paid bucket and the figure holds (never jumps up).
            val safeToSpend =
                cashFlow.income - monthlySpent - cashFlow.billsStillDue - cashFlow.billsPaid
            val today = LocalDate.now()
            val payday = PayCycle.month(today, ctx.monthStartDay, 1).first
            val daysUntilPayday = ChronoUnit.DAYS.between(today, payday).toInt().coerceAtLeast(1)
            HomeUiState(
                isLoaded = true,
                filter = ctx.filter,
                transactions = txns,
                receipts = txns.toReceipts(receiptsById),
                slices = txns.toSlices(colorByCategory),
                total = total,
                monthlySpent = monthlySpent,
                monthlyBills = cashFlow.billsStillDue + cashFlow.billsPaid,
                cycleIncome = cashFlow.income,
                billsStillDue = cashFlow.billsStillDue,
                billsPaid = cashFlow.billsPaid,
                upcomingBills = cashFlow.upcomingBills,
                hasBills = cashFlow.hasBills,
                safeToSpend = safeToSpend,
                daysUntilPayday = daysUntilPayday,
                nextPayday = payday,
                monthlyBudget = brw.budgets[BudgetRepository.MONTHLY],
                monthlyCarried = brw.monthlyCarried,
                weeklySpent = weeklySpent,
                weeklyBudget = brw.budgets[BudgetRepository.WEEKLY],
                hasCategoryBudgets = brw.budgets.keys.any { it.startsWith(BudgetRepository.CATEGORY_PREFIX) },
                previousPeriodSpent = previousSpent.takeIf { it.signum() > 0 },
                dailyAvg = dailyAverage(total, ctx.filter, ctx.monthStartDay, txns),
                monthlyBreakdown = monthlyBreakdown(txns, receiptsById, ctx.filter, ctx.monthStartDay),
                monthlyAverage = monthlyAverage(total, ctx.filter, txns),
            )
        }
            .let { base -> combine(base, wellbeingProvider.summary()) { s, wb -> s.copy(wellbeing = wb) } }
            .stateIn(
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

    /** The selected preset plus the pay-cycle start day, so one combine slot carries both. */
    private data class FilterCtx(val filter: DateRangeFilter, val monthStartDay: Int)

    /** Pairs the current-month transactions with the pay-cycle cash-flow so the outer combine keeps
     *  one slot for both current-month sources. */
    private data class MonthlyData(
        val txns: List<TransactionEntity>,
        val cashFlow: CashFlow,
    )

    /** Current pay-cycle cash-flow inputs (income + bills split paid/still-due) for Safe-to-spend,
     *  plus the date-based upcoming-bills list + a "has any bill" flag for the Upcoming bills card. */
    private data class CashFlow(
        val income: BigDecimal,
        val billsStillDue: BigDecimal,
        val billsPaid: BigDecimal,
        val upcomingBills: List<UpcomingBill>,
        val hasBills: Boolean,
    )

    /** Bundles the nested-combine sources so the outer combine stays within arity limits. */
    private data class BudgetsReceiptsWeeks(
        val budgets: Map<String, BigDecimal>,
        val monthlyCarried: BigDecimal,
        val receipts: List<ReceiptEntity>,
        val weekly: List<TransactionEntity>,
        val previous: List<TransactionEntity>,
    )

    /**
     * Average spend per elapsed day of the selected period: total divided by the number of days
     * from the period's start through today (or the period end, whichever is sooner), so the
     * current month isn't diluted by days that haven't happened yet.
     */
    private fun dailyAverage(
        total: BigDecimal,
        filter: DateRangeFilter,
        monthStartDay: Int,
        txns: List<TransactionEntity>,
    ): BigDecimal {
        val zone = ZoneId.systemDefault()
        val (startMillis, endMillis) = filter.toRange(monthStartDay = monthStartDay)
        // For "All time" the window opens at the epoch, so anchor the average to the first recorded
        // transaction instead — otherwise it's divided across decades of empty pre-history.
        val effectiveStartMillis = maxOf(startMillis, txns.minOfOrNull { it.timestamp } ?: startMillis)
        val startDate = Instant.ofEpochMilli(effectiveStartMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now()
        val lastDay = if (endDate.isAfter(today)) today else endDate
        val days = (ChronoUnit.DAYS.between(startDate, lastDay) + 1).coerceAtLeast(1)
        return total.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
    }

    /**
     * Per-pay-cycle-month spend across a multi-month window (Last 3 / Last 6 months), oldest first, for
     * the summary card's mini trend bars. Empty for single-month and all-time windows — all-time spans
     * too many months to bar sensibly, so it shows the monthly average alone.
     */
    private fun monthlyBreakdown(
        txns: List<TransactionEntity>,
        receiptsById: Map<Long, ReceiptEntity>,
        filter: DateRangeFilter,
        monthStartDay: Int,
    ): List<MonthlySpend> {
        val offsets = when (filter) {
            DateRangeFilter.LAST_3_MONTHS -> (-2..0)
            DateRangeFilter.LAST_6_MONTHS -> (-5..0)
            else -> return emptyList()
        }
        val today = LocalDate.now()
        return offsets.map { offset ->
            val (startDate, endDate) = PayCycle.month(today, monthStartDay, offset)
            val (start, end) = dateRangeToEpochMillis(startDate, endDate)
            val monthTxns = txns.filter { it.timestamp in start..end }
            MonthlySpend(
                month = YearMonth.from(startDate),
                amount = monthTxns.spend() + paidAdjustmentOf(monthTxns, receiptsById),
            )
        }
    }

    /**
     * Average spend per month across the selected multi-month window: the window total split over its
     * month count (3 or 6), or — for All time — over the months since the first recorded transaction.
     * Zero for single-month windows, where an average would just restate the total.
     */
    private fun monthlyAverage(
        total: BigDecimal,
        filter: DateRangeFilter,
        txns: List<TransactionEntity>,
    ): BigDecimal {
        val months = when (filter) {
            DateRangeFilter.LAST_3_MONTHS -> 3
            DateRangeFilter.LAST_6_MONTHS -> 6
            DateRangeFilter.ALL_TIME -> monthsTracked(txns)
            else -> 0
        }
        return if (months <= 0) BigDecimal.ZERO
        else total.divide(BigDecimal(months), 2, RoundingMode.HALF_UP)
    }

    /** Whole months from the earliest recorded transaction through the current month, inclusive (min 1). */
    private fun monthsTracked(txns: List<TransactionEntity>): Int {
        val earliest = txns.minOfOrNull { it.timestamp } ?: return 0
        val firstMonth = YearMonth.from(Instant.ofEpochMilli(earliest).atZone(ZoneId.systemDefault()))
        return (ChronoUnit.MONTHS.between(firstMonth, YearMonth.now()) + 1).toInt().coerceAtLeast(1)
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
