package com.budgetty.app.ui.budget

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.budgetColor
import com.budgetty.app.ui.util.budgetRatio
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.recurringSubtitle
import com.budgetty.app.ui.util.monthlyToWeekly
import com.budgetty.app.ui.util.weeklyToMonthly
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.ui.components.CategoryPickerScreen
import com.budgetty.app.ui.components.CustomCategoryActions
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = koinViewModel(),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val spending by viewModel.categorySpending.collectAsStateWithLifecycle()
    val monthlySpent by viewModel.monthlySpent.collectAsStateWithLifecycle()
    val weeklySpent by viewModel.weeklySpent.collectAsStateWithLifecycle()
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    BudgetScreenContent(
        budgets = budgets,
        spending = spending,
        monthlySpent = monthlySpent,
        weeklySpent = weeklySpent,
        recurring = recurring,
        categories = categories,
        isPremium = isPremium,
        isExpanded = isExpandedWidth(),
        isWide = isWideWidth(),
        onNavigateBack = onNavigateBack,
        onSetBudget = viewModel::setBudget,
        onSaveSingleBudget = viewModel::saveSingleBudget,
        customActions = CustomCategoryActions(
            categories = categories,
            isPremium = isPremium,
            onSave = viewModel::saveCustomCategory,
            onDelete = viewModel::deleteCustomCategory,
            onCountTransactions = viewModel::transactionCount,
            onOpenPaywall = onNavigateToPaywall,
        ),
        onSaveRecurring = viewModel::saveRecurring,
        onDeleteRecurring = viewModel::deleteRecurring,
        onOpenPaywall = onNavigateToPaywall,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetScreenContent(
    budgets: Map<String, BigDecimal>,
    spending: Map<String, BigDecimal>,
    monthlySpent: BigDecimal,
    weeklySpent: BigDecimal,
    isExpanded: Boolean,
    isWide: Boolean,
    onNavigateBack: () -> Unit,
    onSetBudget: (String, String) -> Unit,
    onSaveSingleBudget: (Boolean, String) -> Unit,
    recurring: RecurringUi = RecurringUi(),
    categories: List<CategoryEntity> = emptyList(),
    isPremium: Boolean = false,
    customActions: CustomCategoryActions = CustomCategoryActions(),
    onSaveRecurring: (RecurringEntity?, String, String, Boolean, String, String, Int) -> Unit =
        { _, _, _, _, _, _, _ -> },
    onDeleteRecurring: (Long) -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Per-category budgets still save live; this buffer keeps typing from being snapped back by the
    // persisted value, falling back to the saved amount until the field is edited.
    val texts = remember { mutableStateMapOf<String, String>() }
    fun valueFor(key: String): String = texts[key] ?: budgets[key]?.toPlainString() ?: ""
    fun onChange(key: String, text: String) {
        texts[key] = text
        onSetBudget(key, text)
    }

    // A single top-level budget: the user picks Monthly or Weekly and enters one amount; the other
    // period is derived for display. The active period is whichever of MONTHLY/WEEKLY is stored —
    // Monthly by default (and if both somehow exist), Weekly only when it alone is set. The amount is
    // edited in a buffer and committed via the Save button, which writes the active key and clears
    // the other so the two can never drift apart.
    val storedMonthly = budgets[BudgetRepository.MONTHLY]
    val storedWeekly = budgets[BudgetRepository.WEEKLY]
    val persistedMonthly = storedMonthly != null || storedWeekly == null
    val persistedAmount = (if (persistedMonthly) storedMonthly else storedWeekly)?.toPlainString() ?: ""
    var isMonthly by remember { mutableStateOf(persistedMonthly) }
    var amountText by remember { mutableStateOf(persistedAmount) }
    // Baseline the Save button compares against: advanced on Save and re-synced whenever the saved
    // value loads or changes elsewhere (only while the buffer holds no unsaved edits).
    var submittedMonthly by remember { mutableStateOf(persistedMonthly) }
    var submittedAmount by remember { mutableStateOf(persistedAmount) }
    LaunchedEffect(persistedMonthly, persistedAmount) {
        if (isMonthly == submittedMonthly && amountText == submittedAmount) {
            isMonthly = persistedMonthly
            amountText = persistedAmount
        }
        submittedMonthly = persistedMonthly
        submittedAmount = persistedAmount
    }
    val budgetDirty = isMonthly != submittedMonthly || amountText != submittedAmount
    // Switching period converts the current amount to the new period so the field stays meaningful.
    fun selectPeriod(monthly: Boolean) {
        if (monthly == isMonthly) return
        amountText.toBudgetAmount()?.let { amt ->
            val converted = if (monthly) weeklyToMonthly(amt) else monthlyToWeekly(amt)
            amountText = converted.setScale(0, RoundingMode.HALF_UP).toPlainString()
        }
        isMonthly = monthly
    }
    fun saveBudget() {
        onSaveSingleBudget(isMonthly, amountText)
        submittedMonthly = isMonthly
        submittedAmount = amountText
    }
    val activeSpent = if (isMonthly) monthlySpent else weeklySpent
    val activeBudget = if (isMonthly) storedMonthly else storedWeekly
    val activeLabel = stringResource(if (isMonthly) R.string.budget_monthly else R.string.budget_weekly)

    // Aggregate spend for a top-level group = its own spend plus all its sub-categories'.
    fun groupSpend(group: Categories.Predefined): BigDecimal =
        Categories.children(group.name).fold(spending[group.name] ?: BigDecimal.ZERO) { acc, child ->
            acc + (spending[child.name] ?: BigDecimal.ZERO)
        }

    // The top-level group whose sub-budget sheet is open, if any.
    var sheetGroup by remember { mutableStateOf<Categories.Predefined?>(null) }

    // The income/recurring add-or-edit sheet, if open.
    var recurringDraft by remember { mutableStateOf<RecurringDraft?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_budgets)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        val hasIncome = recurring.income.isNotEmpty()
        val hasBills = recurring.bills.isNotEmpty()
        val billLimitReached =
            !isPremium && recurring.bills.size >= RecurringRepository.FREE_RECURRING_LIMIT
        // Every sub-category that already has a budget, surfaced for inline editing above the groups.
        val activeSubs = Categories.groups.flatMap { group ->
            Categories.children(group.name)
                .filter { (budgets[BudgetRepository.categoryKey(it.name)]?.signum() ?: 0) > 0 }
                .map { ActiveSub(group, it) }
        }

        // ── The money summary: income, recurring payments, breakdown, and the spending budget.
        // This is the whole screen's left half in landscape and the top of the single column
        // otherwise. ──
        val moneyPane: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.cardSpacing),
            ) {
                if (!hasIncome && !hasBills) {
                    Text(
                        text = stringResource(R.string.recurring_money_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs, vertical = MaterialTheme.dimens.xs),
                    )
                }
                if (hasIncome) {
                    IncomeCard(
                        income = recurring.income,
                        monthlyIncome = recurring.monthlyIncome,
                        onEdit = { recurringDraft = RecurringDraft(it, isIncome = true) },
                        onAdd = { recurringDraft = RecurringDraft(null, isIncome = true) },
                    )
                } else {
                    SlimAddRow(
                        text = stringResource(R.string.recurring_add_income),
                        onClick = { recurringDraft = RecurringDraft(null, isIncome = true) },
                    )
                }
                if (hasBills) {
                    RecurringCard(
                        bills = recurring.bills,
                        monthlyBills = recurring.monthlyBills,
                        atLimit = billLimitReached,
                        onEdit = { recurringDraft = RecurringDraft(it, isIncome = false) },
                        onAdd = { recurringDraft = RecurringDraft(null, isIncome = false) },
                        onUpgrade = onOpenPaywall,
                    )
                } else {
                    SlimAddRow(
                        text = stringResource(R.string.recurring_add_payment),
                        onClick = { recurringDraft = RecurringDraft(null, isIncome = false) },
                    )
                }
                // Breakdown only once income exists — it's meaningless without it.
                if (hasIncome) {
                    BreakdownCard(
                        monthlyIncome = recurring.monthlyIncome,
                        monthlyBills = recurring.monthlyBills,
                        spent = monthlySpent,
                    )
                }
                // Header that names the existing spending budget, so it reads apart from income/bills.
                SpendingBudgetHeader()
                BudgetPeriodToggle(isMonthly = isMonthly, onSelect = { selectPeriod(it) })
                BudgetAmountCard(
                    label = activeLabel,
                    value = amountText,
                    emphasized = true,
                    spent = activeSpent,
                    budget = activeBudget,
                    onChange = { amountText = it },
                )
                // Live "≈ X / other-period" equivalent, so the single amount reads at both cadences.
                amountText.toBudgetAmount()?.let { amt ->
                    val equivalent = if (isMonthly) monthlyToWeekly(amt) else weeklyToMonthly(amt)
                    val res = if (isMonthly) R.string.budget_approx_weekly else R.string.budget_approx_monthly
                    Text(
                        text = stringResource(res, equivalent.formatMoney()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
                    )
                }
                // Save appears only while the budget has unsaved changes.
                if (budgetDirty) {
                    Button(
                        onClick = { saveBudget() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimens.buttonHeight),
                    ) {
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // ── The category allocations: the screen's right half in landscape, below the money summary
        // otherwise. [singleColumn] renders one card of rows (tablet / two-pane); false is the
        // phone's two-up grid of boxes. ──
        val categoryPane: @Composable (singleColumn: Boolean) -> Unit = { singleColumn ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.cardSpacing),
            ) {
                if (activeSubs.isNotEmpty()) {
                    ActiveSubBudgetsSection(
                        activeSubs = activeSubs,
                        spending = spending,
                        budgets = budgets,
                        valueFor = ::valueFor,
                        onChange = ::onChange,
                        onOpenParent = { sheetGroup = it },
                    )
                }
                CategoriesHeader()
                if (singleColumn) {
                    // One card, a row per top-level group — matches the TabletPortrait / two-pane design.
                    CategoryBudgetList(
                        groups = Categories.groups,
                        spendOf = { groupSpend(it) },
                        budgets = budgets,
                        onOpenGroup = { sheetGroup = it },
                    )
                } else {
                    // The phone's two-up grid of category boxes; tapping one opens its sub-budget sheet.
                    Categories.groups.chunked(2).forEach { rowGroups ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
                        ) {
                            rowGroups.forEach { group ->
                                val children = Categories.children(group.name)
                                CategoryGroupBox(
                                    emoji = group.emoji,
                                    name = categoryDisplayName(group.name),
                                    spent = groupSpend(group),
                                    budget = budgets[BudgetRepository.categoryKey(group.name)],
                                    activeSubCount = children.count {
                                        (budgets[BudgetRepository.categoryKey(it.name)]?.signum() ?: 0) > 0
                                    },
                                    totalSubCount = children.size,
                                    onClick = { sheetGroup = group },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                            repeat(2 - rowGroups.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        if (isWide) {
            // Landscape tablet: two independently-scrolling panes — money summary | categories.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = MaterialTheme.dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.41f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = MaterialTheme.dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.cardSpacing),
                ) { moneyPane() }
                Column(
                    modifier = Modifier
                        .weight(0.59f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = MaterialTheme.dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.cardSpacing),
                ) { categoryPane(true) }
            }
        } else {
            // Phone & portrait tablet: one column. The portrait tablet caps and centres it (single pane).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = (if (isExpanded) Modifier.widthIn(max = SinglePaneMaxWidth) else Modifier.fillMaxWidth())
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.cardSpacing),
                ) {
                    moneyPane()
                    categoryPane(isExpanded)
                }
            }
        }
    }

    sheetGroup?.let { group ->
        CategoryBudgetSheet(
            group = group,
            valueFor = ::valueFor,
            budgets = budgets,
            spending = spending,
            onChange = ::onChange,
            onDismiss = { sheetGroup = null },
        )
    }

    recurringDraft?.let { draft ->
        RecurringEntrySheet(
            draft = draft,
            customActions = customActions,
            onSave = { label, amount, category, cadence, dueDay ->
                onSaveRecurring(draft.original, label, amount, draft.isIncome, category, cadence, dueDay)
                recurringDraft = null
            },
            onDelete = {
                draft.original?.let { onDeleteRecurring(it.id) }
                recurringDraft = null
            },
            onDismiss = { recurringDraft = null },
        )
    }
}

/** "Categories" title with the "Tap to set sub-budgets" subtitle beside it, baseline-aligned. */
@Composable
private fun CategoriesHeader() {
    Row(modifier = Modifier.padding(top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.xs)) {
        Text(
            text = stringResource(R.string.budget_categories),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.budget_tap_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** Monthly/Weekly period selector. Picking a period keeps a single budget, viewed at that cadence. */
@Composable
private fun BudgetPeriodToggle(
    isMonthly: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Shares [SegmentedToggle] with the History "Receipts | Items" toggle so both read identically.
    SegmentedToggle(
        options = listOf(
            stringResource(R.string.budget_period_monthly),
            stringResource(R.string.budget_period_weekly),
        ),
        selectedIndex = if (isMonthly) 0 else 1,
        onSelect = { onSelect(it == 0) },
        modifier = modifier,
    )
}

/**
 * Monthly/Weekly budget card. The amount field is a filled (background-tinted, borderless) input
 * whose value and currency render at the same weight; once a budget is set, a "spent · % / left"
 * line and a status-colored progress bar appear beneath it.
 */
@Composable
private fun BudgetAmountCard(
    label: String,
    value: String,
    emphasized: Boolean,
    spent: BigDecimal,
    budget: BigDecimal?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBudget = budget != null && budget.signum() > 0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            Text(
                text = label,
                style = if (emphasized) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            // Filled field: a tinted, borderless box; value and the currency suffix share one style.
            val amountStyle = MaterialTheme.typography.headlineSmall
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder = { Text("0", style = amountStyle) },
                suffix = { Text(AppFormats.currencySymbol, style = amountStyle) },
                textStyle = amountStyle,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = sheetFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasBudget) {
                val color = budgetColor(spent, budget!!)
                val remaining = budget.subtract(spent)
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.budget_spent_pct, spent.formatMoney(), usedPercent(spent, budget)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (remaining.signum() >= 0) stringResource(R.string.budget_left, remaining.formatMoney())
                        else stringResource(R.string.budget_over, remaining.negate().formatMoney()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { budgetRatio(spent, budget) },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                )
            }
        }
    }
}

/**
 * A top-level category, presented as a tappable box in the grid: emoji + (shortened) name, then —
 * once a budget is set — a "spent / limit" line above a status-colored progress bar, otherwise a
 * "Tap to set budget" prompt. Tapping opens the group's sub-budget sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryGroupBox(
    emoji: String,
    name: String,
    spent: BigDecimal,
    budget: BigDecimal?,
    activeSubCount: Int,
    totalSubCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBudget = budget != null && budget.signum() > 0
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.lg),
        ) {
            // Emoji top-left; a "set / total sub-categories" count badge top-right (groups with subs).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(text = emoji, fontSize = 26.sp)
                if (totalSubCount > 0) {
                    Text(
                        text = "$activeSubCount/$totalSubCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hasBudget) {
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(
                        text = budgetSummary(spent, budget!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (hasBudget) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { budgetRatio(spent, budget) },
                    color = budgetColor(spent, budget!!),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimens.sm)
                        .clip(RoundedCornerShape(50)),
                )
            } else {
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.budget_tap_set),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The top-level category budgets as a single card of rows — one row per group — used by the tablet
 * single-pane and landscape two-pane layouts (the phone keeps the two-up [CategoryGroupBox] grid).
 * Matches the `TabletPortrait` / two-pane design: tile, name over a spent/limit bar (or a "Tap to
 * set budget" prompt), and a chevron; tapping a row opens that group's sub-budget sheet.
 */
@Composable
private fun CategoryBudgetList(
    groups: List<Categories.Predefined>,
    spendOf: (Categories.Predefined) -> BigDecimal,
    budgets: Map<String, BigDecimal>,
    onOpenGroup: (Categories.Predefined) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        groups.forEachIndexed { index, group ->
            CategoryBudgetRow(
                emoji = group.emoji,
                colorArgb = group.colorArgb,
                name = categoryDisplayName(group.name),
                spent = spendOf(group),
                budget = budgets[BudgetRepository.categoryKey(group.name)],
                onClick = { onOpenGroup(group) },
            )
            if (index < groups.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = MaterialTheme.dimens.lg + 38.dp + MaterialTheme.dimens.md),
                )
            }
        }
    }
}

