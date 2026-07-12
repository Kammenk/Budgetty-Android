package com.budgetty.app.ui.upload

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.ingest.ParsedTransaction
import com.budgetty.app.data.ingest.ReceiptIngestManager
import com.budgetty.app.data.ingest.ReceiptUnreadableException
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.store.StoreNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class UploadStage { IDLE, EXTRACTING, REVIEW, SAVING, DONE }

/**
 * Why an extraction failed, so the error screen can give the right advice:
 * [UNREADABLE] = the photo itself couldn't be read (retake it); [SERVICE] = a backend/network
 * failure reaching the scanner (try again later), e.g. the Cloud Function proxy erroring or the
 * upstream API being out of credits.
 */
enum class UploadErrorKind { UNREADABLE, SERVICE }

data class UploadUiState(
    val stage: UploadStage = UploadStage.IDLE,
    val transactions: List<ParsedTransaction> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    /** Whether the user has Premium — gates how many custom categories they can create. */
    val isPremium: Boolean = false,
    val storeName: String = "",
    val receiptDate: Long = System.currentTimeMillis(),
    val discount: BigDecimal = BigDecimal.ZERO,
    /** Tax/VAT for the receipt; shown as "incl. VAT". 0 = hidden. Added on top of the item sum when [taxOnTop]. */
    val tax: BigDecimal = BigDecimal.ZERO,
    /** True for a tax-exclusive receipt: [tax] is added on top of the (printed) item prices, not contained in them. */
    val taxOnTop: Boolean = false,
    /** Money paid beyond items, discount and tax (delivery/service fees, a courier tip); added to the total, not itemized. */
    val extraCharges: BigDecimal = BigDecimal.ZERO,
    /** What the items should sum to per the receipt (subtotal/total); drives the soft price-mismatch warning. Null = no warning. */
    val expectedItemsTotal: BigDecimal? = null,
    /** The receipt's printed subtotal (clean item-sum anchor); drives the blocking "a line may be missing" check. Null = no check. */
    val receiptSubtotal: BigDecimal? = null,
    val error: String? = null,
    /** Which failure the full-screen error state should describe (only read when [stage] is IDLE with an [error]). */
    val errorKind: UploadErrorKind = UploadErrorKind.UNREADABLE,
    /**
     * True when this receipt was added manually (no scan). Persisted on the receipt, and — while
     * editing — drives the "Add receipt" action that scans a real receipt and appends its items.
     */
    val isManual: Boolean = false,
    /**
     * When set, ask whether to apply a just-made category change to other transactions with the
     * same item name (and/or remember it for future scans). Null = no prompt.
     */
    val propagationPrompt: PropagationPrompt? = null,
)

/**
 * A pending "apply to matching items?" prompt: the user changed [name]'s category to [category],
 * and [otherCount] other saved transactions (rows [ids]) currently have a different category.
 * [ruleExists] tells the dialog whether a saved rule for this name already exists.
 */
data class PropagationPrompt(
    val name: String,
    val category: String,
    val categoryColor: Int,
    /** Siblings whose category differs (those "Update all" would change). 0 = they already all match. */
    val otherCount: Int,
    val ids: List<Long>,
    val ruleExists: Boolean,
)

