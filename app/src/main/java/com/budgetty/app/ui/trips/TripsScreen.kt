package com.budgetty.app.ui.trips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.CustomDateRangeSheet
import com.budgetty.app.ui.components.formatDateRange
import com.budgetty.app.ui.savings.toSavingsAmount
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.res.stringResource

/**
 * Account → Trips (Travel mode). Lists the active trip's live summary (total, pace bar, top
 * categories) and past trips, and starts a new one. A trip is a tag plus metadata, so starting one
 * just makes the add screen auto-apply its tag; everything else (History filter, Insights by tag) is
 * the tag feature working unchanged.
 */
@Composable
fun TripsScreen(
    onNavigateBack: () -> Unit,
    onOpenHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timestamps by viewModel.transactionTimestamps.collectAsStateWithLifecycle()
    TripsContent(
        state = uiState,
        transactionTimestamps = timestamps,
        onStart = viewModel::start,
        onEnd = viewModel::endActive,
        onDelete = viewModel::delete,
        onOpenHistory = onOpenHistory,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal fun TripsContent(
    state: TripsUiState,
    transactionTimestamps: List<Long>,
    onStart: (name: String, start: Long?, end: Long?, budget: BigDecimal?, backfill: Boolean) -> Unit,
    onEnd: () -> Unit,
    onDelete: (Long) -> Unit,
    onOpenHistory: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStart by remember { mutableStateOf(false) }
    var endConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trips_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { showStart = true }) {
                        Text(stringResource(R.string.trips_start))
                    }
                },
                // The nav Scaffold already applies the status-bar inset (edge-to-edge convention).
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(MaterialTheme.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            val active = state.active
            if (active != null) {
                item {
                    ActiveTripSummary(
                        card = active,
                        onOpenHistory = { onOpenHistory(active.trip.tag) },
                        onEnd = { endConfirm = true },
                        modifier = Modifier.widthIn(max = 520.dp),
                    )
                }
            }
            if (state.past.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.trips_past),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
                            .padding(top = MaterialTheme.dimens.sm),
                    )
                }
                items(state.past, key = { it.trip.id }) { card ->
                    PastTripCard(card, onDelete, modifier = Modifier.widthIn(max = 520.dp))
                }
                item {
                    Text(
                        text = stringResource(R.string.trips_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 520.dp).padding(MaterialTheme.dimens.sm),
                    )
                }
            }
            if (state.isLoaded && active == null && state.past.isEmpty()) {
                item { EmptyTrips(onStart = { showStart = true }, modifier = Modifier.widthIn(max = 520.dp)) }
            }
        }
    }

    if (showStart) {
        StartTripSheet(
            transactionTimestamps = transactionTimestamps,
            onStart = { name, start, end, budget, backfill ->
                onStart(name, start, end, budget, backfill)
                showStart = false
            },
            onDismiss = { showStart = false },
        )
    }
    val active = state.active
    if (endConfirm && active != null) {
        EndTripDialog(
            name = active.trip.name,
            onConfirm = { onEnd(); endConfirm = false },
            onDismiss = { endConfirm = false },
        )
    }
}

@Composable
private fun EmptyTrips(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✈️", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            stringResource(R.string.trips_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            stringResource(R.string.trips_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        OutlinedButton(onClick = onStart) { Text(stringResource(R.string.trips_start_cta)) }
    }
}

// ── Active summary (frame 3d) ────────────────────────────────────────────────────────────────────

