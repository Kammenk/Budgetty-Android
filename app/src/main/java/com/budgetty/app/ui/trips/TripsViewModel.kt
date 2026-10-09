package com.budgetty.app.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.TransactionTagEntity
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TagRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One category's share of a trip's spending — an emoji mini-bar row on the trip summary. */
data class TripCategoryStat(
    val name: String,
    val emoji: String,
    val color: Int,
    val amount: BigDecimal,
    /** Width relative to the trip's top category (1.0 = the largest), for the row bar. */
    val fraction: Float,
)

/** A trip with everything the screen draws: its spend, expense count, pace maths and top categories. */
data class TripCard(
    val trip: TripEntity,
    val spent: BigDecimal,
    val expenseCount: Int,
    val stats: TripStatsResult,
    val topCategories: List<TripCategoryStat>,
)

/** [isLoaded] holds the empty/active split blank until the first DB emission (loading-state gate). */
data class TripsUiState(
    val isLoaded: Boolean = false,
    val active: TripCard? = null,
    val past: List<TripCard> = emptyList(),
)

/**
 * Account → Trips (Travel mode). A trip is metadata over a tag ([TripEntity]); its spend is summed
 * live from the transactions carrying that tag — the same net-spend maths (line totals plus each
 * receipt's tax/fees less its discount) the rest of the app shows — so nothing is denormalised and
 * ending a trip leaves every figure intact. There is at most one active trip; the rest are past.
 */
class TripsViewModel(
    private val tripRepository: TripRepository,
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    val uiState: StateFlow<TripsUiState> = combine(
        tripRepository.trips,
        transactionRepository.getAll(),
        tagRepository.allLinks,
        receiptRepository.getAll(),
    ) { trips, transactions, links, receipts ->
        val today = LocalDate.now()
        val receiptsById = receipts.associateBy { it.timestamp }
        val txnById = transactions.associateBy { it.id }
        val idsByTag = links.groupBy({ it.tagName }, { it.transactionId })
        val cards = trips.map { trip ->
            val txns = idsByTag[trip.tag].orEmpty().toSet().mapNotNull { txnById[it] }
            card(trip, txns, receiptsById, today)
        }
        TripsUiState(
            isLoaded = true,
            active = cards.firstOrNull { it.trip.active },
            past = cards.filterNot { it.trip.active },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    /** Transaction timestamps, live — lets the start sheet count the expenses a chosen start date
     *  would back-fill into the new trip. */
    val transactionTimestamps: StateFlow<List<Long>> = transactionRepository.getAll()
        .map { txns -> txns.map { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun card(
        trip: TripEntity,
        txns: List<TransactionEntity>,
        receiptsById: Map<Long, ReceiptEntity>,
        today: LocalDate,
    ): TripCard {
        val spent = txns.spend() + paidAdjustmentOf(txns, receiptsById)
        val byCategory = txns.groupBy { it.category }
            .map { (name, rows) -> name to rows.spend() }
            .filter { it.second.signum() > 0 }
            .sortedByDescending { it.second }
            .take(TOP_CATEGORIES)
        val largest = byCategory.firstOrNull()?.second ?: BigDecimal.ONE
        val topCategories = byCategory.map { (name, amount) ->
            TripCategoryStat(
                name = name,
                emoji = Categories.emojiOf(name),
                color = Categories.colorOf(name),
                amount = amount,
                fraction = if (largest.signum() > 0) {
                    (amount.toDouble() / largest.toDouble()).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
            )
        }
        return TripCard(trip, spent, txns.size, TripStats.compute(trip, spent, today), topCategories)
    }

    /**
     * Starts a trip: derive its tag from the name + year, make sure that tag is in the catalog, start
     * it (ending any other active trip), then — if asked — back-fill by tagging every expense from the
     * start date onward so a trip begun a few days late still counts what came before.
     */
    fun start(
        name: String,
        startMillis: Long?,
        endMillis: Long?,
        budget: BigDecimal?,
        backfill: Boolean,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val tag = tripTag(trimmed, startMillis)
            tagRepository.ensureTags(listOf(tag))
            tripRepository.start(
                TripEntity(
                    name = trimmed,
                    tag = tag,
                    startDate = startMillis,
                    endDate = endMillis,
                    budgetAmount = budget,
                    active = true,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            if (backfill && startMillis != null) {
                val since = transactionRepository.getAllOnce().filter { it.timestamp >= startMillis }
                tagRepository.link(since.map { TransactionTagEntity(it.id, tag) })
            }
        }
    }

    /** Ends the active trip — new expenses stop being tagged; nothing already tagged is touched. */
    fun endActive() {
        viewModelScope.launch { tripRepository.endActive(System.currentTimeMillis()) }
    }

    /** Removes a trip's metadata. Its tag and the expenses carrying it stay (find them under Tags). */
    fun delete(id: Long) {
        viewModelScope.launch { tripRepository.delete(id) }
    }

    /** The trip's tag, stamped with the start year (or this year) — see [TripEntity.tagFor]. */
    private fun tripTag(name: String, startMillis: Long?): String {
        val year = startMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().year }
            ?: LocalDate.now().year
        return TripEntity.tagFor(name, year)
    }

    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    private companion object {
        const val TOP_CATEGORIES = 4
    }
}
