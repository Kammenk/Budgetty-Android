package com.budgetty.app.ui.history

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.Receipt
import com.budgetty.app.ui.components.PriceRangeSheet
import com.budgetty.app.ui.components.ReceiptDetailContent
import com.budgetty.app.ui.components.ReceiptDetailSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.budgetty.app.ui.components.EmptyState
import com.budgetty.app.ui.home.DateRangeFilter
import com.budgetty.app.ui.util.formatDayHeader
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.formatMonth
import com.budgetty.app.ui.util.recurringSubtitle
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToReceipt: (Long) -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreenContent(
        state = state,
        isExpanded = isExpandedWidth(),
        isWide = isWideWidth(),
        onNavigateToReceipt = onNavigateToReceipt,
        onNavigateToBudget = onNavigateToBudget,
        onQueryChange = viewModel::onQueryChange,
        onClearFilters = viewModel::clearFilters,
        onCategorySelected = viewModel::onCategorySelected,
        onStoreSelected = viewModel::onStoreSelected,
        onDateSelected = viewModel::onDateSelected,
        onBudgetPeriodSelected = viewModel::onBudgetPeriodSelected,
        onSortSelected = viewModel::onSortSelected,
        onPriceRangeSelected = viewModel::onPriceRangeSelected,
        onCommitSearch = viewModel::commitRecentSearch,
        onRemoveRecent = viewModel::removeRecentSearch,
        onClearRecent = viewModel::clearRecentSearches,
        onDeleteReceipt = viewModel::deleteReceipt,
        onDeleteTransaction = viewModel::deleteTransaction,
        onUndoLastDelete = viewModel::undoLastDelete,
        modifier = modifier,
    )
}

