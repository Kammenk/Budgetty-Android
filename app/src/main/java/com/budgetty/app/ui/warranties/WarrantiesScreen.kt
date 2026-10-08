package com.budgetty.app.ui.warranties

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.WarrantyEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun LocalDate.toMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Product emojis offered in the editor; 🛡️ is the fallback. */
private val WARRANTY_EMOJIS =
    listOf("🛡️", "💻", "📱", "📺", "🎧", "🍽️", "🧺", "❄️", "🔧", "🛋️", "⌚", "📷", "🚲", "🔌")

private val LENGTH_PRESETS = listOf(12, 24, 36)

@Composable
fun WarrantiesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: WarrantiesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WarrantiesContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WarrantiesContent(
    state: WarrantiesUiState,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onSave: (WarrantyEntity?, String, String, String, String, Long, Int, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<WarrantyEntity?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }
    fun openAdd() { editing = null; sheetOpen = true }
    fun openEdit(w: WarrantyEntity) { editing = w; sheetOpen = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.warranties_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (!state.isPremium && state.total > 0) {
                        Text(
                            text = stringResource(R.string.warranties_count, state.total, Warranties.FREE_LIMIT),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = MaterialTheme.dimens.lg),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        if (state.isLoaded && state.total == 0) {
            EmptyWarranties(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAdd = { openAdd() },
            )
        } else {
            val expiringTitle = stringResource(R.string.warranties_group_expiring)
            val activeTitle = stringResource(R.string.warranties_group_active)
            val expiredTitle = stringResource(R.string.warranties_group_expired)
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = MaterialTheme.dimens.screenPadding,
                        vertical = MaterialTheme.dimens.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
                ) {
                    group(expiringTitle, state.expiringSoon, ::openEdit)
                    group(activeTitle, state.active, ::openEdit)
                    group(expiredTitle, state.expired, ::openEdit)
                }
                AddOrUpgrade(
                    atCap = state.atCap,
                    usedCount = state.total,
                    onAdd = { openAdd() },
                    onUpgrade = onNavigateToPaywall,
                )
            }
        }
    }

    if (sheetOpen) {
        WarrantyEditSheet(
            original = editing,
            onSave = { name, emoji, store, purchaseMillis, months, note ->
                onSave(editing, name, emoji, store, editing?.category.orEmpty(), purchaseMillis, months, note)
                sheetOpen = false
            },
            onDelete = editing?.let { w -> { onDelete(w.id); sheetOpen = false } },
            onDismiss = { sheetOpen = false },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.group(
    title: String,
    items: List<WarrantyCardUi>,
    onClick: (WarrantyEntity) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "h_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = MaterialTheme.dimens.xs, top = MaterialTheme.dimens.sm),
        )
    }
    items(items, key = { it.entity.id }) { card -> WarrantyCard(card, onClick) }
}

@Composable
private fun WarrantyCard(card: WarrantyCardUi, onClick: (WarrantyEntity) -> Unit) {
    val w = card.entity
    val status = card.status
    val color = statusColor(status.state)
    val expired = status.state == WarrantyState.EXPIRED
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(w) },
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Text(w.emoji, style = MaterialTheme.typography.titleLarge) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = w.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (expired) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                val sub = listOf(w.store, status.expiryDate.minusMonths(w.durationMonths.toLong()).format(dateFmt))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                StatusChip(chipText(status), color)
            }
            WarrantyRing(
                fraction = status.elapsedFraction,
                color = if (expired) MaterialTheme.colorScheme.outline else color,
                size = 46.dp,
                stroke = 4.5.dp,
                centerText = if (expired) "—" else "${(status.elapsedFraction * 100).toInt()}%",
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = MaterialTheme.dimens.sm, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun WarrantyRing(fraction: Float, color: Color, size: Dp, stroke: Dp, centerText: String) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val arc = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(sw / 2, sw / 2)
            drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(
                color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arc,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text(centerText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddOrUpgrade(atCap: Boolean, usedCount: Int, onAdd: () -> Unit, onUpgrade: () -> Unit) {
    Column(modifier = Modifier.padding(MaterialTheme.dimens.screenPadding)) {
        if (atCap) {
            Text(
                text = stringResource(R.string.warranties_cap_reached, usedCount, Warranties.FREE_LIMIT),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = MaterialTheme.dimens.sm, start = MaterialTheme.dimens.xs),
            )
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.dimens.sm))
                Text(stringResource(R.string.warranties_unlock))
            }
        } else {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.dimens.sm))
                Text(stringResource(R.string.warranties_add))
            }
        }
    }
}

