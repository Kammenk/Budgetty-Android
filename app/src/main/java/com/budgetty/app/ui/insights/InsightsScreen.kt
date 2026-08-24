package com.budgetty.app.ui.insights

import com.budgetty.app.ui.theme.dimens
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.budgetty.app.ui.components.SectionsMenu
import com.budgetty.app.ui.components.drawPlannedHatch
import com.budgetty.app.ui.savings.SavingsSheetLabel
import com.budgetty.app.ui.util.MatchedBillLine
import com.budgetty.app.ui.components.StoreTransactionsSheet
import com.budgetty.app.ui.components.TransactionLineRow
import com.budgetty.app.ui.components.resolveSectionOrder
import com.budgetty.app.ui.util.formatMoney
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.R
import com.budgetty.app.ui.wellbeing.WellbeingInsightsRow
import com.budgetty.app.ui.subscriptions.SubscriptionsInsightsCard
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
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
        overlayNudgeDismissed = settings.insightsOverlayNudgeDismissed,
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
    overlayNudgeDismissed: Boolean = false,
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
                onNavigateToWellbeing = onNavigateToWellbeing,
                onToggleIncludeRecurringBills = onToggleIncludeRecurringBills,
                onPlannedBadgeClick = { plannedDialog = it },
                onDismissOverlayNudge = onDismissOverlayNudge,
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
                onToggleIncludeRecurringBills = onToggleIncludeRecurringBills,
                onPlannedBadgeClick = { plannedDialog = it },
                onDismissOverlayNudge = onDismissOverlayNudge,
                overlayNudgeDismissed = overlayNudgeDismissed,
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
    onToggleIncludeRecurringBills: (Boolean) -> Unit = {},
    onPlannedBadgeClick: (PlannedDialog) -> Unit = {},
    onDismissOverlayNudge: () -> Unit = {},
    overlayNudgeDismissed: Boolean = false,
) {
    fun shows(section: InsightsSection) = section.key !in hiddenSections
    val hasData = state.slices.isNotEmpty()
    val ordered = resolveSectionOrder(sectionOrder, InsightsSection.entries, InsightsSection::key)
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
                    InsightsLayersToggle(
                        checked = state.includeRecurringBills,
                        onCheckedChange = onToggleIncludeRecurringBills,
                    )
                },
            )
        }
        stepper(Modifier.fillMaxWidth(), true)
        // Pinned above Breakdown: a one-line door into the Wellbeing screen. Hidden via Customize sections.
        if (shows(InsightsSection.WELLBEING)) {
            state.wellbeing?.let { WellbeingInsightsRow(summary = it, onClick = onNavigateToWellbeing) }
        }
        ordered.forEach { section ->
            if (shows(section)) {
                when (section) {
                    // Breakdown shows its own empty state, so it renders even with no data; the rest
                    // only appear once there's spend to summarize.
                    InsightsSection.BREAKDOWN -> if (state.isLoaded) {
                        // One-time nudge (the off-by-default overlay is otherwise invisible), above Breakdown.
                        OverlayDiscoveryNudge(
                            state = state,
                            dismissed = overlayNudgeDismissed,
                            onEnable = { onToggleIncludeRecurringBills(true) },
                            onDismiss = onDismissOverlayNudge,
                        )
                        if (hasData) {
                            BreakdownCard(
                                slices = state.slices,
                                total = state.total,
                                periodLabel = periodLabel,
                                onSliceClick = onSliceClick,
                                includeBills = state.includeRecurringBills,
                                plannedOverlay = state.plannedOverlay,
                                onPlannedBadgeClick = { onPlannedBadgeClick(PlannedDialog.BREAKDOWN) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            // A period with no spend: one friendly, period-aware message in place of
                            // the empty donut, so stepping into an empty month isn't a blank screen.
                            InsightCard { PeriodEmptyState(periodLabel, hasAnyData = state.earliestDate != null) }
                        }
                    }

                    InsightsSection.SUBSCRIPTIONS -> SubscriptionsInsightsCard(
                        onSeeAll = onNavigateToSubscriptions,
                        onUnlock = onNavigateToPaywall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    InsightsSection.SUMMARY -> if (hasData) {
                        InsightCard {
                            SectionTitleRow(
                                title = stringResource(R.string.insights_summary),
                                showPlannedBadge = state.includeRecurringBills && state.plannedOverlay.hasPlanned,
                                onPlannedBadgeClick = { onPlannedBadgeClick(PlannedDialog.SUMMARY) },
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
                            IncomeVsSpendingContent(state, periodLabel, onNavigateToBudget)
                        }

                    InsightsSection.SAVINGS_RATE ->
                        if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                            SavingsRateContent(state, periodLabel, onNavigateToBudget)
                        }

                    InsightsSection.FIXED_FLEXIBLE ->
                        if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                            FixedVsFlexibleContent(state, periodLabel, onNavigateToBudget)
                        }

                    InsightsSection.INCOME_BY_SOURCE ->
                        if (state.isLoaded && (state.hasIncome || state.hasBills)) InsightCard {
                            IncomeBySourceContent(state, periodLabel, onNavigateToBudget)
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
                                onPlannedBadgeClick = { onPlannedBadgeClick(PlannedDialog.TREND) },
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
                        InsightCard { TopCategoriesContent(state.slices, state.total, onSliceClick) }
                    }

                    InsightsSection.TOP_STORES -> if (hasData && state.topStores.isNotEmpty()) {
                        InsightCard { TopStoresContent(state.topStores, onStoreClick) }
                    }

                    InsightsSection.BIGGEST_PURCHASES -> if (hasData && state.biggestPurchases.isNotEmpty()) {
                        InsightCard { BiggestPurchasesContent(state.biggestPurchases, state.storeByReceiptId) }
                    }

                    // Rendered as a pinned row above Breakdown (outside this loop), so nothing here.
                    InsightsSection.WELLBEING -> Unit
                }
            }
        }
        // The per-category-change card isn't user-managed, so it stays anchored at the end.
        if (hasData && state.categoryDeltas.isNotEmpty()) {
            InsightCard { ByCategoryContent(state.categoryDeltas, state.period) }
        }
    }
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
private fun InsightsTabletBody(
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
    onNavigateToWellbeing: () -> Unit = {},
    onToggleIncludeRecurringBills: (Boolean) -> Unit = {},
    onPlannedBadgeClick: (PlannedDialog) -> Unit = {},
    onDismissOverlayNudge: () -> Unit = {},
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
                trendCard(Modifier.fillMaxWidth())
                periodComparisonCard(Modifier.fillMaxWidth())
                breakdownCards()
            }
        }
    }
}

