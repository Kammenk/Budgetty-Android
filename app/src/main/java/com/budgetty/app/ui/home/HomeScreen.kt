package com.budgetty.app.ui.home

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.budgetty.app.ui.util.resolveInitials
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
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
    onNavigateToWellbeing: () -> Unit = {},
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
        onNavigateToWellbeing = onNavigateToWellbeing,
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
    onNavigateToWellbeing: () -> Unit = {},
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
                onNavigateToWellbeing = onNavigateToWellbeing,
            )
            isExpanded -> TabletHomeContent(
                state = state,
                onFilterSelected = onFilterSelected,
                onReceiptClick = { selectedReceiptId = it },
                onAddReceipt = { showAddSheet = true },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToInsights = onNavigateToInsights,
                onNavigateToWellbeing = onNavigateToWellbeing,
            )
            else -> PhoneHomeContent(
                state = state,
                recentReceipts = recentReceipts,
                onFilterSelected = onFilterSelected,
                hiddenSections = hiddenSections,
                sectionOrder = sectionOrder,
                onToggleSection = onToggleSection,
                onReorderSections = onReorderSections,
                onRevertSections = onRevertSections,
                onReceiptClick = { selectedReceiptId = it },
                onAddReceipt = { showAddSheet = true },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToWellbeing = onNavigateToWellbeing,
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
    recentReceipts: List<Receipt> = emptyList(),
    onFilterSelected: (DateRangeFilter) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (HomeSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToWellbeing: () -> Unit = {},
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
                        // The current pay-cycle shows the Safe-to-spend cash-flow card (income − spent −
                        // bills still due); other periods keep the plain period-spend summary.
                        if (state.filter == DateRangeFilter.CURRENT_MONTH) {
                            SafeToSpendCard(
                                state = state,
                                onFilterSelected = onFilterSelected,
                                onSetupIncome = onNavigateToBudget,
                            )
                        } else {
                            SummaryCard(state = state, onFilterSelected = onFilterSelected)
                        }
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                    }

                    // The monthly + weekly budget plan only makes sense for the current month, so it
                    // drops out when the period pill selects any other window (matching the bills strip).
                    HomeSection.BUDGETS ->
                        if (state.filter == DateRangeFilter.CURRENT_MONTH) {
                            item(key = section.key) {
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
                        }

                    // Recurring bills due soon (date-based, so it stays regardless of the period pill);
                    // shown once any bill exists, sitting directly under the budget plan.
                    HomeSection.UPCOMING_BILLS ->
                        if (state.isLoaded && state.hasBills) {
                            item(key = section.key) {
                                UpcomingBillsCard(
                                    bills = state.upcomingBills,
                                    onGoToBudget = onNavigateToBudget,
                                )
                                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                            }
                        }

                    HomeSection.WELLBEING ->
                        state.wellbeing?.let { wb ->
                            item(key = section.key) {
                                WellbeingBanner(summary = wb, onClick = onNavigateToWellbeing)
                                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                            }
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
    onFilterSelected: (DateRangeFilter) -> Unit,
    onReceiptClick: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToWellbeing: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = SinglePaneMaxWidth),
            contentPadding = PaddingValues(start = MaterialTheme.dimens.screenPadding, end = MaterialTheme.dimens.screenPadding, bottom = 110.dp),
        ) {
            item {
                // Brand + period pill (no avatar here — the portrait tablet has no dashboard header).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.dimens.xxl, bottom = MaterialTheme.dimens.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Budgetty",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = MaterialTheme.dimens.xs),
                    )
                    HomePeriodFilter(selected = state.filter, onSelected = onFilterSelected)
                }
            }
            item {
                // Current pay-cycle shows the Safe-to-spend card (the period pill lives in the header
                // above); other periods keep the plain total-spent summary.
                if (state.filter == DateRangeFilter.CURRENT_MONTH) {
                    SafeToSpendCard(
                        state = state,
                        showPeriodPill = false,
                        onSetupIncome = onNavigateToBudget,
                    )
                } else {
                    TabletSummaryCard(state = state)
                }
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
            }
            // The monthly + weekly budget plan is a current-month concept, so it drops out when the
            // period pill selects any other window (mirrors the phone Home and the bills strip).
            if (state.filter == DateRangeFilter.CURRENT_MONTH) {
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
            }
            // Recurring bills due soon, directly under the budget plan (date-based, so it stays for
            // any period); shown once any bill exists — mirrors the phone Home's Upcoming bills section.
            if (state.isLoaded && state.hasBills) {
                item {
                    UpcomingBillsCard(
                        bills = state.upcomingBills,
                        onGoToBudget = onNavigateToBudget,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                }
            }
            state.wellbeing?.let { wb ->
                item {
                    WellbeingBanner(summary = wb, onClick = onNavigateToWellbeing)
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                }
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
    onNavigateToWellbeing: () -> Unit = {},
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
                    // Current pay-cycle shows the Safe-to-spend card (period pill is in the header);
                    // other periods keep the plain total-spent summary.
                    if (state.filter == DateRangeFilter.CURRENT_MONTH) {
                        SafeToSpendCard(
                            state = state,
                            showPeriodPill = false,
                            onSetupIncome = onNavigateToBudget,
                        )
                    } else {
                        TabletSummaryCard(state = state)
                    }
                    // Hidden for non-month windows so the monthly budget plan isn't shown against a
                    // multi-month or all-time total (mirrors the phone Home and the portrait tablet).
                    if (state.filter == DateRangeFilter.CURRENT_MONTH) {
                        BudgetProgressCard(
                            state = state,
                            label = stringResource(R.string.home_budgets),
                            monthlySpent = state.monthlySpent,
                            monthlyBudget = state.monthlyBudget,
                            weeklySpent = state.weeklySpent,
                            weeklyBudget = state.weeklyBudget,
                            onClick = onNavigateToBudget,
                        )
                    }
                    state.wellbeing?.let { wb -> WellbeingBanner(summary = wb, onClick = onNavigateToWellbeing) }
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

/**
 * Tablet "Total spent" header: the period total (with the combined "with bills" total top-right and a
 * spent-vs-planned strip when recurring bills exist), the receipt count, and comparison / daily-average
 * pills. Mirrors the phone summary card so both surfaces treat bills the same way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabletSummaryCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val showBills = state.showsBills()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.xl),
        ) {
            Text(
                text = stringResource(R.string.home_total_spent, monthOrFilterLabel(state.filter)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (state.isLoaded) {
                    Text(
                        text = state.total.formatMoney(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f).basicMarquee(),
                    )
                } else {
                    SkeletonBar(
                        width = 150.dp,
                        height = 40.dp,
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                    )
                }
                if (showBills) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.home_with_bills),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = (state.total + state.monthlyBills).formatMoney(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.widthIn(max = 220.dp).basicMarquee(),
                        )
                    }
                }
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
            if (showBills) {
                BillsBreakdown(
                    spent = state.total,
                    bills = state.monthlyBills,
                    showWithBillsRow = false,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.md),
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
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
        // Scrolls so all three options (and the premium button) stay reachable on short screens, e.g.
        // in landscape; weight(fill = false) keeps the sheet compact at its natural height otherwise.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
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

/**
 * The summary card shows the spent-vs-planned breakdown only for the current month and only once at
 * least one recurring bill exists; otherwise it collapses to the plain period total. Bills are a
 * current-month plan, so we don't project them onto "last 3 months" and similar windows.
 */
private fun HomeUiState.showsBills(): Boolean =
    filter == DateRangeFilter.CURRENT_MONTH && monthlyBills.signum() > 0

private enum class SafeToSpendStatus { HEALTHY, LOW, OVER, SETUP }

/**
 * Card state from the current-cycle cash-flow: setup when there's no income to compute against, over
 * when the balance is negative, low when what's left is a thin slice of income (≤10%), else healthy.
 */
private fun HomeUiState.safeToSpendStatus(): SafeToSpendStatus = when {
    cycleIncome.signum() <= 0 -> SafeToSpendStatus.SETUP
    safeToSpend.signum() < 0 -> SafeToSpendStatus.OVER
    safeToSpend <= cycleIncome.multiply(BigDecimal("0.10")) -> SafeToSpendStatus.LOW
    else -> SafeToSpendStatus.HEALTHY
}

/**
 * The current pay-cycle "safe to spend" card: one headline number — income minus what's already been
 * spent and the bills still owed before the next payday — over a two-up Spent / Bills-still-due strip.
 * It's cash-flow, not a budget target, so it deliberately avoids the Budgets card's ratio/goal
 * language. Shown only for the current-month window; other periods fall back to [SummaryCard].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SafeToSpendCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    showPeriodPill: Boolean = true,
    onFilterSelected: (DateRangeFilter) -> Unit = {},
    onSetupIncome: () -> Unit = {},
) {
    val status = state.safeToSpendStatus()
    val tone = when (status) {
        SafeToSpendStatus.OVER -> budgetBadColor()
        SafeToSpendStatus.LOW -> budgetWarnColor()
        else -> budgetGoodColor()
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.safe_to_spend_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (showPeriodPill) {
                    HomePeriodFilter(selected = state.filter, onSelected = onFilterSelected)
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))

            when {
                !state.isLoaded ->
                    SkeletonBar(width = 160.dp, height = 40.dp, modifier = Modifier.padding(vertical = 2.dp))

                status == SafeToSpendStatus.SETUP -> {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                    Text(
                        text = stringResource(R.string.safe_to_spend_setup_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
                    )
                }

                else -> {
                    Text(
                        text = state.safeToSpend.formatMoney(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = tone,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    val detail = if (status == SafeToSpendStatus.OVER) {
                        stringResource(R.string.safe_to_spend_over, state.safeToSpend.abs().formatMoney())
                    } else {
                        val perDay = state.safeToSpend
                            .divide(BigDecimal(state.daysUntilPayday), 2, RoundingMode.HALF_UP)
                        val perDayText = pluralStringResource(
                            R.plurals.safe_to_spend_per_day,
                            state.daysUntilPayday,
                            perDay.formatMoney(),
                            state.daysUntilPayday,
                        )
                        val resets = state.nextPayday?.let {
                            stringResource(R.string.safe_to_spend_resets, it.formatDayMonth())
                        }
                        if (resets != null) "$perDayText · $resets" else perDayText
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
                    )
                }
            }

            SafeToSpendBar(
                income = state.cycleIncome,
                spent = state.monthlySpent,
                billsStillDue = state.billsStillDue,
                tone = tone,
                inactive = status == SafeToSpendStatus.SETUP,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.md),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.md),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
            ) {
                SafeToSpendStat(
                    hatched = false,
                    label = stringResource(R.string.safe_to_spend_spent, monthOrFilterLabel(state.filter)),
                    amount = state.monthlySpent,
                    sub = if (state.receipts.isNotEmpty()) pluralStringResource(
                        R.plurals.home_across_receipts, state.receipts.size, state.receipts.size,
                    ) else null,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.width(1.dp).height(44.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                SafeToSpendStat(
                    hatched = true,
                    label = stringResource(R.string.safe_to_spend_bills_due),
                    amount = state.billsStillDue,
                    sub = if (state.billsPaid.signum() > 0) stringResource(
                        R.string.safe_to_spend_bills_paid, state.billsPaid.formatMoney(),
                    ) else null,
                    modifier = Modifier.weight(1f),
                )
            }

            val foot = when {
                status == SafeToSpendStatus.SETUP -> stringResource(R.string.safe_to_spend_setup_foot)
                state.cycleIncome.signum() > 0 ->
                    stringResource(R.string.safe_to_spend_income_foot, state.cycleIncome.formatMoney())
                else -> null
            }
            if (foot != null) {
                Text(
                    text = foot,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.md),
                )
            }
            if (status == SafeToSpendStatus.SETUP) {
                FilledTonalButton(
                    onClick = onSetupIncome,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.padding(top = MaterialTheme.dimens.md),
                ) {
                    Text(stringResource(R.string.safe_to_spend_setup_cta))
                }
            }
        }
    }
}

/** One stat in the Spent / Bills-still-due strip: a key swatch + label, the amount, an optional sub. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SafeToSpendStat(
    hatched: Boolean,
    label: String,
    amount: BigDecimal,
    sub: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlannedSwatch(hatched = hatched)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = amount.formatMoney(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.xs).basicMarquee(),
        )
        if (sub != null) {
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A slim bar showing how the cycle's income splits: a solid accent share for what's been spent, a
 * tonal share on the right for the bills still due, and the hatched middle for what's safe to spend.
 * Full-hatched when [inactive] (no income to split).
 */
@Composable
private fun SafeToSpendBar(
    income: BigDecimal,
    spent: BigDecimal,
    billsStillDue: BigDecimal,
    tone: Color,
    inactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(3.dp)
    val incomeD = income.toDouble()
    Box(
        modifier
            .height(6.dp)
            .clip(shape)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawHatch(outline, spacing = 5.dp, stroke = 1.2.dp)
                drawRoundRect(color = outline, cornerRadius = radius, style = Stroke(width = 1.dp.toPx()))
                if (inactive || incomeD <= 0.0) return@drawBehind
                val spentW = (size.width * (spent.toDouble() / incomeD).coerceIn(0.0, 1.0)).toFloat()
                if (spentW > 0f) {
                    drawRoundRect(
                        color = primary,
                        size = Size(spentW.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
                val billsW = (size.width * (billsStillDue.toDouble() / incomeD).coerceIn(0.0, 1.0))
                    .toFloat().coerceAtMost((size.width - spentW).coerceAtLeast(0f))
                if (billsW > 0f) {
                    drawRoundRect(
                        color = tone,
                        topLeft = Offset(size.width - billsW, 0f),
                        size = Size(billsW.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

/** Top section: total spent for the selected period, with recurring bills shown as planned when set. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummaryCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onFilterSelected: (DateRangeFilter) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            // The period pill carries the selected window, so the label stays a plain "Total spent".
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_section_total_spent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                HomePeriodFilter(selected = state.filter, onSelected = onFilterSelected)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            if (state.isLoaded) {
                Text(
                    text = state.total.formatMoney(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().basicMarquee(),
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
            if (state.showsBills()) {
                BillsBreakdown(
                    spent = state.total,
                    bills = state.monthlyBills,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.md),
                )
            }
            // Multi-month windows swap the monthly-plan cards for a spend trend + monthly average.
            if (state.monthlyBreakdown.isNotEmpty()) {
                MonthlyTrendBars(
                    months = state.monthlyBreakdown,
                    modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.lg),
                )
            }
            if (state.monthlyAverage.signum() > 0) {
                Text(
                    text = stringResource(R.string.budget_approx_monthly, state.monthlyAverage.formatMoney()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.md),
                )
            }
        }
    }
}

/**
 * A compact per-month spend trend: one bar per month of the selected multi-month window (heights
 * relative to the busiest month), with the abbreviated month name beneath. Replaces the monthly
 * budget + bills cards, which don't apply once the window spans more than one month.
 */
@Composable
private fun MonthlyTrendBars(
    months: List<MonthlySpend>,
    modifier: Modifier = Modifier,
) {
    val maxAmount = months.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
    val maxBarHeight = 56.dp
    val minBarHeight = 4.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { entry ->
            val fraction = if (maxAmount.signum() > 0) {
                (entry.amount.toDouble() / maxAmount.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minBarHeight + (maxBarHeight - minBarHeight) * fraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = entry.month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The spent-vs-planned strip: a slim bar, then "Spent" and "Bills" each on their own full-width row
 * (so long amounts aren't truncated — and scroll via marquee if they still overflow), then the
 * combined "With bills" total. [showWithBillsRow] is false on tablet, where that total sits top-right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillsBreakdown(
    spent: BigDecimal,
    bills: BigDecimal,
    modifier: Modifier = Modifier,
    showWithBillsRow: Boolean = true,
) {
    val combined = spent + bills
    val spentFraction = if (combined.signum() > 0) {
        (spent.toDouble() / combined.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(modifier = modifier.fillMaxWidth()) {
        SpentPlannedBar(
            spentFraction = spentFraction,
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.dimens.sm),
        )
        LegendMoneyRow(
            hatched = false,
            label = stringResource(R.string.home_legend_spent),
            amount = spent,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        LegendMoneyRow(
            hatched = true,
            label = stringResource(R.string.home_legend_bills),
            amount = bills,
        )
        if (showWithBillsRow) {
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_with_bills),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = combined.formatMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f).padding(start = MaterialTheme.dimens.md).basicMarquee(),
                )
            }
        }
    }
}

/** One legend row: a spent/planned key swatch, its label, and the amount (right-aligned, marquee). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LegendMoneyRow(
    hatched: Boolean,
    label: String,
    amount: BigDecimal,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlannedSwatch(hatched = hatched)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f).padding(start = MaterialTheme.dimens.sm).basicMarquee(),
        )
    }
}

/** A 10dp legend key: a solid accent square for real spend, or a hatched square for planned bills. */
@Composable
private fun PlannedSwatch(hatched: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(3.dp)
    if (hatched) {
        Box(
            modifier
                .size(10.dp)
                .clip(shape)
                .drawBehind { drawHatch(outline, spacing = 3.dp, stroke = 1.dp) }
                .border(1.dp, outline, shape),
        )
    } else {
        Box(
            modifier
                .size(10.dp)
                .clip(shape)
                .background(primary),
        )
    }
}

/** A slim 6dp bar: solid accent for the receipt-backed share, hatched for the planned-bills remainder. */
@Composable
private fun SpentPlannedBar(
    spentFraction: Float,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .height(6.dp)
            .clip(shape)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                // The whole track carries the planned hatch, framed so the planned part reads as a
                // container even when spend is tiny.
                drawHatch(outline, spacing = 5.dp, stroke = 1.2.dp)
                drawRoundRect(color = outline, cornerRadius = radius, style = Stroke(width = 1.dp.toPx()))
                // Solid accent covers the receipt-backed share on the left.
                val solidWidth = size.width * spentFraction
                if (solidWidth > 0f) {
                    drawRoundRect(
                        color = primary,
                        size = Size(solidWidth.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

/** Fills the current draw bounds with thin diagonal stripes — the "planned, not yet real" texture. */
private fun DrawScope.drawHatch(color: Color, spacing: Dp, stroke: Dp) {
    val step = spacing.toPx()
    val strokePx = stroke.toPx()
    val h = size.height
    var x = -h
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, h),
            end = Offset(x + h, 0f),
            strokeWidth = strokePx,
        )
        x += step
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

/**
 * The Home budget card's active-period budget: the effective limit (base + any monthly carry-over)
 * paired with the carried amount. Only the monthly overall budget carries; a weekly budget never does.
 */
private fun activeBudgetWithCarry(
    showMonthly: Boolean,
    monthlyBudget: BigDecimal?,
    weeklyBudget: BigDecimal?,
    monthlyCarried: BigDecimal,
): Pair<BigDecimal?, BigDecimal> {
    val carried = if (showMonthly) monthlyCarried else BigDecimal.ZERO
    val base = if (showMonthly) monthlyBudget else weeklyBudget
    return base?.let { it + carried } to carried
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
    // Unspent budget carried in rolls only onto the monthly overall budget; the effective budget it
    // adds drives the figure, bar and equivalent, and surfaces a "+X carried over" line below.
    val (budget, carried) = activeBudgetWithCarry(showMonthly, monthlyBudget, weeklyBudget, state.monthlyCarried)
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
        if (state.isLoaded && !hasBudget) {
            BudgetEmptyContent(label = label)
        } else {
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
                if (carried.signum() > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.budget_carried_over, carried.formatMoney()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = budgetGoodColor(),
                    )
                }
            }
            if (state.hasCategoryBudgets) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                ViewAllBudgetsLink(onClick = onClick)
            }
        }
        }
    }
}

/** First-run Budgets card content: names the empty state and invites setting the first budget. */
@Composable
private fun BudgetEmptyContent(label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(MaterialTheme.dimens.xl)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.home_no_budgets),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_no_budgets_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.home_set_budget_cta),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
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
    monthlyBills = BigDecimal("485.00"),
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

@Preview(name = "Summary – spent + planned bills", showBackground = true, widthDp = 380)
@Composable
private fun SummaryCardBillsPreview() {
    BudgettyTheme {
        Box(Modifier.padding(MaterialTheme.dimens.lg)) {
            SummaryCard(
                state = HomeUiState(
                    isLoaded = true,
                    total = BigDecimal("340.20"),
                    monthlyBills = BigDecimal("1250.00"),
                    receipts = previewReceipts,
                ),
            )
        }
    }
}

@Preview(name = "Safe to spend – healthy", showBackground = true, widthDp = 380)
@Composable
private fun SafeToSpendHealthyPreview() {
    BudgettyTheme {
        Box(Modifier.padding(MaterialTheme.dimens.lg)) {
            SafeToSpendCard(
                state = HomeUiState(
                    isLoaded = true,
                    monthlySpent = BigDecimal("712.40"),
                    cycleIncome = BigDecimal("2400.00"),
                    billsStillDue = BigDecimal("1067.60"),
                    billsPaid = BigDecimal("182.40"),
                    safeToSpend = BigDecimal("620.00"),
                    daysUntilPayday = 15,
                    nextPayday = java.time.LocalDate.now().plusDays(15),
                    receipts = previewReceipts,
                ),
            )
        }
    }
}

@Preview(name = "Safe to spend – setup", showBackground = true, widthDp = 380)
@Composable
private fun SafeToSpendSetupPreview() {
    BudgettyTheme {
        Box(Modifier.padding(MaterialTheme.dimens.lg)) {
            SafeToSpendCard(
                state = HomeUiState(
                    isLoaded = true,
                    monthlySpent = BigDecimal("712.40"),
                    billsStillDue = BigDecimal("485.00"),
                    receipts = previewReceipts,
                ),
            )
        }
    }
}

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
private fun HomeScreenTabletPreview() {
    BudgettyTheme {
        HomeScreenContent(
            state = previewHomeState(),
            recentReceipts = emptyList(),
            canScan = true,
            scanRemaining = 5,
            isPremium = false,
            isExpanded = true,
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
