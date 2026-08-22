package com.budgetty.app.ui.buyinglimits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.ui.savings.PrimaryPill
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.isDarkTheme
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.formatDayMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Account → Buying limits: keyword-based item purchase caps. Each card shows a limit's keywords,
 * timeframe, a pip progress row and a traffic-light status; tapping one edits it. A free user gets one
 * limit, then the Add row locks and routes to the paywall. Same chrome as Category rules (TopAppBar +
 * back, bottom nav hidden).
 */
@Composable
fun BuyingLimitsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuyingLimitsViewModel = org.koin.androidx.compose.koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BuyingLimitsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onUpgrade = onNavigateToPaywall,
        onSaveLimit = viewModel::saveLimit,
        onDeleteLimit = viewModel::deleteLimit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyingLimitsContent(
    state: BuyingLimitsUiState,
    onNavigateBack: () -> Unit,
    onUpgrade: () -> Unit,
    onSaveLimit: (Long?, String, String, List<String>, BuyingLimitTimeframe, Int) -> Unit,
    onDeleteLimit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // null = editor closed; a wrapped value = the limit under edit (its .value is null for a new one).
    var editorFor by remember { mutableStateOf<EditorTarget?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.buying_limits_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (state.isLoaded && state.limits.isNotEmpty()) {
                        CountPill(count = state.limits.size, atCap = state.atCap)
                        Spacer(Modifier.width(MaterialTheme.dimens.md))
                    }
                },
                // The nav Scaffold already applies the status-bar inset (matches CategoryRulesScreen).
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.buying_limits_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.dimens.xl)
                        .padding(top = MaterialTheme.dimens.xs, bottom = MaterialTheme.dimens.md),
                )
                when {
                    // Brief blank on cold start rather than flashing the empty state (see isLoaded).
                    !state.isLoaded -> Unit
                    state.limits.isEmpty() -> BuyingLimitsEmpty(
                        onAdd = { editorFor = EditorTarget(null) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    else -> BuyingLimitsList(
                        state = state,
                        onEdit = { editorFor = EditorTarget(it) },
                        onAdd = { editorFor = EditorTarget(null) },
                        onUpgrade = onUpgrade,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
    }

    editorFor?.let { target ->
        BuyingLimitEditorSheet(
            initial = target.value,
            items = state.items,
            monthStartDay = state.monthStartDay,
            onSave = { emoji, label, keywords, timeframe, count ->
                onSaveLimit(target.value?.id, emoji, label, keywords, timeframe, count)
                editorFor = null
            },
            onDelete = target.value?.let { limit -> { onDeleteLimit(limit.id); editorFor = null } },
            onDismiss = { editorFor = null },
        )
    }
}

/** Wraps the editor's subject so a null limit (new) is distinguishable from "editor closed". */
private data class EditorTarget(val value: BuyingLimitEntity?)

@Composable
private fun CountPill(count: Int, atCap: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
    ) {
        Text(
            text = if (atCap) {
                stringResource(R.string.buying_limits_cap_pill, count, BuyingLimitsRepository.FREE_LIMIT)
            } else {
                count.toString()
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun BuyingLimitsList(
    state: BuyingLimitsUiState,
    onEdit: (BuyingLimitEntity) -> Unit,
    onAdd: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = MaterialTheme.dimens.lg,
            end = MaterialTheme.dimens.lg,
            bottom = MaterialTheme.dimens.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        items(state.limits, key = { it.limit.id }) { card ->
            BuyingLimitCard(card = card, monthStartDay = state.monthStartDay, onClick = { onEdit(card.limit) })
        }
        item {
            if (state.atCap) {
                LockedAddSection(usedCount = state.limits.size, onUpgrade = onUpgrade)
            } else {
                PrimaryPill(
                    text = stringResource(R.string.buying_limits_add),
                    icon = Icons.Filled.Add,
                    onClick = onAdd,
                    fillWidth = true,
                    modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
                )
            }
        }
    }
}

@Composable
private fun BuyingLimitCard(card: BuyingLimitCardUi, monthStartDay: Int, onClick: () -> Unit) {
    val band = bandColor(card.status)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                LimitEmojiChip(card.limit.emoji)
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Text(
                    text = card.limit.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                StatusChip(status = card.status, band = band)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            KeywordChipsRow(card.limit.keywordList)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Pips(bought = card.bought, limit = card.limit.count, band = band)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            MetaLine(card = card, band = band, monthStartDay = monthStartDay)
        }
    }
}

/** 32dp emoji tile in the category-chip colour; a tag glyph when the limit has no emoji. */
@Composable
private fun LimitEmojiChip(emoji: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotEmpty()) {
            Text(emoji, style = MaterialTheme.typography.bodyLarge)
        } else {
            Icon(
                Icons.Filled.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
    }
}

@Composable
private fun StatusChip(status: BuyingLimitStatus, band: Color) {
    val dark = isDarkTheme()
    val bg = band.copy(alpha = if (dark) 0.24f else 0.15f)
    val fg = if (dark) lerp(band, Color.White, 0.35f) else lerp(band, Color.Black, 0.42f)
    val (icon, labelRes) = when (status) {
        BuyingLimitStatus.ON_TRACK -> Icons.Filled.Check to R.string.buying_limits_status_on_track
        BuyingLimitStatus.AT_LIMIT -> Icons.Filled.PriorityHigh to R.string.buying_limits_status_at_limit
        BuyingLimitStatus.OVER -> Icons.Filled.PriorityHigh to R.string.buying_limits_status_over
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = MaterialTheme.dimens.sm, vertical = MaterialTheme.dimens.xs),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordChipsRow(keywords: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keywords.forEach { kw ->
            Text(
                text = kw,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
            )
        }
    }
}

/**
 * The pip progress row: one pip per allowed unit, filled up to [bought] in the band colour, the rest
 * muted. When over the cap, the overflow pips are set apart after a small gap. Total = max(limit, bought).
 */
@Composable
private fun Pips(bought: Int, limit: Int, band: Color) {
    val total = maxOf(limit, bought).coerceAtLeast(1)
    val muted = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until total) {
            // Set the overflow pips apart from the allowed ones when over the cap.
            if (i == limit && total > limit) Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i < bought) band else muted),
            )
        }
    }
}