/** One [CategoryBudgetList] row: a category tile + name over a progress bar (or prompt), a
 *  spent/limit figure, and a chevron. */
@Composable
private fun CategoryBudgetRow(
    emoji: String,
    colorArgb: Int,
    name: String,
    spent: BigDecimal,
    budget: BigDecimal?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBudget = budget != null && budget.signum() > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(colorArgb).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasBudget) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { budgetRatio(spent, budget!!) },
                    color = budgetColor(spent, budget!!),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                )
            }
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        if (hasBudget) {
            Text(
                text = budgetSummary(spent, budget!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        } else {
            Text(
                text = stringResource(R.string.budget_tap_set),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = MaterialTheme.dimens.xs),
        )
    }
}

/** One row of the "Active sub-budgets" block: a sub-category and the group it belongs to. */
private data class ActiveSub(val group: Categories.Predefined, val child: Categories.Predefined)

/** Max active sub-budget rows shown before the "Show N more" expander. */
private const val ACTIVE_SUBS_CAP = 3

/**
 * "Active sub-budgets" block: every sub-category that currently has a budget, pulled out of its
 * group sheet into one inline-editable list (grouped by parent), so the user can review and adjust
 * them all in one place. Field fills and bar tracks reuse the app's tokens (surfaceContainerLow /
 * surfaceContainerHighest) rather than the mockup's tinted ones. Capped at [ACTIVE_SUBS_CAP] rows
 * with a "Show N more" expander; tapping a row's parent chip/name opens that group's sheet.
 */
