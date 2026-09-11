package com.budgetty.app.ui.insights

import com.budgetty.app.ui.theme.dimens
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.ui.components.CategoryTransactionsSheet
import com.budgetty.app.ui.components.CustomDateRangeSheet
import com.budgetty.app.ui.components.PieChart
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.components.PlannedBadge
import com.budgetty.app.ui.components.PlannedSwatch
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.SectionsMenu
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.components.drawPlannedHatch
import com.budgetty.app.ui.savings.SavingsSheetLabel
import com.budgetty.app.ui.util.MatchedBillLine
import com.budgetty.app.ui.components.StoreTransactionsSheet
import com.budgetty.app.ui.components.TransactionLineRow
import com.budgetty.app.ui.components.resolveSectionOrder
import com.budgetty.app.ui.util.formatMoney
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.R
import com.budgetty.app.ui.recap.RecapReopenRow
import com.budgetty.app.ui.wellbeing.WellbeingInsightsRow
import com.budgetty.app.ui.wellbeing.WellbeingScorePip
import com.budgetty.app.ui.subscriptions.SubscriptionsInsightsCard
import com.budgetty.app.category.Categories
import com.budgetty.app.category.CategoryBucket
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.bucketColor
import com.budgetty.app.ui.theme.bucketContainerAlpha
import com.budgetty.app.ui.theme.bucketLeftoverColor
import com.budgetty.app.ui.theme.wellbeingGoodContainer
import com.budgetty.app.ui.theme.wellbeingWarnContainer
import com.budgetty.app.ui.theme.wellbeingWarnOn
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.recurringSubtitle
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToRecap: () -> Unit = {},
    onNavigateToManageCategories: () -> Unit = {},
    viewModel: InsightsViewModel = koinViewModel(),
    settingsStore: SettingsStore = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    InsightsScreenContent(
        state = state,
        isExpanded = isExpandedWidth(),
        isWide = isWideWidth(),
        onNavigateToBudget = onNavigateToBudget,
        onNavigateToSubscriptions = onNavigateToSubscriptions,
        onNavigateToPaywall = onNavigateToPaywall,
        onNavigateToWellbeing = onNavigateToWellbeing,
        onNavigateToRecap = onNavigateToRecap,
        // The re-open door only exists once a recap has actually been shown for a closed period.
        showRecapEntry = settings.recapLastShownWeek.isNotEmpty() || settings.recapLastShownMonth.isNotEmpty(),
        hiddenSections = settings.hiddenInsightsSections,
        sectionOrder = settings.insightsSectionOrder,
        onToggleSection = { section, hidden -> settingsStore.setInsightsSectionHidden(section.key, hidden) },
        onReorderSections = { settingsStore.setInsightsSectionOrder(it) },
        onRevertSections = { settingsStore.resetInsightsSections() },
        onUnitSelected = viewModel::onUnitSelected,
        onStepBackward = viewModel::onStepBackward,
        onStepForward = viewModel::onStepForward,
        onCustomRangeSelected = viewModel::onCustomRangeSelected,
        onToggleIncludeRecurringBills = viewModel::onIncludeRecurringBillsChanged,
        onDismissOverlayNudge = viewModel::onDismissOverlayNudge,
        onChooseSavingsAllocation = viewModel::onCountLeftoverAsSavings,
        overlayNudgeDismissed = settings.insightsOverlayNudgeDismissed,
        onNavigateToManageCategories = onNavigateToManageCategories,
        dismissedSetup = settings.dismissedInsightsSetup,
        onDismissSetupItem = viewModel::onDismissSetupItem,
        customSections = settings.customInsightsSections,
        onSetCustomSections = settingsStore::setCustomInsightsSections,
        modifier = modifier,
    )
}

