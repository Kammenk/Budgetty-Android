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
import com.budgetty.app.ui.streaks.LimitStreakInput
import com.budgetty.app.ui.streaks.LimitWindow
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.BuyingLimitSuggestions
import com.budgetty.app.ui.util.CountableItem
import com.budgetty.app.ui.util.LimitSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Traffic-light state of a limit: under the cap, exactly at it, or over. */
enum class BuyingLimitStatus { ON_TRACK, AT_LIMIT, OVER }

/**
 * One row on the Buying limits screen: the saved limit plus its derived bought-count and status, the
 * per-limit streak (§4.2) and the last-N-closed-windows history (§4.3). The streak and history are both
 * derived from the SAME closed-window list ([BuyingLimitCounter.closedWindows]) so they can never
 * disagree — nothing is stored.
 */
data class BuyingLimitCardUi(
    val limit: BuyingLimitEntity,
    /** Σ quantity of matching items in the limit's current window. */
    val bought: Int,
    /** Consecutive/best closed windows under cap; surfaced (current ≥ 2) or best-run fallback on the card. */
    val streak: Streak = Streak(StreakKind.LIMIT, "", 0, 0, 0, false),
    /** The last [HISTORY_WINDOWS] CLOSED windows, most-recent first — the history strip (§4.3). */
    val history: List<LimitWindow> = emptyList(),
) {
    val status: BuyingLimitStatus
        get() = when {
            bought > limit.count -> BuyingLimitStatus.OVER
            bought == limit.count -> BuyingLimitStatus.AT_LIMIT
            else -> BuyingLimitStatus.ON_TRACK
        }

    /** How many over the cap (0 unless [BuyingLimitStatus.OVER]) — the N in "Over by N" (§4.1). */
    val overBy: Int get() = (bought - limit.count).coerceAtLeast(0)

    /** Closed windows in [history] that stayed at or under the cap (only windows that held receipts count). */
    val historyMet: Int get() = history.count { it.hasData && it.count <= limit.count }

    /** True once at least one closed window held receipts — gates the strip so a brand-new limit shows none. */
    val hasHistory: Boolean get() = history.any { it.hasData }
}

data class BuyingLimitsUiState(
    val limits: List<BuyingLimitCardUi> = emptyList(),
    /** The saved items, for the editor's live match preview (counts quantity, not rows). */
    val items: List<CountableItem> = emptyList(),
    /** Frequency-derived suggestions the user hasn't dismissed or already covered (§4.4). */
    val suggestions: List<LimitSuggestion> = emptyList(),
    val monthStartDay: Int = 1,
    val isPremium: Boolean = false,
    /** False until the first DB emission, so the empty state doesn't flash on cold start. */
    val isLoaded: Boolean = false,
) {
    /** True for a free user already at the [BuyingLimitsRepository.FREE_LIMIT] cap: Add locks → paywall. */
    val atCap: Boolean get() = !isPremium && limits.size >= BuyingLimitsRepository.FREE_LIMIT
}

/**
 * Backs the "Buying limits" management screen (Account → Buying limits): each saved limit annotated
 * with how many matching items were bought in its current window, its under-cap streak and an 8-window
 * history strip, plus frequency-derived suggestions and add/edit/delete. All counts are derived here —
 * never stored — on the same normalized substring rule the save-time nudge uses, so the card and the
 * nudge always agree. The heavier per-limit window walk runs on [Dispatchers.Default].
 */
class BuyingLimitsViewModel(
    private val repository: BuyingLimitsRepository,
    transactionRepository: TransactionRepository,
    private val settingsStore: SettingsStore,
    billingManager: BillingManager,
    private val analytics: Analytics,
) : ViewModel() {

    val uiState: StateFlow<BuyingLimitsUiState> = combine(
        repository.limits,
        transactionRepository.getAll(),
        settingsStore.settings,
        billingManager.isPremium,
    ) { limits, transactions, settings, isPremium ->
        val items = transactions.map { CountableItem(it.name, it.quantity, it.timestamp) }
        val today = LocalDate.now()
        val monthStartDay = settings.monthStartDay
        val rows = limits.map { limit -> cardFor(limit, items, today, monthStartDay) }
        val existingKeywords = limits.flatMap { it.keywordList }
        BuyingLimitsUiState(
            limits = rows,
            items = items,
            suggestions = BuyingLimitSuggestions.suggest(
                items = items,
                existingKeywords = existingKeywords,
                dismissed = settings.dismissedLimitSuggestions,
                today = today,
            ),
            monthStartDay = monthStartDay,
            isPremium = isPremium,
            isLoaded = true,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BuyingLimitsUiState(),
    )

    /**
     * One card: the live window count, plus the last [HISTORY_WINDOWS] closed windows derived once and
     * shared by the history strip and the streak (§4.3). The live window feeds only [Streak.liveOnTrack].
     */
    private fun cardFor(
        limit: BuyingLimitEntity,
        items: List<CountableItem>,
        today: LocalDate,
        monthStartDay: Int,
    ): BuyingLimitCardUi {
        val keywords = limit.keywordList
        val bought = BuyingLimitCounter.count(items, keywords, limit.timeframe, today, monthStartDay)
        val windows = BuyingLimitCounter.closedWindows(
            items, keywords, limit.timeframe, HISTORY_WINDOWS, today, monthStartDay,
        )
        val (liveStart, liveEnd) = BuyingLimitCounter.window(limit.timeframe, today, monthStartDay)
        val streak = StreakEngine.limitStreak(
            LimitStreakInput(
                label = limit.displayTitle,
                cap = limit.count,
                closedWindows = windows,
                live = LimitWindow(count = bought, hasData = items.any { it.timestamp in liveStart..liveEnd }),
            ),
        )
        return BuyingLimitCardUi(limit = limit, bought = bought, streak = streak, history = windows)
    }

    /**
     * Creates ([id] null) or updates a limit. Keywords are normalized + de-duplicated on the way in;
     * [count] is floored at 1. A blank-keyword save is ignored (the editor only enables Save with at
     * least one keyword). Editing preserves the original creation time so the list order is stable.
     * [source] records whether a brand-new limit came from the editor ([LimitSource.MANUAL]) or a
     * tapped suggestion ([LimitSource.SUGGESTION]).
     */
    fun saveLimit(
        id: Long?,
        emoji: String,
        label: String,
        keywords: List<String>,
        timeframe: BuyingLimitTimeframe,
        count: Int,
        source: LimitSource = LimitSource.MANUAL,
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
            if (isNew) analytics.logLimitCreated(source)
        }
    }

    fun deleteLimit(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Dismisses a suggestion for good by its normalized keyword (§4.4). */
    fun dismissSuggestion(keyword: String) = settingsStore.dismissLimitSuggestion(keyword)

    /** A limit card's under-cap streak caption became visible (§4.2). Fired once per shown streak length. */
    fun onLimitStreakSurfaced(length: Int) = analytics.logStreakSurfaced(StreakKind.LIMIT, length)

    companion object {
        /** How many CLOSED windows the history strip shows and the streak walks (§4.3). */
        const val HISTORY_WINDOWS = 8
    }
}
