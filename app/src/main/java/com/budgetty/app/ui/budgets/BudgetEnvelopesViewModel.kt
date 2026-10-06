package com.budgetty.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetEnvelopeRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One envelope with its live spend + derived pace, for the list. */
data class EnvelopeCardUi(
    val entity: BudgetEnvelopeEntity,
    val spent: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val pace: PaceResult,
)

data class EnvelopesUiState(
    val isLoaded: Boolean = false,
    val isPremium: Boolean = false,
    val envelopes: List<EnvelopeCardUi> = emptyList(),
) {
    val count: Int get() = envelopes.size
    /** A free user at the extra-budget cap: New budget routes to the paywall. */
    val atCap: Boolean get() = !isPremium && count >= BudgetEnvelopeRepository.FREE_LIMIT
}

/** A pickable category for an envelope's scope (name + emoji). */
data class CategoryOption(val name: String, val emoji: String)

/**
 * Multiple named budgets ("envelopes"): each one's spend is summed live from transactions in its date
 * window matching its category scope (same net-spend + paid-adjustment math as the main budget), and
 * its pace is derived by [BudgetPace]. The free tier allows [BudgetEnvelopeRepository.FREE_LIMIT] extra
 * budgets; Premium is unlimited.
 */
class BudgetEnvelopesViewModel(
    private val repository: BudgetEnvelopeRepository,
    private val transactionRepository: TransactionRepository,
    receiptRepository: ReceiptRepository,
    categoryRepository: CategoryRepository,
    billingManager: BillingManager,
) : ViewModel() {

    val uiState: StateFlow<EnvelopesUiState> =
        combine(
            repository.envelopes,
            transactionRepository.getAll(),
            receiptRepository.getAll(),
            billingManager.isPremium,
        ) { envelopes, transactions, receipts, premium ->
            build(envelopes, transactions, receipts.associateBy { it.timestamp }, premium)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            EnvelopesUiState(),
        )

    /** All categories selectable as an envelope scope: predefined set + the user's custom ones. */
    val categoryOptions: StateFlow<List<CategoryOption>> =
        categoryRepository.categories.map { custom ->
            val predefined = Categories.predefined.map { CategoryOption(it.name, it.emoji) }
            val customOptions = custom.filter { it.isCustom }
                .map { CategoryOption(it.name, Categories.emojiOf(it.name)) }
            (predefined + customOptions).distinctBy { it.name }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun build(
        envelopes: List<BudgetEnvelopeEntity>,
        transactions: List<TransactionEntity>,
        receiptsById: Map<Long, com.budgetty.app.data.local.ReceiptEntity>,
        premium: Boolean,
    ): EnvelopesUiState {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val cards = envelopes.map { env ->
            val startD = Instant.ofEpochMilli(env.startDate).atZone(zone).toLocalDate()
            val endD = Instant.ofEpochMilli(env.endDate).atZone(zone).toLocalDate()
            val endMs = endD.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val scope = env.categoryList().toSet()
            val inRange = transactions.filter {
                it.timestamp in env.startDate..endMs && (env.isAllSpending || it.category in scope)
            }
            val spent = inRange.spend().add(paidAdjustmentOf(inRange, receiptsById))
            EnvelopeCardUi(env, spent, startD, endD, BudgetPace.compute(spent, env.limitAmount, startD, endD, today))
        }
        return EnvelopesUiState(isLoaded = true, isPremium = premium, envelopes = cards)
    }

    /** Creates or updates an envelope; [original] is the row being edited (null when adding). */
    fun save(
        original: BudgetEnvelopeEntity?,
        name: String,
        emoji: String,
        limitText: String,
        startMillis: Long,
        endMillis: Long,
        categories: List<String>,
    ) {
        val limit = limitText.replace(',', '.').trim().toBigDecimalOrNull() ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || limit.signum() <= 0) return
        val base = original ?: BudgetEnvelopeEntity(
            name = trimmed,
            limitAmount = limit,
            startDate = startMillis,
            endDate = endMillis,
            createdAt = System.currentTimeMillis(),
        )
        val entity = base.copy(
            name = trimmed,
            emoji = emoji.ifBlank { "🧾" },
            limitAmount = limit,
            startDate = startMillis,
            endDate = endMillis,
            categories = categories.joinToString("\n"),
        )
        viewModelScope.launch { repository.upsert(entity) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc.add(t.price.multiply(BigDecimal(t.quantity))) }
}