@Composable
private fun InsightsScreenContent(
    state: InsightsUiState,
    isExpanded: Boolean,
    isWide: Boolean,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToRecap: () -> Unit = {},
    showRecapEntry: Boolean = false,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (InsightsSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onUnitSelected: (PeriodUnit) -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onCustomRangeSelected: (LocalDate, LocalDate) -> Unit,
    onToggleIncludeRecurringBills: (Boolean) -> Unit = {},
    onDismissOverlayNudge: () -> Unit = {},
    onChooseSavingsAllocation: (Boolean) -> Unit = {},
    overlayNudgeDismissed: Boolean = false,
    onNavigateToManageCategories: () -> Unit = {},
    dismissedSetup: Set<String> = emptySet(),
    onDismissSetupItem: (String) -> Unit = {},
    customSections: List<String> = emptyList(),
    onSetCustomSections: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // The category whose transactions are shown in the bottom sheet, or null when none is open.
    // Holding the slice keeps the sheet's accent color matched to the chart.
    var selectedSlice by remember { mutableStateOf<PieSlice?>(null) }
    // The store whose transactions are shown in the bottom sheet, or null when none is open.
    var selectedStore by remember { mutableStateOf<String?>(null) }
    // Whether the custom date-range picker sheet is open.
    var showDateRangeSheet by remember { mutableStateOf(false) }
    // Which planned-bills overlay explainer dialog is open (Breakdown / Summary / Trend), or null.
    var plannedDialog by remember { mutableStateOf<PlannedDialog?>(null) }
    val customPeriod = state.period as? InsightsPeriod.Custom
    val stepped = state.period as? InsightsPeriod.Stepped
    // Disable the back arrow once the on-screen block already reaches the earliest recorded spend,
    // so the stepper can't page endlessly into empty past periods (nothing before the first receipt).
    val earliest = state.earliestDate
    val canStepBackward = stepped != null && earliest != null &&
        stepped.bounds(monthStartDay = state.monthStartDay).first.isAfter(earliest)
    // Friendly period label, shared by the stepper, the Breakdown sub-label and the category sheet
    // ("This month", "Last week", "Q2 2026", or a date span for weeks / custom ranges).
    val periodLabel = periodFriendlyLabel(state.period, monthStartDay = state.monthStartDay)

    // One stepper instance, wired identically for both layouts; each body places it in its header.
    val stepper: @Composable (Modifier, Boolean) -> Unit = { mod, fill ->
        PeriodStepper(
            label = periodLabel,
            steppable = stepped != null,
            canStepForward = stepped?.let { it.offset < 0 } ?: false,
            canStepBackward = canStepBackward,
            selectedUnit = stepped?.unit,
            customSelected = customPeriod != null,
            onStepBackward = onStepBackward,
            onStepForward = onStepForward,
            onUnitSelected = onUnitSelected,
            onCustomClick = { showDateRangeSheet = true },
            // "All time" reuses the custom-range window, bounded to the first recorded transaction so
            // the trend and averages stay meaningful (no epoch-to-today blow-up).
            onAllTimeClick = { onCustomRangeSelected(state.earliestDate ?: LocalDate.now(), LocalDate.now()) },
            fillWidth = fill,
            modifier = mod,
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isExpanded) {
            InsightsTabletBody(
                state = state,
                isWide = isWide,
                periodLabel = periodLabel,
                stepper = stepper,
                hiddenSections = hiddenSections,
                sectionOrder = sectionOrder,
                onToggleSection = onToggleSection,
                onReorderSections = onReorderSections,
                onRevertSections = onRevertSections,
                onSliceClick = { selectedSlice = it },
                onStoreClick = { selectedStore = it },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToWellbeing = onNavigateToWellbeing,
                onNavigateToRecap = onNavigateToRecap,
                showRecapEntry = showRecapEntry,
                onToggleIncludeRecurringBills = onToggleIncludeRecurringBills,
                onPlannedBadgeClick = { plannedDialog = it },
                onDismissOverlayNudge = onDismissOverlayNudge,
                onChooseSavingsAllocation = onChooseSavingsAllocation,
                overlayNudgeDismissed = overlayNudgeDismissed,
            )
        } else {
            InsightsPhoneBody(
                state = state,
                periodLabel = periodLabel,
                stepper = stepper,
                hiddenSections = hiddenSections,
                sectionOrder = sectionOrder,
                onToggleSection = onToggleSection,
                onReorderSections = onReorderSections,
                onRevertSections = onRevertSections,
                onSliceClick = { selectedSlice = it },
                onStoreClick = { selectedStore = it },
                onNavigateToBudget = onNavigateToBudget,
                onNavigateToWellbeing = onNavigateToWellbeing,
                onNavigateToSubscriptions = onNavigateToSubscriptions,
                onNavigateToPaywall = onNavigateToPaywall,
                onNavigateToRecap = onNavigateToRecap,
                showRecapEntry = showRecapEntry,
                onToggleIncludeRecurringBills = onToggleIncludeRecurringBills,
                onPlannedBadgeClick = { plannedDialog = it },
                onDismissOverlayNudge = onDismissOverlayNudge,
                onChooseSavingsAllocation = onChooseSavingsAllocation,
                overlayNudgeDismissed = overlayNudgeDismissed,
                onNavigateToManageCategories = onNavigateToManageCategories,
                dismissedSetup = dismissedSetup,
                onDismissSetupItem = onDismissSetupItem,
                customSections = customSections,
                onSetCustomSections = onSetCustomSections,
            )
        }
    }

    selectedSlice?.let { slice ->
        CategoryTransactionsSheet(
            category = slice.label,
            periodLabel = periodLabel,
            transactions = state.transactions,
            storeByReceiptId = state.storeByReceiptId,
            onDismiss = { selectedSlice = null },
            // A rolled-up group slice carries all its members; a plain slice just its own category.
            matchCategories = slice.members,
        )
    }

    selectedStore?.let { store ->
        StoreTransactionsSheet(
            store = store,
            periodLabel = periodLabel,
            transactions = state.transactions,
            storeByReceiptId = state.storeByReceiptId,
            onDismiss = { selectedStore = null },
        )
    }

    if (showDateRangeSheet) {
        CustomDateRangeSheet(
            initialStart = customPeriod?.start,
            initialEnd = customPeriod?.end,
            onConfirm = { start, end ->
                onCustomRangeSelected(start, end)
                showDateRangeSheet = false
            },
            onDismiss = { showDateRangeSheet = false },
        )
    }

    // Read-only explainer opened by a section's "Planned" badge; the switch stays in Customize.
    plannedDialog?.let { dialog ->
        PlannedOverlayDialog(
            dialog = dialog,
            state = state,
            periodLabel = periodLabel,
            onDismiss = { plannedDialog = null },
        )
    }
}

/** Which section's planned-bills overlay explainer dialog is open. */
enum class PlannedDialog { BREAKDOWN, SUMMARY, TREND }

/** "Breakdown" title with its period sub-label, shown above the donut. */
@Composable
private fun BreakdownHeader(periodLabel: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(stringResource(R.string.insights_breakdown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The Breakdown card: the donut + legend over the period's spend, with a top-right toggle that rolls
 * the categories up into their top-level groups ("Groceries", "Transportation", …) and back down to
 * every category. Shared by the phone and tablet layouts so both carry the toggle; the state is held
 * locally as it's a pure view over the same [slices].
 */
@Composable
internal fun BreakdownCard(
    slices: List<PieSlice>,
    total: BigDecimal,
    periodLabel: String,
    onSliceClick: (PieSlice) -> Unit,
    modifier: Modifier = Modifier,
    includeBills: Boolean = false,
    plannedOverlay: PlannedOverlay = PlannedOverlay.EMPTY,
    onPlannedBadgeClick: () -> Unit = {},
) {
    // false = every category (default); true = rolled up into top-level groups.
    var groupedByTop by remember { mutableStateOf(false) }
    val shownSlices = remember(slices, groupedByTop) {
        if (groupedByTop) rollUpToGroups(slices) else slices
    }
    val showPlanned = includeBills && plannedOverlay.hasPlanned
    InsightCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BreakdownHeader(periodLabel, modifier = Modifier.weight(1f))
            // The quiet "Planned" badge → the Breakdown explainer dialog (per-bill list + dedup).
            if (showPlanned) PlannedBadge(onClick = onPlannedBadgeClick)
            // Nothing to collapse when there's no spend, so the toggle only shows with data.
            if (slices.isNotEmpty()) {
                IconButton(onClick = { groupedByTop = !groupedByTop }) {
                    Icon(
                        imageVector = if (groupedByTop) Icons.Filled.UnfoldMore else Icons.Filled.UnfoldLess,
                        contentDescription = stringResource(
                            if (groupedByTop) R.string.cd_breakdown_show_all else R.string.cd_breakdown_show_groups,
                        ),
                        tint = if (groupedByTop) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        PieChart(
            slices = shownSlices,
            total = total,
            periodLabel = periodLabel,
            onCategoryClick = onSliceClick,
            chartSize = 300.dp,
            plannedAmount = plannedOverlay.plannedTotal.takeIf { showPlanned },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Collapses [slices] (any mix of groups and sub-categories) into one slice per top-level group,
 * summing each group's spend and taking its canonical color, largest first. Groups, "Other" and
 * custom categories stand for themselves (see [Categories.groupOf]).
 */
private fun rollUpToGroups(slices: List<PieSlice>): List<PieSlice> =
    slices
        .groupBy { Categories.groupOf(it.label) }
        .map { (group, members) ->
            PieSlice(
                label = group,
                value = members.fold(BigDecimal.ZERO) { acc, s -> acc + s.value },
                color = Color(Categories.colorOf(group)),
                // Carry every rolled-up category so tapping the group lists all their transactions.
                members = members.flatMap { it.members }.toSet(),
            )
        }
        .sortedByDescending { it.value }

/** Trend card body: title, day/month sub-label, then the bar chart (or a placeholder when empty). */
@Composable
internal fun TrendCardContent(
    trend: TrendData,
    projectedTotal: BigDecimal? = null,
    includeBills: Boolean = false,
    onPlannedBadgeClick: () -> Unit = {},
) {
    // Caps only make sense in monthly bucketing (a monthly bill isn't a per-day quantity), and only
    // once some month actually carries a planned amount.
    val showPlanned = includeBills && trend.bucketing == TrendBucketing.MONTHLY &&
        trend.buckets.any { it.planned.signum() > 0 }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.insights_trend), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = if (trend.bucketing == TrendBucketing.DAILY) stringResource(R.string.insights_trend_daily) else stringResource(R.string.insights_trend_monthly),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The quiet "Planned" badge → the Trend explainer dialog (reading the hatched caps).
        if (showPlanned) PlannedBadge(onClick = onPlannedBadgeClick)
    }
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    if (trend.hasData) {
        TrendChart(buckets = trend.buckets, showPlanned = showPlanned)
        // For the in-progress current period, a "spending pace" projection of where the period lands.
        if (projectedTotal != null) {
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = "📈 " + stringResource(R.string.insights_pace, projectedTotal.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Text(
            text = stringResource(R.string.insights_trend_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Top-categories card body: the period's biggest categories, each tappable for its transactions. */
@Composable
private fun TopCategoriesContent(slices: List<PieSlice>, total: BigDecimal, onSliceClick: (PieSlice) -> Unit) {
    Text(stringResource(R.string.insights_top_categories), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    slices.take(5).forEachIndexed { index, slice ->
        if (index > 0) Spacer(Modifier.height(10.dp))
        CategoryStatRow(slice, total, onClick = { onSliceClick(slice) })
    }
}

/** Top-stores card body: the period's biggest stores by spend. */
@Composable
private fun TopStoresContent(stores: List<StoreSpend>, onStoreClick: (String) -> Unit) {
    Text(stringResource(R.string.insights_top_stores), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    stores.forEachIndexed { index, store ->
        if (index > 0) Spacer(Modifier.height(10.dp))
        StoreStatRow(store.store, store.amount, onClick = { onStoreClick(store.store) })
    }
}

/** "Biggest purchases" card body: the period's largest single line-item buys, priciest first. */
@Composable
private fun BiggestPurchasesContent(purchases: List<TransactionEntity>, storeByReceiptId: Map<Long, String>) {
    Text(stringResource(R.string.insights_biggest_purchases), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(MaterialTheme.dimens.sm))
    purchases.forEach { txn ->
        TransactionLineRow(
            name = txn.name,
            quantity = txn.quantity,
            unitPrice = txn.price,
            store = storeByReceiptId[txn.receiptId],
            category = txn.category,
            contentPadding = PaddingValues(vertical = MaterialTheme.dimens.sm),
        )
    }
}

/**
 * Friendly empty-state for a period with no spend: a period-aware "nothing recorded for {period}"
 * once the user has data elsewhere, or a first-run "scan a receipt" nudge when there's none at all.
 */
@Composable
private fun PeriodEmptyState(periodLabel: String, hasAnyData: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (hasAnyData) "🗓️" else "📊", fontSize = 40.sp)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            text = if (hasAnyData) {
                stringResource(R.string.insights_empty_period, periodLabel)
            } else {
                stringResource(R.string.insights_empty_no_data)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(
                if (hasAnyData) R.string.insights_empty_period_sub else R.string.insights_empty_no_data_sub,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Per-category movers card body: the biggest per-category changes vs the previous period. */
@Composable
private fun ByCategoryContent(deltas: List<CategoryDelta>, period: InsightsPeriod) {
    Text(
        stringResource(R.string.insights_by_category, previousPeriodNoun(period)),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    deltas.forEachIndexed { index, delta ->
        if (index > 0) Spacer(Modifier.height(10.dp))
        CategoryDeltaRow(delta)
    }
}

/** "Highlights" card body: up to three rule-based callouts about the period's spending. */
@Composable
private fun HighlightsContent(highlights: List<Highlight>, period: InsightsPeriod) {
    Text(stringResource(R.string.insights_highlights), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    highlights.forEachIndexed { index, highlight ->
        if (index > 0) Spacer(Modifier.height(MaterialTheme.dimens.md))
        HighlightRow(highlight, period)
    }
}

/** One highlight row: a color-tinted emoji tile beside its plain-language sentence. */
@Composable
private fun HighlightRow(highlight: Highlight, period: InsightsPeriod) {
    val (emoji, text) = when (highlight) {
        is Highlight.NewCategory -> "🆕" to stringResource(
            R.string.insights_highlight_new,
            categoryDisplayName(highlight.category),
            highlight.amount.formatMoney(),
        )
        is Highlight.CategoryMove -> (if (highlight.up) "📈" else "📉") to stringResource(
            if (highlight.up) R.string.insights_highlight_up else R.string.insights_highlight_down,
            categoryDisplayName(highlight.category),
            highlight.percent,
            previousPeriodNoun(period),
        )
        is Highlight.TopShare -> "🥇" to stringResource(
            R.string.insights_highlight_share,
            categoryDisplayName(highlight.category),
            highlight.percent,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(highlight.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Phone Insights: every card stacked in a single scrolling column, rendered in the user's saved
 * [sectionOrder]. Each card can be shown/hidden or reordered via the header menu, on top of the
 * existing data-availability checks. The per-category-change card isn't user-managed, so it stays
 * anchored at the end (when there's data to fill it).
 */
@Composable
private fun InsightsPhoneBody(
    state: InsightsUiState,
    periodLabel: String,
    stepper: @Composable (Modifier, Boolean) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (InsightsSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onSliceClick: (PieSlice) -> Unit,
    onStoreClick: (String) -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToRecap: () -> Unit = {},
    showRecapEntry: Boolean = false,
    onToggleIncludeRecurringBills: (Boolean) -> Unit = {},
    onPlannedBadgeClick: (PlannedDialog) -> Unit = {},
    onDismissOverlayNudge: () -> Unit = {},
    onChooseSavingsAllocation: (Boolean) -> Unit = {},
    overlayNudgeDismissed: Boolean = false,
    onNavigateToManageCategories: () -> Unit = {},
    dismissedSetup: Set<String> = emptySet(),
    onDismissSetupItem: (String) -> Unit = {},
    customSections: List<String> = emptyList(),
    onSetCustomSections: (List<String>) -> Unit = {},
) {
    fun shows(section: InsightsSection) = section.key !in hiddenSections
    val hasData = state.slices.isNotEmpty()
    val ordered = resolveSectionOrder(sectionOrder, InsightsSection.entries, InsightsSection::key)
    // Callbacks a section card may fire, bundled once so both the fixed tabs and Custom render through
    // the same InsightSectionCard.
    val sectionActions = SectionCardActions(
        onSliceClick = onSliceClick,
        onStoreClick = onStoreClick,
        onNavigateToBudget = onNavigateToBudget,
        onNavigateToSubscriptions = onNavigateToSubscriptions,
        onNavigateToPaywall = onNavigateToPaywall,
        onChooseSavingsAllocation = onChooseSavingsAllocation,
        onPlannedBadgeClick = onPlannedBadgeClick,
    )
    // P1: the sections are grouped into a few tabs (a segmented toggle below the header) instead of
    // one long scroll. Tab is view-only state — default lands on Spending; Overview/Custom arrive later.
    var selectedTab by rememberSaveable { mutableStateOf(InsightsTab.OVERVIEW) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.screenPadding)
            .padding(bottom = MaterialTheme.dimens.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.dimens.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MaterialTheme.dimens.xs),
            )
            // Wellbeing + recap live in the toolbar (out of the scroll); recap shows only when ready.
            if (shows(InsightsSection.WELLBEING)) {
                state.wellbeing?.let { WellbeingScorePip(summary = it, onClick = onNavigateToWellbeing) }
            }
            if (showRecapEntry) {
                RecapToolbarButton(onClick = onNavigateToRecap)
            }
            SectionsMenu(
                sections = InsightsSection.entries,
                order = sectionOrder,
                hiddenSections = hiddenSections,
                sectionKey = { it.key },
                labelRes = { it.labelRes },
                onToggle = onToggleSection,
                onReorder = onReorderSections,
                onRevertToDefault = onRevertSections,
                // A "Layers" group above the section list: the opt-in switch for the planned-bills overlay.
                header = {
                    SavingsAllocationCustomize(
                        current = state.savingsAllocation,
                        onChoose = onChooseSavingsAllocation,
                    )
                    InsightsLayersToggle(
                        checked = state.includeRecurringBills,
                        onCheckedChange = onToggleIncludeRecurringBills,
                    )
                },
            )
        }
        stepper(Modifier.fillMaxWidth(), true)
        SegmentedToggle(
            options = InsightsTab.entries.map { stringResource(it.labelRes) },
            selectedIndex = selectedTab.ordinal,
            onSelect = { selectedTab = InsightsTab.entries[it] },
            modifier = Modifier.fillMaxWidth(),
        )
        when (selectedTab) {
            InsightsTab.OVERVIEW -> OverviewTabContent(
                state = state,
                onGoToTab = { selectedTab = it },
                onSliceClick = onSliceClick,
                controls = OverviewControls(
                    dismissedSetup = dismissedSetup,
                    overlayNudgeDismissed = overlayNudgeDismissed,
                    onNavigateToBudget = onNavigateToBudget,
                    onNavigateToManageCategories = onNavigateToManageCategories,
                    onToggleIncludeRecurringBills = onToggleIncludeRecurringBills,
                    onChooseSavingsAllocation = onChooseSavingsAllocation,
                    onDismissOverlayNudge = onDismissOverlayNudge,
                    onDismissSetupItem = onDismissSetupItem,
                ),
            )

            // The one user-curated tab: the chosen sections, in the user's order, plus its picker.
            InsightsTab.CUSTOM -> CustomTabContent(
                customSections = customSections,
                state = state,
                periodLabel = periodLabel,
                actions = sectionActions,
                onSetCustomSections = onSetCustomSections,
            )

            // A fixed group: render its own sections (in the saved order) through the shared card.
            else -> {
                ordered.forEach { section ->
                    // Only the selected tab's sections render; WELLBEING (tab == null) stays pinned above.
                    if (shows(section) && section.tab() == selectedTab) {
                        InsightSectionCard(section, state, periodLabel, sectionActions)
                    }
                }
                // The per-category-change card isn't user-managed; it's a Trends card at that tab's end.
                if (selectedTab == InsightsTab.TRENDS && hasData && state.categoryDeltas.isNotEmpty()) {
                    InsightCard { ByCategoryContent(state.categoryDeltas, state.period) }
                }
                // Fill an otherwise-blank Money / Trends pane with a friendly state (P8).
                BlankTabInvitation(selectedTab, state, periodLabel, hasData, onNavigateToBudget)
            }
        }
    }
}

/** Callbacks a section card may fire, bundled to keep [InsightSectionCard] a short parameter list. */
private class SectionCardActions(
    val onSliceClick: (PieSlice) -> Unit,
    val onStoreClick: (String) -> Unit,
    val onNavigateToBudget: () -> Unit,
    val onNavigateToSubscriptions: () -> Unit,
    val onNavigateToPaywall: () -> Unit,
    val onChooseSavingsAllocation: (Boolean) -> Unit,
    val onPlannedBadgeClick: (PlannedDialog) -> Unit,
)

/**
 * Renders one Insights [section]'s card — the body of what used to be the per-tab `when(section)`,
 * lifted out so the fixed tabs and the Custom tab render sections through one place. Self-gates on
 * data / plan presence exactly as before and emits nothing when the section has nothing to show;
 * WELLBEING emits nothing (it's pinned in the toolbar).
 */
@Suppress("CyclomaticComplexMethod") // An exhaustive when-dispatch over the section enum; inherent, flat.
@Composable
private fun InsightSectionCard(
    section: InsightsSection,
    state: InsightsUiState,
    periodLabel: String,
    actions: SectionCardActions,
) {
    val hasData = state.slices.isNotEmpty()
    when (section) {
        // Breakdown shows its own empty state, so it renders even with no data; the rest
        // only appear once there's spend to summarize.
        InsightsSection.BREAKDOWN -> if (state.isLoaded) {
            // The planned-bills overlay discovery nudge now lives in the Overview "things to
            // set up" checklist (P3), so Breakdown no longer pins its own copy here.
            if (hasData) {
                BreakdownCard(
                    slices = state.slices,
                    total = state.total,
                    periodLabel = periodLabel,
                    onSliceClick = actions.onSliceClick,
                    includeBills = state.includeRecurringBills,
                    plannedOverlay = state.plannedOverlay,
                    onPlannedBadgeClick = { actions.onPlannedBadgeClick(PlannedDialog.BREAKDOWN) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // A period with no spend: one friendly, period-aware message in place of
                // the empty donut, so stepping into an empty month isn't a blank screen.
                InsightCard { PeriodEmptyState(periodLabel, hasAnyData = state.earliestDate != null) }
            }
        }

        InsightsSection.SUBSCRIPTIONS -> SubscriptionsInsightsCard(
            onSeeAll = actions.onNavigateToSubscriptions,
            onUnlock = actions.onNavigateToPaywall,
            modifier = Modifier.fillMaxWidth(),
        )

        InsightsSection.SUMMARY -> if (hasData) {
            InsightCard {
                SectionTitleRow(
                    title = stringResource(R.string.insights_summary),
                    showPlannedBadge = state.includeRecurringBills && state.plannedOverlay.hasPlanned,
                    onPlannedBadgeClick = { actions.onPlannedBadgeClick(PlannedDialog.SUMMARY) },
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
                ) {
                    StatTile(stringResource(R.string.insights_stat_avg_day), state.avgPerDay.formatMoney(), Modifier.weight(1f))
                    StatTile(stringResource(R.string.home_receipts), state.receiptCount.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
                ) {
                    StatTile(stringResource(R.string.insights_stat_avg_receipt), state.avgPerReceipt.formatMoney(), Modifier.weight(1f))
                    StatTile(
                        stringResource(R.string.insights_stat_saved),
                        state.totalSaved.formatMoney(),
                        Modifier.weight(1f),
                        valueColor = budgetGoodColor(),
                    )
                }
            }
        }

        // Money-flow cards render once any income/bills exist (each shows its own nudge
        // for the partial cases); a user with no plan at all sees none of them.
        InsightsSection.INCOME_SPENDING ->
            if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                IncomeVsSpendingContent(state, periodLabel, actions.onNavigateToBudget)
            }

        InsightsSection.SAVINGS_RATE ->
            if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                SavingsRateContent(state, periodLabel, actions.onNavigateToBudget)
            }

        // The split card shows a setup state when there's no income yet, so it gates on
        // isLoaded alone; the trend only appears beneath a populated split with enough months.
        InsightsSection.NEEDS_WANTS_SAVINGS -> if (state.isLoaded) {
            InsightCard {
                NeedsWantsSplitContent(
                    split = state.needsWantsSplit,
                    periodLabel = periodLabel,
                    onGoToBudget = actions.onNavigateToBudget,
                    onChooseAllocation = actions.onChooseSavingsAllocation,
                )
            }
            if (state.showsBucketTrend) {
                InsightCard { BucketTrendContent(state.bucketTrend) }
            }
        }

        InsightsSection.INCOME_BY_SOURCE ->
            if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                IncomeBySourceContent(state, periodLabel, actions.onNavigateToBudget)
            }

        InsightsSection.HIGHLIGHTS -> if (hasData && state.highlights.isNotEmpty()) {
            InsightCard { HighlightsContent(state.highlights, state.period) }
        }

        InsightsSection.TREND -> if (hasData && state.trend.hasData) {
            InsightCard {
                TrendCardContent(
                    state.trend,
                    state.projectedTotal,
                    includeBills = state.includeRecurringBills,
                    onPlannedBadgeClick = { actions.onPlannedBadgeClick(PlannedDialog.TREND) },
                )
            }
        }

        // Only appears once there's a previous-period total to compare against.
        InsightsSection.PERIOD_COMPARISON -> if (hasData) {
            state.periodComparison?.let { comparison ->
                InsightCard { PeriodComparisonContent(comparison, state.period, state.monthStartDay) }
            }
        }

        InsightsSection.TOP_CATEGORIES -> if (hasData) {
            InsightCard { TopCategoriesContent(state.slices, state.total, actions.onSliceClick) }
        }

        InsightsSection.TOP_STORES -> if (hasData && state.topStores.isNotEmpty()) {
            InsightCard { TopStoresContent(state.topStores, actions.onStoreClick) }
        }

        InsightsSection.BIGGEST_PURCHASES -> if (hasData && state.biggestPurchases.isNotEmpty()) {
            InsightCard { BiggestPurchasesContent(state.biggestPurchases, state.storeByReceiptId) }
        }

        // Pinned in the toolbar, not rendered as a card.
        InsightsSection.WELLBEING -> Unit
    }
}

/**
 * The user-curated Custom tab (P4): a header (chosen count + a "Choose sections" entry to the picker),
 * then the chosen sections rendered in the user's order through [InsightSectionCard], plus an
 * "Add another" affordance. A friendly invite stands in when the tab is empty. Sections keep living in
 * their fixed home tabs too — this is an additional, personal view, not a move.
 */
@Composable
private fun CustomTabContent(
    customSections: List<String>,
    state: InsightsUiState,
    periodLabel: String,
    actions: SectionCardActions,
    onSetCustomSections: (List<String>) -> Unit,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val byKey = remember { InsightsSection.entries.associateBy { it.key } }
    // Resolve stored keys to sections, dropping any unknown key while keeping the user's chosen order.
    val chosen = customSections.mapNotNull { byKey[it] }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (chosen.isEmpty()) {
                stringResource(R.string.insights_custom_none).uppercase()
            } else {
                pluralStringResource(R.plurals.insights_custom_count, chosen.size, chosen.size).uppercase() +
                    " · " + periodLabel.uppercase()
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        ChooseSectionsChip(onClick = { pickerOpen = true })
    }

    if (chosen.isEmpty()) {
        CustomEmptyState(onAdd = { pickerOpen = true })
    } else {
        chosen.forEach { section -> InsightSectionCard(section, state, periodLabel, actions) }
        AddSectionRow(onClick = { pickerOpen = true })
    }

    if (pickerOpen) {
        CustomSectionsSheet(
            selectedOrder = customSections,
            onSetCustomSections = onSetCustomSections,
            onDismiss = { pickerOpen = false },
        )
    }
}

/** The "Choose sections" pill in the Custom header — a tonal chip that opens the section picker. */
@Composable
private fun ChooseSectionsChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.insights_custom_choose),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The Custom tab's empty state: a friendly invite to compose a personal view, in place of a blank pane. */
@Composable
private fun CustomEmptyState(onAdd: () -> Unit) {
    InsightCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(MaterialTheme.dimens.radiusLg)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = stringResource(R.string.insights_custom_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.insights_custom_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledPillButton(text = stringResource(R.string.insights_custom_add), onClick = onAdd)
        }
    }
}

/** The dashed "Add another section" row at the end of a populated Custom list. */
@Composable
private fun AddSectionRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.dimens.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.insights_custom_add_another),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A fully-rounded (pill) filled action button, per the button-shape convention. */
@Composable
private fun FilledPillButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(50)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/**
 * Fills an otherwise-blank Money / Trends pane with a friendly state (P8): Money with no income or
 * budget gets a verb-first invitation to add one; Trends with no spend this period reuses the same
 * period-aware empty as Breakdown (first-run vs stepped-into-an-empty-period), keeping its copy honest.
 * A no-op for other tabs, before the first load, or when the tab already has content.
 */
@Composable
private fun BlankTabInvitation(
    tab: InsightsTab,
    state: InsightsUiState,
    periodLabel: String,
    hasData: Boolean,
    onNavigateToBudget: () -> Unit,
) {
    if (!state.isLoaded) return
    val moneyNeedsPlan = !state.hasIncome && !state.hasBills
    when {
        tab == InsightsTab.MONEY && moneyNeedsPlan -> TabInvitationCard(
            titleRes = R.string.insights_money_empty_title,
            bodyRes = R.string.insights_money_empty_body,
            ctaRes = R.string.insights_money_empty_cta,
            icon = Icons.Filled.AccountBalanceWallet,
            onCta = onNavigateToBudget,
        )

        tab == InsightsTab.TRENDS && !hasData ->
            InsightCard { PeriodEmptyState(periodLabel, hasAnyData = state.earliestDate != null) }
    }
}

/**
 * A friendly per-tab invitation card (P8): an icon, a title, one line, and a verb-first CTA — shown in
 * place of an otherwise-blank pane so a group with nothing to show (e.g. Money with no income or
 * budget) reads as an intentional next step rather than an empty screen.
 */
@Composable
private fun TabInvitationCard(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    @StringRes ctaRes: Int,
    icon: ImageVector,
    onCta: () -> Unit,
) {
    InsightCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledPillButton(text = stringResource(ctaRes), onClick = onCta)
        }
    }
}

/**
 * The Custom section picker — an [AdaptiveSheet] (bottom sheet on phones, centered dialog on tablets)
 * that toggles sections in or out of the Custom tab and reorders the chosen ones with up/down arrows
 * (native, no drag library — matching the Customize-sections menu). Chosen sections list first, in the
 * user's order; the rest follow. The scrolling list is capped with `weight(1f, fill = false)` per the
 * bottom-sheet scroll convention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSectionsSheet(
    selectedOrder: List<String>,
    onSetCustomSections: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val byKey = remember { InsightsSection.entries.associateBy { it.key } }
    val members = selectedOrder.mapNotNull { byKey[it] }
    val memberSet = members.toSet()
    // Chosen sections first (in order), then the remaining offerable ones; only members can reorder.
    val rows = members + customizableSections.filter { it !in memberSet }

    AdaptiveSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.sm, top = MaterialTheme.dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insights_custom_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (members.isEmpty()) {
                        stringResource(R.string.insights_custom_none)
                    } else {
                        pluralStringResource(R.plurals.insights_custom_count, members.size, members.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_insights_custom_close))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.lg),
        ) {
            rows.forEachIndexed { index, section ->
                val isMember = index < members.size
                CustomPickerRow(
                    section = section,
                    checked = isMember,
                    canMoveUp = isMember && index > 0,
                    canMoveDown = isMember && index < members.lastIndex,
                    onToggle = {
                        onSetCustomSections(
                            if (isMember) selectedOrder - section.key else selectedOrder + section.key,
                        )
                    },
                    onMoveUp = { onSetCustomSections(selectedOrder.swappedKeys(index, index - 1)) },
                    onMoveDown = { onSetCustomSections(selectedOrder.swappedKeys(index, index + 1)) },
                )
            }
            Text(
                text = stringResource(R.string.insights_custom_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.dimens.md),
            )
        }
    }
}

/** One row of the Custom picker: a membership switch, the section name + its home group, and (for a
 *  chosen section) up/down reorder arrows. */
@Composable
private fun CustomPickerRow(
    section: InsightsSection,
    checked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                .clickable(onClick = onToggle)
                .padding(vertical = MaterialTheme.dimens.xs),
        ) {
            Text(stringResource(section.labelRes), style = MaterialTheme.typography.bodyLarge)
            section.tab()?.let { home ->
                Text(
                    text = stringResource(home.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_section_up))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_section_down))
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

/** Returns a copy of this key list with the entries at [a] and [b] swapped (no-op if out of range). */
private fun List<String>.swappedKeys(a: Int, b: Int): List<String> {
    if (a !in indices || b !in indices) return this
    return toMutableList().apply { val t = this[a]; this[a] = this[b]; this[b] = t }
}

/**
 * Tablet Insights: a single centred column on portrait (capped at [SinglePaneMaxWidth]); a two-pane
 * layout on landscape — charts (donut, stat tiles, trend) on the left, the numeric breakdown
 * (categories, stores, budget, deltas) on the right.
 *
 * Section visibility follows the same customize setting as the phone (menu in the header). The
 * saved custom *order* is persisted but not applied here: the two-pane split is positional, so
 * cards keep their pane slots.
 */
@Composable
internal fun InsightsTabletBody(
    state: InsightsUiState,
    isWide: Boolean,
    periodLabel: String,
    stepper: @Composable (Modifier, Boolean) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (InsightsSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onSliceClick: (PieSlice) -> Unit,
    onStoreClick: (String) -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToWellbeing: () -> Unit = {},
    onNavigateToRecap: () -> Unit = {},
    showRecapEntry: Boolean = false,
    onToggleIncludeRecurringBills: (Boolean) -> Unit = {},
    onPlannedBadgeClick: (PlannedDialog) -> Unit = {},
    onDismissOverlayNudge: () -> Unit = {},
    onChooseSavingsAllocation: (Boolean) -> Unit = {},
    overlayNudgeDismissed: Boolean = false,
) {
    fun shows(section: InsightsSection) = section.key !in hiddenSections
    val hasData = state.slices.isNotEmpty()

    // Card builders shared by the portrait single column and the landscape two panes.
    val donutCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.BREAKDOWN)) BreakdownCard(
            slices = state.slices,
            total = state.total,
            periodLabel = periodLabel,
            onSliceClick = onSliceClick,
            includeBills = state.includeRecurringBills,
            plannedOverlay = state.plannedOverlay,
            onPlannedBadgeClick = { onPlannedBadgeClick(PlannedDialog.BREAKDOWN) },
            modifier = mod,
        )
    }
    // The one-time discovery nudge, spanning the content above Breakdown (both panes/columns).
    val overlayNudge: @Composable () -> Unit = {
        OverlayDiscoveryNudge(
            state = state,
            dismissed = overlayNudgeDismissed,
            onEnable = { onToggleIncludeRecurringBills(true) },
            onDismiss = onDismissOverlayNudge,
        )
    }
    val trendCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.TREND)) InsightCard(modifier = mod) {
            TrendCardContent(
                state.trend,
                state.projectedTotal,
                includeBills = state.includeRecurringBills,
                onPlannedBadgeClick = { onPlannedBadgeClick(PlannedDialog.TREND) },
            )
        }
    }
    // Needs/Wants/Savings 50/30/20 split + its closed-month trend (the split shows a setup state
    // without income; the trend only beneath a populated split, with per-month savings % labels).
    val nwsSplitCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.NEEDS_WANTS_SAVINGS)) InsightCard(modifier = mod) {
            NeedsWantsSplitContent(
                split = state.needsWantsSplit,
                periodLabel = periodLabel,
                onGoToBudget = onNavigateToBudget,
                onChooseAllocation = onChooseSavingsAllocation,
            )
        }
    }
    val nwsTrendCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.NEEDS_WANTS_SAVINGS) && state.showsBucketTrend) {
            InsightCard(modifier = mod) { BucketTrendContent(state.bucketTrend, showMonthLabels = true) }
        }
    }
    // Period-over-period comparison as its own card below the trend (mirrors the phone layout);
    // renders nothing when there's no previous period to compare against.
    val periodComparisonCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.PERIOD_COMPARISON)) state.periodComparison?.let { comparison ->
            InsightCard(modifier = mod) { PeriodComparisonContent(comparison, state.period, state.monthStartDay) }
        }
    }
    // Total / Receipts / Avg / Saved as a compact 2×2 tile grid (fits both the pane and the column).
    val statTiles: @Composable () -> Unit = {
        if (shows(InsightsSection.SUMMARY)) Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                StatTile(stringResource(R.string.insights_stat_avg_day), state.avgPerDay.formatMoney(), Modifier.weight(1f))
                StatTile(stringResource(R.string.home_receipts), state.receiptCount.toString(), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                StatTile(stringResource(R.string.insights_stat_avg_receipt), state.avgPerReceipt.formatMoney(), Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.insights_stat_saved),
                    state.totalSaved.formatMoney(),
                    Modifier.weight(1f),
                    valueColor = budgetGoodColor(),
                )
            }
        }
    }
    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = MaterialTheme.dimens.xs),
            )
            stepper(Modifier, false)
            SectionsMenu(
                sections = InsightsSection.entries,
                order = sectionOrder,
                hiddenSections = hiddenSections,
                sectionKey = { it.key },
                labelRes = { it.labelRes },
                onToggle = onToggleSection,
                onReorder = onReorderSections,
                onRevertToDefault = onRevertSections,
                // A "Layers" group above the section list: the opt-in switch for the planned-bills overlay.
                header = {
                    SavingsAllocationCustomize(
                        current = state.savingsAllocation,
                        onChoose = onChooseSavingsAllocation,
                    )
                    InsightsLayersToggle(
                        checked = state.includeRecurringBills,
                        onCheckedChange = onToggleIncludeRecurringBills,
                    )
                },
            )
        }
    }
    // The numeric breakdown — the right pane in landscape, stacked below the charts in portrait.
    val breakdownCards: @Composable () -> Unit = {
        if (shows(InsightsSection.TOP_CATEGORIES)) {
            InsightCard { TopCategoriesContent(state.slices, state.total, onSliceClick) }
        }
        if (shows(InsightsSection.TOP_STORES) && state.topStores.isNotEmpty()) {
            InsightCard { TopStoresContent(state.topStores, onStoreClick) }
        }
        if (shows(InsightsSection.BIGGEST_PURCHASES) && state.biggestPurchases.isNotEmpty()) {
            InsightCard { BiggestPurchasesContent(state.biggestPurchases, state.storeByReceiptId) }
        }
        if (state.categoryDeltas.isNotEmpty()) {
            InsightCard { ByCategoryContent(state.categoryDeltas, state.period) }
        }
    }

    if (isWide) {
        // Landscape two-pane: charts on the left, the numeric breakdown on the right.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.dimens.screenPadding)
                .padding(bottom = MaterialTheme.dimens.lg),
        ) {
            header()
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            if (shows(InsightsSection.WELLBEING)) {
                state.wellbeing?.let { WellbeingInsightsRow(summary = it, onClick = onNavigateToWellbeing) }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }
            if (showRecapEntry) {
                RecapReopenRow(onClick = onNavigateToRecap, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }
            // Discovery nudge spans the content above the panes (manual layout ⇒ gate its spacer too).
            if (shouldShowOverlayNudge(state, overlayNudgeDismissed)) {
                overlayNudge()
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }
            if (!hasData) {
                // No spend yet — surface the breakdown's period empty-state so the screen isn't blank.
                if (state.isLoaded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
                    ) {
                        if (shows(InsightsSection.BREAKDOWN)) {
                            InsightCard { PeriodEmptyState(periodLabel, hasAnyData = state.earliestDate != null) }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
                    ) {
                        donutCard(Modifier.fillMaxWidth())
                        statTiles()
                        nwsSplitCard(Modifier.fillMaxWidth())
                        nwsTrendCard(Modifier.fillMaxWidth())
                        trendCard(Modifier.fillMaxWidth())
                        periodComparisonCard(Modifier.fillMaxWidth())
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
                    ) {
                        breakdownCards()
                    }
                }
            }
        }
    } else {
        // Portrait single-pane: one centred, capped column with everything stacked.
        Column(
            modifier = Modifier
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.screenPadding)
                .padding(bottom = MaterialTheme.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
        ) {
            header()
            if (shows(InsightsSection.WELLBEING)) {
                state.wellbeing?.let { WellbeingInsightsRow(summary = it, onClick = onNavigateToWellbeing) }
            }
            if (showRecapEntry) {
                RecapReopenRow(onClick = onNavigateToRecap, modifier = Modifier.fillMaxWidth())
            }
            if (!hasData) {
                // No spend yet — surface the breakdown's period empty-state so the screen isn't blank.
                if (state.isLoaded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sectionSpacing),
                    ) {
                        if (shows(InsightsSection.BREAKDOWN)) {
                            InsightCard { PeriodEmptyState(periodLabel, hasAnyData = state.earliestDate != null) }
                        }
                    }
                }
            } else {
                overlayNudge()
                donutCard(Modifier.fillMaxWidth())
                statTiles()
                nwsSplitCard(Modifier.fillMaxWidth())
                nwsTrendCard(Modifier.fillMaxWidth())
                trendCard(Modifier.fillMaxWidth())
                periodComparisonCard(Modifier.fillMaxWidth())
                breakdownCards()
            }
        }
    }
}

