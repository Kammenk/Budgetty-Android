package com.budgetty.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.Receipt
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.home.DateRangeFilter
import com.budgetty.app.ui.util.monthlyAmount
import com.budgetty.app.ui.util.periodAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** How the History list is ordered. Persisted in settings so it survives relaunch. */
enum class SortOrder { NEWEST, OLDEST, PRICE_HIGH, PRICE_LOW }

/** A purchased line item joined with the store it came from (taken from its receipt). */
data class HistoryItem(
    val transaction: TransactionEntity,
    val store: String,
) {
    /** Quantity-aware line total — what the row shows and what price filters/sorts compare against. */
    val lineTotal: BigDecimal
        get() = transaction.price.multiply(BigDecimal(transaction.quantity))
}

/** A single calendar day's line items, ordered newest-first, with that day's total spend. */
data class DayGroup(
    val day: LocalDate,
    val items: List<HistoryItem>,
    val total: BigDecimal,
)

/** A month header, the per-day groups recorded in it, and that month's total spend. */
data class MonthGroup(
    val month: YearMonth,
    val days: List<DayGroup>,
    val total: BigDecimal,
)

/**
 * Which History view is showing: whole receipts, individual items, or the read-only "Budgets"
 * snapshot of income + recurring payments (planning data mirrored from the Budget screen).
 */
enum class HistoryMode { RECEIPTS, ITEMS, BUDGETS }

/** A single calendar day's receipts, ordered by the active sort, with that day's total spend. */
data class ReceiptDayGroup(
    val day: LocalDate,
    val receipts: List<Receipt>,
    val total: BigDecimal,
)

/** A month header, the per-day receipt groups recorded in it, and that month's total spend. */
data class ReceiptMonthGroup(
    val month: YearMonth,
    val days: List<ReceiptDayGroup>,
    val total: BigDecimal,
)

/** The active search query plus the Category / Store / Date / Price filters. */
data class HistoryFilters(
    val query: String = "",
    val category: String? = null,
    val store: String? = null,
    val date: DateRangeFilter? = null,
    /** Inclusive line-total bounds; null means that end is unbounded. */
    val priceMin: BigDecimal? = null,
    val priceMax: BigDecimal? = null,
) {
    /** True when nothing is narrowing the list — used to highlight the "All" chip. */
    val isEmpty: Boolean
        get() = query.isBlank() && category == null && store == null && date == null &&
            priceMin == null && priceMax == null

    /** True when a price range is active (drives the Price chip's selected state + label). */
    val hasPrice: Boolean get() = priceMin != null || priceMax != null
}

