package com.budgetty.app.ui.buyinglimits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.analytics.Analytics
import com.budgetty.app.analytics.LimitSource
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.CountableItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Traffic-light state of a limit: under the cap, exactly at it, or over. */
enum class BuyingLimitStatus { ON_TRACK, AT_LIMIT, OVER }

/** One row on the Buying limits screen: the saved limit plus its derived bought-count and status. */
data class BuyingLimitCardUi(
    val limit: BuyingLimitEntity,
    /** Σ quantity of matching items in the limit's current window. */
    val bought: Int,
) {
    val status: BuyingLimitStatus
        get() = when {
            bought > limit.count -> BuyingLimitStatus.OVER
            bought == limit.count -> BuyingLimitStatus.AT_LIMIT
            else -> BuyingLimitStatus.ON_TRACK
        }
}

data class BuyingLimitsUiState(
    val limits: List<BuyingLimitCardUi> = emptyList(),
    /** The saved items, for the editor's live match preview (counts quantity, not rows). */
    val items: List<CountableItem> = emptyList(),
    val monthStartDay: Int = 1,
    val isPremium: Boolean = false,
    /** False until the first DB emission, so the empty state doesn't flash on cold start. */
    val isLoaded: Boolean = false,
) {
    /** True for a free user already at the 1-limit cap: the Add row locks and routes to the paywall. */
    val atCap: Boolean get() = !isPremium && limits.size >= BuyingLimitsRepository.FREE_LIMIT
}

/**
 * Backs the "Buying limits" management screen (Account → Buying limits): each saved limit annotated
 * with how many matching items were bought in its current window, plus add/edit/delete. The count is
 * derived here — never stored — on the same normalized substring rule the save-time nudge uses, so the
 * card and the nudge always agree.
 */
class BuyingLimitsViewModel(
    private val repository: BuyingLimitsRepository,
    transactionRepository: TransactionRepository,
    settingsStore: SettingsStore,
    billingManager: BillingManager,
    private val analytics: Analytics,
) : ViewModel() {

    val uiState: StateFlow<BuyingLimitsUiState> = combine(
        repository.limits,
        transactionRepository.getAll(),
        settingsStore.settings.map { it.monthStartDay },
        billingManager.isPremium,
    ) { limits, transactions, monthStartDay, isPremium ->
        val items = transactions.map { CountableItem(it.name, it.quantity, it.timestamp) }
        val today = LocalDate.now()
        val rows = limits.map { limit ->
            BuyingLimitCardUi(
                limit = limit,
                bought = BuyingLimitCounter.count(items, limit.keywordList, limit.timeframe, today, monthStartDay),
            )
        }
        BuyingLimitsUiState(
            limits = rows,
            items = items,
            monthStartDay = monthStartDay,
            isPremium = isPremium,
            isLoaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BuyingLimitsUiState(),
    )

    /**
     * Creates ([id] null) or updates a limit. Keywords are normalized + de-duplicated on the way in;
     * [count] is floored at 1. A blank-keyword save is ignored (the editor only enables Save with at
     * least one keyword). Editing preserves the original creation time so the list order is stable.
     */
    fun saveLimit(
        id: Long?,
        emoji: String,
        label: String,
        keywords: List<String>,
        timeframe: BuyingLimitTimeframe,
        count: Int,
    ) {
        val joined = BuyingLimitEntity.joinKeywords(keywords)
        if (joined.isEmpty()) return
        val isNew = id == null
        viewModelScope.launch {
            val existing = id?.let { key -> uiState.value.limits.firstOrNull { it.limit.id == key }?.limit }
            repository.upsert(
                BuyingLimitEntity(
                    id = id ?: 0L,
                    emoji = emoji.trim(),
                    label = label.trim(),
                    keywords = joined,
                    timeframe = timeframe,
                    count = count.coerceAtLeast(1),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            // A brand-new limit (not an edit). The suggestion-sourced path arrives with §4.4; until
            // then every create from the editor is manual.
            if (isNew) analytics.logLimitCreated(LimitSource.MANUAL)
        }
    }

    fun deleteLimit(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