// ── Income & recurring-payment cards ───────────────────────────────────────────────────────────

private val IncomeSourceColors = listOf(
    Color(0xFF4FA85A), Color(0xFFD08A4A), Color(0xFF4AA3C7), Color(0xFF9A78D0), Color(0xFFC98A00),
)

/** A card title with a trailing period pill ("This month"), shared by the money-flow cards. */
@Composable
private fun MoneyFlowCardHeader(title: String, periodLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Centered "add income/bills in Budget" nudge shown inside a card that has nothing to plot yet. */
@Composable
private fun CardNudge(emoji: String, title: String?, text: String, onGoToBudget: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 28.sp)
        if (title != null) {
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = "${stringResource(R.string.insights_go_to_budget)} →",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onGoToBudget)
                .padding(horizontal = MaterialTheme.dimens.sm, vertical = 4.dp),
        )
    }
}

/** "In" / "Out" row: a labelled arrow + amount over a proportional bar. */
@Composable
private fun MoneyFlowRow(
    up: Boolean,
    label: String,
    amount: String,
    amountColor: Color,
    barFraction: Float,
    barColor: Color,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = if (up) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = amountColor)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(barFraction)
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
    }
}

/** "Money in vs. out": income vs actual spend for the period, with the net and a plain-language read. */
@Composable
private fun IncomeVsSpendingContent(state: InsightsUiState, periodLabel: String, onGoToBudget: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        MoneyFlowCardHeader(stringResource(R.string.insights_income_spending), periodLabel)
        val income = state.periodIncome
        if (income.signum() <= 0) {
            CardNudge("💡", null, stringResource(R.string.insights_income_nudge_flow), onGoToBudget)
        } else {
            // "Out" is everything committed: recurring bills (planned) + actual spend, so Net here
            // matches "Left" on the Fixed/Flexible card and the Savings-rate figure.
            val out = state.periodBills.add(state.total)
            val net = income.subtract(out)
            val positive = net.signum() >= 0
            val ref = maxOf(income, out).coerceAtLeast(BigDecimal.ONE)
            val green = budgetGoodColor()
            val red = budgetBadColor()
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            MoneyFlowRow(
                up = true,
                label = stringResource(R.string.insights_income_in),
                amount = "+${income.formatMoney()}",
                amountColor = green,
                barFraction = (income.toDouble() / ref.toDouble()).toFloat().coerceIn(0f, 1f),
                barColor = green,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            MoneyFlowRow(
                up = false,
                label = stringResource(R.string.insights_income_out),
                amount = out.formatMoney(),
                amountColor = if (positive) MaterialTheme.colorScheme.onSurface else red,
                barFraction = (out.toDouble() / ref.toDouble()).toFloat().coerceIn(0f, 1f),
                barColor = if (positive) MaterialTheme.colorScheme.primary else red,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.insights_income_net), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = (if (positive) "+" else "−") + net.abs().formatMoney(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (positive) green else red,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = if (positive) {
                    stringResource(R.string.insights_income_read_positive, net.abs().formatMoney())
                } else {
                    stringResource(R.string.insights_income_read_negative, net.abs().formatMoney())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A ring gauge: a full track with a rounded arc filling [fraction] of it, [centerContent] inside. */
@Composable
private fun SavingsRing(fraction: Float, color: Color, centerContent: @Composable () -> Unit) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * (4.8f / 42f)
            val diameter = size.minDimension - strokeW
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = strokeW))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
        }
        centerContent()
    }
}