data class HistoryUiState(
    // False only for the initial placeholder emitted before the first DB load; the screen shows just
    // its header until this flips true, so the "No transactions yet" empty state doesn't flash.
    val isLoaded: Boolean = false,
    val groups: List<MonthGroup> = emptyList(),
    /** The same data grouped by whole receipt (for the "Receipts" tab). */
    val receiptGroups: List<ReceiptMonthGroup> = emptyList(),
    val filters: HistoryFilters = HistoryFilters(),
    /** Current sort order applied to the list. */
    val sort: SortOrder = SortOrder.NEWEST,
    /** Distinct categories present across all transactions, for the Category dropdown. */
    val categories: List<String> = emptyList(),
    /** Distinct stores present across all receipts, for the Store dropdown. */
    val stores: List<String> = emptyList(),
    /** Most-used stores (by item count) shown as quick-find chips when the search is empty. */
    val topStores: List<String> = emptyList(),
    /** Most-used categories (by item count) shown as quick-find chips when the search is empty. */
    val topCategories: List<String> = emptyList(),
    /** Recent search terms, most-recent first, shown in the quick-find panel. */
    val recentSearches: List<String> = emptyList(),
    /** Upper bound for the price-range slider — the largest line total, rounded up to a tidy number. */
    val priceUpperBound: BigDecimal = DEFAULT_PRICE_BOUND,
    /** Whether the user has any transactions at all (distinguishes "empty" from "no matches"). */
    val hasAnyTransactions: Boolean = false,
    // ── Budgets tab: a snapshot of the money plan (income + recurring), mirrored from Budget ──
    /**
     * The concrete window the Budgets snapshot is scoped to. Shares the list's Date filter (see
     * [HistoryFilters.date]) so the window carries across all three tabs; when that's "All time"
     * (null) it falls back to the current month, since a plan needs a bounded span to scale.
     */
    val budgetPeriod: DateRangeFilter = DateRangeFilter.CURRENT_MONTH,
    /** Whether the user has any money plan at all — drives the empty state, ignoring the window. */
    val hasBudgetPlan: Boolean = false,
    /** Income sources active in [budgetPeriod] (one-offs only in their window), newest-added first. */
    val income: List<RecurringEntity> = emptyList(),
    /** Recurring payments (bills) active in [budgetPeriod], newest-added first. */
    val bills: List<RecurringEntity> = emptyList(),
    /** Income summed to a monthly-equivalent figure (the per-month rate shown on section headers). */
    val monthlyIncome: BigDecimal = BigDecimal.ZERO,
    /** Recurring bills summed to a monthly-equivalent figure. */
    val monthlyBills: BigDecimal = BigDecimal.ZERO,
    /** Income scaled to the whole [budgetPeriod] window (recurring × month-span; one-offs once). */
    val periodIncome: BigDecimal = BigDecimal.ZERO,
    /** Recurring bills scaled to the whole [budgetPeriod] window. */
    val periodBills: BigDecimal = BigDecimal.ZERO,
) {
    private companion object {
        val DEFAULT_PRICE_BOUND: BigDecimal = BigDecimal(100)
    }
}

class HistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val recurringRepository: RecurringRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    // One filter set drives all three tabs. The Receipts/Items lists read every field; the Budgets
    // snapshot reads only the Date window (below) — so choosing a date on any tab carries across.
    private val filters = MutableStateFlow(HistoryFilters())

    val uiState: StateFlow<HistoryUiState> =
        combine(
            transactionRepository.getAll(),
            receiptRepository.getAll(),
            recurringRepository.items,
            filters,
            settingsStore.settings,
        ) { transactions, receipts, recurring, activeFilters, settings ->
            // Transactions join to their receipt by the upload timestamp (see ReceiptEntity).
            // Normalize to the canonical brand so the list, the Store filter dropdown, and store
            // filtering all agree — and legacy receipts saved with a raw name collapse correctly.
            val storeByReceiptId = receipts.associate { it.timestamp to StoreNormalizer.normalize(it.store) }
            val items = transactions.map { txn ->
                HistoryItem(txn, storeByReceiptId[txn.receiptId].orEmpty())
            }

            // Dropdown options come from all data (not the filtered subset) so the user can always
            // switch between them.
            val categories = items.map { it.transaction.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            val stores = items.map { it.store }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val sort = runCatching { SortOrder.valueOf(settings.historySort) }.getOrDefault(SortOrder.NEWEST)

            // Budgets tab: split the recurring rows into income/bills, scoped to the selected window.
            // The window is the list's shared Date filter (so it carries across tabs); "All time"
            // (null) falls back to the current month, since a plan needs a bounded span to scale.
            // Recurring entries always apply; one-offs show only when they were added inside it, so the
            // date chip narrows the snapshot the way it narrows the Receipts/Items lists. Section
            // headers keep the per-month rate (monthlyAmount); the summary scales to the whole window.
            val period = activeFilters.date ?: DateRangeFilter.CURRENT_MONTH
            val (windowStart, windowEnd) = period.toRange()
            val span = period.monthSpan
            val inWindow = { r: RecurringEntity ->
                r.cadence != RecurringEntity.Cadence.ONCE || r.createdAt in windowStart..windowEnd
            }
            val incomeAll = recurring.filter { it.isIncome }
            val billsAll = recurring.filterNot { it.isIncome }
            val income = incomeAll.filter(inWindow)
            val bills = billsAll.filter(inWindow)

            HistoryUiState(
                isLoaded = true,
                groups = items.applyFilters(activeFilters).groupIntoMonths(sort),
                receiptGroups = items.buildReceipts(receipts).applyReceiptFilters(activeFilters).groupReceiptsIntoMonths(sort),
                filters = activeFilters,
                sort = sort,
                categories = categories,
                stores = stores,
                topStores = items.topBy { it.store }.take(8),
                topCategories = items.topBy { it.transaction.category }.take(8),
                recentSearches = settings.recentSearches,
                priceUpperBound = items.priceUpperBound(),
                hasAnyTransactions = transactions.isNotEmpty(),
                budgetPeriod = period,
                hasBudgetPlan = incomeAll.isNotEmpty() || billsAll.isNotEmpty(),
                income = income,
                bills = bills,
                monthlyIncome = income.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(windowStart, windowEnd) },
                monthlyBills = bills.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(windowStart, windowEnd) },
                periodIncome = income.fold(BigDecimal.ZERO) { a, r -> a + r.periodAmount(windowStart, windowEnd, span) },
                periodBills = bills.fold(BigDecimal.ZERO) { a, r -> a + r.periodAmount(windowStart, windowEnd, span) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun onQueryChange(query: String) = filters.update { it.copy(query = query) }
    fun onCategorySelected(category: String?) = filters.update { it.copy(category = category) }
    fun onStoreSelected(store: String?) = filters.update { it.copy(store = store) }
    fun onDateSelected(date: DateRangeFilter?) = filters.update { it.copy(date = date) }

    /**
     * Scopes the Budgets snapshot to [period]. This is the same shared Date filter the Receipts &
     * Items lists use, so choosing a window on any tab applies to all three (a plan always picks a
     * concrete period — the list's extra "All time" option maps to the current-month fallback).
     */
    fun onBudgetPeriodSelected(period: DateRangeFilter) = onDateSelected(period)

    /** Applies (or clears, when both are null) the price-range filter. */
    fun onPriceRangeSelected(min: BigDecimal?, max: BigDecimal?) =
        filters.update { it.copy(priceMin = min, priceMax = max) }

    fun onSortSelected(order: SortOrder) = settingsStore.setHistorySort(order.name)

    /** Records the term so it appears under "Recent searches" next time the field is empty. */
    fun commitRecentSearch(query: String) = settingsStore.addRecentSearch(query)
    fun removeRecentSearch(query: String) = settingsStore.removeRecentSearch(query)
    fun clearRecentSearches() = settingsStore.clearRecentSearches()

    /** Clears the search + filters (but keeps the chosen sort order and recent-search history). */
    fun clearFilters() { filters.value = HistoryFilters() }

    // --- Receipt detail sheet actions (mirror HomeViewModel so the shared sheet behaves identically) ---

    /** Deletes a whole receipt: all its line items plus the receipt metadata. Undoable. */
    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            val meta = receiptRepository.getById(receipt.id)
            lastDeleted = DeletedSnapshot(receipt.transactions, listOfNotNull(meta))
            transactionRepository.deleteByReceiptId(receipt.id)
            receiptRepository.deleteById(receipt.id)
        }
    }

    /** Deletes one line item; if it was the receipt's last item, drops the receipt too. Undoable. */
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val receipt = uiState.value.receiptGroups
                .asSequence()
                .flatMap { it.days.asSequence() }
                .flatMap { it.receipts.asSequence() }
                .find { it.id == transaction.receiptId }
            val wasLast = receipt != null && receipt.transactions.size <= 1
            val meta = if (wasLast) receiptRepository.getById(transaction.receiptId) else null
            lastDeleted = DeletedSnapshot(listOf(transaction), listOfNotNull(meta))
            transactionRepository.deleteById(transaction.id)
            if (wasLast) receiptRepository.deleteById(transaction.receiptId)
        }
    }

    /** Restores the most recently deleted receipt/line item with its original ids. */
    fun undoLastDelete() {
        val snapshot = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            transactionRepository.insertAll(snapshot.transactions)
            snapshot.receipts.forEach { receiptRepository.insert(it) }
        }
    }

    private var lastDeleted: DeletedSnapshot? = null

    private data class DeletedSnapshot(
        val transactions: List<TransactionEntity>,
        val receipts: List<ReceiptEntity>,
    )

    private fun List<HistoryItem>.applyFilters(f: HistoryFilters): List<HistoryItem> {
        val query = f.query.trim()
        val dateRange = f.date?.toRange()
        return filter { item ->
            val txn = item.transaction
            (query.isBlank() || txn.name.contains(query, ignoreCase = true)) &&
                (f.category == null || txn.category.equals(f.category, ignoreCase = true)) &&
                (f.store == null || item.store.equals(f.store, ignoreCase = true)) &&
                (dateRange == null || txn.timestamp in dateRange.first..dateRange.second) &&
                (f.priceMin == null || item.lineTotal >= f.priceMin) &&
                (f.priceMax == null || item.lineTotal <= f.priceMax)
        }
    }

    /** Distinct values of [selector] ordered by how often they occur (most-used first). */
    private fun List<HistoryItem>.topBy(selector: (HistoryItem) -> String): List<String> =
        filter { selector(it).isNotBlank() }
            .groupingBy(selector).eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }

    /** Largest line total across all items, rounded up to a tidy slider maximum (min 10). */
    private fun List<HistoryItem>.priceUpperBound(): BigDecimal {
        val max = maxOfOrNull { it.lineTotal } ?: BigDecimal.ZERO
        val ceiling = max.setScale(0, RoundingMode.CEILING).toLong()
        val step = when {
            ceiling <= 50 -> 5L
            ceiling <= 200 -> 10L
            ceiling <= 1000 -> 50L
            else -> 100L
        }
        val rounded = ((ceiling + step - 1) / step) * step
        return BigDecimal(rounded.coerceAtLeast(10L))
    }

    private fun List<HistoryItem>.groupIntoMonths(sort: SortOrder): List<MonthGroup> {
        val zone = ZoneId.systemDefault()
        // Newest/price keep the most-recent month on top; Oldest flips the whole thing chronological.
        val dateAscending = sort == SortOrder.OLDEST
        val itemComparator: Comparator<HistoryItem> = when (sort) {
            SortOrder.NEWEST -> compareByDescending { it.transaction.timestamp }
            SortOrder.OLDEST -> compareBy { it.transaction.timestamp }
            SortOrder.PRICE_HIGH -> compareByDescending { it.lineTotal }
            SortOrder.PRICE_LOW -> compareBy { it.lineTotal }
        }
        val monthComparator: Comparator<YearMonth> =
            if (dateAscending) compareBy { it } else compareByDescending { it }
        val dayComparator: Comparator<LocalDate> =
            if (dateAscending) compareBy { it } else compareByDescending { it }

        return groupBy { item ->
            YearMonth.from(Instant.ofEpochMilli(item.transaction.timestamp).atZone(zone))
        }
            .toSortedMap(monthComparator)
            .map { (month, monthItems) ->
                val days = monthItems
                    .groupBy { item ->
                        Instant.ofEpochMilli(item.transaction.timestamp).atZone(zone).toLocalDate()
                    }
                    .toSortedMap(dayComparator)
                    .map { (day, dayItems) ->
                        val ordered = dayItems.sortedWith(itemComparator)
                        DayGroup(day = day, items = ordered, total = ordered.sumOfSpend())
                    }
                MonthGroup(
                    month = month,
                    days = days,
                    total = days.fold(BigDecimal.ZERO) { acc, d -> acc + d.total },
                )
            }
    }

    private fun List<HistoryItem>.sumOfSpend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal }

    /** Re-assembles whole [Receipt]s from the line items (grouped by upload id), with each receipt's
     *  store (already normalized on the items), total, item list and saved discount. */
    private fun List<HistoryItem>.buildReceipts(meta: List<ReceiptEntity>): List<Receipt> {
        val metaById = meta.associateBy { it.timestamp }
        return groupBy { it.transaction.receiptId }.map { (receiptId, its) ->
            val receiptMeta = metaById[receiptId]
            val netSum = its.fold(BigDecimal.ZERO) { acc, i -> acc + i.lineTotal }
            // Anchor on the printed total: add on-top tax (tax-exclusive receipts) plus any extra
            // charges (delivery/service fees, a courier tip) so the receipt total equals what was paid —
            // item rows still show the printed net prices; a reconciling tax-inclusive receipt adds nothing.
            val addedCharges = (if (receiptMeta?.taxOnTop == true) receiptMeta.tax else BigDecimal.ZERO) +
                (receiptMeta?.extraCharges ?: BigDecimal.ZERO)
            Receipt(
                id = receiptId,
                store = its.firstOrNull()?.store.orEmpty(),
                transactions = its.map { it.transaction },
                timestamp = its.firstOrNull()?.transaction?.timestamp ?: receiptId,
                price = netSum + addedCharges,
                discount = receiptMeta?.discount ?: BigDecimal.ZERO,
                tax = receiptMeta?.tax ?: BigDecimal.ZERO,
            )
        }
    }

    /** Receipt-level filtering: a whole receipt shows if it matches (query hits store or any item;
     *  category/price match any item / the receipt total; store + date match the receipt itself). */
    private fun List<Receipt>.applyReceiptFilters(f: HistoryFilters): List<Receipt> {
        val query = f.query.trim()
        val dateRange = f.date?.toRange()
        return filter { r ->
            (query.isBlank() || r.store.contains(query, ignoreCase = true) ||
                r.transactions.any { it.name.contains(query, ignoreCase = true) }) &&
                (f.category == null || r.transactions.any { it.category.equals(f.category, ignoreCase = true) }) &&
                (f.store == null || r.store.equals(f.store, ignoreCase = true)) &&
                (dateRange == null || r.timestamp in dateRange.first..dateRange.second) &&
                (f.priceMin == null || r.paid >= f.priceMin) &&
                (f.priceMax == null || r.paid <= f.priceMax)
        }
    }

    private fun List<Receipt>.groupReceiptsIntoMonths(sort: SortOrder): List<ReceiptMonthGroup> {
        val zone = ZoneId.systemDefault()
        // Newest/price keep the most-recent month & day on top; Oldest flips the whole thing.
        val dateAscending = sort == SortOrder.OLDEST
        val comparator: Comparator<Receipt> = when (sort) {
            SortOrder.NEWEST -> compareByDescending { it.timestamp }
            SortOrder.OLDEST -> compareBy { it.timestamp }
            SortOrder.PRICE_HIGH -> compareByDescending { it.paid }
            SortOrder.PRICE_LOW -> compareBy { it.paid }
        }
        val monthComparator: Comparator<YearMonth> =
            if (dateAscending) compareBy { it } else compareByDescending { it }
        val dayComparator: Comparator<LocalDate> =
            if (dateAscending) compareBy { it } else compareByDescending { it }

        return groupBy { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) }
            .toSortedMap(monthComparator)
            .map { (month, monthReceipts) ->
                val days = monthReceipts
                    .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
                    .toSortedMap(dayComparator)
                    .map { (day, dayReceipts) ->
                        val ordered = dayReceipts.sortedWith(comparator)
                        ReceiptDayGroup(day, ordered, ordered.fold(BigDecimal.ZERO) { acc, r -> acc + r.paid })
                    }
                ReceiptMonthGroup(
                    month = month,
                    days = days,
                    total = days.fold(BigDecimal.ZERO) { acc, d -> acc + d.total },
                )
            }
    }
}
