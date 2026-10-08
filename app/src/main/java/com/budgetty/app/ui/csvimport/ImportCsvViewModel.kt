package com.budgetty.app.ui.csvimport

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.csvimport.CsvField
import com.budgetty.app.data.csvimport.CsvImport
import com.budgetty.app.data.csvimport.CsvTable
import com.budgetty.app.data.csvimport.RowKind
import com.budgetty.app.data.csvimport.SignConvention
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/** The four wizard steps: pick a file, map its columns, review options, then the summary. */
enum class ImportStep { PICK, MAP, REVIEW, DONE }

/** The date layout the CSV uses, chosen on the Review step; maps to a [CsvImport] pattern. */
enum class ImportDateFormat(val pattern: String, @param:StringRes val labelRes: Int) {
    DMY(CsvImport.PATTERN_DMY, R.string.import_date_dmy),
    MDY(CsvImport.PATTERN_MDY, R.string.import_date_mdy),
    ISO(CsvImport.PATTERN_ISO, R.string.import_date_iso),
}

/** A recoverable problem with the chosen file (shown in place of the file card on the Pick step). */
enum class ImportError { READ_FAILED, EMPTY_FILE }

/** One transaction the import will write once Review is confirmed. */
data class PlannedTxn(val dateMillis: Long, val name: String, val amount: BigDecimal, val category: String)

/** The parsed outcome of the whole file under the current options — drives the Review counts. */
data class ImportPlan(
    val toImport: List<PlannedTxn> = emptyList(),
    val duplicates: Int = 0,
    val income: Int = 0,
    val invalid: Int = 0,
    val filedAsOther: Int = 0,
    val minDate: Long? = null,
    val maxDate: Long? = null,
    val expenseTotal: BigDecimal = BigDecimal.ZERO,
)

/** What the summary step reports after the write completes. */
data class ImportResult(
    val imported: Int,
    val skippedDuplicates: Int,
    val income: Int,
    val invalid: Int,
    val filedAsOther: Int,
    val minDate: Long?,
    val maxDate: Long?,
    val expenseTotal: BigDecimal,
)

data class ImportUiState(
    val step: ImportStep = ImportStep.PICK,
    val fileName: String = "",
    val table: CsvTable = CsvTable(emptyList(), emptyList()),
    val mapping: List<CsvField> = emptyList(),
    val dateFormat: ImportDateFormat = ImportDateFormat.DMY,
    val sign: SignConvention = SignConvention.MINUS_IS_EXPENSE,
    val skipDuplicates: Boolean = true,
    val plan: ImportPlan? = null,
    val planning: Boolean = false,
    val importing: Boolean = false,
    val result: ImportResult? = null,
    val error: ImportError? = null,
) {
    val hasFile: Boolean get() = !table.isEmpty
    val mappingError: CsvImport.MappingError? get() = CsvImport.mappingError(mapping)
}

/**
 * Drives the CSV import wizard: reads a picked file into a [CsvTable], guesses a column mapping, plans
 * the import (parse + category match + de-dup against existing data) for Review, then writes every row
 * as one transaction + a paired manual receipt inside a single DB transaction. The written receipt ids
 * are kept so the summary's Undo removes exactly this batch. Import is free; no premium gate.
 */
class ImportCsvViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val db: UserDatabaseManager,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState(dateFormat = defaultDateFormat()))
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    /** Receipt ids written by the last import, so Undo can delete exactly this batch. */
    private var lastBatch: List<Long> = emptyList()

    /** Handles the SAF result: parse the text, guess a mapping, or surface a read/empty error. */
    fun onFileRead(fileName: String, text: String?) {
        if (text == null) {
            _state.update { it.copy(error = ImportError.READ_FAILED) }
            return
        }
        viewModelScope.launch {
            val table = withContext(Dispatchers.Default) { CsvImport.parse(text) }
            if (table.isEmpty || table.rows.isEmpty()) {
                _state.update { it.copy(error = ImportError.EMPTY_FILE, fileName = fileName) }
                return@launch
            }
            _state.update {
                it.copy(
                    fileName = fileName,
                    table = table,
                    mapping = CsvImport.guessMapping(table.headers),
                    error = null,
                )
            }
        }
    }

    /** Clears the chosen file, returning the Pick step to its empty state. */
    fun clearFile() {
        _state.update {
            it.copy(table = CsvTable(emptyList(), emptyList()), mapping = emptyList(), fileName = "", error = null)
        }
    }

    /** Cycles column [index] through Date → Amount → Store → Category → Ignore. */
    fun cycleColumn(index: Int) {
        _state.update { s ->
            val mapping = s.mapping.toMutableList()
            if (index in mapping.indices) {
                val next = CsvField.entries[(mapping[index].ordinal + 1) % CsvField.entries.size]
                mapping[index] = next
            }
            s.copy(mapping = mapping)
        }
    }

    fun goToMap() = _state.update { it.copy(step = ImportStep.MAP) }

    fun goToReview() {
        if (_state.value.mappingError != null) return
        _state.update { it.copy(step = ImportStep.REVIEW) }
        replan()
    }

    fun back() = _state.update {
        when (it.step) {
            ImportStep.REVIEW -> it.copy(step = ImportStep.MAP)
            ImportStep.MAP -> it.copy(step = ImportStep.PICK)
            else -> it
        }
    }

    fun setDateFormat(value: ImportDateFormat) { _state.update { it.copy(dateFormat = value) }; replan() }
    fun setSign(value: SignConvention) { _state.update { it.copy(sign = value) }; replan() }
    fun setSkipDuplicates(value: Boolean) { _state.update { it.copy(skipDuplicates = value) }; replan() }

    /** Re-parses the whole file against the current options; no-op unless the user is on Review. */
    private fun replan() {
        if (_state.value.step != ImportStep.REVIEW) return
        _state.update { it.copy(planning = true) }
        viewModelScope.launch {
            val plan = buildPlan()
            _state.update { it.copy(plan = plan, planning = false) }
        }
    }

    private suspend fun buildPlan(): ImportPlan {
        val s = _state.value
        val known = knownCategories()
        val existing = withContext(Dispatchers.Default) {
            transactionRepository.getAllOnce()
                .map { CsvImport.dedupKey(it.timestamp, it.price.multiply(BigDecimal(it.quantity)), it.name) }
                .toSet()
        }
        return withContext(Dispatchers.Default) {
            val seen = HashSet<String>()
            val toImport = ArrayList<PlannedTxn>()
            var duplicates = 0
            var income = 0
            var invalid = 0
            var filedAsOther = 0
            var minDate: Long? = null
            var maxDate: Long? = null
            var total = BigDecimal.ZERO
            for (row in s.table.rows) {
                val p = CsvImport.parseRow(row, s.mapping, s.dateFormat.pattern, s.sign)
                when (p.kind) {
                    RowKind.INVALID -> invalid++
                    RowKind.INCOME -> income++
                    RowKind.EXPENSE -> {
                        // kind == EXPENSE guarantees both are non-null (see CsvImport.parseRow).
                        val amount = p.amount
                        val date = p.dateMillis
                        if (amount != null && date != null) {
                            val key = CsvImport.dedupKey(date, amount, p.name)
                            if (s.skipDuplicates && (key in existing || key in seen)) {
                                duplicates++
                            } else {
                                seen.add(key)
                                val category = resolveCategory(p.category, known)
                                if (category == Categories.OTHER) filedAsOther++
                                toImport.add(PlannedTxn(date, p.name, amount, category))
                                total = total.add(amount)
                                minDate = if (minDate == null) date else minOf(minDate, date)
                                maxDate = if (maxDate == null) date else maxOf(maxDate, date)
                            }
                        }
                    }
                }
            }
            ImportPlan(toImport, duplicates, income, invalid, filedAsOther, minDate, maxDate, total)
        }
    }

    /** Writes the planned rows as transactions + paired manual receipts, atomically. */
    fun import() {
        val plan = _state.value.plan ?: return
        if (_state.value.importing) return
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val base = System.currentTimeMillis()
            val txns = ArrayList<TransactionEntity>(plan.toImport.size)
            val receipts = ArrayList<ReceiptEntity>(plan.toImport.size)
            plan.toImport.forEachIndexed { i, p ->
                val receiptId = base + i
                txns.add(
                    TransactionEntity(
                        name = p.name,
                        timestamp = p.dateMillis,
                        price = p.amount,
                        quantity = 1,
                        category = p.category,
                        receiptId = receiptId,
                    ),
                )
                receipts.add(
                    ReceiptEntity(
                        timestamp = receiptId,
                        store = p.name,
                        date = p.dateMillis,
                        discount = BigDecimal.ZERO,
                        isManual = true,
                    ),
                )
            }
            db.database.withTransaction {
                db.database.transactionDao().insertAll(txns)
                db.database.receiptDao().insertAll(receipts)
            }
            lastBatch = receipts.map { it.timestamp }
            _state.update {
                it.copy(
                    importing = false,
                    step = ImportStep.DONE,
                    result = ImportResult(
                        imported = plan.toImport.size,
                        skippedDuplicates = plan.duplicates,
                        income = plan.income,
                        invalid = plan.invalid,
                        filedAsOther = plan.filedAsOther,
                        minDate = plan.minDate,
                        maxDate = plan.maxDate,
                        expenseTotal = plan.expenseTotal,
                    ),
                )
            }
        }
    }

    /** Removes exactly the last imported batch (summary Undo), then resets the wizard. */
    fun undo() {
        val batch = lastBatch
        if (batch.isEmpty()) return
        viewModelScope.launch {
            db.database.withTransaction {
                batch.forEach { id ->
                    db.database.transactionDao().deleteByReceiptId(id)
                    db.database.receiptDao().deleteById(id)
                }
            }
            lastBatch = emptyList()
            _state.value = ImportUiState(dateFormat = defaultDateFormat())
        }
    }

    /** Lowercased CSV-category → canonical/custom display name, for matching the import's Category column. */
    private suspend fun knownCategories(): Map<String, String> {
        val custom = categoryRepository.categories.first().map { it.name }
        val all = Categories.predefined.map { it.name } + custom
        return all.associateBy { it.lowercase() }
    }

    private fun resolveCategory(raw: String, known: Map<String, String>): String {
        val key = raw.trim().lowercase()
        if (key.isEmpty()) return Categories.OTHER
        return known[key] ?: Categories.OTHER
    }

    private fun defaultDateFormat(): ImportDateFormat =
        when (settingsStore.settings.value.dateFormat) {
            DateFormatOption.MDY_SLASH -> ImportDateFormat.MDY
            DateFormatOption.ISO -> ImportDateFormat.ISO
            else -> ImportDateFormat.DMY
        }
}
