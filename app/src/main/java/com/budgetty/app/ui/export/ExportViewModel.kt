package com.budgetty.app.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.export.ExportSource
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.AppFormats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Feeds the export sheet: all-time transactions/receipts/income + the pay-cycle day and currency. */
class ExportViewModel(
    transactionRepository: TransactionRepository,
    receiptRepository: ReceiptRepository,
    recurringRepository: RecurringRepository,
    settingsStore: SettingsStore,
    billingManager: BillingManager,
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    val source: StateFlow<ExportSource> =
        combine(
            transactionRepository.getAll(),
            receiptRepository.getAll(),
            recurringRepository.items,
            settingsStore.settings.map { it.monthStartDay },
        ) { transactions, receipts, recurring, monthStartDay ->
            ExportSource(
                transactions = transactions,
                receiptsById = receipts.associateBy { it.timestamp },
                incomeRecurring = recurring.filter { it.isIncome },
                currencySymbol = AppFormats.currencySymbol,
                monthStartDay = monthStartDay,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportSource())
}