@Composable
private fun ActiveSubBudgetsSection(
    activeSubs: List<ActiveSub>,
    spending: Map<String, BigDecimal>,
    budgets: Map<String, BigDecimal>,
    valueFor: (String) -> String,
    onChange: (String, String) -> Unit,
    onOpenParent: (Categories.Predefined) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val capped = !expanded && activeSubs.size > ACTIVE_SUBS_CAP
    val shown = if (capped) activeSubs.take(ACTIVE_SUBS_CAP) else activeSubs

    Column(modifier = modifier.fillMaxWidth().padding(bottom = MaterialTheme.dimens.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.budget_active_subs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = MaterialTheme.dimens.sm, vertical = 2.dp),
            ) {
                Text(
                    text = activeSubs.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            shown.forEach { sub ->
                val key = BudgetRepository.categoryKey(sub.child.name)
                ActiveSubBudgetRow(
                    sub = sub,
                    spent = spending[sub.child.name] ?: BigDecimal.ZERO,
                    budget = budgets[key],
                    value = valueFor(key),
                    onChange = { onChange(key, it) },
                    onOpenParent = { onOpenParent(sub.group) },
                )
            }
            if (capped) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.budget_show_more, activeSubs.size - ACTIVE_SUBS_CAP),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** One inline row in the active sub-budgets block: parent chip + sub/parent names (tap to open the
 *  group sheet), a spent line, a compact budget field, and a status bar — same data as the sheet row. */
@Composable
private fun ActiveSubBudgetRow(
    sub: ActiveSub,
    spent: BigDecimal,
    budget: BigDecimal?,
    value: String,
    onChange: (String) -> Unit,
    onOpenParent: () -> Unit,
) {
    val hasBudget = budget != null && budget.signum() > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                    .clickable(onClick = onOpenParent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                        .background(Color(sub.group.colorArgb).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = sub.group.emoji, fontSize = 14.sp)
                }
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryDisplayName(sub.child.name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = categoryDisplayName(sub.group.name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = if (spent.signum() > 0) stringResource(R.string.budget_spent, spent.formatMoney())
                else stringResource(R.string.budget_no_spend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            val fieldStyle = MaterialTheme.typography.bodyMedium
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder = { Text("0", style = fieldStyle) },
                suffix = { Text(AppFormats.currencySymbol, style = fieldStyle) },
                textStyle = fieldStyle,
                singleLine = true,
                shape = SheetFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = sheetFieldColors(),
                modifier = Modifier.width(104.dp),
            )
        }
        if (hasBudget) {
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            LinearProgressIndicator(
                progress = { budgetRatio(spent, budget) },
                color = budgetColor(spent, budget!!),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.xs)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}

/**
 * Bottom sheet for one top-level [group]: a large field for the group's own budget, then a
 * scrollable list of its sub-categories, each with a compact budget field plus a spent line and
 * status progress bar. A pinned "Done" button dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBudgetSheet(
    group: Categories.Predefined,
    valueFor: (String) -> String,
    budgets: Map<String, BigDecimal>,
    spending: Map<String, BigDecimal>,
    onChange: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Sub-categories that already have a budget set appear at the top; snapshotted when the sheet
    // opens (keyed on group) so rows don't reshuffle while the user is typing. This order is derived
    // from the saved budgets, so it persists naturally across reopens.
    val children = remember(group) {
        Categories.children(group.name).sortedByDescending { child ->
            val amount = budgets[BudgetRepository.categoryKey(child.name)]
            amount != null && amount.signum() > 0
        }
    }
    val groupKey = BudgetRepository.categoryKey(group.name)

    AdaptiveSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = group.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Text(
                    text = categoryDisplayName(group.name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            SheetSectionLabel(stringResource(R.string.budget_section_category))
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            val amountStyle = MaterialTheme.typography.headlineSmall
            TextField(
                value = valueFor(groupKey),
                onValueChange = { onChange(groupKey, it) },
                placeholder = { Text("0", style = amountStyle) },
                suffix = { Text(AppFormats.currencySymbol, style = amountStyle) },
                textStyle = amountStyle,
                singleLine = true,
                shape = SheetFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = sheetFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (children.isNotEmpty()) {
                Spacer(Modifier.height(MaterialTheme.dimens.xl))
                SheetSectionLabel(stringResource(R.string.budget_section_subcategories))
            }
        }

        // Only this list scrolls — the header above and the Done button below stay pinned.
        // weight(fill = false) keeps the sheet compact for groups with few sub-categories, yet
        // caps and scrolls a long list within the sheet's own bounds so the content never overflows
        // past the sheet edge. A fixed heightIn(max = 420.dp) overflowed on shorter screens, which
        // made the bottom sheet jitter/bounce once the list was scrolled to the end.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.xs),
        ) {
            items(children, key = { it.name }) { child ->
                val key = BudgetRepository.categoryKey(child.name)
                SubcategoryBudgetRow(
                    name = categoryDisplayName(child.name),
                    value = valueFor(key),
                    spent = spending[child.name] ?: BigDecimal.ZERO,
                    budget = budgets[key],
                    onChange = { onChange(key, it) },
                )
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.xl)
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(
                text = stringResource(R.string.action_done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** One sub-category inside the sheet: name + spent stacked on the left, a compact filled budget
 *  field on the right, and — once a budget is set — a status progress bar spanning beneath. */
@Composable
private fun SubcategoryBudgetRow(
    name: String,
    value: String,
    spent: BigDecimal,
    budget: BigDecimal?,
    onChange: (String) -> Unit,
) {
    val hasBudget = budget != null && budget.signum() > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = if (spent.signum() > 0) stringResource(R.string.budget_spent, spent.formatMoney())
                else stringResource(R.string.budget_no_spend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            val fieldStyle = MaterialTheme.typography.bodyLarge
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder = { Text("0", style = fieldStyle) },
                suffix = { Text(AppFormats.currencySymbol, style = fieldStyle) },
                textStyle = fieldStyle,
                singleLine = true,
                shape = SheetFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = sheetFieldColors(),
                modifier = Modifier.width(112.dp),
            )
        }
        if (hasBudget) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { budgetRatio(spent, budget) },
                color = budgetColor(spent, budget!!),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}

// ── Income & recurring payments ──────────────────────────────────────────────────────────────────

/** The recurring entry the add/edit sheet is working on: [original] null = adding a new one. */
private data class RecurringDraft(val original: RecurringEntity?, val isIncome: Boolean)

/** Cadence order shared by the "How often" toggle and its index/label lookups. */
private val CADENCES = listOf(
    RecurringEntity.Cadence.MONTHLY,
    RecurringEntity.Cadence.WEEKLY,
    RecurringEntity.Cadence.YEARLY,
    RecurringEntity.Cadence.ONCE,
)

/** A slim, low-chrome "add" row shown in place of an empty section — keeps it discoverable. */
@Composable
private fun SlimAddRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.xs, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** surfaceContainer card holding a section's header + rows (income or recurring payments). */
@Composable
private fun RecurringSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Header row of a money section: a bold title on the left, a trailing slot (total / limit) right. */
@Composable
private fun RecurringSectionHeader(title: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** One entry row: category/income tile, name + subtitle, and the amount (colored for income). */
@Composable
private fun MoneyRow(
    emoji: String,
    tileColor: Color,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tileColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
            maxLines = 1,
        )
    }
}

/** In-card "add" row (primary-tinted) at the bottom of a populated section. */
@Composable
private fun AddRowInline(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun IncomeCard(
    income: List<RecurringEntity>,
    monthlyIncome: BigDecimal,
    onEdit: (RecurringEntity) -> Unit,
    onAdd: () -> Unit,
) {
    RecurringSectionCard {
        RecurringSectionHeader(stringResource(R.string.recurring_income)) {
            Text(
                text = stringResource(R.string.recurring_per_month, monthlyIncome.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        income.forEach { item ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MoneyRow(
                emoji = "💰",
                tileColor = MaterialTheme.colorScheme.secondaryContainer,
                title = item.label,
                subtitle = recurringSubtitle(item, includeCategory = false),
                amount = "+${item.amount.formatMoney()}",
                amountColor = budgetGoodColor(),
                onClick = { onEdit(item) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AddRowInline(stringResource(R.string.recurring_add_income), onAdd)
    }
}

@Composable
private fun RecurringCard(
    bills: List<RecurringEntity>,
    monthlyBills: BigDecimal,
    atLimit: Boolean,
    onEdit: (RecurringEntity) -> Unit,
    onAdd: () -> Unit,
    onUpgrade: () -> Unit,
) {
    RecurringSectionCard {
        RecurringSectionHeader(stringResource(R.string.recurring_payments)) {
            if (atLimit) {
                Text(
                    text = "${bills.size} / ${RecurringRepository.FREE_RECURRING_LIMIT}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = MaterialTheme.dimens.sm, vertical = 2.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.recurring_per_month, monthlyBills.formatMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        bills.forEach { item ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MoneyRow(
                emoji = Categories.emojiOf(item.category),
                tileColor = Color(Categories.colorOf(item.category)).copy(alpha = 0.16f),
                title = item.label,
                subtitle = recurringSubtitle(item, includeCategory = true),
                amount = item.amount.formatMoney(),
                amountColor = MaterialTheme.colorScheme.onSurface,
                onClick = { onEdit(item) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (atLimit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUpgrade)
                    .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.lg),
                )
                Text(
                    text = stringResource(R.string.recurring_upgrade_more),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        } else {
            AddRowInline(stringResource(R.string.recurring_add_payment), onAdd)
        }
    }
}

/** The monthly breakdown: income − recurring bills − spent = what's left. Shown only with income. */
@Composable
private fun BreakdownCard(monthlyIncome: BigDecimal, monthlyBills: BigDecimal, spent: BigDecimal) {
    val left = monthlyIncome.subtract(monthlyBills).subtract(spent)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.recurring_this_month),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, top = 14.dp, bottom = MaterialTheme.dimens.xs),
            )
            BreakdownLine(
                label = stringResource(R.string.recurring_breakdown_income),
                amount = "+${monthlyIncome.formatMoney()}",
                amountColor = budgetGoodColor(),
                divider = true,
            )
            BreakdownLine(
                label = stringResource(R.string.recurring_breakdown_bills),
                amount = "−${monthlyBills.formatMoney()}",
                amountColor = MaterialTheme.colorScheme.onSurfaceVariant,
                divider = true,
            )
            BreakdownLine(
                label = stringResource(R.string.recurring_breakdown_spent),
                amount = "−${spent.formatMoney()}",
                amountColor = MaterialTheme.colorScheme.onSurfaceVariant,
                divider = false,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.recurring_breakdown_left),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = left.formatMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (left.signum() >= 0) budgetGoodColor() else budgetBadColor(),
                )
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, amount: String, amountColor: Color, divider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = amountColor)
    }
    if (divider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
        )
    }
}

/** "Spending budget" header + hint, so the existing budget reads apart from income/bills. */
@Composable
private fun SpendingBudgetHeader() {
    Column(modifier = Modifier.padding(top = MaterialTheme.dimens.xs)) {
        Text(
            text = stringResource(R.string.budget_spending_budget),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.budget_spending_budget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The shared add/edit sheet for income and recurring payments. Captures name, amount, cadence, the
 * day it lands, and — for a bill — its category (via the existing [CategoryPickerScreen]). Bills carry
 * a category; income does not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEntrySheet(
    draft: RecurringDraft,
    customActions: CustomCategoryActions,
    onSave: (label: String, amount: String, category: String, cadence: String, dueDay: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val original = draft.original
    val isIncome = draft.isIncome
    var name by remember { mutableStateOf(original?.label ?: "") }
    var amount by remember { mutableStateOf(original?.amount?.toPlainString() ?: "") }
    var cadence by remember { mutableStateOf(original?.cadence ?: RecurringEntity.Cadence.MONTHLY) }
    var dueDay by remember { mutableStateOf(original?.dueDay ?: if (isIncome) 25 else 1) }
    var category by remember {
        mutableStateOf(original?.category?.takeIf { it.isNotBlank() } ?: Categories.DEFAULT)
    }
    var pickerOpen by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && amount.toBudgetAmount() != null

    AdaptiveSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        // Scrolls so the whole form (and the Save button) stays reachable on short screens and once the
        // keyboard is up; weight(fill = false) keeps the sheet compact when the form is short.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        when {
                            isIncome && original != null -> R.string.recurring_income_edit_title
                            isIncome -> R.string.recurring_add_income
                            original != null -> R.string.recurring_payment_edit_title
                            else -> R.string.recurring_payment_add_title
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (original != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))

            SheetSectionLabel(stringResource(R.string.recurring_field_name))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        stringResource(
                            if (isIncome) R.string.recurring_name_hint_income
                            else R.string.recurring_name_hint_payment,
                        ),
                    )
                },
                singleLine = true,
                shape = SheetFieldShape,
                colors = sheetFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SheetSectionLabel(stringResource(R.string.recurring_field_amount))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' || it == ',' } },
                placeholder = { Text("0") },
                suffix = { Text(AppFormats.currencySymbol) },
                singleLine = true,
                shape = SheetFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = sheetFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SheetSectionLabel(stringResource(R.string.recurring_field_how_often))
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.recurring_monthly),
                    stringResource(R.string.recurring_weekly),
                    stringResource(R.string.recurring_yearly),
                    stringResource(R.string.recurring_once),
                ),
                selectedIndex = CADENCES.indexOf(cadence).coerceAtLeast(0),
                onSelect = { index ->
                    cadence = CADENCES.getOrElse(index) { RecurringEntity.Cadence.MONTHLY }
                    dueDay = if (cadence == RecurringEntity.Cadence.WEEKLY) dueDay.coerceIn(1, 7)
                    else dueDay.coerceIn(1, 31)
                },
            )

            // A one-time entry has no recurring day, so the day/payday picker is hidden for it.
            if (cadence != RecurringEntity.Cadence.ONCE) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                SheetSectionLabel(
                    stringResource(
                        when {
                            cadence == RecurringEntity.Cadence.WEEKLY -> R.string.recurring_field_day_of_week
                            isIncome -> R.string.recurring_field_payday
                            else -> R.string.recurring_field_due_day
                        },
                    ),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                DayStepper(cadence = cadence, value = dueDay, onChange = { dueDay = it })
            }

            if (!isIncome) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                SheetSectionLabel(stringResource(R.string.recurring_field_category))
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SheetFieldShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { pickerOpen = true }
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(MaterialTheme.dimens.xxxl)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(Categories.colorOf(category)).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(Categories.emojiOf(category), fontSize = 17.sp)
                    }
                    Text(
                        text = categoryDisplayName(category),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(MaterialTheme.dimens.xl))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { if (valid) onSave(name, amount, category, cadence, dueDay) },
                    enabled = valid,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
        }
    }

    if (pickerOpen) {
        CategoryPickerScreen(
            selected = category,
            onSelect = { category = it },
            onDismiss = { pickerOpen = false },
            custom = customActions,
        )
    }
}

