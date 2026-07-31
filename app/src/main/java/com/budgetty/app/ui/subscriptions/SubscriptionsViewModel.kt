package com.budgetty.app.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.SubscriptionsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.util.DetectedSubscription
import com.budgetty.app.ui.util.MerchantCharge
import com.budgetty.app.ui.util.SubscriptionCadence
import com.budgetty.app.ui.util.SubscriptionDetector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** A merchant that's close to being detected — has a couple of charges but not yet [MIN_CHARGES]. */
data class NearMiss(val merchant: String, val count: Int)

data class SubscriptionsUiState(
    val loaded: Boolean = false,
    val isPremium: Boolean = false,
    /** Detected subscriptions the user hasn't ignored, most expensive first. */
    val detected: List<DetectedSubscription> = emptyList(),
    /** Detected-but-ignored, for the quiet "Ignored" group. */
    val ignored: List<DetectedSubscription> = emptyList(),
    val monthlyTotal: BigDecimal = BigDecimal.ZERO,
    val yearlyTotal: BigDecimal = BigDecimal.ZERO,
    val receiptCount: Int = 0,
    val nearestMiss: NearMiss? = null,
)

/**
 * Detects recurring merchants on-device from the app's own receipts (see [SubscriptionDetector]) and
 * exposes them for the Insights entry card, the list and the detail. Also owns ignore/restore and the
 * "track as bill" handoff that creates a recurring bill (which then feeds Safe to spend).
 */
class SubscriptionsViewModel(
    transactionRepository: TransactionRepository,
    receiptRepository: ReceiptRepository,
    subscriptionsRepository: SubscriptionsRepository,
    private val recurringRepository: RecurringRepository,
    billingManager: BillingManager,
) : ViewModel() {

    private val subs = subscriptionsRepository

    val uiState: StateFlow<SubscriptionsUiState> =
        combine(
            transactionRepository.getAll(),
            receiptRepository.getAll(),
            subscriptionsRepository.ignored,
            billingManager.isPremium,
        ) { transactions, receipts, ignoredRows, isPremium ->
            val charges = buildCharges(transactions, receipts)
            val all = SubscriptionDetector.detect(charges)
            val ignoredSet = ignoredRows.map { it.merchant }.toSet()
            val detected = all.filterNot { it.merchant in ignoredSet }
            val ignored = all.filter { it.merchant in ignoredSet }
            val monthly = detected.fold(BigDecimal.ZERO) { acc, s -> acc + s.monthlyEquivalent }
            SubscriptionsUiState(
                loaded = true,
                isPremium = isPremium,
                detected = detected,
                ignored = ignored,
                monthlyTotal = monthly,
                yearlyTotal = monthly.multiply(BigDecimal(12)),
                receiptCount = charges.size,
                nearestMiss = nearestMiss(charges, all.map { it.merchant }.toSet()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    fun ignore(merchant: String) {
        viewModelScope.launch { subs.ignore(merchant, System.currentTimeMillis()) }
    }

    fun restore(merchant: String) {
        viewModelScope.launch { subs.restore(merchant) }
    }

    /** Creates a recurring bill from a detected subscription; it then counts in Safe to spend. */
    fun trackAsBill(subscription: DetectedSubscription, amount: BigDecimal, dueDay: Int) {
        viewModelScope.launch {
            recurringRepository.upsert(
                RecurringEntity(
                    label = subscription.merchant,
                    amount = amount,
                    isIncome = false,
                    category = SUBSCRIPTION_CATEGORY,
                    cadence = if (subscription.cadence == SubscriptionCadence.YEARLY) {
                        RecurringEntity.Cadence.YEARLY
                    } else {
                        RecurringEntity.Cadence.MONTHLY
                    },
                    dueDay = dueDay,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** One [MerchantCharge] per receipt: normalized store, the receipt's paid total, its date. */
    private fun buildCharges(
        transactions: List<TransactionEntity>,
        receipts: List<ReceiptEntity>,
    ): List<MerchantCharge> {
        val receiptsById = receipts.associateBy { it.timestamp }
        return transactions.groupBy { it.receiptId }.mapNotNull { (receiptId, group) ->
            val store = StoreNormalizer.normalize(receiptsById[receiptId]?.store.orEmpty())
            if (store.isBlank()) return@mapNotNull null
            val net = group.fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }
            val total = net + paidAdjustmentOf(group, receiptsById)
            if (total.signum() <= 0) return@mapNotNull null
            val category = group.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key
                ?: group.first().category
            MerchantCharge(
                merchant = store,
                emoji = Categories.emojiOf(category),
                amount = total,
                dateMillis = group.firstOrNull()?.timestamp ?: receiptId,
            )
        }
    }

    /** The un-detected merchant with the most charges (2..MIN−1), to nudge "one more and we'll flag it". */
    private fun nearestMiss(charges: List<MerchantCharge>, detectedMerchants: Set<String>): NearMiss? =
        charges.groupBy { it.merchant }
            .filterKeys { it !in detectedMerchants }
            .mapValues { it.value.size }
            .filterValues { it in 2 until SubscriptionDetector.MIN_CHARGES }
            .maxByOrNull { it.value }
            ?.let { NearMiss(it.key, it.value) }

    private companion object {
        const val SUBSCRIPTION_CATEGORY = "Subscriptions"
    }
}
