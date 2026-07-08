package com.budgetty.app.ui.home

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.Receipt
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.category.Categories
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.auth.AuthState
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.Avatar
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.components.ReceiptDetailSheet
import com.budgetty.app.ui.components.SectionsMenu
import com.budgetty.app.ui.components.StoreLogo
import com.budgetty.app.ui.components.TransactionRow
import com.budgetty.app.ui.components.resolveSectionOrder
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.budgetColor
import com.budgetty.app.ui.util.budgetRatio
import com.budgetty.app.ui.util.monthlyToWeekly
import com.budgetty.app.ui.util.weeklyToMonthly
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.formatDate
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.budgetty.app.ui.util.resolveInitials
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.math.BigDecimal
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import kotlin.math.roundToInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToUpload: (String) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    settingsStore: SettingsStore = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentReceipts by viewModel.recentReceipts.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    val email = (authState as? AuthState.SignedIn)?.email
    HomeScreenContent(
        state = state,
        recentReceipts = recentReceipts,
        canScan = viewModel.canScan(),
        scanRemaining = viewModel.scanRemaining(),
        isPremium = viewModel.isPremium(),
        isExpanded = isExpandedWidth(),
        isWide = isWideWidth(),
        initials = resolveInitials(settings.displayName, email),
        hiddenSections = settings.hiddenHomeSections,
        sectionOrder = settings.homeSectionOrder,
        onToggleSection = { section, hidden -> settingsStore.setHomeSectionHidden(section.key, hidden) },
        onReorderSections = { settingsStore.setHomeSectionOrder(it) },
        onRevertSections = { settingsStore.resetHomeSections() },
        onFilterSelected = viewModel::onFilterSelected,
        onDeleteReceipt = viewModel::deleteReceipt,
        onDeleteTransaction = viewModel::deleteTransaction,
        onUndoLastDelete = viewModel::undoLastDelete,
        onNavigateToUpload = onNavigateToUpload,
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToBudget = onNavigateToBudget,
        onNavigateToPaywall = onNavigateToPaywall,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToAccount = onNavigateToAccount,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    recentReceipts: List<Receipt>,
    canScan: Boolean,
    scanRemaining: Int,
    isPremium: Boolean,
    isExpanded: Boolean,
    isWide: Boolean,
    initials: String,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (HomeSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onFilterSelected: (DateRangeFilter) -> Unit,
    onDeleteReceipt: (Receipt) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onUndoLastDelete: () -> Unit,
    onNavigateToUpload: (String) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedReceiptId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here (not in the coroutine) because stringResource is @Composable-only.
    val receiptDeletedMsg = stringResource(R.string.snackbar_receipt_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    // Derive the open receipt from live state so the sheet updates as items are deleted. The phone
    // Home lists all-time recent receipts (decoupled from the period filter), so search those too —
    // otherwise tapping a receipt outside the current period finds nothing and the sheet never opens.
    val selectedReceipt = (state.receipts + recentReceipts).find { it.id == selectedReceiptId }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isWide -> WideHomeContent(
                state = state,
                initials = initials,
                onFilterSelected = onFilterSelected,
                onReceiptClick = { selectedReceiptId = it },
                onAddReceipt = { showAddSheet = true },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToInsights = onNavigateToInsights,
                onNavigateToAccount = onNavigateToAccount,
            )
            isExpanded -> TabletHomeContent(
                state = state,
                onReceiptClick = { selectedReceiptId = it },
                onAddReceipt = { showAddSheet = true },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToInsights = onNavigateToInsights,
            )
            else -> PhoneHomeContent(
                state = state,
                recentReceipts = recentReceipts,
                hiddenSections = hiddenSections,
                sectionOrder = sectionOrder,
                onToggleSection = onToggleSection,
                onReorderSections = onReorderSections,
                onRevertSections = onRevertSections,
                onReceiptClick = { selectedReceiptId = it },
                onAddReceipt = { showAddSheet = true },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToHistory = onNavigateToHistory,
            )
        }

        // "Add receipt" stays one tap away via the floating action button in every layout.
        ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
            text = { Text(stringResource(R.string.add_receipt_title)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.dimens.xxl),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showAddSheet) {
        AddReceiptSheet(
            canScan = canScan,
            remaining = scanRemaining,
            isPremium = isPremium,
            onDismiss = { showAddSheet = false },
            onSelect = { source ->
                showAddSheet = false
                onNavigateToUpload(source)
            },
            onGetPremium = {
                showAddSheet = false
                onNavigateToPaywall()
            },
        )
    }

    if (selectedReceipt != null) {
        ReceiptDetailSheet(
            receipt = selectedReceipt,
            onDismiss = { selectedReceiptId = null },
            onEditReceipt = {
                // Close the sheet before navigating so it doesn't reappear on the way back.
                selectedReceiptId = null
                onNavigateToEdit(selectedReceipt.id)
            },
            onDeleteItem = { txn -> onDeleteTransaction(txn) },
            onUndo = { onUndoLastDelete() },
            onDeleteReceipt = {
                onDeleteReceipt(selectedReceipt)
                selectedReceiptId = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = receiptDeletedMsg,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndoLastDelete()
                    }
                }
            },
        )
    }
}