/** −/+ stepper for the day the entry lands: weekday name when weekly, otherwise the day-of-month. */
@Composable
private fun DayStepper(cadence: String, value: Int, onChange: (Int) -> Unit) {
    val max = if (cadence == RecurringEntity.Cadence.WEEKLY) 7 else 31
    val display = if (cadence == RecurringEntity.Cadence.WEEKLY) {
        DayOfWeek.of(value.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Locale.getDefault())
    } else {
        value.coerceIn(1, 31).toString()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SheetFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(if (value > 1) value - 1 else max) }) {
            Icon(Icons.Filled.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = display,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(if (value < max) value + 1 else 1) }) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Parses a budget field's text into a positive amount, or null if blank/invalid/non-positive. */
private fun String.toBudgetAmount(): BigDecimal? =
    replace(',', '.').trim().toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

/** Rounded shape for the sheet's filled input fields — matches the upload/edit receipt form. */
private val SheetFieldShape = RoundedCornerShape(12.dp)

/**
 * Filled, borderless, light-fill field colors used inside the sub-budget sheet — the same
 * treatment as the upload/edit receipt screen (light inset fill, no indicator line).
 */
@Composable
private fun sheetFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    // Clearly-muted placeholder so an empty field reads as a hint, not as already-filled text.
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
)