@Composable
private fun SavingsLegendChip(dotColor: Color, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** "Savings rate": what share of income the user kept, as a color-graded ring + health legend. */
@Composable
private fun SavingsRateContent(state: InsightsUiState, periodLabel: String, onGoToBudget: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        MoneyFlowCardHeader(stringResource(R.string.insights_savings_rate), periodLabel)
        val income = state.periodIncome
        if (income.signum() <= 0) {
            CardNudge("💡", null, stringResource(R.string.insights_savings_nudge), onGoToBudget)
        } else {
            val saved = income.subtract(state.periodBills).subtract(state.total)
            val rate = (saved.toDouble() / income.toDouble() * 100).roundToInt()
            val ringColor = when {
                rate >= 20 -> budgetGoodColor()
                rate >= 0 -> budgetWarnColor()
                else -> budgetBadColor()
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SavingsRing(fraction = kotlin.math.abs(rate).coerceAtMost(100) / 100f, color = ringColor) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (if (rate < 0) "−" else "") + "${kotlin.math.abs(rate)}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ringColor,
                        )
                        Text(
                            text = if (rate >= 0) stringResource(R.string.insights_savings_tag_saved) else stringResource(R.string.insights_savings_tag_over),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(80.dp),
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = if (rate >= 0) {
                        stringResource(R.string.insights_savings_sub_saved, saved.formatMoney(), income.formatMoney())
                    } else {
                        stringResource(R.string.insights_savings_sub_over, saved.abs().formatMoney())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                    SavingsLegendChip(budgetGoodColor(), stringResource(R.string.insights_savings_legend_great))
                    SavingsLegendChip(budgetWarnColor(), stringResource(R.string.insights_savings_legend_ok))
                    SavingsLegendChip(budgetBadColor(), stringResource(R.string.insights_savings_legend_bad))
                }
            }
        }
    }
}

