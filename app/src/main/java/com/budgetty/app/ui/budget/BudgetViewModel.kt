package com.budgetty.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.budgetty.app.ui.util.monthlyAmount
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Income sources + recurring payments, split and summed to their monthly-equivalent totals. */
data class RecurringUi(
    val income: List<RecurringEntity> = emptyList(),
    val bills: List<RecurringEntity> = emptyList(),
    val monthlyIncome: BigDecimal = BigDecimal.ZERO,
    val monthlyBills: BigDecimal = BigDecimal.ZERO,
)

class BudgetViewModel(
    private val repository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    billingManager: BillingManager,
    receiptRepository: ReceiptRepository,
) : ViewModel() {

    /** Saved budgets as key -> amount (keys from [BudgetRepository]). */
    val budgets: StateFlow<Map<String, BigDecimal>> = repository.budgets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )

    // Current calendar month's transactions, shared by the per-category bars and the monthly total.
    private val monthlyTransactions = run {
        val (start, end) = currentMonthRange()
        transactionRepository.getBetween(start, end)
    }

    /** Current-month spend per category name, powering the per-category progress bars. */
    val categorySpending: StateFlow<Map<String, BigDecimal>> =
        monthlyTransactions
            .map { txns -> txns.groupBy { it.category }.mapValues { (_, items) -> items.spend() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Period totals are adjusted to what was paid — on-top tax (tax-exclusive receipts) and extra
    // charges added, order discounts subtracted — so budget progress matches the receipt totals; the
    // per-category bars above stay on the net line prices (tax/discount have no single category).
    private val receipts = receiptRepository.getAll()

    /** Total spend this calendar month, for the Monthly budget card's progress and the breakdown. */
    val monthlySpent: StateFlow<BigDecimal> =
        combine(monthlyTransactions, receipts) { txns, receiptList ->
            txns.spend() + paidAdjustmentOf(txns, receiptList.associateBy { it.timestamp })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    /** Total spend this Mon–Sun week, for the Weekly budget card's progress. */
    val weeklySpent: StateFlow<BigDecimal> = run {
        val (start, end) = currentWeekRange()
        combine(transactionRepository.getBetween(start, end), receipts) { txns, receiptList ->
            txns.spend() + paidAdjustmentOf(txns, receiptList.associateBy { it.timestamp })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)
    }

    /** Income sources + recurring payments, split and summed to monthly-equivalent totals. */
    val recurring: StateFlow<RecurringUi> =
        recurringRepository.items
            .map { it.toUi() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUi())

    /** Saved categories — for the recurring-payment category picker (shows custom categories too). */
    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.categories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Premium unlocks unlimited recurring payments; the free tier is capped (income stays uncapped). */
    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    /** Persists the [key] budget from raw input text; blank/invalid clears it. */
    fun setBudget(key: String, text: String) {
        val amount = text.replace(',', '.').trim().toBigDecimalOrNull()
        viewModelScope.launch { repository.setBudget(key, amount) }
    }

    /**
     * Saves the single top-level budget: writes the chosen period's amount and clears the other,
     * so only one of [BudgetRepository.MONTHLY]/[BudgetRepository.WEEKLY] is ever set at a time.
     */
    fun saveSingleBudget(monthly: Boolean, text: String) {
        val amount = text.replace(',', '.').trim().toBigDecimalOrNull()
        val (activeKey, otherKey) =
            if (monthly) BudgetRepository.MONTHLY to BudgetRepository.WEEKLY
            else BudgetRepository.WEEKLY to BudgetRepository.MONTHLY
        viewModelScope.launch {
            repository.setBudget(activeKey, amount)
            repository.setBudget(otherKey, null)
        }
    }

    /**
     * Creates or updates one recurring entry (income or bill). [original] is the row being edited
     * (null when adding), so its id / createdAt are preserved. Silently no-ops on invalid input.
     */
    fun saveRecurring(
        original: RecurringEntity?,
        label: String,
        amountText: String,
        isIncome: Boolean,
        category: String,
        cadence: String,
        dueDay: Int,
    ) {
        val amount = amountText.replace(',', '.').trim().toBigDecimalOrNull() ?: return
        val name = label.trim()
        if (name.isEmpty() || amount.signum() <= 0) return
        val base = original ?: RecurringEntity(
            label = name,
            amount = amount,
            isIncome = isIncome,
            createdAt = System.currentTimeMillis(),
        )
        val entity = base.copy(
            label = name,
            amount = amount,
            isIncome = isIncome,
            category = if (isIncome) "" else category,
            cadence = cadence,
            dueDay = dueDay,
        )
        viewModelScope.launch { recurringRepository.upsert(entity) }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch { recurringRepository.delete(id) }
    }

    // ── Custom categories ────────────────────────────────────────────────────────────────────────
    // Mirrors UploadViewModel so the recurring-payment category picker creates / renames / deletes
    // custom categories identically (rename cascades across transactions, rules, and budgets).

    /** Creates a new custom category, or renames/updates an existing one. No-ops on invalid input. */
    fun saveCustomCategory(original: String?, rawName: String, icon: String, colorArgb: Int) {
        val name = rawName.trim()
        if (name.isEmpty() || icon.isEmpty() || isDuplicateName(name, original)) return
        viewModelScope.launch {
            val existing = categories.value
            if (original == null) {
                val cap = if (isPremium.value) Categories.MAX_CUSTOM_LIMIT else Categories.FREE_CUSTOM_LIMIT
                if (existing.count { it.isCustom } >= cap) return@launch
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
                    transactionRepository.reassignCategory(original, name)
                    categoryRuleRepository.reassignCategory(original, name)
                    repository.renameCategoryBudget(original, name)
                    categoryRepository.deleteByName(original)
                }
            }
        }
    }

    /** Deletes a custom category: its transactions fall back to "Other"; rules and budget are cleared. */
    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            transactionRepository.reassignCategory(name, Categories.OTHER)
            categoryRuleRepository.removeRulesForCategory(name)
            repository.clearCategoryBudget(name)
            categoryRepository.deleteByName(name)
        }
    }

    /** True when [name] (trimmed, case-insensitive) already names a category other than [original]. */
    fun isDuplicateName(name: String, original: String?): Boolean {
        val key = name.trim().lowercase()
        if (original != null && key == original.trim().lowercase()) return false
        return categories.value.any { it.name.trim().lowercase() == key }
    }

    /** Count of saved transactions in [category] — drives the delete-category confirmation copy. */
    suspend fun transactionCount(category: String): Int = transactionRepository.countByCategory(category)

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        if (isBlank()) null else try { BigDecimal(this) } catch (e: NumberFormatException) { null }

    /** Summed price × quantity across the transactions. */
    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /** Splits recurring rows into income/bills and sums each to its monthly-equivalent total. */
    private fun List<RecurringEntity>.toUi(): RecurringUi {
        val (monthStart, monthEnd) = currentMonthRange()
        val income = filter { it.isIncome }
        val bills = filterNot { it.isIncome }
        return RecurringUi(
            income = income,
            bills = bills,
            monthlyIncome = income.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(monthStart, monthEnd) },
            monthlyBills = bills.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(monthStart, monthEnd) },
        )
    }

    /** Inclusive [start, end] epoch-millis window for the current calendar month. */
    private fun currentMonthRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val month = YearMonth.from(today)
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Inclusive [start, end] epoch-millis window for the current Mon–Sun week. */
    private fun currentWeekRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