/** Small uppercase section label used inside the sub-budget sheet. */
@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
    )
}

/** Compact whole-number "spent / limit" summary for a category box, e.g. "242 / 400 лв". */
private fun budgetSummary(spent: BigDecimal, budget: BigDecimal): String {
    val s = spent.setScale(0, RoundingMode.HALF_UP).toPlainString()
    val b = budget.setScale(0, RoundingMode.HALF_UP).toPlainString()
    return "$s / $b ${AppFormats.currencySymbol}"
}

/** Percentage of [budget] consumed by [spent], rounded to a whole number (unclamped). */
private fun usedPercent(spent: BigDecimal, budget: BigDecimal): Int =
    if (budget.signum() <= 0) 0 else (spent.toDouble() / budget.toDouble() * 100).roundToInt()

private val previewBudgets = mapOf(
    BudgetRepository.MONTHLY to BigDecimal("1200"),
    BudgetRepository.WEEKLY to BigDecimal("300"),
    BudgetRepository.categoryKey("Groceries") to BigDecimal("400"),
    BudgetRepository.categoryKey("Dining & Entertainment") to BigDecimal("150"),
)

private val previewSpending = mapOf(
    "Groceries" to BigDecimal("180"),
    "Bakery" to BigDecimal("62"),
    "Restaurant & Dining" to BigDecimal("114"),
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BudgetScreenPreview() {
    BudgettyTheme {
        BudgetScreenContent(
            budgets = previewBudgets,
            spending = previewSpending,
            monthlySpent = BigDecimal("712.40"),
            weeklySpent = BigDecimal("73.20"),
            isExpanded = false,
            isWide = false,
            onNavigateBack = {},
            onSetBudget = { _, _ -> },
            onSaveSingleBudget = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun BudgetScreenTabletPreview() {
    BudgettyTheme {
        BudgetScreenContent(
            budgets = previewBudgets,
            spending = previewSpending,
            monthlySpent = BigDecimal("712.40"),
            weeklySpent = BigDecimal("73.20"),
            isExpanded = true,
            isWide = true,
            onNavigateBack = {},
            onSetBudget = { _, _ -> },
            onSaveSingleBudget = { _, _ -> },
        )
    }
}
