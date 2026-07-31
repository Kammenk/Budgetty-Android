package com.budgetty.app.ui.insights

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.budgetty.app.ui.components.SectionsMenu
import com.budgetty.app.ui.components.StoreTransactionsSheet
import com.budgetty.app.ui.components.TransactionLineRow
import com.budgetty.app.ui.components.resolveSectionOrder
import com.budgetty.app.ui.util.formatMoney
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.R
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
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
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
        hiddenSections = settings.hiddenInsightsSections,
        sectionOrder = settings.insightsSectionOrder,
        onToggleSection = { section, hidden -> settingsStore.setInsightsSectionHidden(section.key, hidden) },
        onReorderSections = { settingsStore.setInsightsSectionOrder(it) },
        onRevertSections = { settingsStore.resetInsightsSections() },
        onUnitSelected = viewModel::onUnitSelected,
        onStepBackward = viewModel::onStepBackward,
        onStepForward = viewModel::onStepForward,
        onCustomRangeSelected = viewModel::onCustomRangeSelected,
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
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    onToggleSection: (InsightsSection, Boolean) -> Unit,
    onReorderSections: (List<String>) -> Unit,
    onRevertSections: () -> Unit,
    onUnitSelected: (PeriodUnit) -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onCustomRangeSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The category whose transactions are shown in the bottom sheet, or null when none is open.
    // Holding the slice keeps the sheet's accent color matched to the chart.
    var selectedSlice by remember { mutableStateOf<PieSlice?>(null) }
    // The store whose transactions are shown in the bottom sheet, or null when none is open.
    var selectedStore by remember { mutableStateOf<String?>(null) }
    // Whether the custom date-range picker sheet is open.
    var showDateRangeSheet by remember { mutableStateOf(false) }
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
                onNavigateToSubscriptions = onNavigateToSubscriptions,
                onNavigateToPaywall = onNavigateToPaywall,
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
}

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
private fun BreakdownCard(
    slices: List<PieSlice>,
    total: BigDecimal,
    periodLabel: String,
    onSliceClick: (PieSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // false = every category (default); true = rolled up into top-level groups.
    var groupedByTop by remember { mutableStateOf(false) }
    val shownSlices = remember(slices, groupedByTop) {
        if (groupedByTop) rollUpToGroups(slices) else slices
    }
    InsightCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BreakdownHeader(periodLabel, modifier = Modifier.weight(1f))
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
private fun TrendCardContent(trend: TrendData, projectedTotal: BigDecimal? = null) {
    Text(stringResource(R.string.insights_trend), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        text = if (trend.bucketing == TrendBucketing.DAILY) stringResource(R.string.insights_trend_daily) else stringResource(R.string.insights_trend_monthly),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    if (trend.hasData) {
        TrendChart(buckets = trend.buckets)
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
            )
        }
        stepper(Modifier.fillMaxWidth(), true)
        ordered.forEach { section ->
            if (shows(section)) {
                when (section) {
                    // Breakdown shows its own empty state, so it renders even with no data; the rest
                    // only appear once there's spend to summarize.
                    InsightsSection.BREAKDOWN -> if (state.isLoaded) {
                        if (hasData) {
                            BreakdownCard(
                                slices = state.slices,
                                total = state.total,
                                periodLabel = periodLabel,
                                onSliceClick = onSliceClick,
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
                            Text(stringResource(R.string.insights_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        InsightCard { TrendCardContent(state.trend, state.projectedTotal) }
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
            modifier = mod,
        )
    }
    val trendCard: @Composable (Modifier) -> Unit = { mod ->
        if (shows(InsightsSection.TREND)) InsightCard(modifier = mod) {
            TrendCardContent(state.trend, state.projectedTotal)
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
) {
    if (buckets.isEmpty()) return
    val maxTotal = buckets.maxOf { it.total }
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
                    isSelected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                    modifier = if (scrollable) Modifier.width(SCROLL_BAR_WIDTH) else Modifier.weight(1f),
                )
            }
        }
    }
}

/** A single column in [TrendChart]: the bar sized to [fraction] of the tallest, with its axis
 *  label below. The whole column is tappable so even slim bars are easy to hit. */
@Composable
private fun TrendBar(
    bucket: TrendBucket,
    fraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = when {
        // Not-yet-elapsed padding day: a faint empty stub that isn't tappable.
        !bucket.enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        isSelected -> MaterialTheme.colorScheme.primary
        bucket.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .then(if (bucket.enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height((MAX_BAR_HEIGHT * fraction).coerceAtLeast(MaterialTheme.dimens.xs))
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(barColor),
        )
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