@Suppress("LongMethod")
@Composable
private fun ActiveTripSummary(
    card: TripCard,
    onOpenHistory: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip = card.trip
    val stats = card.stats
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
        Text(trip.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Hero: dates + Active badge, big total, per-day, and the day strip when the trip has a span.
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✈️ " + dateRangeLabel(trip),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            stringResource(R.string.trips_active_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    card.spent.formatMoney(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        R.string.trips_per_day_expenses,
                        stats.perDay.formatMoney(),
                        card.expenseCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (stats.dayStrip.isNotEmpty() && stats.totalDays != null) {
                    Spacer(Modifier.height(MaterialTheme.dimens.md))
                    DayStrip(stats.dayStrip)
                    Spacer(Modifier.height(MaterialTheme.dimens.xs))
                    Row {
                        Text(
                            stringResource(R.string.trips_day_of, stats.daysElapsed, stats.totalDays),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        stats.daysLeft?.let {
                            Text(
                                stringResource(R.string.trips_days_left, it),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        stats.pace?.let { TripBudgetCard(spent = card.spent, budget = trip.budgetAmount, pace = it) }

        if (card.topCategories.isNotEmpty()) TopCategoriesCard(card)

        HistoryLinkRow(tag = trip.tag, count = card.expenseCount, onClick = onOpenHistory)

        OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.trips_end))
        }
    }
}

@Composable
private fun DayStrip(days: List<Boolean>) {
    val filled = MaterialTheme.colorScheme.onPrimaryContainer
    val faint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEach { on ->
            Box(
                Modifier.weight(1f).height(6.dp)
                    .background(if (on) filled else faint, RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun TripBudgetCard(spent: BigDecimal, budget: BigDecimal?, pace: TripPace) {
    val color = paceColor(pace.state)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg)) {
            Row {
                Text(
                    stringResource(R.string.trips_budget),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(
                        R.string.trips_budget_of,
                        spent.formatMoney(),
                        (budget ?: BigDecimal.ZERO).formatMoney(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            PaceBar(fill = pace.fill, tick = pace.tickFraction, fillColor = color)
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Row {
                Text(
                    paceLabel(pace.state),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (pace.remaining.signum() < 0) {
                        stringResource(R.string.trips_over_by, pace.remaining.abs().formatMoney())
                    } else {
                        stringResource(R.string.trips_daily_hint, pace.suggestedDaily.formatMoney())
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PaceBar(fill: Float, tick: Float, fillColor: Color) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val barH = 8.dp.toPx()
        val top = (size.height - barH) / 2f
        val radius = CornerRadius(barH / 2f, barH / 2f)
        drawRoundRect(track, topLeft = Offset(0f, top), size = Size(size.width, barH), cornerRadius = radius)
        val w = (size.width * fill).coerceIn(0f, size.width)
        if (w > 0f) {
            drawRoundRect(fillColor, topLeft = Offset(0f, top), size = Size(w, barH), cornerRadius = radius)
        }
        val x = (size.width * tick).coerceIn(0f, size.width)
        drawLine(tickColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun TopCategoriesCard(card: TripCard) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
        Text(
            stringResource(R.string.trips_top_categories),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(MaterialTheme.dimens.md)) {
                card.topCategories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.xs),
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp)
                                .background(Color(cat.color).copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(cat.emoji) }
                        Spacer(Modifier.width(MaterialTheme.dimens.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier.fillMaxWidth().height(4.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(cat.fraction).height(4.dp)
                                        .background(Color(cat.color), RoundedCornerShape(50)),
                                )
                            }
                        }
                        Spacer(Modifier.width(MaterialTheme.dimens.md))
                        Text(
                            cat.amount.formatMoney(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLinkRow(tag: String, count: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.md),
        ) {
            TripPill(tag)
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Text(
                stringResource(R.string.trips_expenses_in_history, count),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Past trips (frame 3g) ──────────────────────────────────────────────────────────────────────

@Composable
private fun PastTripCard(card: TripCard, onDelete: (Long) -> Unit, modifier: Modifier = Modifier) {
    val trip = card.trip
    var menu by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(MaterialTheme.dimens.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    card.spent.formatMoney(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.trips_delete)) },
                            onClick = { menu = false; onDelete(trip.id) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    dateRangeLabel(trip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.trips_per_day_short, card.stats.perDay.formatMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTagPill(trip.tag)
                Spacer(Modifier.weight(1f))
                card.stats.pace?.let { BudgetResultChip(it) }
            }
        }
    }
}

@Composable
private fun BudgetResultChip(pace: TripPace) {
    val color = paceColor(pace.state)
    val label = if (pace.remaining.signum() >= 0) {
        stringResource(R.string.trips_under_by, pace.remaining.formatMoney())
    } else {
        stringResource(R.string.trips_over_by, pace.remaining.abs().formatMoney())
    }
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// ── Start sheet (frame 3b) ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun StartTripSheet(
    transactionTimestamps: List<Long>,
    onStart: (name: String, start: Long?, end: Long?, budget: BigDecimal?, backfill: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var datesOn by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    var budgetOn by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var backfill by remember { mutableStateOf(true) }
    var showRange by remember { mutableStateOf(false) }

    val startMillis = start?.takeIf { datesOn }?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val backfillCount = startMillis?.let { s -> transactionTimestamps.count { it >= s } } ?: 0
    val tagPreview = "#" + TagEntity.normalize(name).ifBlank { "trip" } +
        "-" + (start?.year ?: LocalDate.now().year)

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.dimens.lg)
                .padding(bottom = MaterialTheme.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            Text(
                stringResource(R.string.trips_new_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.trips_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            ToggleRow(
                title = stringResource(R.string.trips_dates_label),
                subtitle = stringResource(R.string.trips_dates_sub),
                checked = datesOn,
                onChecked = { datesOn = it },
            )
            if (datesOn) {
                OutlinedButton(onClick = { showRange = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (start != null && end != null) formatDateRange(start!!, end!!)
                        else stringResource(R.string.trips_pick_dates),
                    )
                }
            }

            ToggleRow(
                title = stringResource(R.string.trips_budget_label),
                subtitle = stringResource(R.string.trips_budget_sub),
                checked = budgetOn,
                onChecked = { budgetOn = it },
            )
            if (budgetOn) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input -> budgetText = input.filter { it.isDigit() || it == '.' || it == ',' } },
                    suffix = { Text(AppFormats.currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(R.string.trips_autotag_explainer, tagPreview),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(MaterialTheme.dimens.md),
                )
            }

            if (datesOn && backfillCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { backfill = !backfill },
                ) {
                    Checkbox(checked = backfill, onCheckedChange = { backfill = it })
                    Text(
                        stringResource(R.string.trips_backfill, backfillCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Button(
                onClick = {
                    val endMillis = end?.takeIf { datesOn }
                        ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    onStart(
                        name,
                        startMillis,
                        endMillis,
                        budgetText.takeIf { budgetOn }?.toSavingsAmount(),
                        backfill,
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.trips_start_trip)) }
        }
    }

    if (showRange) {
        CustomDateRangeSheet(
            initialStart = start,
            initialEnd = end,
            onConfirm = { s, e -> start = s; end = e; showRange = false },
            onDismiss = { showRange = false },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun EndTripDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trips_end_title, name)) },
        text = { Text(stringResource(R.string.trips_end_body)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.trips_end)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.trips_end_keep)) } },
    )
}

// ── Pills, colors, dates ─────────────────────────────────────────────────────────────────────────

/** The active trip's tag — a filled primary-container "✈️ #tag" pill, so an auto-applied trip tag
 *  reads as special (plain tags stay outlined and emoji-free per the tags design). */
@Composable
private fun TripPill(tag: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "✈️ #$tag",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A past trip's tag — the regular outlined style, matching plain tags once a trip ends. */
@Composable
private fun OutlinedTagPill(tag: String) {
    Box(
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            "#$tag",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun paceColor(state: TripPaceState): Color {
    val dark = isSystemInDarkTheme()
    return when (state) {
        TripPaceState.UNDER_PACE -> if (dark) Color(0xFF4FA85A) else Color(0xFF1C7C54)
        TripPaceState.OVER_PACE -> if (dark) Color(0xFFF2B04C) else Color(0xFFB26A00)
        TripPaceState.OVER_BUDGET -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun paceLabel(state: TripPaceState): String = stringResource(
    when (state) {
        TripPaceState.UNDER_PACE -> R.string.trips_pace_under
        TripPaceState.OVER_PACE -> R.string.trips_pace_over
        TripPaceState.OVER_BUDGET -> R.string.trips_pace_over_budget
    },
)

@Composable
private fun dateRangeLabel(trip: TripEntity): String {
    val zone = ZoneId.systemDefault()
    fun day(m: Long): LocalDate = Instant.ofEpochMilli(m).atZone(zone).toLocalDate()
    val s = trip.startDate
    val e = trip.endDate
    return if (s != null && e != null) formatDateRange(day(s), day(e)) else stringResource(R.string.trips_open_ended)
}

// ── Preview ────────────────────────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun TripsPreview() {
    val zone = ZoneId.of("UTC")
    fun millis(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()
    val today = LocalDate.of(2026, 10, 14)
    fun card(
        id: Long, name: String, tag: String, spent: String, budget: String?,
        s: Long?, e: Long?, active: Boolean, ended: Long?,
        cats: List<TripCategoryStat> = emptyList(), count: Int = 0,
    ): TripCard {
        val trip = TripEntity(
            id = id, name = name, tag = tag, startDate = s, endDate = e,
            budgetAmount = budget?.let(::BigDecimal), active = active, createdAt = s ?: 0, endedAt = ended,
        )
        return TripCard(trip, BigDecimal(spent), count, TripStats.compute(trip, BigDecimal(spent), today, zone), cats)
    }
    val lisbonCats = listOf(
        TripCategoryStat("Lodging", "🏨", 0xFF5B6CD9.toInt(), BigDecimal("312"), 1f),
        TripCategoryStat("Restaurants", "🍽️", 0xFFD9763C.toInt(), BigDecimal("118"), 0.38f),
        TripCategoryStat("Transport", "🚕", 0xFFE0A030.toInt(), BigDecimal("46"), 0.15f),
    )
    BudgettyTheme {
        TripsContent(
            state = TripsUiState(
                isLoaded = true,
                active = card(
                    1, "Lisbon", "lisbon-2026", "512", "900",
                    millis(2026, 10, 12), millis(2026, 10, 20), true, null, lisbonCats, 14,
                ),
                past = listOf(
                    card(
                        2, "Porto weekend", "porto-weekend-2026", "296", null,
                        millis(2026, 6, 19), millis(2026, 6, 21), false, millis(2026, 6, 21), count = 8,
                    ),
                    card(
                        3, "Kraków", "krakow-2026", "420", "400",
                        millis(2026, 3, 6), millis(2026, 3, 10), false, millis(2026, 3, 10), count = 11,
                    ),
                ),
            ),
            transactionTimestamps = emptyList(),
            onStart = { _, _, _, _, _ -> }, onEnd = {}, onDelete = {}, onOpenHistory = {}, onNavigateBack = {},
        )
    }
}