/**
 * The phone's single-column Home list. Content blocks render in the user's saved [sectionOrder] and
 * each can be shown/hidden or reordered via the header's customize menu.
 */
@Composable
private fun PhoneHomeContent(
    state: HomeUiState,
    recentReceipts: List<Receipt>,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (HomeSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    val ordered = resolveSectionOrder(sectionOrder, HomeSection.entries, HomeSection::key)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MaterialTheme.dimens.screenPadding, end = MaterialTheme.dimens.screenPadding, bottom = 96.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimens.lg, bottom = MaterialTheme.dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Budgetty",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = MaterialTheme.dimens.xs),
                )
                SectionsMenu(
                    sections = HomeSection.entries,
                    order = sectionOrder,
                    hiddenSections = hiddenSections,
                    sectionKey = { it.key },
                    labelRes = { it.labelRes },
                    onToggle = onToggleSection,
                    onReorder = onReorderSections,
                    onRevertToDefault = onRevertSections,
                )
            }
        }
        ordered.forEach { section ->
            if (section.key !in hiddenSections) {
                when (section) {
                    HomeSection.TOTAL_SPENT -> item(key = section.key) {
                        SummaryCard(state = state)
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                    }

                    HomeSection.WEEK_COMPARISON ->
                        if (state.lastWeekSpent.signum() > 0 || state.topCategory != null) {
                            item(key = section.key) {
                                QuickStatsStrip(state = state)
                                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                            }
                        }

                    HomeSection.BUDGETS -> item(key = section.key) {
                        BudgetProgressCard(
                            state = state,
                            label = stringResource(R.string.home_budgets),
                            monthlySpent = state.monthlySpent,
                            monthlyBudget = state.monthlyBudget,
                            weeklySpent = state.weeklySpent,
                            weeklyBudget = state.weeklyBudget,
                            onClick = onNavigateToBudget,
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                    }

                    HomeSection.RECEIPTS -> {
                        item(key = section.key) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = MaterialTheme.dimens.xs, end = MaterialTheme.dimens.xs, bottom = MaterialTheme.dimens.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.home_recent_receipts),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (recentReceipts.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.home_view_all),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                                            .clickable(onClick = onNavigateToHistory)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        if (recentReceipts.isEmpty()) {
                            // Only after the first DB load, so the placeholder doesn't flash on
                            // cold start before the saved receipts arrive.
                            if (state.isLoaded) {
                                item { EmptyReceipts(onAddReceipt = onAddReceipt) }
                            }
                        } else {
                            items(recentReceipts, key = { it.id }) { receipt ->
                                ReceiptRow(receipt = receipt, onClick = { onReceiptClick(receipt.id) })
                                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Number of receipts the tablet Home previews before the "See all" link jumps to History. */
private const val TABLET_RECEIPT_PREVIEW = 6

/**
 * The tablet's wider Home: a richer summary header (comparison + daily average), the budgets and
 * top-categories cards side by side, then a previewed receipts list. Content is centred and capped
 * at [ExpandedContentMaxWidth] so it stays readable on large landscape tablets.
 */
@Composable
private fun TabletHomeContent(
    state: HomeUiState,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInsights: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = SinglePaneMaxWidth),
            contentPadding = PaddingValues(start = MaterialTheme.dimens.screenPadding, end = MaterialTheme.dimens.screenPadding, bottom = 110.dp),
        ) {
            item {
                Text(
                    text = "Budgetty",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = MaterialTheme.dimens.xs, top = MaterialTheme.dimens.xxl, bottom = MaterialTheme.dimens.lg),
                )
            }
            item {
                TabletSummaryCard(state = state)
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
            }
            item {
                BudgetProgressCard(
                    state = state,
                    label = stringResource(R.string.home_budgets),
                    monthlySpent = state.monthlySpent,
                    monthlyBudget = state.monthlyBudget,
                    weeklySpent = state.weeklySpent,
                    weeklyBudget = state.weeklyBudget,
                    onClick = onNavigateToBudget,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
            }
            item {
                TopCategoriesCard(
                    slices = state.slices,
                    total = state.total,
                    isLoaded = state.isLoaded,
                    onClick = onNavigateToInsights,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
            }
            item {
                ReceiptsHeader(
                    count = state.receipts.size,
                    onSeeAll = onNavigateToHistory.takeIf { state.receipts.size > TABLET_RECEIPT_PREVIEW },
                )
            }
            if (state.receipts.isEmpty()) {
                if (state.isLoaded) {
                    item { EmptyReceipts(onAddReceipt = onAddReceipt) }
                }
            } else {
                items(state.receipts.take(TABLET_RECEIPT_PREVIEW), key = { it.id }) { receipt ->
                    ReceiptRow(receipt = receipt, onClick = { onReceiptClick(receipt.id) })
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                }
            }
        }
    }
}

/**
 * The landscape-tablet Home: a three-column dashboard. A header (brand, period filter, avatar) sits
 * above three equal-width columns that each fill the height — the "Total spent" summary with an
 * inline add button, the budgets and top-categories cards, and the receipts list — so the page
 * itself doesn't scroll. Content is centred and capped at [ExpandedContentMaxWidth] on wide screens.
 */
@Composable
private fun WideHomeContent(
    state: HomeUiState,
    initials: String,
    onFilterSelected: (DateRangeFilter) -> Unit,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToAccount: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.dimens.screenPadding)
                .padding(bottom = MaterialTheme.dimens.lg),
        ) {
            WideHomeHeader(
                filter = state.filter,
                onFilterSelected = onFilterSelected,
                initials = initials,
                onAvatarClick = onNavigateToAccount,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
            ) {
                // LEFT: the glance — total, budgets, and the category breakdown, stacked and scrollable.
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
                ) {
                    TabletSummaryCard(state = state)
                    BudgetProgressCard(
                        state = state,
                        label = stringResource(R.string.home_budgets),
                        monthlySpent = state.monthlySpent,
                        monthlyBudget = state.monthlyBudget,
                        weeklySpent = state.weeklySpent,
                        weeklyBudget = state.weeklyBudget,
                        onClick = onNavigateToBudget,
                    )
                    TopCategoriesCard(
                        slices = state.slices,
                        total = state.total,
                        isLoaded = state.isLoaded,
                        onClick = onNavigateToInsights,
                    )
                }
                // RIGHT: the activity feed — the receipts list, filling the rest of the width.
                ReceiptsColumn(
                    state = state,
                    onReceiptClick = onReceiptClick,
                    onAddReceipt = onAddReceipt,
                    onSeeAll = onNavigateToHistory,
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
    }
}

/** Header for the wide Home: brand title on the left, the period filter and profile avatar trailing. */
@Composable
private fun WideHomeHeader(
    filter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit,
    initials: String,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Budgetty",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        HomePeriodFilter(selected = filter, onSelected = onFilterSelected)
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Avatar(
            initials = initials,
            size = 40.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick),
        )
    }
}

/** A rounded period pill (e.g. "This month") that opens the filter dropdown; the active preset is checked. */
@Composable
private fun HomePeriodFilter(
    selected: DateRangeFilter,
    onSelected: (DateRangeFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = MaterialTheme.dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FilterList,
                contentDescription = stringResource(R.string.cd_filter_period),
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(selected.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DateRangeFilter.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Box(Modifier.size(MaterialTheme.dimens.icon))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Right dashboard column: a full-height "Receipts" card with the section header and a scrolling list
 * of flat receipt rows (or the empty-state placeholder when there are none).
 */
@Composable
private fun ReceiptsColumn(
    state: HomeUiState,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = MaterialTheme.dimens.lg),
        ) {
            ReceiptsHeader(
                count = state.receipts.size,
                onSeeAll = onSeeAll.takeIf { state.receipts.isNotEmpty() },
                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
            )
            if (state.receipts.isEmpty()) {
                if (state.isLoaded) {
                    EmptyReceipts(
                        onAddReceipt = onAddReceipt,
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        state.receipts,
                        key = { _, receipt -> receipt.id },
                    ) { index, receipt ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl),
                            )
                        }
                        ReceiptRowBody(
                            receipt = receipt,
                            onClick = { onReceiptClick(receipt.id) },
                            contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.xl, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Tablet "Total spent" header: the period total with a receipt count plus comparison / daily-average pills. */
@Composable
private fun TabletSummaryCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.xl),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.home_total_spent,
                        monthOrFilterLabel(state.filter),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.isLoaded) {
                    Text(
                        text = state.total.formatMoney(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    SkeletonBar(width = 150.dp, height = 40.dp, modifier = Modifier.padding(vertical = 2.dp))
                }
                if (state.receipts.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_across_receipts,
                            state.receipts.size,
                            state.receipts.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
            ) {
                state.previousPeriodSpent?.let { prev ->
                    ComparisonPill(current = state.total, previous = prev, filter = state.filter)
                }
                StatPill(
                    label = stringResource(R.string.home_daily_avg),
                    value = state.dailyAvg.formatMoney(),
                )
            }
        }
    }
}

/** Small tinted pill with a muted label over an emphasized value, used in the tablet summary header. */
@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

/** "vs last month +12.4%" pill — red when spend rose versus the previous period, green when it fell. */
@Composable
private fun ComparisonPill(
    current: BigDecimal,
    previous: BigDecimal,
    filter: DateRangeFilter,
    modifier: Modifier = Modifier,
) {
    val pct = if (previous.signum() > 0) {
        (current.toDouble() - previous.toDouble()) / previous.toDouble() * 100
    } else {
        0.0
    }
    val color = when {
        pct > 0 -> budgetBadColor()
        pct < 0 -> budgetGoodColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val label = if (filter == DateRangeFilter.CURRENT_MONTH) {
        stringResource(R.string.home_vs_last_month)
    } else {
        stringResource(R.string.home_vs_last_period)
    }
    StatPill(
        label = label,
        value = String.format(Locale.getDefault(), "%+.1f%%", pct),
        valueColor = color,
        modifier = modifier,
    )
}

/** Tablet "Top categories" card: the period's biggest categories with a "View Insights" link. */
@Composable
private fun TopCategoriesCard(
    slices: List<PieSlice>,
    total: BigDecimal,
    isLoaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            Text(
                text = stringResource(R.string.insights_top_categories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            if (slices.isEmpty()) {
                if (isLoaded) {
                    Text(
                        text = stringResource(R.string.home_no_receipts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                slices.take(4).forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(MaterialTheme.dimens.md))
                    HomeCategoryRow(slice = slice, total = total)
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = MaterialTheme.dimens.xs, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_view_insights),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

/** One category line inside [TopCategoriesCard]: color dot, name, spend, and a share progress bar. */
@Composable
private fun HomeCategoryRow(slice: PieSlice, total: BigDecimal) {
    val pct = if (total.signum() > 0) (slice.value.toDouble() / total.toDouble()).toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(slice.color),
            )
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = categoryDisplayName(slice.label),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = slice.value.formatMoney(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct },
            color = slice.color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
        )
    }
}

/** "Receipts" section header with an optional "See all N" link (tablet only). */
@Composable
private fun ReceiptsHeader(count: Int, onSeeAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.dimens.xs, end = MaterialTheme.dimens.xs, bottom = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_receipts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (onSeeAll != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 6.dp, vertical = MaterialTheme.dimens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.home_see_all)} $count",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReceiptSheet(
    canScan: Boolean,
    remaining: Int,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onGetPremium: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.add_receipt_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = when {
                    isPremium -> stringResource(R.string.add_premium_unlimited)
                    canScan -> stringResource(R.string.add_scans_left, remaining, ScanQuota.FREE_LIMIT)
                    else -> stringResource(R.string.add_scans_used, ScanQuota.FREE_LIMIT)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            AddOption(
                icon = Icons.Filled.PhotoCamera,
                title = stringResource(R.string.add_take_photo),
                subtitle = stringResource(R.string.add_take_photo_sub),
                enabled = canScan,
            ) { onSelect("camera") }
            Spacer(Modifier.height(10.dp))
            AddOption(
                icon = Icons.Filled.UploadFile,
                title = stringResource(R.string.add_upload_file),
                subtitle = stringResource(R.string.add_upload_file_sub),
                enabled = canScan,
            ) { onSelect("file") }
            Spacer(Modifier.height(10.dp))
            AddOption(
                icon = Icons.Filled.EditNote,
                title = stringResource(R.string.add_manually),
                subtitle = stringResource(R.string.add_manually_sub),
                enabled = true,
            ) { onSelect("manual") }
            if (!canScan && !isPremium) {
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                Button(
                    onClick = onGetPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.go_premium))
                }
            }
        }
    }
}

@Composable
private fun AddOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The period suffix for the "Total spent" label: the current month's own name (e.g. "July") while
 * the default current-month filter is active, otherwise the lowercased filter label ("this week").
 */
@Composable
private fun monthOrFilterLabel(filter: DateRangeFilter): String {
    val monthName = remember {
        java.time.LocalDate.now().month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    }
    return if (filter == DateRangeFilter.CURRENT_MONTH) monthName
    else stringResource(filter.labelRes).lowercase(Locale.getDefault())
}

/** Top section: total spent for the selected period, the period filter, and a quick summary. */
@Composable
private fun SummaryCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.home_total_spent,
                            monthOrFilterLabel(state.filter),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.isLoaded) {
                        Text(
                            text = state.total.formatMoney(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        SkeletonBar(width = 150.dp, height = 40.dp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

/** A receipt summary row card; tap to open its detail bottom sheet. */
@Composable
private fun ReceiptRow(
    receipt: Receipt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        ReceiptRowBody(receipt = receipt, onClick = onClick)
    }
}

/**
 * The inner content of a receipt row: store logo, name and date, price and any discount. Used inside
 * [ReceiptRow]'s card on phone/portrait, and flat (via [contentPadding]) in the wide Receipts card.
 */
@Composable
private fun ReceiptRowBody(
    receipt: Receipt,
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.dimens.lg),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoreLogo(store = receipt.store)
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receipt.store.ifBlank { "Receipt" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${receipt.timestamp.formatDate()} · ${pluralStringResource(R.plurals.item_count, receipt.transactions.size, receipt.transactions.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = receipt.paid.formatMoney(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (receipt.discount.signum() > 0) {
                Text(
                    text = "−${receipt.discount.formatMoney()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = budgetGoodColor(),
                )
            }
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Budget progress for the active period; the figure + bar turn green/yellow/red by usage. Tap to edit budgets. */
@Composable
private fun BudgetProgressCard(
    state: HomeUiState,
    label: String,
    monthlySpent: BigDecimal,
    monthlyBudget: BigDecimal?,
    weeklySpent: BigDecimal,
    weeklyBudget: BigDecimal?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMonthly = monthlyBudget != null && monthlyBudget.signum() > 0
    val hasWeekly = weeklyBudget != null && weeklyBudget.signum() > 0
    // A single budget period is active (Monthly wins if both or neither is set); the other is derived.
    val showMonthly = hasMonthly || !hasWeekly
    val spent = if (showMonthly) monthlySpent else weeklySpent
    val budget = if (showMonthly) monthlyBudget else weeklyBudget
    val hasBudget = budget != null && budget.signum() > 0
    val ratio = budgetRatio(spent, budget)
    val color = if (hasBudget) budgetColor(spent, budget!!) else MaterialTheme.colorScheme.onSurfaceVariant
    val progressLabel = stringResource(R.string.home_budget_progress)
    val periodLabel = stringResource(
        if (showMonthly) R.string.budget_period_monthly else R.string.budget_period_weekly,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                ) {
                Text(
                    text = "$label - $periodLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (!state.isLoaded || hasBudget) "" else stringResource(R.string.home_set_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.isLoaded) {
                    Text(
                        text = if (hasBudget) "${spent.formatMoney()} / ${budget!!.formatMoney()}"
                        else BigDecimal.ZERO.formatMoney(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                } else {
                    SkeletonBar(width = 88.dp, height = MaterialTheme.dimens.lg)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ratio },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
            )
            if (hasBudget) {
                Spacer(Modifier.height(6.dp))
                val equivalent = if (showMonthly) monthlyToWeekly(budget!!) else weeklyToMonthly(budget!!)
                val approxRes = if (showMonthly) R.string.budget_approx_weekly else R.string.budget_approx_monthly
                Text(
                    text = stringResource(approxRes, equivalent.formatMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.hasCategoryBudgets) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                ViewAllBudgetsLink(onClick = onClick)
            }
        }
    }
}

/** A link shown under the budget cards when the user has set any per-category budgets. */
@Composable
private fun ViewAllBudgetsLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.xs, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = stringResource(R.string.home_view_all_budgets),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Compact stats strip. The "This week" card (spend vs last week) only appears when there's
 * previous-week data to compare against; the "Top category" card shows the month's top category.
 * When only one card is present it spans the full width.
 */
@Composable
private fun QuickStatsStrip(state: HomeUiState, modifier: Modifier = Modifier) {
    val showThisWeek = state.lastWeekSpent.signum() > 0
    val topCategory = state.topCategory
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
    ) {
        if (showThisWeek) {
            QuickStatCard(
                label = stringResource(R.string.home_this_week),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = state.weeklySpent.formatMoney(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                WeekDeltaLabel(state.weeklySpent, state.lastWeekSpent)
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

/** "↓ 12% vs last week" — green when spending fell, red when it rose. */
@Composable
private fun WeekDeltaLabel(thisWeek: BigDecimal, lastWeek: BigDecimal) {
    if (lastWeek.signum() <= 0) {
        Text(
            text = "vs last week",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val pct = ((thisWeek.toDouble() - lastWeek.toDouble()) / lastWeek.toDouble() * 100).roundToInt()
    val (text, color) = when {
        pct < 0 -> "↓ ${-pct}% vs last week" to budgetGoodColor()
        pct > 0 -> "↑ $pct% vs last week" to budgetBadColor()
        else -> "Same as last week" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

/** First-run placeholder for the receipt list: a dashed, tinted card inviting the first scan. */
@Composable
private fun EmptyReceipts(
    onAddReceipt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
            .dashedRoundedBorder(color = accent.copy(alpha = 0.4f), cornerRadius = MaterialTheme.dimens.xl)
            .clickable(onClick = onAddReceipt)
            .padding(horizontal = MaterialTheme.dimens.xxl, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            text = stringResource(R.string.home_no_receipts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                append("Tap ")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                    append("+ Add receipt")
                }
                append(" to scan your first one — we'll read & categorize it for you.")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Draws a dashed rounded-rect outline inset within the node's bounds. */
private fun Modifier.dashedRoundedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 5.dp,
) = drawBehind {
    val inset = strokeWidth.toPx() / 2f
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                0f,
            ),
        ),
    )
}

/** A neutral rounded placeholder shown where a figure will sit until the first DB load lands, so the
 *  Home cards don't briefly read "0.00" before the real numbers arrive. */
@Composable
private fun SkeletonBar(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

/** A populated [HomeUiState] shared by the tablet/landscape previews. */
private fun previewHomeState(): HomeUiState = HomeUiState(
    isLoaded = true,
    total = BigDecimal("712.40"),
    receipts = previewReceipts,
    slices = listOf(
        PieSlice("Groceries", BigDecimal("242"), Color(0xFF52B770)),
        PieSlice("Shopping", BigDecimal("117"), Color(0xFFB75285)),
        PieSlice("Dining & Entertainment", BigDecimal("114"), Color(0xFFB77052)),
        PieSlice("Transport", BigDecimal("90"), Color(0xFFB79552)),
    ),
    monthlySpent = BigDecimal("712.40"),
    monthlyBudget = BigDecimal("1200"),
    weeklySpent = BigDecimal("132"),
    weeklyBudget = BigDecimal("300"),
    previousPeriodSpent = BigDecimal("634"),
    dailyAvg = BigDecimal("25.80"),
)

/** Sample receipts for previews (transactions are placeholders, only their count is shown). */
private val previewReceipts: List<Receipt> = listOf(
    previewReceipt(1L, "Kaufland", "47.86", 12, "3.20"),
    previewReceipt(2L, "Lidl", "31.40", 8),
    previewReceipt(3L, "Shell", "90.00", 1),
    previewReceipt(4L, "La Trattoria", "78.50", 3),
    previewReceipt(5L, "dm drogerie", "22.10", 4),
    previewReceipt(6L, "Penny Market", "18.35", 6),
)

private fun previewReceipt(id: Long, store: String, price: String, items: Int, discount: String = "0"): Receipt =
    Receipt(
        id = id,
        store = store,
        transactions = List(items) {
            TransactionEntity(name = "Item", timestamp = id, price = BigDecimal.ONE, quantity = 1, receiptId = id)
        },
        timestamp = 1_782_000_000_000L - id * 86_400_000L,
        price = BigDecimal(price),
        discount = BigDecimal(discount),
    )

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    BudgettyTheme {
        HomeScreenContent(
            state = HomeUiState(isLoaded = true),
            recentReceipts = emptyList(),
            canScan = true,
            scanRemaining = 5,
            isPremium = false,
            isExpanded = false,
            isWide = false,
            initials = "AR",
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onFilterSelected = {},
            onDeleteReceipt = {},
            onDeleteTransaction = {},
            onUndoLastDelete = {},
            onNavigateToUpload = {},
            onNavigateToEdit = {},
            onNavigateToBudget = {},
            onNavigateToPaywall = {},
            onNavigateToHistory = {},
            onNavigateToInsights = {},
            onNavigateToAccount = {},
        )
    }
}

@Preview(name = "Home – portrait tablet", showBackground = true, widthDp = 800, heightDp = 1100)
@Composable
internal fun HomeScreenTabletPreview() {
    BudgettyTheme {
        HomeScreenContent(
            state = previewHomeState(),
            recentReceipts = emptyList(),
            canScan = true,
            scanRemaining = 5,
            isPremium = false,
            isExpanded = isExpandedWidth(),
            isWide = isWideWidth(),
            initials = "AR",
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onFilterSelected = {},
            onDeleteReceipt = {},
            onDeleteTransaction = {},
            onUndoLastDelete = {},
            onNavigateToUpload = {},
            onNavigateToEdit = {},
            onNavigateToBudget = {},
            onNavigateToPaywall = {},
            onNavigateToHistory = {},
            onNavigateToInsights = {},
            onNavigateToAccount = {},
        )
    }
}

@Preview(name = "Home – landscape tablet", showBackground = true, widthDp = 1280, heightDp = 820)
@Composable
private fun HomeScreenWidePreview() {
    BudgettyTheme {
        HomeScreenContent(
            state = previewHomeState(),
            recentReceipts = emptyList(),
            canScan = true,
            scanRemaining = 5,
            isPremium = false,
            isExpanded = true,
            isWide = true,
            initials = "AR",
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onFilterSelected = {},
            onDeleteReceipt = {},
            onDeleteTransaction = {},
            onUndoLastDelete = {},
            onNavigateToUpload = {},
            onNavigateToEdit = {},
            onNavigateToBudget = {},
            onNavigateToPaywall = {},
            onNavigateToHistory = {},
            onNavigateToInsights = {},
            onNavigateToAccount = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyReceiptsPreview() {
    BudgettyTheme {
        EmptyReceipts(onAddReceipt = {}, modifier = Modifier.padding(MaterialTheme.dimens.lg))
    }
}