@Composable
private fun EmptyWarranties(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(MaterialTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🛡️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            stringResource(R.string.warranties_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            stringResource(R.string.warranties_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xl))
        Button(onClick = onAdd, modifier = Modifier.height(MaterialTheme.dimens.buttonHeight)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.warranties_add_first))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod") // One self-contained add/edit form sheet.
@Composable
private fun WarrantyEditSheet(
    original: WarrantyEntity?,
    onSave: (String, String, String, Long, Int, String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(original?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(original?.emoji ?: "🛡️") }
    var store by remember { mutableStateOf(original?.store.orEmpty()) }
    var note by remember { mutableStateOf(original?.coverageNote.orEmpty()) }
    var purchaseMillis by remember { mutableLongStateOf(original?.purchaseDate ?: LocalDate.now().toMillis()) }
    var months by remember { mutableIntStateOf(original?.durationMonths ?: 24) }
    var showDatePicker by remember { mutableStateOf(false) }

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Text(
                text = stringResource(if (original == null) R.string.warranties_add else R.string.warranties_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs)) {
                WARRANTY_EMOJIS.forEach { e ->
                    val selected = e == emoji
                    Box(
                        modifier = Modifier
                            .padding(bottom = MaterialTheme.dimens.xs)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.size(MaterialTheme.dimens.xs))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.warranties_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = store,
                onValueChange = { store = it },
                label = { Text(stringResource(R.string.warranties_field_store)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) {
                Text(stringResource(R.string.warranties_field_purchased, purchaseMillis.toLocalDate().format(dateFmt)))
            }
            Text(
                text = stringResource(R.string.warranties_field_length),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                LENGTH_PRESETS.forEach { preset ->
                    LengthChip(label = "${preset / 12} yr", selected = months == preset) { months = preset }
                    Spacer(Modifier.size(MaterialTheme.dimens.xs))
                }
            }
            OutlinedTextField(
                value = months.toString(),
                onValueChange = { months = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0 },
                label = { Text(stringResource(R.string.warranties_field_months)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            if (months > 0) {
                val expiry = purchaseMillis.toLocalDate().plusMonths(months.toLong()).format(dateFmt)
                Text(
                    text = stringResource(R.string.warranties_expires_on, expiry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.warranties_field_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(name, emoji, store, purchaseMillis, months, note) },
                enabled = name.isNotBlank() && months > 0,
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) { Text(stringResource(R.string.warranties_save)) }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = purchaseMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { purchaseMillis = it.toLocalDate().toMillis() }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun LengthChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusColor(state: WarrantyState): Color = when (state) {
    WarrantyState.ACTIVE -> budgetGoodColor()
    WarrantyState.EXPIRING_SOON -> budgetWarnColor()
    WarrantyState.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun chipText(status: WarrantyStatus): String = when (status.state) {
    WarrantyState.EXPIRED -> stringResource(R.string.warranties_chip_expired, status.expiryDate.format(dateFmt))
    WarrantyState.EXPIRING_SOON -> stringResource(R.string.warranties_chip_days, status.daysLeft.toInt())
    WarrantyState.ACTIVE ->
        stringResource(R.string.warranties_chip_months, (status.daysLeft / 30).coerceAtLeast(1).toInt())
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, heightDp = 900)
@Composable
private fun WarrantiesPreview() {
    fun w(id: Long, name: String, emoji: String, store: String, purchase: LocalDate, months: Int) =
        WarrantyEntity(
            id = id, name = name, emoji = emoji, store = store,
            purchaseDate = purchase.toMillis(), durationMonths = months,
        )
    val today = LocalDate.of(2026, 10, 6)
    fun card(e: WarrantyEntity) =
        WarrantyCardUi(e, Warranties.status(e.purchaseDate.toLocalDate(), e.durationMonths, today))
    BudgettyTheme {
        WarrantiesContent(
            state = WarrantiesUiState(
                isLoaded = true, isPremium = false,
                expiringSoon = listOf(card(w(1, "Bosch dishwasher", "🍽️", "Saturn", LocalDate.of(2024, 10, 29), 24))),
                active = listOf(
                    card(w(2, "MacBook Air 13″", "💻", "MediaMarkt", LocalDate.of(2026, 3, 6), 24)),
                    card(w(3, "Samsung TV 55″", "📺", "MediaMarkt", LocalDate.of(2025, 11, 20), 36)),
                ),
                expired = listOf(card(w(4, "Sony WH-1000XM5", "🎧", "Amazon", LocalDate.of(2024, 8, 14), 12))),
            ),
            onNavigateBack = {}, onNavigateToPaywall = {}, onSave = { _, _, _, _, _, _, _, _ -> }, onDelete = {},
        )
    }
}
