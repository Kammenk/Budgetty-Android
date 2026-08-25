package com.budgetty.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.BudgetRolloverEntity
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.BudgetRolloverRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.budgetty.app.ui.savings.SavingsGoalCardUi
import com.budgetty.app.ui.streaks.BudgetStreakInput
import com.budgetty.app.ui.streaks.LiveBudgetPeriod
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.streaks.StreakTxn
import com.budgetty.app.ui.util.BudgetRollover
import com.budgetty.app.ui.util.PayCycle
import com.budgetty.app.ui.util.currentMonthRange
import com.budgetty.app.ui.util.isAutoPayActive
import com.budgetty.app.ui.util.isEffectivelyPaidThisCycle
import com.budgetty.app.ui.util.monthlyAmount
import com.budgetty.app.ui.util.SavingsMath
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Income sources + recurring payments, split and summed to their monthly-equivalent totals. */
data class RecurringUi(
    val income: List<RecurringEntity> = emptyList(),
    val bills: List<RecurringEntity> = emptyList(),
    val monthlyIncome: BigDecimal = BigDecimal.ZERO,
    val monthlyBills: BigDecimal = BigDecimal.ZERO,
    /** Ids of bills marked paid for the current cycle (checkmark + hidden from "upcoming"). */
    val paidBillIds: Set<Long> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    billingManager: BillingManager,
    receiptRepository: ReceiptRepository,
    private val settingsStore: SettingsStore,
    private val rolloverRepository: BudgetRolloverRepository,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    /** Saved budgets as key -> amount (keys from [BudgetRepository]). */
    val budgets: StateFlow<Map<String, BigDecimal>> = repository.budgets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )

    /** Whether unspent budget carries into the next period (opt-in). */
    val rolloverEnabled: StateFlow<Boolean> =
        settingsStore.settings.map { it.budgetRolloverEnabled }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Per-key carried-over amount to add on top of the base budget; empty when rollover is off. */
    val carried: StateFlow<Map<String, BigDecimal>> =
        combine(rolloverRepository.carried, settingsStore.settings) { carried, settings ->
            if (settings.budgetRolloverEnabled) carried else emptyMap()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        // Bring carry-over up to date whenever the Budget screen is opened.
        viewModelScope.launch { rollForwardIfNeeded() }
    }

    // The pay-day the financial month starts on; the monthly budget + recurring plan follow it.
    private val monthStartDay = settingsStore.settings.map { it.monthStartDay }.distinctUntilChanged()

    // Current pay-cycle month's transactions, shared by the per-category bars and the monthly total.
    private val monthlyTransactions = monthStartDay.flatMapLatest { day ->
        val (start, end) = currentMonthRange(monthStartDay = day)
        transactionRepository.getBetween(start, end)
    }

    /** Current-month spend per category name, powering the per-category progress bars. */
    val categorySpending: StateFlow<Map<String, BigDecimal>> =
        monthlyTransactions
            .map { txns -> txns.groupBy { it.category }.mapValues { (_, items) -> items.spend() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * §2.6 the 4th streak surface: per-category monthly budget streaks (category name → surfaced
     * [Streak]), so a category row can carry a quiet "· N months under" caption. Only surfaced runs
     * (current ≥ 2, §2.7) are kept; a category with no run isn't in the map, so the row shows nothing.
     * Per-category scopes use net line prices — the paid adjustment applies only to the whole-budget
     * scope, which never captions a category row — so no receipts are needed. Computed off the main
     * thread; re-sourced from [StreakEngine] like Recap/Wellbeing so there is one streak implementation.
     */
    val categoryStreaks: StateFlow<Map<String, Streak>> =
        combine(
            transactionRepository.getAll(),
            repository.budgets,
            settingsStore.settings.map { it.monthStartDay }.distinctUntilChanged(),
        ) { txns, budgets, day ->
            surfacedCategoryStreaks(txns, budgets, day)
        }.flowOn(Dispatchers.Default)
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
        combine(recurringRepository.items, monthStartDay) { items, day -> items.toUi(day) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUi())

    /** Saved categories — for the recurring-payment category picker (shows custom categories too). */
    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.categories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Premium unlocks unlimited recurring payments; the free tier is capped (income stays uncapped). */
    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    /** Savings goals with derived progress; each card sums that goal's contributions. */
    val savingsGoals: StateFlow<List<SavingsGoalCardUi>> =
        combine(savingsRepository.goals, savingsRepository.allContributions) { goals, contributions ->
            val byGoal = contributions.groupBy { it.goalId }
            goals.map { goal -> SavingsGoalCardUi(goal, SavingsMath.progress(goal, byGoal[goal.id].orEmpty())) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Turns rollover on/off; on enable it seeds the carry-over rows (accrual starts next period). */
    fun setRolloverEnabled(enabled: Boolean) {
        settingsStore.setBudgetRolloverEnabled(enabled)
        viewModelScope.launch { rollForwardIfNeeded() }
    }

    /**
     * Brings each budget key's carried-over amount up to the current period by accumulating every
     * elapsed period's unspent leftover (see [BudgetRollover.rollForward]). Only monthly-period budgets
     * carry — the overall MONTHLY budget and every category; a WEEKLY overall budget doesn't. When
     * rollover is off, wipes any stored carry-over. Past-period spend uses net line prices (an estimate,
     * since budget amounts have no history).
     */
    private suspend fun rollForwardIfNeeded() {
        val settings = settingsStore.settings.value
        if (!settings.budgetRolloverEnabled) {
            rolloverRepository.clearAll()
            return
        }
        val startDay = settings.monthStartDay
        val currentKey = BudgetRollover.currentPeriodKey(LocalDate.now(), startDay)
        val budgets = repository.budgets.first()
        val allTxns = transactionRepository.getAll().first()
        val zone = ZoneId.systemDefault()
        val rolloverKeys = budgets.keys.filter {
            it == BudgetRepository.MONTHLY || it.startsWith(BudgetRepository.CATEGORY_PREFIX)
        }
        for (key in rolloverKeys) {
            val budget = budgets[key] ?: continue
            val stored = rolloverRepository.get(key)
            when {
                // First time / just enabled: carry 0 now, start accruing next period.
                stored == null ->
                    rolloverRepository.upsert(BudgetRolloverEntity(key, BigDecimal.ZERO, currentKey))
                stored.periodKey < currentKey -> {
                    val category = key.removePrefix(BudgetRepository.CATEGORY_PREFIX).takeIf { it != key }
                    val carried = BudgetRollover.rollForward(
                        storedCarried = stored.carried,
                        storedPeriodKey = stored.periodKey,
                        currentPeriodKey = currentKey,
                        budget = budget,
                    ) { periodKey ->
                        val (start, end) = BudgetRollover.periodWindow(periodKey, startDay)
                        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
                        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                        allTxns
                            .filter { t ->
                                t.timestamp in startMs..endMs && (category == null || t.category == category)
                            }
                            .fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }
                    }
                    rolloverRepository.upsert(BudgetRolloverEntity(key, carried, currentKey))
                }
                // else: already caught up for this period.
            }
        }
        // Drop carry-over rows for budgets that no longer exist.
        rolloverRepository.carried.first().keys
            .filter { it !in budgets.keys }
            .forEach { rolloverRepository.delete(it) }
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
        autoPay: Boolean,
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
            autoPay = autoPay,
        )
        // Autopay applies to monthly/weekly bills only; force it off for income, yearly, and one-off
        // entries so a hidden or stale switch can't persist a stuck "Auto" state (isAutoPayActive() is
        // the shared rule the Budget row displays by too).
        viewModelScope.launch { recurringRepository.upsert(entity.copy(autoPay = entity.isAutoPayActive())) }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch { recurringRepository.delete(id) }
    }

    /** Creates a new savings goal from the Savings section's create sheet. */
    fun createSavingsGoal(name: String, emoji: String, target: BigDecimal, targetDate: Long?) {
        viewModelScope.launch {
            savingsRepository.upsertGoal(
                SavingsGoalEntity(
                    name = name.trim(),
                    emoji = emoji,
                    targetAmount = target,
                    targetDate = targetDate,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    // ── Custom categories ────────────────────────────────────────────────────────────────────────
    // Mirrors UploadViewModel so the recurring-payment category picker creates / renames / deletes
    // custom categories identically (rename cascades across transactions, rules, and budgets).

    /** Creates a new custom category, or renames/updates an existing one. No-ops on invalid input. */
    fun saveCustomCategory(original: String?, rawName: String, icon: String, colorArgb: Int, parent: String?) {
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
                        parent = parent,
                    ),
                )
            } else {
                val createdAt = existing.firstOrNull { it.name == original }?.createdAt
                    ?: System.currentTimeMillis()
                categoryRepository.upsert(
                    CategoryEntity(name, colorArgb, icon, isCustom = true, createdAt = createdAt, parent = parent),
                )
                if (!name.equals(original, ignoreCase = true)) {
                    transactionRepository.reassignCategory(original, name)
                    categoryRuleRepository.reassignCategory(original, name)
                    repository.renameCategoryBudget(original, name)
                    categoryRepository.reparentChildren(original, name)
                    categoryRepository.deleteByName(original)
                }
                if (parent != null) categoryRepository.promoteChildrenOf(name)
            }
        }
    }

    /** Re-homes any category under [parent] (null = top-level); mirrors UploadViewModel. */
    fun setCategoryParent(name: String, parent: String?) {
        viewModelScope.launch {
            categoryRepository.setParent(name, parent)
            if (parent != null) categoryRepository.promoteChildrenOf(name)
        }
    }

    /** Deletes a custom category: its transactions fall back to "Other"; rules and budget are cleared. */
    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.promoteChildrenOf(name)
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

    /**
     * §2.6: per-category monthly budget streaks that clear the surface bar (current ≥ 2), keyed by
     * category name. Tags each transaction with the closed pay-cycle month index its date falls in
     * (0 = the just-closed month, positive into the past; the current OPEN month feeds only
     * [Streak.liveOnTrack]) — mirrors [com.budgetty.app.ui.recap.RecapProvider]/WellbeingProvider —
     * then walks per-category via [StreakEngine]. Only category scopes are asked for (monthlyBudget =
     * null), so the whole-budget scope never leaks onto a category row.
     */
    private fun surfacedCategoryStreaks(
        txns: List<TransactionEntity>,
        budgets: Map<String, BigDecimal>,
        monthStartDay: Int,
    ): Map<String, Streak> {
        val catBudgets = budgets.filterKeys { it.startsWith(BudgetRepository.CATEGORY_PREFIX) }
            .mapKeys { it.key.removePrefix(BudgetRepository.CATEGORY_PREFIX) }
        if (catBudgets.isEmpty()) return emptyMap()
        val zone = ZoneId.systemDefault()
        val endCycle = YearMonth.from(PayCycle.month(LocalDate.now(), monthStartDay, -1).first)
        val byIndex = txns
            .mapNotNull { t ->
                val date = Instant.ofEpochMilli(t.timestamp).atZone(zone).toLocalDate()
                val txnCycle = YearMonth.from(PayCycle.month(date, monthStartDay, 0).first)
                val idx = ChronoUnit.MONTHS.between(txnCycle, endCycle).toInt()
                if (idx == LIVE_INDEX || idx in 0 until StreakEngine.MAX_STREAK) idx to t else null
            }
            .groupBy({ it.first }, { it.second })
        val closed = byIndex.filterKeys { it >= 0 }.flatMap { (idx, list) ->
            list.map { StreakTxn(idx, it.category, it.price.multiply(BigDecimal(it.quantity))) }
        }
        val liveList = byIndex[LIVE_INDEX].orEmpty()
        val streaks = StreakEngine.budgetStreaks(
            BudgetStreakInput(
                transactions = closed,
                categoryBudgets = catBudgets,
                monthlyBudget = null,
                kind = StreakKind.BUDGET_MONTH,
                live = LiveBudgetPeriod(
                    liveList.map { StreakTxn(0, it.category, it.price.multiply(BigDecimal(it.quantity))) },
                ),
            ),
        )
        return StreakEngine.surfaced(streaks).associateBy { it.label }
    }

    /** Summed price × quantity across the transactions. */
    private fun List<TransactionEntity>.spend(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /** Splits recurring rows into income/bills and sums each to its monthly-equivalent total. */
    private fun List<RecurringEntity>.toUi(monthStartDay: Int): RecurringUi {
        val (monthStart, monthEnd) = currentMonthRange(monthStartDay = monthStartDay)
        val today = LocalDate.now()
        val income = filter { it.isIncome }
        val bills = filterNot { it.isIncome }
        return RecurringUi(
            income = income,
            bills = bills,
            monthlyIncome = income.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(monthStart, monthEnd) },
            monthlyBills = bills.fold(BigDecimal.ZERO) { a, r -> a + r.monthlyAmount(monthStart, monthEnd) },
            paidBillIds = bills.filter { it.isEffectivelyPaidThisCycle(today, monthStartDay) }
                .map { it.id }.toSet(),
        )
    }

    /** Marks a bill paid (or not) for its current cycle; auto-resets when the next occurrence begins. */
    fun setBillPaid(item: RecurringEntity, paid: Boolean) {
        viewModelScope.launch {
            recurringRepository.setPaid(item.id, if (paid) System.currentTimeMillis() else 0L)
        }
    }

    /** Inclusive [start, end] epoch-millis window for the current Mon–Sun week. */
    private fun currentWeekRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private companion object {
        /** Period index of the current OPEN pay-cycle month in the streak tagging (never counted; §2.3). */
        const val LIVE_INDEX = -1
    }
}