// ── Income & recurring-payment cards ───────────────────────────────────────────────────────────

private val FixedBillsColor = Color(0xFFD08A4A)
private val FlexibleLeftColor = Color(0xFF4FA85A)
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

@Composable
private fun LegendRow(dotColor: Color, label: String, amount: String, percent: Int, amountColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(dotColor))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = amountColor)
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
    }
}

/** "Where your income goes": a stacked bar of fixed bills / flexible spend / left, with a legend. */
@Composable
private fun FixedVsFlexibleContent(state: InsightsUiState, periodLabel: String, onGoToBudget: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        MoneyFlowCardHeader(stringResource(R.string.insights_fixed_flexible), periodLabel)
        val income = state.periodIncome
        if (income.signum() <= 0) {
            CardNudge("💡", null, stringResource(R.string.insights_fixed_nudge), onGoToBudget)
        } else {
            val incD = income.toDouble()
            val fixed = state.periodBills
            val flexible = state.total
            val left = income.subtract(fixed).subtract(flexible)
            val fixedFrac = (fixed.toDouble() / incD).coerceIn(0.0, 1.0).toFloat()
            val flexFrac = (flexible.toDouble() / incD).coerceIn(0.0, (1.0 - fixedFrac).coerceAtLeast(0.0)).toFloat()
            val leftFrac = (1f - fixedFrac - flexFrac).coerceIn(0f, 1f)
            fun pct(v: BigDecimal) = (v.toDouble() / incD * 100).roundToInt()
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(
                modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (fixedFrac > 0.001f) Box(Modifier.fillMaxHeight().weight(fixedFrac).background(FixedBillsColor))
                if (flexFrac > 0.001f) Box(Modifier.fillMaxHeight().weight(flexFrac).background(MaterialTheme.colorScheme.primary))
                if (leftFrac > 0.001f) Box(Modifier.fillMaxHeight().weight(leftFrac).background(FlexibleLeftColor))
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            LegendRow(FixedBillsColor, stringResource(R.string.insights_fixed_bills), fixed.formatMoney(), pct(fixed), MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            LegendRow(MaterialTheme.colorScheme.primary, stringResource(R.string.insights_flexible_spending), flexible.formatMoney(), pct(flexible), MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            LegendRow(
                FlexibleLeftColor,
                stringResource(R.string.insights_left),
                left.formatMoney(),
                pct(left),
                if (left.signum() >= 0) budgetGoodColor() else budgetBadColor(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = stringResource(R.string.insights_fixed_read, pct(fixed).coerceAtLeast(0)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
