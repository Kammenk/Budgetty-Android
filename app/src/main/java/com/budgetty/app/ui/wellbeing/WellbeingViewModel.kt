package com.budgetty.app.ui.wellbeing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Which cadence the screen is showing. */
enum class WellbeingMode { MONTHLY, WEEKLY }

data class WellbeingUiState(
    val loaded: Boolean = false,
    val summary: WellbeingSummary? = null,
    /** Monthly tips with the user's dismissals for this period already removed. */
    val monthlyTips: List<WellbeingTip> = emptyList(),
    /** Weekly tips with dismissals removed. */
    val weeklyTips: List<WellbeingTip> = emptyList(),
)

/**
 * Backs the full-screen Wellbeing destination. Reads the shared [WellbeingProvider] summary and drops
 * tips the user dismissed this pay-cycle month (the dismissal set is the only persisted state). The
 * Weekly/Monthly mode and the open component are UI state held by the screen, not here.
 */
class WellbeingViewModel(
    provider: WellbeingProvider,
    private val settings: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<WellbeingUiState> =
        combine(provider.summary(), settings.settings) { summary, s ->
            val dismissed = s.dismissedWellbeingTips
            fun visible(tips: List<WellbeingTip>) =
                tips.filterNot { "${summary.periodId}|${it.id}" in dismissed }
            WellbeingUiState(
                loaded = true,
                summary = summary,
                monthlyTips = visible(summary.monthlyTips),
                weeklyTips = visible(summary.weeklyTips),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WellbeingUiState())

    /** Dismisses a tip for the current period; it resurfaces next month if it still qualifies. */
    fun dismiss(tipId: String) {
        val periodId = uiState.value.summary?.periodId ?: return
        settings.dismissWellbeingTip("$periodId|$tipId")
    }
}