@Composable
private fun MetaLine(card: BuyingLimitCardUi, band: Color, monthStartDay: Int) {
    val timeframeLabel = stringResource(
        if (card.limit.timeframe == BuyingLimitTimeframe.WEEKLY) {
            R.string.buying_limits_weekly
        } else {
            R.string.buying_limits_monthly
        },
    )
    val progress = if (card.status == BuyingLimitStatus.OVER) {
        stringResource(R.string.buying_limits_bought_over, card.bought, card.limit.count)
    } else {
        stringResource(R.string.buying_limits_bought_of, card.bought, card.limit.count)
    }
    val reset = stringResource(R.string.buying_limits_resets, resetLabel(card.limit.timeframe, monthStartDay))
    Text(
        text = buildAnnotatedString {
            append("$timeframeLabel · ")
            withStyle(SpanStyle(color = band, fontWeight = FontWeight.Bold)) { append(progress) }
            append(" · $reset")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "Mon" for weekly, "1 Sep" for monthly — the date the current window rolls over. */
@Composable
private fun resetLabel(timeframe: BuyingLimitTimeframe, monthStartDay: Int): String {
    val next = remember(timeframe, monthStartDay) {
        BuyingLimitCounter.nextReset(timeframe, LocalDate.now(), monthStartDay)
    }
    return if (timeframe == BuyingLimitTimeframe.WEEKLY) {
        next.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        next.formatDayMonth()
    }
}

@Composable
private fun BuyingLimitsEmpty(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.dimens.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Card(
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Sell,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = stringResource(R.string.buying_limits_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.buying_limits_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                PrimaryPill(
                    text = stringResource(R.string.buying_limits_add),
                    icon = Icons.Filled.Add,
                    onClick = onAdd,
                    modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(R.string.buying_limits_free_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ExplainerBlock()
    }
}

@Composable
private fun ExplainerBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(MaterialTheme.dimens.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.buying_limits_explainer))
                append("  ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(stringResource(R.string.buying_limits_explainer_example))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The free-cap locked state: a dimmed padlock Add row + the "unlock" upsell card + a used footnote. */
@Composable
private fun LockedAddSection(usedCount: Int, onUpgrade: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
        Card(
            onClick = onUpgrade,
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().alpha(0.55f),
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(R.string.buying_limits_add),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = MaterialTheme.dimens.sm, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tier_premium).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Card(
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(
                        text = stringResource(R.string.buying_limits_unlock_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.buying_limits_unlock_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                PrimaryPill(
                    text = stringResource(R.string.go_premium),
                    onClick = onUpgrade,
                    fillWidth = true,
                    modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
                )
            }
        }
        Text(
            text = stringResource(R.string.buying_limits_used, usedCount, BuyingLimitsRepository.FREE_LIMIT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun bandColor(status: BuyingLimitStatus): Color = when (status) {
    BuyingLimitStatus.ON_TRACK -> budgetGoodColor()
    BuyingLimitStatus.AT_LIMIT -> budgetWarnColor()
    BuyingLimitStatus.OVER -> budgetBadColor()
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun BuyingLimitsPreview() {
    BudgettyTheme {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(
                limits = listOf(
                    BuyingLimitCardUi(
                        BuyingLimitEntity(
                            1, "⚡", "Energy drinks", "red bull\nmonster", BuyingLimitTimeframe.WEEKLY, 2,
                        ),
                        bought = 1,
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(2, "☕", "Takeaway coffee", "coffee", BuyingLimitTimeframe.WEEKLY, 5),
                        bought = 5,
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(
                            3, "🥤", "Fizzy drinks", "coke\ncola\nfanta", BuyingLimitTimeframe.MONTHLY, 3,
                        ),
                        bought = 4,
                    ),
                ),
                monthStartDay = 1,
                isPremium = true,
                isLoaded = true,
            ),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _ -> },
            onDeleteLimit = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun BuyingLimitsEmptyPreview() {
    BudgettyTheme {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(items = emptyList(), isLoaded = true),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _ -> },
            onDeleteLimit = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun BuyingLimitsLockedPreview() {
    BudgettyTheme {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(
                limits = listOf(
                    BuyingLimitCardUi(
                        BuyingLimitEntity(1, "🥤", "Fizzy drinks", "coke\ncola", BuyingLimitTimeframe.MONTHLY, 3),
                        bought = 2,
                    ),
                ),
                isPremium = false,
                isLoaded = true,
            ),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _ -> },
            onDeleteLimit = {},
        )
    }
}
