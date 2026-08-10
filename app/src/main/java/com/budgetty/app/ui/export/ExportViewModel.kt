package com.budgetty.app.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.export.DataExporter
import com.budgetty.app.data.export.ExportBuilder
import com.budgetty.app.data.export.ExportData
import com.budgetty.app.data.export.ExportSource
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.AppFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val _exporting = MutableStateFlow(false)

    /** True while a file is being generated, so the UI can disable the Export button. */
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    /**
     * Builds the export file off the main thread — a multi-page PDF render or the CSV write over an
     * all-time dataset is heavy enough to ANR — then delivers the finished [File] on the main thread
     * via [onReady], where the caller turns it into a share Intent (it owns the Context/FileProvider).
     * The busy flag makes a double-tap a no-op so a second export can't start on top of the first.
     * Runs on [viewModelScope] so it survives the sheet being recomposed or dismissed mid-render.
     */
    fun export(
        data: ExportData,
        isPdf: Boolean,
        fileBaseName: String,
        cacheDir: File,
        onReady: (file: File, mimeType: String) -> Unit,
        onError: () -> Unit = {},
    ) {
        if (_exporting.value) return
        _exporting.value = true
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "exports").apply { mkdirs() }
                    if (isPdf) {
                        File(dir, "$fileBaseName.pdf").also { DataExporter.renderPdf(data, it) } to "application/pdf"
                    } else {
                        File(dir, "$fileBaseName.csv").also { it.writeText(ExportBuilder.toCsv(data)) } to "text/csv"
                    }
                }
            }
            _exporting.value = false
            result.fold(
                onSuccess = { (file, mime) -> onReady(file, mime) },
                onFailure = { onError() },
            )
        }
    }
}
