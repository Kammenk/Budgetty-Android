package com.budgetty.app.ui.budgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.data.repository.BudgetEnvelopeRepository
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.CustomDateRangeSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.components.formatDateRange
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private fun LocalDate.toMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private val ENVELOPE_EMOJIS =
    listOf("🧾", "✈️", "🛒", "🎁", "🏠", "🎄", "🍽️", "🚗", "💊", "🎓", "🔧", "🐾", "💻", "🎉")

@Composable
fun BudgetEnvelopesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: BudgetEnvelopesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categoryOptions.collectAsStateWithLifecycle()
    EnvelopesContent(
        state = state,
        categories = categories,
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EnvelopesContent(
    state: EnvelopesUiState,
    categories: List<CategoryOption>,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onSave: (BudgetEnvelopeEntity?, String, String, String, Long, Long, List<String>) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<BudgetEnvelopeEntity?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.envelopes_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            if (state.isLoaded && state.envelopes.isEmpty()) {
                EnvelopesIntro()
            }
            state.envelopes.forEach { card ->
                EnvelopeCard(card) { editing = card.entity; sheetOpen = true }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            if (state.atCap) {
                Text(
                    stringResource(R.string.envelopes_cap_reached, BudgetEnvelopeRepository.FREE_LIMIT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MaterialTheme.dimens.xs),
                )
                Button(
                    onClick = onNavigateToPaywall,
                    modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(MaterialTheme.dimens.sm))
                    Text(stringResource(R.string.envelopes_unlock))
                }
            } else {
                Button(
                    onClick = { editing = null; sheetOpen = true },
                    modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(MaterialTheme.dimens.sm))
                    Text(stringResource(R.string.envelopes_new))
                }
            }
        }
    }

    if (sheetOpen) {
        EnvelopeEditSheet(
            original = editing,
            categories = categories,
            onSave = { name, emoji, amount, start, end, cats ->
                onSave(editing, name, emoji, amount, start, end, cats)
                sheetOpen = false
            },
            onDelete = editing?.let { e -> { onDelete(e.id); sheetOpen = false } },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun EnvelopesIntro() {
    Text(
        stringResource(R.string.envelopes_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = MaterialTheme.dimens.md),
    )
}

@Composable
private fun EnvelopeCard(card: EnvelopeCardUi, onClick: () -> Unit) {
    val e = card.entity
    val color = paceColor(card.pace.state)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(e.emoji, style = MaterialTheme.typography.titleMedium) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        e.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val categoryCount = e.categoryList().size
                    val scope = if (e.isAllSpending) stringResource(R.string.envelopes_scope_all)
                    else pluralStringResource(R.plurals.envelopes_scope_categories, categoryCount, categoryCount)
                    Text(
                        "${formatDateRange(card.startDate, card.endDate)} · $scope",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${card.spent.formatMoney()} / ${e.limitAmount.formatMoney()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            PaceBar(card.pace, color)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(paceSubtitle(card.pace), style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun PaceBar(pace: PaceResult, color: Color) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.onSurface
    val halo = MaterialTheme.colorScheme.surfaceContainer
    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
        val h = size.height
        val w = size.width
        val r = CornerRadius(h / 2, h / 2)
        drawRoundRect(track, size = Size(w, h), cornerRadius = r)
        val fillW = if (pace.fill <= 0f) 0f else (w * pace.fill).coerceAtLeast(h)
        if (fillW > 0f) drawRoundRect(color, size = Size(fillW, h), cornerRadius = r)
        val tx = (w * pace.todayFraction).coerceIn(1.5f, w - 1.5f)
        drawLine(halo, Offset(tx, -3f), Offset(tx, h + 3f), strokeWidth = 7f)
        drawLine(tick, Offset(tx, -3f), Offset(tx, h + 3f), strokeWidth = 3f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod") // One self-contained create/edit form sheet.
@Composable
private fun EnvelopeEditSheet(
    original: BudgetEnvelopeEntity?,
    categories: List<CategoryOption>,
    onSave: (String, String, String, Long, Long, List<String>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(original?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(original?.emoji ?: "🧾") }
    var amount by remember { mutableStateOf(original?.limitAmount?.toPlainString().orEmpty()) }
    val month = YearMonth.now()
    var start by remember { mutableStateOf(original?.startDate?.toLocalDate() ?: month.atDay(1)) }
    var end by remember { mutableStateOf(original?.endDate?.toLocalDate() ?: month.atEndOfMonth()) }
    var scoped by remember { mutableStateOf(original?.isAllSpending == false) }
    var picked by remember { mutableStateOf(original?.categoryList()?.toSet().orEmpty()) }
    var showRange by remember { mutableStateOf(false) }

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Text(
                stringResource(if (original == null) R.string.envelopes_new else R.string.envelopes_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs)) {
                ENVELOPE_EMOJIS.forEach { candidate ->
                    EmojiChip(candidate, candidate == emoji) { emoji = candidate }
                    Spacer(Modifier.size(MaterialTheme.dimens.xs))
                }
            }
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.envelopes_field_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text(stringResource(R.string.envelopes_field_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { showRange = true },
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) { Text(formatDateRange(start, end)) }
            TextButton(onClick = { start = month.atDay(1); end = month.atEndOfMonth() }) {
                Text(stringResource(R.string.envelopes_this_month))
            }
            Text(
                stringResource(R.string.envelopes_field_scope),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.envelopes_scope_all),
                    stringResource(R.string.envelopes_scope_some),
                ),
                selectedIndex = if (scoped) 1 else 0,
                onSelect = { scoped = it == 1 },
            )
            if (scoped) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs)) {
                    categories.forEach { option ->
                        val on = option.name in picked
                        CategoryChip(option, on) {
                            picked = if (on) picked - option.name else picked + option.name
                        }
                        Spacer(Modifier.size(MaterialTheme.dimens.xs))
                    }
                }
            }
            Button(
                onClick = {
                    onSave(
                        name, emoji, amount, start.toMillis(), end.toMillis(),
                        if (scoped) picked.toList() else emptyList(),
                    )
                },
                enabled = name.isNotBlank() && amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) { Text(stringResource(R.string.envelopes_save)) }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
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
private fun EmojiChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun CategoryChip(option: CategoryOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(bottom = MaterialTheme.dimens.xs)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
    ) {
        Text(
            "${if (selected) "✓ " else ""}${option.emoji} ${option.name}",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun paceColor(state: PaceState): Color = when (state) {
    PaceState.ON_PACE -> budgetGoodColor()
    PaceState.AHEAD -> budgetWarnColor()
    PaceState.OVER_PACE, PaceState.OVER_BUDGET -> budgetBadColor()
}

@Composable
private fun paceSubtitle(pace: PaceResult): String = when (pace.state) {
    PaceState.OVER_BUDGET ->
        stringResource(R.string.envelopes_sub_over_budget, pace.remaining.negate().formatMoney(), pace.daysLeft.toInt())
    PaceState.OVER_PACE ->
        stringResource(R.string.envelopes_sub_over_pace, pace.remaining.formatMoney(), pace.daysLeft.toInt())
    PaceState.AHEAD ->
        stringResource(R.string.envelopes_sub_ahead, pace.dailyAllowance.formatMoney(), pace.daysLeft.toInt())
    PaceState.ON_PACE ->
        stringResource(R.string.envelopes_sub_on_pace, pace.dailyAllowance.formatMoney(), pace.daysLeft.toInt())
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, heightDp = 900)
@Composable
private fun EnvelopesPreview() {
    val today = LocalDate.of(2026, 10, 22)
    fun card(
        emoji: String, name: String, spent: String, limit: String,
        start: LocalDate, end: LocalDate, cats: String,
    ) =
        EnvelopeCardUi(
            BudgetEnvelopeEntity(
                name = name, emoji = emoji, limitAmount = java.math.BigDecimal(limit),
                startDate = start.toMillis(), endDate = end.toMillis(), categories = cats,
            ),
            java.math.BigDecimal(spent), start, end,
            BudgetPace.compute(java.math.BigDecimal(spent), java.math.BigDecimal(limit), start, end, today),
        )
    BudgettyTheme {
        EnvelopesContent(
            state = EnvelopesUiState(
                isLoaded = true, isPremium = false,
                envelopes = listOf(
                    card("🧾", "Everyday", "842", "1200", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ""),
                    card("✈️", "Lisbon trip", "512", "600", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 27), ""),
                ),
            ),
            categories = emptyList(),
            onNavigateBack = {}, onNavigateToPaywall = {}, onSave = { _, _, _, _, _, _, _ -> }, onDelete = {},
        )
    }
}