// ── Needs / Wants / Savings (the 50/30/20 split) ───────────────────────────────────────────────

/** The split card: the populated 50/30/20 view when there's income to measure against, else the
 *  setup state. Shared by phone and tablet. */
@Composable
internal fun NeedsWantsSplitContent(
    split: NeedsWantsSplit?,
    periodLabel: String,
    onGoToBudget: () -> Unit,
    onChooseAllocation: (Boolean) -> Unit = {},
) {
    if (split == null) {
        NeedsWantsSetup(onGoToBudget)
    } else {
        NeedsWantsSplitCard(split, periodLabel, onGoToBudget, onChooseAllocation)
    }
}

/** Title + "share of income" subtitle + period pill — the split card's header. */
@Composable
private fun NeedsWantsHeader(income: BigDecimal, periodLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.insights_needs_wants_savings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.insights_nws_income_subtitle, income.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun NeedsWantsSplitCard(
    split: NeedsWantsSplit,
    periodLabel: String,
    onGoToBudget: () -> Unit,
    onChooseAllocation: (Boolean) -> Unit,
) {
    val unset = split.allocation == SavingsAllocation.UNSET
    Column(Modifier.fillMaxWidth()) {
        NeedsWantsHeader(split.income, periodLabel)
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        BucketSplitBar(split)
        Spacer(Modifier.height(6.dp))
        Text(
            text = splitBarCaption(split),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The one-time ask sits between the bar and the rows, until the user answers it.
        if (unset) {
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            SavingsAllocationAsk(split, onChooseAllocation)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        BucketRow(split.needs, showPill = !unset)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        BucketRow(split.wants, showPill = !unset)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        if (unset) {
            SavingsWaitingRow(split)
        } else {
            BucketRow(split.savings)
            if (split.allocation == SavingsAllocation.SET_ASIDE && split.leftover.signum() > 0) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                LeftoverRow(split)
            }
        }
        // Summary + goal nudge + footer note only once the definition is chosen.
        if (!unset) {
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = allocationSummary(split),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            SavingsGoalCta(split.allocation, onGoToBudget)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = stringResource(
                    if (split.allocation == SavingsAllocation.COUNT_KEPT) {
                        R.string.insights_nws_footer_keep
                    } else {
                        R.string.insights_nws_footer_aside
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The caption under the bar, worded for whichever Savings definition is in force. */
@Composable
private fun splitBarCaption(split: NeedsWantsSplit): String = when (split.allocation) {
    SavingsAllocation.UNSET -> stringResource(R.string.insights_nws_bar_unspent, split.leftover.formatMoney())
    SavingsAllocation.COUNT_KEPT -> stringResource(R.string.insights_nws_targets_caption)
    SavingsAllocation.SET_ASIDE -> stringResource(R.string.insights_nws_bar_leftover)
}

@Composable
private fun allocationSummary(split: NeedsWantsSplit): String = when (split.allocation) {
    SavingsAllocation.COUNT_KEPT -> stringResource(
        R.string.insights_nws_summary_kept,
        split.savings.amount.formatMoney(),
        split.income.formatMoney(),
    )
    else -> splitSummary(split)
}

/** The Savings row before the user has chosen — no percent, no pill, a "waiting" sub-line. */
@Composable
private fun SavingsWaitingRow(split: NeedsWantsSplit) {
    val kept = split.leftover.add(split.savings.amount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bucketColor(CategoryBucket.SAVINGS)))
        Column(Modifier.weight(1f)) {
            Text(bucketLabel(CategoryBucket.SAVINGS), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.insights_nws_savings_waiting, kept.formatMoney()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The 4th "Left over" row (only under "only money I set aside"): income counted as no bucket. */
@Composable
private fun LeftoverRow(split: NeedsWantsSplit) {
    val pct = (split.leftover.toDouble() / split.income.toDouble() * 100).roundToInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bucketLeftoverColor()))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.insights_nws_leftover),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.insights_nws_leftover_sub, split.leftover.formatMoney()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The "give it a goal" nudge under the chosen split; taps through to Budget, where goals live. */
@Composable
private fun SavingsGoalCta(allocation: SavingsAllocation, onGoToBudget: () -> Unit) {
    val keep = allocation == SavingsAllocation.COUNT_KEPT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onGoToBudget)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bucketColor(CategoryBucket.SAVINGS).copy(alpha = bucketContainerAlpha())),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = bucketColor(CategoryBucket.SAVINGS), modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (keep) R.string.insights_nws_cta_keep_title else R.string.insights_nws_cta_aside_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(if (keep) R.string.insights_nws_cta_keep_sub else R.string.insights_nws_cta_aside_sub),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The one-time inline ask: two choice cards, each with a live mini-bar of what it does to this month. */
@Composable
private fun SavingsAllocationAsk(split: NeedsWantsSplit, onChoose: (Boolean) -> Unit) {
    val keptAmount = split.income.subtract(split.needs.amount).subtract(split.wants.amount).max(BigDecimal.ZERO)
    val keptPct = (keptAmount.toDouble() / split.income.toDouble() * 100).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.insights_nws_ask_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.insights_nws_ask_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AllocationChoiceCard(
            label = stringResource(R.string.insights_nws_ask_keep),
            percent = "$keptPct%",
            percentColor = bucketColor(CategoryBucket.SAVINGS),
            desc = stringResource(R.string.insights_nws_ask_keep_desc),
            needsFraction = split.needs.fraction,
            wantsFraction = split.wants.fraction,
            tailColor = bucketColor(CategoryBucket.SAVINGS),
            onClick = { onChoose(true) },
        )
        AllocationChoiceCard(
            label = stringResource(R.string.insights_nws_ask_aside),
            percent = "${split.savings.percent}%",
            percentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            desc = stringResource(R.string.insights_nws_ask_aside_desc),
            needsFraction = split.needs.fraction,
            wantsFraction = split.wants.fraction,
            tailColor = bucketLeftoverColor(),
            onClick = { onChoose(false) },
        )
    }
}

@Composable
private fun AllocationChoiceCard(
    label: String,
    percent: String,
    percentColor: Color,
    desc: String,
    needsFraction: Float,
    wantsFraction: Float,
    tailColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(percent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = percentColor)
        }
        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (needsFraction > 0.001f) Box(Modifier.fillMaxHeight().weight(needsFraction).background(bucketColor(CategoryBucket.NEED)))
            if (wantsFraction > 0.001f) Box(Modifier.fillMaxHeight().weight(wantsFraction).background(bucketColor(CategoryBucket.WANT)))
            Box(Modifier.fillMaxHeight().weight((1f - needsFraction - wantsFraction).coerceAtLeast(0.001f)).background(tailColor))
        }
    }
}

/** The "How Savings is counted" radio group in the Insights Customize sheet header — the change-later
 *  counterpart to the split's one-time ask. */
@Composable
private fun SavingsAllocationCustomize(current: Boolean?, onChoose: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = MaterialTheme.dimens.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.insights_nws_alloc_header).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.insights_nws_new).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
        Text(
            stringResource(R.string.insights_nws_alloc_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        )
        AllocationRadioRow(
            selected = current == true,
            label = stringResource(R.string.insights_nws_ask_keep),
            desc = stringResource(R.string.insights_nws_alloc_keep_desc),
            swatch = bucketColor(CategoryBucket.SAVINGS),
            onClick = { onChoose(true) },
        )
        AllocationRadioRow(
            selected = current != true,
            label = stringResource(R.string.insights_nws_ask_aside),
            desc = stringResource(R.string.insights_nws_alloc_aside_desc),
            swatch = bucketLeftoverColor(),
            onClick = { onChoose(false) },
        )
        Text(
            stringResource(R.string.insights_nws_alloc_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AllocationRadioRow(selected: Boolean, label: String, desc: String, swatch: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(swatch).align(Alignment.CenterVertically))
    }
}

/** The 20px stacked bar carrying the three shares, with target ticks fixed at the cumulative 50% and
 *  80% marks (a segment ending past its tick is over target without reading a number). */
@Composable
private fun BucketSplitBar(split: NeedsWantsSplit) {
    // If the three buckets exceed income (overspend), scale them to fit the bar; the remainder is the
    // leftover track. The row numbers still report the true, unscaled percentages.
    val raw = floatArrayOf(split.needs.fraction, split.wants.fraction, split.savings.fraction)
    val sum = raw.sum()
    val scale = if (sum > 1f) 1f / sum else 1f
    val n = raw[0] * scale
    val w = raw[1] * scale
    val s = raw[2] * scale
    val leftover = (1f - n - w - s).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(9.dp)) {
        Box(Modifier.fillMaxWidth(0.50f)) {
            Box(
                Modifier.align(Alignment.CenterEnd).width(2.dp).height(7.dp)
                    .clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.outline),
            )
        }
        Box(Modifier.fillMaxWidth(0.80f)) {
            Box(
                Modifier.align(Alignment.CenterEnd).width(2.dp).height(7.dp)
                    .clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.outline),
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (n > 0.001f) Box(Modifier.fillMaxHeight().weight(n).background(bucketColor(CategoryBucket.NEED)))
        if (w > 0.001f) Box(Modifier.fillMaxHeight().weight(w).background(bucketColor(CategoryBucket.WANT)))
        if (s > 0.001f) Box(Modifier.fillMaxHeight().weight(s).background(bucketColor(CategoryBucket.SAVINGS)))
        if (leftover > 0.001f) {
            Box(Modifier.fillMaxHeight().weight(leftover).background(bucketLeftoverColor()))
        }
    }
}

/** One bucket's legend row: colour dot, name + "amount · target", the big percent, and a delta pill
 *  (suppressed under the one-time ask, where no definition is chosen yet). */
@Composable
private fun BucketRow(share: BucketShare, showPill: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bucketColor(share.bucket)))
        Column(Modifier.weight(1f)) {
            Text(bucketLabel(share.bucket), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.insights_nws_row_sub, share.amount.formatMoney(), share.targetPercent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("${share.percent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (showPill) BucketDeltaPill(share)
    }
}

@Composable
private fun BucketDeltaPill(share: BucketShare) {
    val (label, fg, bg) = when (share.status) {
        BucketDeltaStatus.ON_TARGET ->
            Triple(stringResource(R.string.insights_nws_on_target), budgetGoodColor(), wellbeingGoodContainer())
        BucketDeltaStatus.OVER_GOOD ->
            Triple(signedPoints(share.deltaPoints), budgetGoodColor(), wellbeingGoodContainer())
        BucketDeltaStatus.OVER_WARN ->
            Triple(signedPoints(share.deltaPoints), wellbeingWarnOn(), wellbeingWarnContainer())
        BucketDeltaStatus.UNDER_WARN ->
            Triple(signedPoints(share.deltaPoints), wellbeingWarnOn(), wellbeingWarnContainer())
        BucketDeltaStatus.UNDER_NEUTRAL -> Triple(
            stringResource(R.string.insights_nws_under, -share.deltaPoints),
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** One sentence under the divider: states the gap and one concrete move, never a scolding. */
@Composable
private fun splitSummary(split: NeedsWantsSplit): String = when (split.tone) {
    SplitTone.BALANCED -> stringResource(R.string.insights_nws_summary_balanced)
    SplitTone.WANTS_OVER -> stringResource(R.string.insights_nws_summary_wants_over, split.wants.deltaPoints)
    SplitTone.NEEDS_OVER -> stringResource(R.string.insights_nws_summary_needs_over, split.needs.deltaPoints)
    SplitTone.UNDER_SAVING -> {
        val gapPoints = split.savings.targetPercent - split.savings.percent
        val gapAmount = split.income
            .multiply(BigDecimal(split.savings.targetPercent))
            .divide(BigDecimal(100))
            .subtract(split.savings.amount)
            .coerceAtLeast(BigDecimal.ZERO)
        stringResource(R.string.insights_nws_summary_under_saving, gapPoints, gapAmount.formatMoney())
    }
}

/** The setup state: the target preview it will become, plus one CTA. Shown when there's no income —
 *  built-in categories come pre-tagged, so income (the 50/30/20 denominator) is the real precondition. */
@Composable
private fun NeedsWantsSetup(onGoToBudget: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.insights_needs_wants_savings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.insights_nws_setup_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SetupTargetTile(CategoryBucket.NEED, 50, R.string.insights_nws_needs, Modifier.weight(50f))
            SetupTargetTile(CategoryBucket.WANT, 30, R.string.insights_nws_wants, Modifier.weight(30f))
            SetupTargetTile(CategoryBucket.SAVINGS, 20, R.string.insights_nws_saved, Modifier.weight(20f))
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            stringResource(R.string.insights_nws_setup_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            stringResource(R.string.insights_nws_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onGoToBudget)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.insights_nws_setup_cta),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            stringResource(R.string.insights_nws_setup_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SetupTargetTile(bucket: CategoryBucket, target: Int, labelRes: Int, modifier: Modifier) {
    val accent = bucketColor(bucket)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = bucketContainerAlpha())),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

/** The split trend: Savings on top of every column so its share grows against a dashed 20% line;
 *  closed months only. Shared by phone and tablet ([showMonthLabels] adds per-column savings %). */
@Composable
internal fun BucketTrendContent(months: List<BucketMonth>, showMonthLabels: Boolean = false) {
    val savingsFirst = months.first().savingsPercent
    val savingsLast = months.last().savingsPercent
    val delta = savingsLast - savingsFirst
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.insights_nws_trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pluralStringResource(R.plurals.insights_nws_trend_subtitle, months.size, months.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val positive = delta >= 0
            Text(
                text = stringResource(R.string.insights_nws_trend_delta, signedPoints(delta)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (positive) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (positive) wellbeingGoodContainer() else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        BucketTrendChart(months)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            months.forEach { m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showMonthLabels) {
                        Text(
                            "${m.savingsPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = bucketColor(CategoryBucket.SAVINGS),
                        )
                    }
                    Text(
                        m.axisLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BucketLegendDot(CategoryBucket.NEED)
            BucketLegendDot(CategoryBucket.WANT)
            BucketLegendDot(CategoryBucket.SAVINGS)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            stringResource(R.string.insights_nws_trend_footer, savingsFirst, savingsLast, months.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BucketTrendChart(months: List<BucketMonth>) {
    val needsC = bucketColor(CategoryBucket.NEED)
    val wantsC = bucketColor(CategoryBucket.WANT)
    val savingsC = bucketColor(CategoryBucket.SAVINGS)
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.fillMaxWidth().height(150.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            months.forEach { m ->
                val leftover = (100 - m.needsPercent - m.wantsPercent - m.savingsPercent).coerceAtLeast(0)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(track),
                ) {
                    // Savings caps the column (top) so it grows toward the 20% line; then Wants, Needs,
                    // and the unallocated remainder as the track at the bottom.
                    if (m.savingsPercent > 0) Box(Modifier.fillMaxWidth().weight(m.savingsPercent.toFloat()).background(savingsC))
                    if (m.wantsPercent > 0) Box(Modifier.fillMaxWidth().weight(m.wantsPercent.toFloat()).background(wantsC))
                    if (m.needsPercent > 0) Box(Modifier.fillMaxWidth().weight(m.needsPercent.toFloat()).background(needsC))
                    if (leftover > 0) Box(Modifier.fillMaxWidth().weight(leftover.toFloat()))
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * (SAVINGS_TARGET_FRACTION)
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }
    }
}

@Composable
private fun BucketLegendDot(bucket: CategoryBucket) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(bucketColor(bucket)))
        Text(bucketLabel(bucket), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun bucketLabel(bucket: CategoryBucket): String = stringResource(
    when (bucket) {
        CategoryBucket.NEED -> R.string.insights_nws_needs
        CategoryBucket.WANT -> R.string.insights_nws_wants
        CategoryBucket.SAVINGS -> R.string.insights_nws_savings
    },
)

/** The dashed savings-target line sits this far down from the top of the trend chart (20%). */
private const val SAVINGS_TARGET_FRACTION = 0.20f

/** A signed whole-point label using a true minus sign, e.g. "+10" / "−5". */
private fun signedPoints(points: Int): String = if (points >= 0) "+$points" else "−${-points}"

@Composable
private fun IncomeSourceRow(source: IncomeSourceUi, amountColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("💰", fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(source.entity.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(recurringSubtitle(source.entity, includeCategory = false), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("+${source.amount.formatMoney()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = amountColor)
            Text("${source.percent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Income by source": a mini stacked bar + per-source share, shown only with two or more sources. */
@Composable
private fun IncomeBySourceContent(state: InsightsUiState, periodLabel: String, onGoToBudget: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        MoneyFlowCardHeader(stringResource(R.string.insights_income_by_source), periodLabel)
        val sources = state.incomeSources
        when {
            state.periodIncome.signum() <= 0 || sources.isEmpty() ->
                CardNudge("💡", null, stringResource(R.string.insights_income_nudge_sources), onGoToBudget)

            sources.size < 2 ->
                CardNudge("💰", null, stringResource(R.string.insights_income_single), onGoToBudget)

            else -> {
                val green = budgetGoodColor()
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                Row(
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    sources.forEachIndexed { i, s ->
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(s.percent.coerceAtLeast(1).toFloat())
                                .background(IncomeSourceColors[i % IncomeSourceColors.size]),
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                sources.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    IncomeSourceRow(s, green)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.insights_income_total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("+${state.periodIncome.formatMoney()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = green)
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl), content = content)
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

/**
 * The Overview tab (P2): a bespoke summary that leads the screen — total spent, the 50/30/20 split,
 * a few headline stats, the top categories, and a couple of highlights, each linking into the tab
 * that holds the full detail. Built entirely from existing [InsightsUiState] data (no new derivation).
 */
/**
 * The Overview tab's setup/global-toggle wiring (P3), bundled so [OverviewTabContent] stays a short
 * parameter list: the two dismissed-state flags the checklist reads, plus the checklist/chip actions.
 */
private class OverviewControls(
    val dismissedSetup: Set<String>,
    val overlayNudgeDismissed: Boolean,
    val onNavigateToBudget: () -> Unit,
    val onNavigateToManageCategories: () -> Unit,
    val onToggleIncludeRecurringBills: (Boolean) -> Unit,
    val onChooseSavingsAllocation: (Boolean) -> Unit,
    val onDismissOverlayNudge: () -> Unit,
    val onDismissSetupItem: (String) -> Unit,
)

@Composable
private fun OverviewTabContent(
    state: InsightsUiState,
    onGoToTab: (InsightsTab) -> Unit,
    onSliceClick: (PieSlice) -> Unit,
    controls: OverviewControls,
) {
    // Hero: total spent + period-over-period delta + the 50/30/20 mini split + headline stats.
    InsightCard {
        Text(
            text = stringResource(R.string.insights_overview_spent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            Text(
                text = state.total.formatMoney(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            state.periodComparison?.takeIf { it.deltaPercent != 0 }?.let { pc ->
                val down = pc.deltaPercent < 0
                val magnitude = if (down) -pc.deltaPercent else pc.deltaPercent
                Text(
                    text = "${if (down) "↓" else "↑"} $magnitude%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (down) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.sm),
                )
            }
        }
        state.needsWantsSplit?.let { split ->
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            BucketSplitBar(split)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                CompactBucketLabel(CategoryBucket.NEED, split.needs.percent)
                CompactBucketLabel(CategoryBucket.WANT, split.wants.percent)
                CompactBucketLabel(CategoryBucket.SAVINGS, split.savings.percent)
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            StatTile(stringResource(R.string.insights_stat_avg_day), state.avgPerDay.formatMoney(), Modifier.weight(1f))
            StatTile(stringResource(R.string.home_receipts), state.receiptCount.toString(), Modifier.weight(1f))
            StatTile(
                stringResource(R.string.insights_stat_saved),
                state.totalSaved.formatMoney(),
                Modifier.weight(1f),
                valueColor = budgetGoodColor(),
            )
        }
    }
    // Top spending: a compact donut + the top three categories, linking into the Spending tab.
    if (state.slices.isNotEmpty()) {
        InsightCard {
            OverviewLinkHeader(R.string.insights_overview_top, InsightsTab.SPENDING, onGoToTab)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            state.slices.take(4).forEachIndexed { index, slice ->
                if (index > 0) Spacer(Modifier.height(MaterialTheme.dimens.md))
                TopSliceRow(slice, onClick = { onSliceClick(slice) })
            }
        }
    }
    // Worth knowing: the top highlights + the on-pace projection, linking into the Trends tab.
    if (state.highlights.isNotEmpty() || state.projectedTotal != null) {
        InsightCard {
            OverviewLinkHeader(R.string.insights_overview_worth, InsightsTab.TRENDS, onGoToTab)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            state.highlights.take(2).forEachIndexed { index, highlight ->
                if (index > 0) Spacer(Modifier.height(MaterialTheme.dimens.md))
                HighlightRow(highlight, state.period)
            }
            state.projectedTotal?.let { projected ->
                if (state.highlights.isNotEmpty()) Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = stringResource(R.string.insights_overview_on_pace, projected.formatMoney()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    // "Things to set up": the consolidated setup checklist, then the two global quick-toggle chips.
    // Both self-hide when nothing applies; gated on isLoaded so neither flashes on cold start.
    if (state.isLoaded) {
        OverviewSetupChecklist(
            items = activeSetupItems(state, controls.dismissedSetup, controls.overlayNudgeDismissed),
            onAction = { item ->
                when (item) {
                    // Savings allocation is chosen inline on the Money tab's Needs/Wants card.
                    InsightsSetupItem.SAVINGS -> onGoToTab(InsightsTab.MONEY)
                    InsightsSetupItem.INCOME -> controls.onNavigateToBudget()
                    InsightsSetupItem.OVERLAY -> controls.onToggleIncludeRecurringBills(true)
                    InsightsSetupItem.BUCKETS -> controls.onNavigateToManageCategories()
                }
            },
            onDismiss = { item ->
                // The overlay item shares the older discovery-nudge flag; the rest use the setup set.
                if (item == InsightsSetupItem.OVERLAY) {
                    controls.onDismissOverlayNudge()
                } else {
                    controls.onDismissSetupItem(item.key)
                }
            },
        )
        OverviewToggleChips(
            state = state,
            onToggleIncludeRecurringBills = controls.onToggleIncludeRecurringBills,
            onChooseSavingsAllocation = controls.onChooseSavingsAllocation,
        )
    }
}

/**
 * Which setup-checklist items are live for [state] right now. Each fires only while its setup is
 * genuinely incomplete and it hasn't been dismissed; the overlay item reuses the existing
 * discovery-nudge gate (and its own dismissed flag) so it never double-shows against that feature.
 */
private fun activeSetupItems(
    state: InsightsUiState,
    dismissedSetup: Set<String>,
    overlayNudgeDismissed: Boolean,
): List<InsightsSetupItem> = buildList {
    // Savings allocation not yet answered, but there's a split for it to matter to.
    if (state.needsWantsSplit != null && state.savingsAllocation == null &&
        InsightsSetupItem.SAVINGS.key !in dismissedSetup
    ) {
        add(InsightsSetupItem.SAVINGS)
    }
    // No money plan at all (neither income nor bills), so the whole Money tab is empty.
    if (!state.hasIncome && !state.hasBills && InsightsSetupItem.INCOME.key !in dismissedSetup) {
        add(InsightsSetupItem.INCOME)
    }
    // Recurring bills exist but the planned overlay is off (same gate as the retired inline nudge).
    if (shouldShowOverlayNudge(state, overlayNudgeDismissed)) add(InsightsSetupItem.OVERLAY)
    // There's a split, but every category is still on its default Needs/Wants bucket.
    if (state.needsWantsSplit != null && !state.hasCustomBuckets &&
        InsightsSetupItem.BUCKETS.key !in dismissedSetup
    ) {
        add(InsightsSetupItem.BUCKETS)
    }
}

/**
 * The Overview "things to set up" card: a tonal container that starts as a one-line summary chip and
 * expands to a short, per-row-dismissible checklist (P3). Renders nothing when [items] is empty, so
 * the card is simply absent once there's nothing to set up. The header ✕ dismisses every shown item.
 */
@Composable
private fun OverviewSetupChecklist(
    items: List<InsightsSetupItem>,
    onAction: (InsightsSetupItem) -> Unit,
    onDismiss: (InsightsSetupItem) -> Unit,
) {
    if (items.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.insights_setup_count, items.size, items.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(if (expanded) R.string.insights_setup_hide else R.string.insights_setup_review),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = MaterialTheme.dimens.sm, vertical = MaterialTheme.dimens.xs),
            )
            SetupDismissButton(tint = onContainer, onClick = { items.forEach(onDismiss) })
        }
        if (expanded) {
            items.forEach { item ->
                HorizontalDivider(color = onContainer.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
                ) {
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(item.ctaRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onAction(item) }
                            .padding(horizontal = MaterialTheme.dimens.sm, vertical = MaterialTheme.dimens.xs),
                    )
                    SetupDismissButton(tint = onContainer, onClick = { onDismiss(item) })
                }
            }
        }
    }
}

/** The compact ✕ used in the setup checklist (header + per row); a small, labelled touch target. */
@Composable
private fun SetupDismissButton(tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.xs),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_insights_setup_dismiss),
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The two Overview global quick-toggle chips: the planned-bills overlay (shown when there are bills to
 * overlay) and the Needs/Wants savings-allocation mode (shown once a split exists and the mode is set;
 * the first choice is made through the checklist). Each is a compact pill toggle; the whole chip is the
 * control. Absent entirely when neither applies.
 */
@Composable
private fun OverviewToggleChips(
    state: InsightsUiState,
    onToggleIncludeRecurringBills: (Boolean) -> Unit,
    onChooseSavingsAllocation: (Boolean) -> Unit,
) {
    val showPlanned = state.hasBills
    val showSavings = state.needsWantsSplit != null && state.savingsAllocation != null
    if (!showPlanned && !showSavings) return
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
        if (showPlanned) {
            OverviewToggleChip(
                checked = state.includeRecurringBills,
                label = stringResource(R.string.insights_overview_chip_planned),
                modifier = Modifier.weight(1f),
                onToggle = { onToggleIncludeRecurringBills(!state.includeRecurringBills) },
            )
        }
        if (showSavings) {
            val kept = state.savingsAllocation == true
            OverviewToggleChip(
                checked = kept,
                label = stringResource(
                    if (kept) R.string.insights_overview_chip_savings_kept
                    else R.string.insights_overview_chip_savings_aside,
                ),
                modifier = Modifier.weight(1f),
                onToggle = { onChooseSavingsAllocation(!kept) },
            )
        }
    }
}

/** One compact pill toggle-chip: a mini track+knob and a label, the whole row toggling [checked]. */
@Composable
private fun OverviewToggleChip(
    checked: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        val track = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 15.dp)
                .clip(RoundedCornerShape(50))
                .background(track),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** A card header with a title and a "<tab> ›" deep-link into the tab that holds the full detail. */
@Composable
private fun OverviewLinkHeader(@StringRes titleRes: Int, linkTab: InsightsTab, onGoToTab: (InsightsTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${stringResource(linkTab.labelRes)} ›",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onGoToTab(linkTab) }
                .padding(horizontal = MaterialTheme.dimens.sm, vertical = MaterialTheme.dimens.xs),
        )
    }
}

/** Compact "Needs 52%" label under the Overview split bar. */
@Composable
private fun CompactBucketLabel(bucket: CategoryBucket, percent: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(bucketColor(bucket)))
        Text(
            text = "${bucketLabel(bucket)} $percent%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One "top spending" row: colour dot, category name, amount; taps open its transactions. */
@Composable
private fun TopSliceRow(slice: PieSlice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(slice.color))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(slice.value.formatMoney(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** The toolbar recap control: a play button that opens the last recap (only shown when one exists). */
@Composable
private fun RecapToolbarButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.insights_open_recap),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CategoryStatRow(slice: PieSlice, total: BigDecimal, onClick: () -> Unit) {
    val pct = if (total.signum() > 0) slice.value.toDouble() / total.toDouble() else 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
            .clickable(onClick = onClick),
    ) {
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
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        LinearProgressIndicator(
            progress = { pct.toFloat() },
            color = slice.color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun StoreStatRow(store: String, amount: BigDecimal, onClick: () -> Unit) {
    val tile = Color.hsv(((store.hashCode() and 0x7FFFFFFF) % 360).toFloat(), 0.45f, 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = store.trim().take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = store,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** "the previous week/month/quarter/half-year" (stepped) or "the previous period" (custom range). */
@Composable
private fun previousPeriodNoun(period: InsightsPeriod): String = when (period) {
    is InsightsPeriod.Stepped -> when (period.unit) {
        PeriodUnit.WEEK -> stringResource(R.string.insights_prev_week)
        PeriodUnit.MONTH -> stringResource(R.string.insights_prev_month)
        PeriodUnit.QUARTER -> stringResource(R.string.insights_prev_quarter)
        PeriodUnit.HALF_YEAR -> stringResource(R.string.insights_prev_half)
    }
    is InsightsPeriod.Custom -> stringResource(R.string.insights_prev_period)
}

/**
 * Period-over-period card: a trend icon beside "12% less than the previous month" and the two
 * periods' totals. Green with a down arrow when spending fell, red with an up arrow when it rose;
 * the "previous …" noun and the labelled totals follow the active [period].
 */
@Composable
private fun PeriodComparisonContent(comparison: PeriodComparison, period: InsightsPeriod, monthStartDay: Int) {
    val green = budgetGoodColor()
    val red = budgetBadColor()
    val previousNoun = previousPeriodNoun(period)
    val currentLabel = periodFriendlyLabel(period, monthStartDay = monthStartDay)
    val previousLabel = periodFriendlyLabel(period.previousPeriod(), monthStartDay = monthStartDay)
    val (icon, accent, headline) = when {
        comparison.deltaPercent < 0 -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            green,
            stringResource(R.string.insights_compare_less, -comparison.deltaPercent, previousNoun),
        )
        comparison.deltaPercent > 0 -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            red,
            stringResource(R.string.insights_compare_more, comparison.deltaPercent, previousNoun),
        )
        else -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.insights_compare_same, previousNoun),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.touchTarget)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Column {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$currentLabel: ${comparison.currentTotal.formatMoney()} · " +
                    "$previousLabel: ${comparison.previousTotal.formatMoney()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One row of "By category vs last month": the category's color dot and name, with its signed
 * change in spend — red when spending rose, green when it fell.
 */
@Composable
private fun CategoryDeltaRow(delta: CategoryDelta) {
    val increased = delta.delta.signum() > 0
    val color = if (increased) budgetBadColor() else budgetGoodColor()
    val sign = if (increased) "+" else "−"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(delta.color),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            text = categoryDisplayName(delta.category),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$sign${delta.delta.abs().formatMoney()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** Tallest a bar can grow; the row reserves extra room below for the axis-label strip. */
private val MAX_BAR_HEIGHT = 120.dp

/** Up to this many bars fill the card width; beyond it the chart scrolls horizontally instead
 *  of squeezing the bars thinner. */
private const val MAX_FIT_BARS = 7

/** Fixed column width per bar once the chart scrolls, wide enough to stay tappable and to fit a
 *  day-number label underneath. */
private val SCROLL_BAR_WIDTH = 36.dp

private fun barFraction(total: BigDecimal, maxTotal: BigDecimal): Float =
    if (maxTotal.signum() > 0) (total.toDouble() / maxTotal.toDouble()).toFloat() else 0f

/**
 * Bar chart over the period's [buckets] (one per day or month). Tapping a bar selects it; the
 * header above shows the selected bucket's date and exact spend, and the current day/month keeps a
 * subtle highlight. With seven or fewer bars they share the card width; beyond that the chart
 * scrolls horizontally (opened to the most recent bars) so each bar stays a comfortable width.
 */
@Composable
private fun TrendChart(
    buckets: List<TrendBucket>,
    modifier: Modifier = Modifier,
    showPlanned: Boolean = false,
) {
    if (buckets.isEmpty()) return
    // With the overlay on, the tallest bar is spend + its planned cap, so every bar rescales to fit
    // both — the amounts don't change, the axis does (explained in the Trend dialog).
    val maxTotal = buckets.maxOf { if (showPlanned) it.total + it.planned else it.total }
    // Default selection: the most recent bar with spend, falling back to the last real (enabled) bar
    // so the header never lands on an inactive future placeholder.
    val defaultIndex = remember(buckets) {
        buckets.indexOfLast { it.total.signum() > 0 }.takeIf { it >= 0 }
            ?: buckets.indexOfLast { it.enabled }.takeIf { it >= 0 }
            ?: buckets.lastIndex
    }
    var selectedIndex by remember(buckets) { mutableStateOf(defaultIndex) }
    val selected = buckets[selectedIndex.coerceIn(buckets.indices)]
    val scrollable = buckets.size > MAX_FIT_BARS
    val scrollState = rememberScrollState()
    // Open the scroller on the latest bars (right edge), matching the default selection.
    if (scrollable) {
        LaunchedEffect(buckets, scrollState.maxValue) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = selected.fullLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selected.total.formatMoney(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(if (scrollable) 6.dp else MaterialTheme.dimens.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEachIndexed { index, bucket ->
                TrendBar(
                    bucket = bucket,
                    fraction = barFraction(bucket.total, maxTotal),
                    plannedFraction = if (showPlanned) barFraction(bucket.planned, maxTotal) else 0f,
                    isSelected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                    modifier = if (scrollable) Modifier.width(SCROLL_BAR_WIDTH) else Modifier.weight(1f),
                )
            }
        }
    }
}

/** A single column in [TrendChart]: the solid spend bar sized to [fraction] of the tallest, optionally
 *  capped by a hatched "planned bills" segment of [plannedFraction], with its axis label below. The
 *  whole column is tappable so even slim bars are easy to hit. */
@Composable
private fun TrendBar(
    bucket: TrendBucket,
    fraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plannedFraction: Float = 0f,
) {
    val barColor = when {
        // Not-yet-elapsed padding day: a faint empty stub that isn't tappable.
        !bucket.enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        isSelected -> MaterialTheme.colorScheme.primary
        bucket.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    }
    val hatchColor = MaterialTheme.colorScheme.outlineVariant
    val hasCap = plannedFraction > 0f
    val capTop = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .then(if (bucket.enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Bar area (wraps its content): the hatched planned cap on top, the solid spend bar below.
        Column(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hasCap) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((MAX_BAR_HEIGHT * plannedFraction).coerceAtLeast(MaterialTheme.dimens.xs))
                        .clip(capTop)
                        .drawBehind { drawPlannedHatch(hatchColor, spacing = 5.dp, stroke = 1.2.dp) }
                        .border(1.dp, hatchColor, capTop),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((MAX_BAR_HEIGHT * fraction).coerceAtLeast(MaterialTheme.dimens.xs))
                    .clip(if (hasCap) RectangleShape else capTop)
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = bucket.axisLabel,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = when {
                !bucket.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Planned recurring-bills overlay: Customize toggle, discovery nudge, section badge, dialogs ──────

/** Whether the one-time overlay discovery nudge should show: loaded, not dismissed, overlay off, and
 *  there are recurring bills projecting a positive amount this period (the figure Home already shows). */
private fun shouldShowOverlayNudge(state: InsightsUiState, dismissed: Boolean): Boolean =
    state.isLoaded && !dismissed && !state.includeRecurringBills &&
        state.hasBills && state.periodBills.signum() > 0

/**
 * The "Layers" group at the top of the Customize-sections sheet: the opt-in switch for the
 * planned-bills overlay, with the hatch swatch as its icon so the sheet teaches the texture before it
 * appears on the charts. Off by default; flipping it is remembered per user.
 */
@Composable
internal fun InsightsLayersToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SavingsSheetLabel(stringResource(R.string.insights_overlay_layers))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = MaterialTheme.dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlannedSwatch(hatched = true, size = 20.dp)
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insights_overlay_toggle_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.insights_overlay_toggle_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
    }
}

/**
 * The one-time discovery nudge above Breakdown: names, in the user's own terms, the disagreement the
 * tester reported ("Home also counts €X of recurring bills") and offers to switch the overlay on — the
 * only surface that reveals an otherwise-invisible, off-by-default preference. Self-gating; shown once
 * per user until enabled or dismissed.
 */
@Composable
private fun OverlayDiscoveryNudge(
    state: InsightsUiState,
    dismissed: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!shouldShowOverlayNudge(state, dismissed)) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(
                start = MaterialTheme.dimens.lg,
                end = MaterialTheme.dimens.sm,
                top = MaterialTheme.dimens.sm,
                bottom = MaterialTheme.dimens.md,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.insights_overlay_nudge_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_insights_overlay_nudge_dismiss),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Text(
            text = stringResource(R.string.insights_overlay_nudge_text, state.periodBills.formatMoney()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(end = MaterialTheme.dimens.sm),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        TextButton(onClick = onEnable) {
            PlannedSwatch(hatched = true, size = 12.dp)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.insights_overlay_nudge_action), fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A section card title with an optional trailing "Planned" badge (Summary / Trend headers). */
@Composable
private fun SectionTitleRow(title: String, showPlannedBadge: Boolean, onPlannedBadgeClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (showPlannedBadge) PlannedBadge(onClick = onPlannedBadgeClick)
    }
}

/**
 * The read-only explainer opened by a section's "Planned" badge. Purely explanatory — the switch stays
 * in Customize — with a shared "Spent vs Planned" header and section-specific body (the per-bill wedge
 * makeup + dedup for Breakdown, why the tiles hold still for Summary, how to read the caps for Trend).
 */
@Composable
private fun PlannedOverlayDialog(
    dialog: PlannedDialog,
    state: InsightsUiState,
    periodLabel: String,
    onDismiss: () -> Unit,
) {
    val overlay = state.plannedOverlay
    val sectionName = stringResource(
        when (dialog) {
            PlannedDialog.BREAKDOWN -> R.string.insights_breakdown
            PlannedDialog.SUMMARY -> R.string.insights_summary
            PlannedDialog.TREND -> R.string.insights_trend
        },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        title = {
            Column {
                Text(
                    text = stringResource(R.string.insights_overlay_bills_planned),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.insights_overlay_dialog_subtitle, sectionName, periodLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                PlannedSpentPlannedRow(spent = state.total, planned = overlay.plannedTotal)
                when (dialog) {
                    PlannedDialog.BREAKDOWN -> PlannedBreakdownDialogBody(overlay = overlay, spent = state.total)
                    PlannedDialog.SUMMARY -> PlannedSummaryDialogBody(overlay = overlay)
                    PlannedDialog.TREND -> PlannedTrendDialogBody(overlay = overlay)
                }
            }
        },
    )
}

/** The shared "Spent €950  ·  Planned €967" key at the top of every overlay dialog. */
@Composable
private fun PlannedSpentPlannedRow(spent: BigDecimal, planned: BigDecimal) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xl)) {
        AmountKey(hatched = false, label = stringResource(R.string.insights_overlay_spent), amount = spent)
        AmountKey(hatched = true, label = stringResource(R.string.insights_overlay_planned), amount = planned)
    }
}

@Composable
private fun AmountKey(hatched: Boolean, label: String, amount: BigDecimal) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
        ) {
            PlannedSwatch(hatched = hatched, size = 10.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(amount.formatMoney(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlannedBreakdownDialogBody(overlay: PlannedOverlay, spent: BigDecimal) {
    SavingsSheetLabel(stringResource(R.string.insights_overlay_wedge_header))
    overlay.bills.forEach { bill -> PlannedBillRow(label = bill.label, amount = bill.amount) }
    Text(
        text = stringResource(R.string.insights_overlay_denominator, spent.formatMoney()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (overlay.matched.isNotEmpty()) PlannedDedupNote(overlay.matched)
}

@Composable
private fun PlannedBillRow(label: String, amount: BigDecimal) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(amount.formatMoney(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** The dedup note: the bills hidden as already-matched to a receipt (counted once, in spend). */
@Composable
private fun PlannedDedupNote(matched: List<MatchedBillLine>) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(MaterialTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Text(
            text = pluralStringResource(R.plurals.insights_overlay_dedup_note, matched.size, matched.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        matched.forEach { m ->
            val date = Instant.ofEpochMilli(m.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = m.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.insights_overlay_matched_detail,
                        dateFormat.format(date),
                        m.amount.formatMoney(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.insights_overlay_counted_once),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlannedSummaryDialogBody(overlay: PlannedOverlay) {
    Text(
        text = stringResource(R.string.insights_overlay_summary_lead),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SavingsSheetLabel(stringResource(R.string.insights_overlay_summary_header))
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_summary_avgday_title),
        stringResource(R.string.insights_overlay_summary_avgday_body),
    )
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_summary_receipts_title),
        stringResource(R.string.insights_overlay_summary_receipts_body),
    )
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_summary_avg_title),
        stringResource(R.string.insights_overlay_summary_avg_body),
    )
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_summary_saved_title),
        stringResource(R.string.insights_overlay_summary_saved_body),
    )
    if (overlay.matched.isNotEmpty()) {
        Text(
            text = pluralStringResource(
                R.plurals.insights_overlay_dedup_excludes,
                overlay.matched.size,
                overlay.plannedTotal.formatMoney(),
                overlay.matched.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlannedTrendDialogBody(overlay: PlannedOverlay) {
    SavingsSheetLabel(stringResource(R.string.insights_overlay_trend_header))
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_trend_flat_title),
        stringResource(R.string.insights_overlay_trend_flat_body, overlay.plannedTotal.formatMoney()),
    )
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_trend_early_title),
        stringResource(R.string.insights_overlay_trend_early_body),
    )
    PlannedExplainRow(
        stringResource(R.string.insights_overlay_trend_scale_title),
        stringResource(R.string.insights_overlay_trend_scale_body),
    )
}

/** A titled explanation row used in the Summary and Trend overlay dialogs. */
@Composable
private fun PlannedExplainRow(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val previewInsightsState = InsightsUiState(
    isLoaded = true,
    slices = listOf(
        PieSlice("Groceries", BigDecimal("242"), Color(0xFF52B770)),
        PieSlice("Dining", BigDecimal("114"), Color(0xFFB77052)),
        PieSlice("Fuel", BigDecimal("90"), Color(0xFFB79552)),
        PieSlice("Household", BigDecimal("85"), Color(0xFFB75285)),
        PieSlice("Health", BigDecimal("64"), Color(0xFF52B7B4)),
        PieSlice("Other", BigDecimal("117"), Color(0xFF9B97A1)),
    ),
    total = BigDecimal("712"),
    receiptCount = 18,
    totalSaved = BigDecimal("12.40"),
    avgPerReceipt = BigDecimal("39.58"),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun InsightsScreenPreview() {
    BudgettyTheme {
        InsightsScreenContent(
            state = previewInsightsState,
            isExpanded = false,
            isWide = false,
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onUnitSelected = {},
            onStepBackward = {},
            onStepForward = {},
            onCustomRangeSelected = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun InsightsScreenTabletPreview() {
    BudgettyTheme {
        InsightsScreenContent(
            state = previewInsightsState,
            isExpanded = true,
            isWide = true,
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onUnitSelected = {},
            onStepBackward = {},
            onStepForward = {},
            onCustomRangeSelected = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun InsightsScreenEmptyPreview() {
    BudgettyTheme {
        InsightsScreenContent(
            state = InsightsUiState(isLoaded = true),
            isExpanded = false,
            isWide = false,
            hiddenSections = emptySet(),
            sectionOrder = emptyList(),
            onToggleSection = { _, _ -> },
            onReorderSections = {},
            onRevertSections = {},
            onUnitSelected = {},
            onStepBackward = {},
            onStepForward = {},
            onCustomRangeSelected = { _, _ -> },
        )
    }
}
