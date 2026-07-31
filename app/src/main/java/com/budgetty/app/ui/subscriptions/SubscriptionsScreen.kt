package com.budgetty.app.ui.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.savings.EmojiChip
import com.budgetty.app.ui.savings.PrimaryPill
import com.budgetty.app.ui.savings.SavingsFieldShape
import com.budgetty.app.ui.savings.SavingsSheetLabel
import com.budgetty.app.ui.savings.savingsFieldColors
import com.budgetty.app.ui.savings.toSavingsAmount
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.DetectedSubscription
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }
    var trackSheet by remember { mutableStateOf<DetectedSubscription?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val current = selected?.let { m -> (state.detected + state.ignored).firstOrNull { it.merchant == m } }
    // Selected merchant disappeared (ignored/restored elsewhere) → fall back to the list.
    if (selected != null && current == null) selected = null

    BackHandler(enabled = selected != null || trackSheet != null) {
        when {
            trackSheet != null -> trackSheet = null
            else -> selected = null
        }
    }

    val addedMsg = stringResource(R.string.sub_added_to_bills)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (current != null) {
                            EmojiChip(current.emoji, size = 26.dp)
                            Spacer(Modifier.width(MaterialTheme.dimens.sm))
                            Text(current.merchant, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text(stringResource(R.string.sub_title), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (current != null) selected = null else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.screenPadding),
        ) {
            when {
                current != null -> DetailContent(
                    sub = current,
                    onTrackAsBill = { trackSheet = current },
                    onIgnore = { viewModel.ignore(current.merchant); selected = null },
                )
                state.detected.isEmpty() && state.ignored.isEmpty() -> EmptyContent(state)
                else -> ListContent(state, onRowClick = { selected = it }, onRestore = viewModel::restore)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
        }
    }

    trackSheet?.let { sub ->
        TrackAsBillSheet(
            sub = sub,
            onConfirm = { amount ->
                viewModel.trackAsBill(sub, amount, sub.dueDay)
                trackSheet = null
                selected = null
                scope.launch { snackbar.showSnackbar(addedMsg) }
            },
            onDismiss = { trackSheet = null },
        )
    }
}

@Composable
internal fun ListContent(state: SubscriptionsUiState, onRowClick: (String) -> Unit, onRestore: (String) -> Unit) {
    // Total header
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(MaterialTheme.dimens.lg)) {
            Text(stringResource(R.string.sub_detected), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(state.monthlyTotal.formatMoney(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sub_per_month_suffix), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(
                text = pluralStringResource(R.plurals.sub_count_year, state.detected.size, state.detected.size, state.yearlyTotal.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(stringResource(R.string.sub_list_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = MaterialTheme.dimens.sm))
        }
    }
    Spacer(Modifier.height(MaterialTheme.dimens.md))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            state.detected.forEachIndexed { i, sub ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PreviewRow(
                    sub = sub,
                    modifier = Modifier.clickable { onRowClick(sub.merchant) }.padding(MaterialTheme.dimens.md),
                )
            }
        }
    }

    if (state.ignored.isNotEmpty()) {
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sub_ignored), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.sub_ignored_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(MaterialTheme.dimens.md)) {
                state.ignored.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { PreviewRow(sub) }
                    }
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sub_ignored_still), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRestore(sub.merchant) }) { Text(stringResource(R.string.sub_restore), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Text(stringResource(R.string.sub_footer_ondevice), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun DetailContent(sub: DetectedSubscription, onTrackAsBill: () -> Unit, onIgnore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(MaterialTheme.dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiChip(sub.emoji, size = 44.dp)
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Column(Modifier.weight(1f)) {
                    Text(sub.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cadenceLabel(sub.cadence), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(sub.amount.formatMoney(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.sub_stat_last), sub.lastChargeMillis.formatDayMonth(), Modifier.weight(1f))
                VDivider()
                Stat(stringResource(R.string.sub_stat_next), sub.nextExpectedMillis.formatDayMonth(), Modifier.weight(1f))
                VDivider()
                Stat(stringResource(R.string.sub_stat_charges), sub.chargeCount.toString(), Modifier.weight(1f))
            }
        }
    }

    if (sub.priceHike != null) {
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        val hike = sub.priceHike
        val color = com.budgetty.app.ui.theme.budgetWarnColor()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(MaterialTheme.dimens.md)) {
                Text(
                    text = "↑ " + stringResource(R.string.sub_hike_title, (hike.newAmount - hike.oldAmount).formatMoney(), hike.sinceMillis.formatDayMonth()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                val annual = (hike.newAmount - hike.oldAmount).multiply(java.math.BigDecimal(12))
                Text(
                    text = stringResource(R.string.sub_hike_body, hike.oldAmount.formatMoney(), hike.newAmount.formatMoney(), annual.formatMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
                )
            }
        }
    }

    // Charge history
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            sub.charges.take(6).forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val isHikePoint = sub.priceHike != null && c.dateMillis == sub.priceHike.sinceMillis
                Row(Modifier.fillMaxWidth().padding(MaterialTheme.dimens.md), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.dateMillis.formatDayMonth(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (isHikePoint) {
                        Text(stringResource(R.string.sub_price_up_tag), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = com.budgetty.app.ui.theme.budgetWarnColor(), modifier = Modifier.padding(end = MaterialTheme.dimens.sm))
                    }
                    Text(c.amount.formatMoney(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(Modifier.height(MaterialTheme.dimens.md))
    PrimaryPill(text = stringResource(R.string.sub_track_as_bill), icon = Icons.Filled.Add, onClick = onTrackAsBill, fillWidth = true)
    Text(stringResource(R.string.sub_track_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.sm), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(Modifier.height(MaterialTheme.dimens.sm))
    FilledTonalButton(onClick = onIgnore, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(MaterialTheme.dimens.xs))
        Text(stringResource(R.string.sub_ignore_merchant), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyContent(state: SubscriptionsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            EmojiChip("🔁", size = 46.dp)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.sub_empty_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.sub_empty_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = MaterialTheme.dimens.xs))
        }
    }
    state.nearestMiss?.let { miss ->
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(MaterialTheme.dimens.md)) {
                Row {
                    Text(stringResource(R.string.sub_receipts_so_far), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(state.receiptCount.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.sub_nearest, miss.count, miss.merchant), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = MaterialTheme.dimens.xs))
            }
        }
    }
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Text(stringResource(R.string.sub_footer_ondevice), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackAsBillSheet(sub: DetectedSubscription, onConfirm: (java.math.BigDecimal) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(sub.amount.toPlainString()) }
    val parsed = amount.toSavingsAmount()
    AdaptiveSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.dimens.xl)) {
            Text(stringResource(R.string.sub_track_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.sub_track_sheet_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = MaterialTheme.dimens.xs))
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(SavingsFieldShape).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(MaterialTheme.dimens.sm)) {
                EmojiChip(sub.emoji, size = 32.dp)
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(sub.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            SavingsSheetLabel(stringResource(R.string.savings_field_amount))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' || it == ',' } },
                suffix = { Text(AppFormats.currencySymbol) },
                singleLine = true,
                shape = SavingsFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.sub_track_charge_day, cadenceLabel(sub.cadence), sub.nextExpectedMillis.formatDayMonth()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.sub_track_sheet_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = MaterialTheme.dimens.sm))
            Row(Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.md), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                PrimaryPill(text = stringResource(R.string.sub_add_bill), onClick = { parsed?.let(onConfirm) })
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
