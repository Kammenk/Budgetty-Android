package com.budgetty.app.widget

import android.content.Context
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the home-screen widgets in sync while the app process is alive: whenever budgets,
 * transactions, the currency, or the Premium entitlement change, every widget is re-rendered via
 * Glance's `updateAll`. Started once from [com.budgetty.app.BudgettyApplication]. (Rollover while
 * the app is backgrounded is handled by each receiver's periodic `APPWIDGET_UPDATE` tick.)
 */
class WidgetUpdater(
    private val context: Context,
    private val scope: CoroutineScope,
    budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    settingsStore: SettingsStore,
    billingManager: BillingManager,
) {
    private val triggers = combine(
        budgetRepository.budgets,
        transactionRepository.getAll(),
        settingsStore.settings.map { it.currency }.distinctUntilChanged(),
        // Buying Premium lifts the widget cap, so every locked widget has to redraw as itself.
        // No distinctUntilChanged: it's a StateFlow, which already conflates equal values.
        billingManager.isPremium,
    ) { _, _, _, _ -> Unit }

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            triggers
                .drop(1) // skip the initial replay; the system already draws widgets on placement
                .debounce(400)
                .collect { refresh() }
        }
    }

    private suspend fun refresh() = WidgetRefresh.refreshAll(context)
}