@Composable
private fun HistoryScreenContent(
    state: HistoryUiState,
    isExpanded: Boolean,
    isWide: Boolean,
    onNavigateToReceipt: (Long) -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onStoreSelected: (String?) -> Unit,
    onDateSelected: (DateRangeFilter?) -> Unit,
    onBudgetPeriodSelected: (DateRangeFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onPriceRangeSelected: (BigDecimal?, BigDecimal?) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onDeleteReceipt: (Receipt) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onUndoLastDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }
    var showPriceSheet by remember { mutableStateOf(false) }
    // Receipts vs Items is a pure view toggle over the same data, so it lives in UI state.
    var mode by remember { mutableStateOf(HistoryMode.RECEIPTS) }
    // Tapping a receipt opens the same detail sheet as Home (rather than jumping into the editor).
    var selectedReceiptId by remember { mutableStateOf<Long?>(null) }
    // Derived from live state so the sheet's items update as they're deleted, and it closes itself
    // when the whole receipt is removed.
    val selectedReceipt = state.receiptGroups
        .asSequence()
        .flatMap { it.days.asSequence() }
        .flatMap { it.receipts.asSequence() }
        .find { it.id == selectedReceiptId }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here (not in the coroutine) because stringResource is @Composable-only.
    val receiptDeletedMsg = stringResource(R.string.snackbar_receipt_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    // The list + controls column — the whole screen on phone/portrait, and the left panel of the
    // landscape two-pane. [colModifier] sizes it (capped single-pane, or a weighted panel).
    val historyColumn: @Composable (Modifier) -> Unit = { colModifier ->
        Column(modifier = colModifier.fillMaxHeight()) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, top = MaterialTheme.dimens.xxl, bottom = MaterialTheme.dimens.lg),
            )

            // Until the first DB load lands, show just the header rather than flashing an empty state.
            if (!state.isLoaded) return@Column

            // Truly empty — no receipts AND no money plan: skip the controls, show one empty state.
            if (!state.hasAnyTransactions && !state.hasBudgetPlan) {
                EmptyBox(
                    modifier = Modifier.weight(1f),
                    emoji = "🧾",
                    title = "No transactions yet",
                    subtitle = "Items from your saved receipts will appear here.",
                )
                return@Column
            }

            // Search + filters apply to receipts/items only; the Budgets tab is a plan snapshot, not
            // searchable, so both are hidden there (matching the design).
            val showControls = mode != HistoryMode.BUDGETS && state.hasAnyTransactions
            if (showControls) {
                SearchField(
                    query = state.filters.query,
                    onQueryChange = onQueryChange,
                    onFocusChange = { focused ->
                        // Leaving the field with a non-blank query records it as a recent search.
                        if (searchFocused && !focused) {
                            state.filters.query.trim().takeIf { it.isNotEmpty() }?.let(onCommitSearch)
                        }
                        searchFocused = focused
                    },
                    onSearch = {
                        state.filters.query.trim().takeIf { it.isNotEmpty() }?.let(onCommitSearch)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }
            HistoryModeToggle(
                mode = mode,
                onModeChange = { mode = it },
                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
            )
            Spacer(Modifier.height(10.dp))
            if (showControls) {
                FilterRow(
                    state = state,
                    onClearAll = onClearFilters,
                    onCategory = onCategorySelected,
                    onStore = onStoreSelected,
                    onDate = onDateSelected,
                    onSort = onSortSelected,
                    onOpenPrice = { showPriceSheet = true },
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            } else if (mode == HistoryMode.BUDGETS && state.hasBudgetPlan) {
                // The Budgets tab has just the one Date chip — no store/category/price for a plan —
                // sitting in the same slot as the other tabs' filter row.
                BudgetFilterRow(
                    selected = state.budgetPeriod,
                    onSelect = onBudgetPeriodSelected,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            }

            // While the search field is focused but empty, offer recent searches + quick filters
            // instead of the full list — the "quick find" entry point.
            val showQuickFind = searchFocused && state.filters.query.isBlank()
            val activeEmpty = if (mode == HistoryMode.RECEIPTS) state.receiptGroups.isEmpty() else state.groups.isEmpty()
            when {
                mode == HistoryMode.BUDGETS -> HistoryBudgetsTab(
                    state = state,
                    onManage = onNavigateToBudget,
                    modifier = Modifier.weight(1f),
                )

                showQuickFind -> QuickFindPanel(
                    recentSearches = state.recentSearches,
                    topStores = state.topStores,
                    topCategories = state.topCategories,
                    onRecentClick = onQueryChange,
                    onRecentRemove = onRemoveRecent,
                    onClearRecent = onClearRecent,
                    onStoreClick = { store -> onStoreSelected(store); focusManager.clearFocus() },
                    onCategoryClick = { category -> onCategorySelected(category); focusManager.clearFocus() },
                    modifier = Modifier.weight(1f),
                )

                activeEmpty -> EmptyBox(
                    modifier = Modifier.weight(1f),
                    // With a money plan but no receipts yet, say so plainly instead of "no matches".
                    emoji = if (state.hasAnyTransactions) "🔍" else "🧾",
                    title = when {
                        !state.hasAnyTransactions -> "No transactions yet"
                        mode == HistoryMode.RECEIPTS -> "No matching receipts"
                        else -> "No matching items"
                    },
                    subtitle = if (state.hasAnyTransactions) {
                        "Try a different search, or clear your filters."
                    } else {
                        "Items from your saved receipts will appear here."
                    },
                )

                mode == HistoryMode.RECEIPTS -> {
                    // Day headers group receipts by date exactly like the Items tab; each day can be
                    // collapsed to just its header (keyed by date, unique across the whole list).
                    var collapsedDays by remember { mutableStateOf(emptySet<LocalDate>()) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        state.receiptGroups.forEach { group ->
                            item(key = "rmonth-${group.month}") { ReceiptMonthHeader(group) }
                            group.days.forEach { day ->
                                val expanded = day.day !in collapsedDays
                                item(key = "rday-${day.day}") {
                                    DayHeader(
                                        day = day.day,
                                        total = day.total,
                                        expanded = expanded,
                                        onToggle = {
                                            collapsedDays = if (day.day in collapsedDays) {
                                                collapsedDays - day.day
                                            } else {
                                                collapsedDays + day.day
                                            }
                                        },
                                    )
                                }
                                // Collapsed: render only the header, skipping this day's receipts.
                                if (!expanded) return@forEach
                                items(day.receipts, key = { it.id }) { receipt ->
                                    ReceiptHistoryRow(
                                        receipt = receipt,
                                        onClick = { selectedReceiptId = receipt.id },
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Single column on phones/portrait; a multi-column card grid in landscape, matching
                    // the TabletLs History design's 3-column item grid.
                    val columns = 1
                    // Days the user has collapsed (keyed by date, which is unique across the whole list).
                    // Empty by default, so every day starts expanded; collapsing hides only its items.
                    var collapsedDays by remember { mutableStateOf(emptySet<LocalDate>()) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        state.groups.forEach { group ->
                            item(key = "month-${group.month}") { MonthHeader(group) }
                            group.days.forEach { day ->
                                val expanded = day.day !in collapsedDays
                                item(key = "day-${day.day}") {
                                    DayHeader(
                                        day = day.day,
                                        total = day.total,
                                        expanded = expanded,
                                        onToggle = {
                                            collapsedDays = if (day.day in collapsedDays) {
                                                collapsedDays - day.day
                                            } else {
                                                collapsedDays + day.day
                                            }
                                        },
                                    )
                                }
                                // Collapsed: render only the header, skipping this day's items.
                                if (!expanded) return@forEach
                                if (columns == 1) {
                                    items(day.items, key = { it.transaction.id }) { item ->
                                        HistoryRow(item)
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                } else {
                                    items(
                                        day.items.chunked(columns),
                                        key = { it.first().transaction.id },
                                    ) { rowItems ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.xs),
                                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
                                        ) {
                                            rowItems.forEach { historyItem ->
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                                    shape = RoundedCornerShape(14.dp),
                                                ) {
                                                    HistoryRow(historyItem)
                                                }
                                            }
                                            repeat(columns - rowItems.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The landscape detail panel: the selected receipt inline, or a placeholder prompting a pick.
    val detailPane: @Composable () -> Unit = {
        val sel = selectedReceipt
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            if (sel != null) {
                ReceiptDetailContent(
                    receipt = sel,
                    onEditReceipt = {
                        selectedReceiptId = null
                        onNavigateToReceipt(sel.id)
                    },
                    onDeleteItem = onDeleteTransaction,
                    onUndo = onUndoLastDelete,
                    onDeleteReceipt = {
                        onDeleteReceipt(sel)
                        selectedReceiptId = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = receiptDeletedMsg,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) onUndoLastDelete()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🧾", fontSize = 40.sp)
                        Spacer(Modifier.height(MaterialTheme.dimens.sm))
                        Text(
                            text = "Select a receipt to see its items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isWide) {
            // Landscape: receipt list on the left, the selected receipt's detail on the right.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.dimens.screenPadding)
                    .padding(bottom = MaterialTheme.dimens.lg),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
            ) {
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                    historyColumn(Modifier.fillMaxWidth())
                }
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(top = MaterialTheme.dimens.xxl),
                ) {
                    detailPane()
                }
            }
        } else {
            // Phone & portrait tablet: the single column, capped and centred on the portrait tablet.
            historyColumn(
                if (isExpanded) Modifier.widthIn(max = SinglePaneMaxWidth) else Modifier.fillMaxWidth(),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showPriceSheet) {
        PriceRangeSheet(
            initialMin = state.filters.priceMin,
            initialMax = state.filters.priceMax,
            upperBound = state.priceUpperBound,
            onConfirm = { min, max ->
                onPriceRangeSelected(min, max)
                showPriceSheet = false
            },
            onDismiss = { showPriceSheet = false },
        )
    }

    if (selectedReceipt != null && !isWide) {
        ReceiptDetailSheet(
            receipt = selectedReceipt,
            onDismiss = { selectedReceiptId = null },
            onEditReceipt = {
                // Close the sheet before navigating so it doesn't reappear on the way back.
                selectedReceiptId = null
                onNavigateToReceipt(selectedReceipt.id)
            },
            onDeleteItem = onDeleteTransaction,
            onUndo = onUndoLastDelete,
            onDeleteReceipt = {
                onDeleteReceipt(selectedReceipt)
                selectedReceiptId = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = receiptDeletedMsg,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndoLastDelete()
                }
            },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.history_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(percent = 50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
    )
}

@Composable
private fun FilterRow(
    state: HistoryUiState,
    onClearAll: () -> Unit,
    onCategory: (String?) -> Unit,
    onStore: (String?) -> Unit,
    onDate: (DateRangeFilter?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onOpenPrice: () -> Unit,
) {
    val f = state.filters
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        HistoryFilterChip(selected = f.isEmpty, onClick = onClearAll, label = stringResource(R.string.filter_all))
        DropdownChip(
            label = stringResource(R.string.filter_category),
            allLabel = "All categories",
            selected = f.category,
            options = state.categories.map {
                ChipOption(value = it, label = "${Categories.emojiOf(it)} ${categoryDisplayName(context, it)}".trim())
            },
            onSelect = onCategory,
        )
        DropdownChip(
            label = stringResource(R.string.filter_store),
            allLabel = "All stores",
            selected = f.store,
            options = state.stores.map { ChipOption(value = it, label = it) },
            onSelect = onStore,
        )
        DateChip(selected = f.date, onSelect = onDate)
        HistoryFilterChip(
            selected = f.hasPrice,
            onClick = onOpenPrice,
            label = stringResource(R.string.filter_price),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        SortChip(current = state.sort, onSelect = onSort)
    }
}

/** A value the chip filters by, plus the label shown for it in the menu / on the chip. */
private data class ChipOption(val value: String, val label: String)

/** A borderless [FilterChip] filled to match the search field when unselected. */
@Composable
private fun HistoryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = trailingIcon,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

/** A [FilterChip] that opens a dropdown of [options]; the chip lights up while a value is picked. */
@Composable
private fun DropdownChip(
    label: String,
    allLabel: String,
    selected: String?,
    options: List<ChipOption>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label ?: label
    Box {
        HistoryFilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = selectedLabel,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(text = allLabel, checked = selected == null) {
                onSelect(null)
                expanded = false
            }
            options.forEach { option ->
                MenuItem(text = option.label, checked = option.value == selected) {
                    onSelect(option.value)
                    expanded = false
                }
            }
        }
    }
}

/** The Date filter chip: the preset windows plus an "All time" reset. */
@Composable
private fun DateChip(
    selected: DateRangeFilter?,
    onSelect: (DateRangeFilter?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HistoryFilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = selected?.let { stringResource(it.labelRes) } ?: stringResource(R.string.filter_date),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(text = stringResource(R.string.filter_all_time), checked = selected == null) {
                onSelect(null)
                expanded = false
            }
            DateRangeFilter.entries.forEach { option ->
                MenuItem(text = stringResource(option.labelRes), checked = option == selected) {
                    onSelect(option)
                    expanded = false
                }
            }
        }
    }
}

/** The Budgets tab's filter row — just the period chip, since a plan has no store/category/price. */
@Composable
private fun BudgetFilterRow(
    selected: DateRangeFilter,
    onSelect: (DateRangeFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        BudgetPeriodChip(selected = selected, onSelect = onSelect)
    }
}

/** Like [DateChip] but for the plan snapshot: always a concrete period, so there's no "All time". */
@Composable
private fun BudgetPeriodChip(
    selected: DateRangeFilter,
    onSelect: (DateRangeFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HistoryFilterChip(
            // Highlighted once narrowed away from the default month, matching the other filter chips.
            selected = selected != DateRangeFilter.CURRENT_MONTH,
            onClick = { expanded = true },
            label = stringResource(selected.labelRes),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DateRangeFilter.entries.forEach { option ->
                MenuItem(text = stringResource(option.labelRes), checked = option == selected) {
                    onSelect(option)
                    expanded = false
                }
            }
        }
    }
}

/** The Sort chip: lights up when a non-default order is active; opens a single-select menu. */
@Composable
private fun SortChip(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HistoryFilterChip(
            selected = current != SortOrder.NEWEST,
            onClick = { expanded = true },
            label = stringResource(R.string.filter_sort),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = stringResource(R.string.sort_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.xs),
            )
            SortOrder.entries.forEach { order ->
                MenuItem(text = stringResource(order.labelRes()), checked = order == current) {
                    onSelect(order)
                    expanded = false
                }
            }
        }
    }
}

private fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.NEWEST -> R.string.sort_newest
    SortOrder.OLDEST -> R.string.sort_oldest
    SortOrder.PRICE_HIGH -> R.string.sort_price_high
    SortOrder.PRICE_LOW -> R.string.sort_price_low
}

@Composable
private fun MenuItem(text: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        trailingIcon = if (checked) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
    )
}

/** Shown when the search field is focused but empty: recent searches + most-used stores/categories. */
@Composable
private fun QuickFindPanel(
    recentSearches: List<String>,
    topStores: List<String>,
    topCategories: List<String>,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onClearRecent: () -> Unit,
    onStoreClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.xs),
    ) {
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickFindHeader(stringResource(R.string.history_recent_searches))
                Text(
                    text = stringResource(R.string.history_clear_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                        .clickable(onClick = onClearRecent)
                        .padding(horizontal = MaterialTheme.dimens.xs, vertical = 2.dp),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                recentSearches.forEach { term ->
                    RecentPill(
                        term = term,
                        onClick = { onRecentClick(term) },
                        onRemove = { onRecentRemove(term) },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        if (topStores.isNotEmpty()) {
            QuickFindHeader(stringResource(R.string.history_top_stores))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                topStores.forEach { store -> StoreChip(store) { onStoreClick(store) } }
            }
            Spacer(Modifier.height(22.dp))
        }

        if (topCategories.isNotEmpty()) {
            QuickFindHeader(stringResource(R.string.history_top_categories))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                topCategories.forEach { category ->
                    CategoryChip(
                        emoji = Categories.emojiOf(category).ifEmpty { "🧾" },
                        label = categoryDisplayName(context, category),
                    ) { onCategoryClick(category) }
                }
            }
        }
    }
}

@Composable
private fun QuickFindHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RecentPill(term: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
        ) {
            Text(
                text = term,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_remove_search),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StoreChip(store: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            modifier = Modifier.padding(start = MaterialTheme.dimens.sm, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.xl)
                    .clip(RoundedCornerShape(6.dp))
                    .background(storeColor(store)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = store.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(text = store, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CategoryChip(emoji: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private val storeColors = listOf(
    0xFFC0392B, 0xFF1F5FBF, 0xFF1C7C54, 0xFFC98A00, 0xFF7E57C2, 0xFF00838F, 0xFFD81B60, 0xFF5D4037,
)

/** A stable per-store avatar color (no real logos in-app), derived from the store name. */
private fun storeColor(name: String): Color =
    Color(storeColors[(name.hashCode() and Int.MAX_VALUE) % storeColors.size])

@Composable
private fun MonthHeader(group: MonthGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, top = MaterialTheme.dimens.lg, bottom = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.month.formatMonth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = group.total.formatMoney(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// Shared by the Items and Receipts tabs: a collapsible date header with the day's total spend.
@Composable
private fun DayHeader(
    day: LocalDate,
    total: BigDecimal,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, top = MaterialTheme.dimens.md, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.cd_collapse_day else R.string.cd_expand_day,
                ),
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = dayHeaderLabel(day),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Emphasized so the day's spend stands out from the muted label and the per-item prices.
        Text(
            text = total.formatMoney(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Today" / "Yesterday" for the two most recent days, otherwise e.g. "Wed, 25 Jun". */
@Composable
private fun dayHeaderLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> date.formatDayHeader()
    }
}

// Item rows are intentionally non-interactive: in the Items tab a tap does nothing (only whole
// receipts open), so there's no click handler and no trailing chevron. Single line: the item name
// marquees when long, with an inline dimmed "· Category · Store" sitting right after it.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(item: HistoryItem) {
    val txn = item.transaction
    // The day header already states the date, so the inline meta is just category · store.
    val meta = buildList {
        if (txn.category.isNotBlank()) add(categoryDisplayName(txn.category))
        if (item.store.isNotBlank()) add(item.store)
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryTile(category = txn.category)
        // The item name keeps its space; the dimmed "· Category · Store" meta marquees when too long.
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = txn.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) {
                Text(
                    text = " · $meta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f, fill = false).basicMarquee(),
                )
            }
        }
        Text(
            text = item.lineTotal.formatMoney(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A month header for the Receipts tab: month name + that month's total. */
@Composable
private fun ReceiptMonthHeader(group: ReceiptMonthGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, top = MaterialTheme.dimens.lg, bottom = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(group.month.formatMonth(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(group.total.formatMoney(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * One whole-receipt row on a single line: a small store logo, the store name (which marquees when
 * too long) with an inline "· N items" count, the total, an optional green discount pill, and a
 * chevron. The date lives in the day header above, so it isn't repeated here (mirrors Items rows).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiptHistoryRow(receipt: Receipt, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ReceiptStoreLogo(store = receipt.store)
        // Store name marquees in the space left after the fixed "· N items" count sitting next to it.
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = receipt.store.ifBlank { "Receipt" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f, fill = false).basicMarquee(),
            )
            Text(
                text = " · ${pluralStringResource(R.plurals.item_count, receipt.transactions.size, receipt.transactions.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (receipt.discount.signum() > 0) DiscountBadge(receipt.discount)
        Text(
            text = receipt.paid.formatMoney(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
    }
}

/** A receipt's saving shown as a compact green pill after the total (e.g. "−€1.50"). */
@Composable
private fun DiscountBadge(discount: BigDecimal) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(budgetGoodColor().copy(alpha = 0.13f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "−${discount.formatMoney()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = budgetGoodColor(),
            maxLines = 1,
        )
    }
}

/** A rounded square avatar for a receipt's store: first letter on a name-derived color tile. */
@Composable
private fun ReceiptStoreLogo(store: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (store.isBlank()) MaterialTheme.colorScheme.surfaceContainerHighest else storeColor(store)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (store.isBlank()) "🧾" else store.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** The "Receipts | Items | Budgets" segmented toggle that switches the History view. */
@Composable
private fun HistoryModeToggle(
    mode: HistoryMode,
    onModeChange: (HistoryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = HistoryMode.entries
    SegmentedToggle(
        options = entries.map {
            when (it) {
                HistoryMode.RECEIPTS -> stringResource(R.string.history_tab_receipts)
                HistoryMode.ITEMS -> stringResource(R.string.history_tab_items)
                HistoryMode.BUDGETS -> stringResource(R.string.history_tab_budgets)
            }
        },
        selectedIndex = entries.indexOf(mode),
        onSelect = { onModeChange(entries[it]) },
        modifier = modifier,
    )
}

/**
 * The read-only "Budgets" tab: a snapshot of the money plan (income + recurring payments) mirrored
 * from the Budget screen, topped by a summary of what's left after bills. Not searchable or
 * time-scoped — it reflects the current plan, so it links out to Budget to make changes.
 */
@Composable
private fun HistoryBudgetsTab(
    state: HistoryUiState,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.hasBudgetPlan) {
        BudgetsEmptyState(onGoToBudget = onManage, modifier = modifier)
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BudgetsSummaryCard(
            periodLabel = stringResource(state.budgetPeriod.labelRes),
            income = state.periodIncome,
            bills = state.periodBills,
        )

        if (state.income.isNotEmpty()) {
            BudgetsSectionCard(
                title = stringResource(R.string.recurring_income),
                total = state.monthlyIncome,
            ) {
                state.income.forEach { item ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BudgetsMoneyRow(
                        emoji = "💰",
                        tileColor = MaterialTheme.colorScheme.secondaryContainer,
                        title = item.label,
                        subtitle = recurringSubtitle(item, includeCategory = false),
                        amount = "+${item.amount.formatMoney()}",
                        amountColor = budgetGoodColor(),
                    )
                }
            }
        }

        if (state.bills.isNotEmpty()) {
            BudgetsSectionCard(
                title = stringResource(R.string.recurring_payments),
                total = state.monthlyBills,
            ) {
                state.bills.forEach { item ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BudgetsMoneyRow(
                        emoji = Categories.emojiOf(item.category),
                        tileColor = Color(Categories.colorOf(item.category)).copy(alpha = 0.16f),
                        title = item.label,
                        subtitle = recurringSubtitle(item, includeCategory = true),
                        amount = item.amount.formatMoney(),
                        amountColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onManage) {
                Text(
                    text = "${stringResource(R.string.history_budgets_manage)}  →",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
    }
}

/** Period summary: income − recurring bills = what's left after fixed costs for the chosen window. */
@Composable
private fun BudgetsSummaryCard(periodLabel: String, income: BigDecimal, bills: BigDecimal) {
    val left = income.subtract(bills)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 14.dp),
    ) {
        Text(
            text = periodLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        BudgetsSummaryLine(
            label = stringResource(R.string.recurring_breakdown_income),
            amount = "+${income.formatMoney()}",
            amountColor = budgetGoodColor(),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BudgetsSummaryLine(
            label = stringResource(R.string.recurring_breakdown_bills),
            amount = "−${bills.formatMoney()}",
            amountColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_budgets_left_after_bills),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = left.formatMoney(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (left.signum() >= 0) budgetGoodColor() else budgetBadColor(),
            )
        }
    }
}

@Composable
private fun BudgetsSummaryLine(label: String, amount: String, amountColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}

/** surfaceContainer card with a "Title … €X / mo" header and its rows (income or bills). */
@Composable
private fun BudgetsSectionCard(
    title: String,
    total: BigDecimal,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.recurring_per_month, total.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/** One read-only money row: income/category tile, name + subtitle, and the amount. */
@Composable
private fun BudgetsMoneyRow(
    emoji: String,
    tileColor: Color,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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

/** Budgets tab with no income/bills set: nudge the user to set them up on the Budget screen. */
@Composable
private fun BudgetsEmptyState(onGoToBudget: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("💰", fontSize = 38.sp)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.history_budgets_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(R.string.history_budgets_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Button(onClick = onGoToBudget, modifier = Modifier.height(MaterialTheme.dimens.buttonHeight)) {
            Text(stringResource(R.string.history_budgets_go_to_budget))
        }
    }
}

/** A rounded square tinted in the category's color with its emoji glyph centered. */
@Composable
private fun CategoryTile(category: String) {
    val emoji = Categories.emojiOf(category).ifEmpty { "🧾" }
    val tile = Color(Categories.colorOf(category))
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tile),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 16.sp)
    }
}

@Composable
private fun EmptyBox(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(emoji = emoji, title = title, subtitle = subtitle)
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun HistoryScreenPreview() {
    BudgettyTheme {
        HistoryScreenContent(
            state = HistoryUiState(isLoaded = true),
            isExpanded = false,
            isWide = false,
            onNavigateToReceipt = {},
            onQueryChange = {},
            onClearFilters = {},
            onCategorySelected = {},
            onStoreSelected = {},
            onDateSelected = {},
            onBudgetPeriodSelected = {},
            onSortSelected = {},
            onPriceRangeSelected = { _, _ -> },
            onCommitSearch = {},
            onRemoveRecent = {},
            onClearRecent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, name = "History · grouped by day (landscape)")
@Composable
internal fun HistoryGroupedByDayPreview() {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()

    fun sample(id: Long, name: String, category: String, price: String, store: String, date: LocalDate): HistoryItem {
        val ts = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return HistoryItem(
            transaction = TransactionEntity(
                id = id,
                name = name,
                timestamp = ts,
                price = BigDecimal(price),
                quantity = 1,
                category = category,
            ),
            store = store,
        )
    }

    fun dayOf(date: LocalDate, items: List<HistoryItem>) =
        DayGroup(date, items, items.fold(BigDecimal.ZERO) { acc, i -> acc + i.transaction.price })

    val prevDate = today.minusMonths(1).withDayOfMonth(15)
    val thisMonth = MonthGroup(
        month = YearMonth.from(today),
        days = listOf(
            dayOf(
                today,
                listOf(
                    sample(1, "Whole milk", "Dairy", "2.50", "Lidl", today),
                    sample(2, "Sourdough loaf", "Bakery", "1.20", "Lidl", today),
                ),
            ),
            dayOf(
                today.minusDays(1),
                listOf(sample(3, "Fuel", "Fuel", "60.00", "Shell", today.minusDays(1))),
            ),
        ),
        total = BigDecimal("63.70"),
    )
    val lastMonth = MonthGroup(
        month = YearMonth.from(prevDate),
        days = listOf(
            dayOf(prevDate, listOf(sample(4, "Cappuccino", "Restaurant & Dining", "3.40", "Costa", prevDate))),
        ),
        total = BigDecimal("3.40"),
    )

    BudgettyTheme {
        HistoryScreenContent(
            state = HistoryUiState(
                isLoaded = true,
                groups = listOf(thisMonth, lastMonth),
                hasAnyTransactions = true,
            ),
            isExpanded = isExpandedWidth(),
            isWide = isWideWidth(),
            onNavigateToReceipt = {},
            onQueryChange = {},
            onClearFilters = {},
            onCategorySelected = {},
            onStoreSelected = {},
            onDateSelected = {},
            onBudgetPeriodSelected = {},
            onSortSelected = {},
            onPriceRangeSelected = { _, _ -> },
            onCommitSearch = {},
            onRemoveRecent = {},
            onClearRecent = {},
        )
    }
}