class UploadViewModel(
    private val ingestManager: ReceiptIngestManager,
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val receiptRepository: ReceiptRepository,
    private val scanQuota: ScanQuota,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val billingManager: BillingManager,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    /** When editing an existing receipt, its id (upload timestamp); null for a new upload. */
    private var editingReceiptId: Long? = null

    /**
     * True while the receipt currently under review contains a fresh AI scan that hasn't yet been
     * counted against the free scan quota. Set when an extraction succeeds, consumed (incremented)
     * only when the receipt is actually finalized — so a failed read, or a scan the user reviews but
     * abandons without saving, never burns a scan. Manual entry and editing an existing receipt leave
     * this false, so they never count.
     */
    private var scanPendingCount = false

    /**
     * Guards the one-time initial capture/load driven by [UploadScreen]'s launch effect. A
     * configuration change — rotating, or folding/unfolding a foldable — recreates the Activity and
     * re-runs that effect while this ViewModel is retained; without this guard the camera/file picker
     * would re-open over an already-captured receipt. The flag survives config changes with the
     * ViewModel and resets with it on a fresh navigation to the screen (new back-stack entry) or
     * process death — where the captured data resets too — so a genuinely new capture still starts.
     */
    private var initialLaunchDone = false

    /** Returns true on the first call only, so the screen launches its source exactly once. */
    fun consumeInitialLaunch(): Boolean {
        if (initialLaunchDone) return false
        initialLaunchDone = true
        return true
    }

    init {
        viewModelScope.launch {
            categoryRepository.categories.collect { cats ->
                // Refresh in-flight review rows' colors so a just-created/edited category's color
                // reaches items already showing it (e.g. right after creating a custom category).
                _uiState.update { state ->
                    state.copy(
                        categories = cats,
                        transactions = state.transactions.map { txn ->
                            val saved = cats.firstOrNull { it.name == txn.category }?.colorArgb
                            if (saved != null && saved != txn.categoryColor) {
                                txn.copy(categoryColor = saved)
                            } else {
                                txn
                            }
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            billingManager.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
    }

    /** Saved color for [category], or null if it isn't a known category yet. */
    private fun colorOf(category: String): Int? =
        _uiState.value.categories.firstOrNull { it.name == category }?.colorArgb

    fun onReceiptPicked(uri: Uri) {
        editingReceiptId = null
        // A brand-new capture: clear any pending count from a previous, abandoned scan on this VM.
        scanPendingCount = false
        _uiState.update { it.copy(stage = UploadStage.EXTRACTING, error = null) }
        viewModelScope.launch {
            try {
                val receipt = ingestManager.extract(uri)
                val rules = categoryRuleRepository.rulesByName()
                val parsed = receipt.items.map { applyRule(it, rules) }.map(::applySavedColor)
                _uiState.update {
                    it.copy(
                        stage = UploadStage.REVIEW,
                        // Always land on the review screen, even with no rows, so the
                        // user can add transactions manually.
                        transactions = parsed.ifEmpty { listOf(ParsedTransaction()) },
                        storeName = receipt.storeName,
                        receiptDate = receipt.date,
                        discount = receipt.discount,
                        tax = receipt.tax,
                        taxOnTop = receipt.taxOnTop,
                        extraCharges = receipt.extraCharges,
                        expectedItemsTotal = receipt.expectedItemsTotal,
                        receiptSubtotal = receipt.receiptSubtotal,
                        isManual = false,
                    )
                }
                // Don't count the scan yet — only a finalized receipt counts (see [finalizeUpload]).
                scanPendingCount = true
            } catch (e: Exception) {
                // Log (no receipt content) so a scan failure's real cause is visible in logcat — the
                // on-screen SERVICE/UNREADABLE copy is deliberately generic. Tag: BudgettyScan.
                Log.w("BudgettyScan", "Receipt extraction failed", e)
                _uiState.update {
                    it.copy(
                        stage = UploadStage.IDLE,
                        error = e.message ?: "Failed to read receipt",
                        // A poor photo / model abstention is the user's to fix (retake); anything else
                        // — HTTP, network, or a billing/outage error from the proxy — is a backend
                        // problem, so point the user at "try again later" instead of blaming the photo.
                        errorKind = if (e is ReceiptUnreadableException) UploadErrorKind.UNREADABLE
                        else UploadErrorKind.SERVICE,
                    )
                }
            }
        }
    }

    /** Adopt the user's saved color for the Haiku-assigned category, if one exists. */
    private fun applySavedColor(txn: ParsedTransaction): ParsedTransaction {
        val saved = colorOf(txn.category) ?: return txn
        return txn.copy(categoryColor = saved)
    }

    /**
     * Overrides a scanned item's category with the user's saved rule for its (normalized) name, if
     * one exists — so a learned preference sticks across receipts. Resolves the new category's color
     * the same way the review screen does. No matching rule => item left unchanged.
     */
    private fun applyRule(txn: ParsedTransaction, rules: Map<String, String>): ParsedTransaction {
        val ruled = rules[CategoryRuleEntity.key(txn.name)] ?: return txn
        return txn.copy(
            category = ruled,
            categoryColor = colorOf(ruled) ?: Categories.colorOf(ruled),
            fromRule = true,
        )
    }

    /** Starts a manual entry: a single empty row to fill in (no AI scan, no quota use). */
    fun startManual() {
        editingReceiptId = null
        scanPendingCount = false
        _uiState.update {
            it.copy(
                stage = UploadStage.REVIEW,
                transactions = listOf(ParsedTransaction()),
                storeName = "",
                receiptDate = System.currentTimeMillis(),
                discount = BigDecimal.ZERO,
                tax = BigDecimal.ZERO,
                taxOnTop = false,
                extraCharges = BigDecimal.ZERO,
                expectedItemsTotal = null,
                receiptSubtotal = null,
                error = null,
                isManual = true,
            )
        }
    }

    /**
     * Loads an existing receipt and its items into the review screen for editing. No scan or quota
     * use; [finalizeUpload] updates the receipt in place (same id) instead of creating a new one.
     */
    fun startEdit(receiptId: Long) {
        editingReceiptId = receiptId
        // Editing an existing receipt is not a new scan; only an attached scan (attachAndScan) counts.
        scanPendingCount = false
        viewModelScope.launch {
            val meta = receiptRepository.getById(receiptId)
            val items = repository.getByReceiptId(receiptId)
            val parsed = items.map { txn ->
                ParsedTransaction(
                    name = txn.name,
                    price = txn.price,
                    quantity = txn.quantity,
                    category = txn.category,
                    categoryColor = colorOf(txn.category) ?: Categories.colorOf(txn.category),
                )
            }
            _uiState.update {
                it.copy(
                    stage = UploadStage.REVIEW,
                    // Keep at least one row to edit, even for a receipt with no items left.
                    transactions = parsed.ifEmpty { listOf(ParsedTransaction()) },
                    storeName = meta?.store.orEmpty(),
                    receiptDate = meta?.date ?: System.currentTimeMillis(),
                    discount = meta?.discount ?: BigDecimal.ZERO,
                    tax = meta?.tax ?: BigDecimal.ZERO,
                    taxOnTop = meta?.taxOnTop == true,
                    extraCharges = meta?.extraCharges ?: BigDecimal.ZERO,
                    expectedItemsTotal = null,
                    receiptSubtotal = null,
                    error = null,
                    // Only manual receipts offer "Add receipt" while editing.
                    isManual = meta?.isManual == true,
                )
            }
        }
    }

    /**
     * "Add receipt" on a manual edit: scans a photo/file and **appends** its items to the ones the
     * user already entered (keeping the existing transactions). The receipt stays manual — store,
     * date and discount the user set are kept. No image is stored. Counts as a scan.
     */
    fun attachAndScan(uri: Uri) {
        _uiState.update { it.copy(stage = UploadStage.EXTRACTING, error = null) }
        viewModelScope.launch {
            try {
                val receipt = ingestManager.extract(uri)
                val rules = categoryRuleRepository.rulesByName()
                val scanned = receipt.items.map { applyRule(it, rules) }.map(::applySavedColor)
                _uiState.update {
                    // Drop a single blank placeholder row so the appended items read cleanly.
                    val existing = it.transactions.filterNot { row -> row.name.isBlank() }
                    it.copy(
                        stage = UploadStage.REVIEW,
                        transactions = (existing + scanned).ifEmpty { listOf(ParsedTransaction()) },
                    )
                }
                // Counts only when the (still-manual) receipt is finalized — see [finalizeUpload].
                scanPendingCount = true
            } catch (e: Exception) {
                Log.w("BudgettyScan", "Receipt attach-scan failed", e)
                // Keep the manual data the user already had; just surface the error.
                _uiState.update {
                    it.copy(stage = UploadStage.REVIEW, error = e.message ?: "Failed to read receipt")
                }
            }
        }
    }

    fun updateName(clientId: String, name: String) = mutate(clientId) { it.copy(name = name) }

    /** Updates the store name as the user edits the store field on the review screen. */
    fun updateStore(name: String) {
        _uiState.update { it.copy(storeName = name) }
    }

    /** Updates the receipt's printed date (epoch millis) as picked on the review screen. */
    fun updateDate(millis: Long) {
        _uiState.update { it.copy(receiptDate = millis) }
    }

    fun updateCategory(clientId: String, category: String) {
        val previous = _uiState.value.transactions.firstOrNull { it.clientId == clientId }?.category
        // Prefer the user's saved color for this category, else its predefined color. A hand-pick
        // clears the "from your rules" badge — the category is now the user's explicit choice.
        mutate(clientId) {
            it.copy(
                category = category,
                categoryColor = colorOf(category) ?: Categories.colorOf(category),
                fromRule = false,
            )
        }
        // A real change to a named row: offer to apply it to other same-name transactions.
        val name = _uiState.value.transactions.firstOrNull { it.clientId == clientId }?.name.orEmpty()
        if (category.isNotBlank() && category != previous && name.isNotBlank()) {
            maybePromptPropagation(name, category)
        }
    }

    /**
     * Raises the propagation prompt when the item has other saved occurrences ("siblings"), so a
     * repeated item can be recategorized and/or remembered. Matching runs in Kotlin on the normalized
     * key ([CategoryRuleEntity.key]) so it folds case and Cyrillic; the current receipt's own rows are
     * excluded while editing (they re-save from memory). [PropagationPrompt.otherCount] counts only the
     * siblings whose category differs (those "Update all" would change) — it is 0 when they already all
     * match [category], e.g. pinning an item to the default "Groceries", where the prompt still offers
     * to remember the choice. Nothing to change AND the rule already is this category ⇒ no prompt.
     */
    private fun maybePromptPropagation(name: String, category: String) {
        viewModelScope.launch {
            val key = CategoryRuleEntity.key(name)
            val editing = editingReceiptId
            val siblings = repository.getAllOnce().filter {
                CategoryRuleEntity.key(it.name) == key &&
                    (editing == null || it.receiptId != editing)
            }
            if (siblings.isEmpty()) return@launch // brand-new item name: nothing to offer
            val differing = siblings.filter { it.category != category }
            val existingRule = categoryRuleRepository.rulesByName()[key]
            if (differing.isEmpty() && existingRule == category) return@launch // already fully set
            _uiState.update {
                it.copy(
                    propagationPrompt = PropagationPrompt(
                        name = name,
                        category = category,
                        categoryColor = colorOf(category) ?: Categories.colorOf(category),
                        otherCount = differing.size,
                        ids = differing.map(TransactionEntity::id),
                        ruleExists = existingRule != null,
                    ),
                )
            }
        }
    }

    /**
     * Resolves the propagation prompt. [applyToAll] recategorizes the matched saved transactions
     * (and the current receipt's same-name rows, so it stays consistent on save); [remember] saves
     * the name → category rule for future scans, or clears any existing one when false.
     */
    fun confirmPropagation(applyToAll: Boolean, remember: Boolean) {
        val prompt = _uiState.value.propagationPrompt ?: return
        _uiState.update { it.copy(propagationPrompt = null) }
        viewModelScope.launch {
            if (applyToAll) {
                repository.updateCategoryForIds(prompt.ids, prompt.category)
                val key = CategoryRuleEntity.key(prompt.name)
                val color = colorOf(prompt.category) ?: Categories.colorOf(prompt.category)
                _uiState.update { state ->
                    state.copy(
                        transactions = state.transactions.map {
                            if (CategoryRuleEntity.key(it.name) == key) {
                                it.copy(category = prompt.category, categoryColor = color)
                            } else {
                                it
                            }
                        },
                    )
                }
            }
            if (remember) {
                categoryRuleRepository.setRule(prompt.name, prompt.category)
            } else {
                categoryRuleRepository.removeRule(prompt.name)
            }
        }
    }

    /** Dismisses the propagation prompt without propagating (the single-row change already applied). */
    fun dismissPropagation() {
        _uiState.update { it.copy(propagationPrompt = null) }
    }

    // --- Custom categories ---------------------------------------------------------------------

    /**
     * Creates a new custom category, or updates / renames an existing one. [original] is the
     * category's current name when editing (null when creating). Validates the name (non-blank and
     * unique) and the icon, and enforces the create cap; silently no-ops on invalid input, since the
     * picker only enables Save when the form is valid. A rename cascades the new name across saved
     * transactions, category rules, and any per-category budget, then drops the old row.
     */
    fun saveCustomCategory(original: String?, rawName: String, icon: String, colorArgb: Int) {
        val name = rawName.trim()
        if (name.isEmpty() || icon.isEmpty() || isDuplicateName(name, original)) return
        viewModelScope.launch {
            val existing = _uiState.value.categories
            if (original == null) {
                val customCount = existing.count { it.isCustom }
                val cap = if (_uiState.value.isPremium) {
                    Categories.MAX_CUSTOM_LIMIT
                } else {
                    Categories.FREE_CUSTOM_LIMIT
                }
                if (customCount >= cap) return@launch
                categoryRepository.upsert(
                    CategoryEntity(
                        name = name,
                        colorArgb = colorArgb,
                        icon = icon,
                        isCustom = true,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                val createdAt = existing.firstOrNull { it.name == original }?.createdAt
                    ?: System.currentTimeMillis()
                categoryRepository.upsert(
                    CategoryEntity(name, colorArgb, icon, isCustom = true, createdAt = createdAt),
                )
                if (!name.equals(original, ignoreCase = true)) {
                    repository.reassignCategory(original, name)
                    categoryRuleRepository.reassignCategory(original, name)
                    budgetRepository.renameCategoryBudget(original, name)
                    categoryRepository.deleteByName(original)
                    _uiState.update { st ->
                        st.copy(
                            transactions = st.transactions.map {
                                if (it.category == original) {
                                    it.copy(category = name, categoryColor = colorArgb)
                                } else {
                                    it
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Deletes a custom category: its saved transactions are reassigned to "Other", any rules mapping
     * to it are dropped, its per-category budget is cleared, and in-flight review rows fall back to
     * "Other".
     */
    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            repository.reassignCategory(name, Categories.OTHER)
            categoryRuleRepository.removeRulesForCategory(name)
            budgetRepository.clearCategoryBudget(name)
            categoryRepository.deleteByName(name)
            val otherColor = Categories.colorOf(Categories.OTHER)
            _uiState.update { st ->
                st.copy(
                    transactions = st.transactions.map {
                        if (it.category == name) {
                            it.copy(category = Categories.OTHER, categoryColor = otherColor)
                        } else {
                            it
                        }
                    },
                )
            }
        }
    }

    /** True when [name] (trimmed, case-insensitive) already names a category other than [original]. */
    fun isDuplicateName(name: String, original: String?): Boolean {
        val key = name.trim().lowercase()
        if (original != null && key == original.trim().lowercase()) return false
        // A name is only "taken" if it belongs to a predefined category or an existing custom one.
        // A leftover non-predefined isCustom=0 row (an orphaned/downgraded "ghost" category) does not
        // block creation: saving upserts by name, so the new custom simply reclaims that row.
        return _uiState.value.categories.any {
            it.name.trim().lowercase() == key && (it.isCustom || Categories.isPredefined(it.name))
        }
    }

    /** Count of saved transactions in [category] — drives the delete-category confirmation copy. */
    suspend fun transactionCount(category: String): Int = repository.countByCategory(category)

    fun updateQuantity(clientId: String, quantity: Int) =
        mutate(clientId) { it.copy(quantity = quantity.coerceAtLeast(1)) }

    fun updatePrice(clientId: String, priceText: String) = mutate(clientId) {
        val price = priceText.replace(',', '.').toBigDecimalOrNull() ?: it.price
        it.copy(price = price)
    }

    /** Order-level discount; blank clears it, invalid input (e.g. a trailing ".") is ignored. */
    fun updateDiscount(text: String) {
        val value = text.replace(',', '.').toBigDecimalOrNull() ?: return
        _uiState.update { it.copy(discount = value.coerceAtLeast(BigDecimal.ZERO)) }
    }

    fun addRow() {
        _uiState.update { it.copy(transactions = it.transactions + ParsedTransaction()) }
    }

    fun removeRow(clientId: String) {
        _uiState.update { state ->
            state.copy(transactions = state.transactions.filterNot { it.clientId == clientId })
        }
    }

    /**
     * Saves the receipt. Refuses to create an "empty" receipt: there must be at least one named
     * item ([noItemsMessage]) and the items must carry a positive total ([noAmountMessage]); the
     * messages are passed in already-localized from the screen.
     */
    fun finalizeUpload(noItemsMessage: String, noAmountMessage: String, onDone: () -> Unit) {
        val rows = _uiState.value.transactions.filter { it.name.isNotBlank() }
        if (rows.isEmpty()) {
            _uiState.update { it.copy(error = noItemsMessage) }
            return
        }
        // Guard against value-less receipts: named rows but every price left at zero.
        val gross = rows.fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }
        if (gross.signum() <= 0) {
            _uiState.update { it.copy(error = noAmountMessage) }
            return
        }
        _uiState.update { it.copy(stage = UploadStage.SAVING, error = null) }
        viewModelScope.launch {
            val editing = editingReceiptId
            // Editing keeps the receipt's id so it updates in place; a new upload mints one.
            val uploadId = editing ?: System.currentTimeMillis()
            val madeAt = _uiState.value.receiptDate
            // Resolve each row's category name + color. A known category keeps its saved color and
            // whole row (never clobbered); only a brand-new name gets persisted, with the row's color.
            val resolved = rows.map { parsed ->
                val name = parsed.category.trim().ifBlank { Categories.DEFAULT }
                val color = colorOf(name) ?: parsed.categoryColor
                Triple(parsed, name, color)
            }
            // insert-or-ignore, not upsert: overwriting would strip a custom category's isCustom/icon
            // (dropping it from the picker) and wipe predefined emoji. Existing rows already resolve
            // their own color, so only genuinely new names need a row here.
            categoryRepository.insertMissing(
                resolved.map { (_, name, color) -> CategoryEntity(name, color) }.distinctBy { it.name },
            )
            // When editing, swap the receipt's previous items for the edited set (kept under the
            // same receiptId); the receipt row itself is REPLACEd by the insert below.
            if (editing != null) repository.deleteByReceiptId(uploadId)
            repository.insertAll(
                resolved.map { (parsed, name, _) ->
                    TransactionEntity(
                        name = parsed.name.trim(),
                        timestamp = madeAt,
                        receiptId = uploadId,
                        price = parsed.price,
                        quantity = parsed.quantity,
                        category = name,
                    )
                },
            )
            receiptRepository.insert(
                ReceiptEntity(
                    timestamp = uploadId,
                    // Canonicalize again at save: catches manual entry and any hand-edit of the
                    // store field on the review screen. Idempotent for already-normalized names.
                    store = StoreNormalizer.normalize(_uiState.value.storeName),
                    date = madeAt,
                    discount = _uiState.value.discount,
                    isManual = _uiState.value.isManual,
                    tax = _uiState.value.tax,
                    taxOnTop = _uiState.value.taxOnTop,
                    extraCharges = _uiState.value.extraCharges,
                ),
            )
            // Count the scan now that a real receipt was saved — only successful, finalized scans
            // count against the free quota. Consume the flag so re-saving an edit can't double-count.
            if (scanPendingCount) {
                scanQuota.increment()
                scanPendingCount = false
            }
            editingReceiptId = null
            _uiState.update { it.copy(stage = UploadStage.DONE) }
            onDone()
        }
    }

    private fun mutate(clientId: String, transform: (ParsedTransaction) -> ParsedTransaction) {
        _uiState.update { state ->
            state.copy(
                transactions = state.transactions.map {
                    if (it.clientId == clientId) transform(it) else it
                },
            )
        }
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try { if (isBlank()) BigDecimal.ZERO else BigDecimal(this) } catch (e: NumberFormatException) { null }
}
