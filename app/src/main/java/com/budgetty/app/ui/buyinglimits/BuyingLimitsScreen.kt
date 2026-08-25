package com.budgetty.app.ui.buyinglimits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.pluralStringResource
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
import com.budgetty.app.analytics.LimitSource
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.ui.savings.PrimaryPill
import com.budgetty.app.ui.streaks.LimitWindow
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.streaks.StreakMotif
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.isDarkTheme
import com.budgetty.app.ui.theme.wellbeingWarnOn
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.LimitSuggestion
import com.budgetty.app.ui.util.formatDayMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Account → Buying limits: keyword-based item purchase caps as opt-in challenges. Each card shows a
 * limit's keywords, timeframe, a pip progress row, a calm status, an under-cap streak caption (§4.2)
 * and an 8-window history strip (§4.3); tapping one edits it. Frequency-derived suggestions offer a
 * way in (§4.4). A free user gets [BuyingLimitsRepository.FREE_LIMIT] limits, then the Add row locks
 * and routes to the paywall. Same chrome as Category rules (TopAppBar + back, bottom nav hidden).
 */
@Composable
fun BuyingLimitsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuyingLimitsViewModel = org.koin.androidx.compose.koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // §4.2 impression analytics: fire once per unique under-cap streak caption shown (current ≥ 2).
    val surfaced = state.limits.map { it.streak }.filter { it.current >= StreakEngine.MIN_TO_SURFACE }
    LaunchedEffect(surfaced.joinToString { "${it.label}:${it.current}" }) {
        surfaced.forEach { viewModel.onLimitStreakSurfaced(it.current) }
    }

    BuyingLimitsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onUpgrade = onNavigateToPaywall,
        onSaveLimit = viewModel::saveLimit,
        onDeleteLimit = viewModel::deleteLimit,
        onDismissSuggestion = viewModel::dismissSuggestion,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BuyingLimitsContent(
    state: BuyingLimitsUiState,
    onNavigateBack: () -> Unit,
    onUpgrade: () -> Unit,
    onSaveLimit: (Long?, String, String, List<String>, BuyingLimitTimeframe, Int, LimitSource) -> Unit,
    onDeleteLimit: (Long) -> Unit,
    onDismissSuggestion: (String) -> Unit,
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
                        suggestions = state.suggestions,
                        onAdd = { editorFor = EditorTarget(null) },
                        onUseSuggestion = { editorFor = it.toEditorTarget() },
                        onDismissSuggestion = onDismissSuggestion,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    else -> BuyingLimitsList(
                        state = state,
                        onEdit = { editorFor = EditorTarget(it) },
                        onAdd = { editorFor = EditorTarget(null) },
                        onUseSuggestion = { editorFor = it.toEditorTarget() },
                        onDismissSuggestion = onDismissSuggestion,
                        onUpgrade = onUpgrade,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
    }

    editorFor?.let { target ->
        BuyingLimitEditorSheet(
            initial = target.value ?: target.prefill,
            isEditing = target.value != null,
            items = state.items,
            monthStartDay = state.monthStartDay,
            onSave = { emoji, label, keywords, timeframe, count ->
                onSaveLimit(target.value?.id, emoji, label, keywords, timeframe, count, target.source)
                editorFor = null
            },
            onDelete = target.value?.let { limit -> { onDeleteLimit(limit.id); editorFor = null } },
            onDismiss = { editorFor = null },
        )
    }
}

/**
 * Wraps the editor's subject. [value] is the limit under edit (null when adding). [prefill] seeds the
 * fields for a suggestion-created NEW limit (§4.4), and [source] tags where the created limit came from.
 */
private data class EditorTarget(
    val value: BuyingLimitEntity?,
    val prefill: BuyingLimitEntity? = null,
    val source: LimitSource = LimitSource.MANUAL,
)

/** A tapped suggestion becomes a new-limit editor pre-filled with its keyword, weekly cap and timeframe. */
private fun LimitSuggestion.toEditorTarget() = EditorTarget(
    value = null,
    prefill = BuyingLimitEntity(
        keywords = BuyingLimitEntity.joinKeywords(listOf(keyword)),
        timeframe = timeframe,
        count = suggestedCap,
    ),
    source = LimitSource.SUGGESTION,
)

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
    onUseSuggestion: (LimitSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
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
        // §4.4: one dismissible suggestion above the list when they already have limits.
        state.suggestions.firstOrNull()?.let { suggestion ->
            item(key = "suggestion") {
                LimitSuggestionRow(
                    suggestion = suggestion,
                    onUse = { onUseSuggestion(suggestion) },
                    onDismiss = { onDismissSuggestion(suggestion.keyword) },
                )
            }
        }
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
internal fun BuyingLimitCard(card: BuyingLimitCardUi, monthStartDay: Int, onClick: () -> Unit) {
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
                StatusChip(status = card.status, overBy = card.overBy, band = band)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            KeywordChipsRow(card.limit.keywordList)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Pips(bought = card.bought, limit = card.limit.count, band = band)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            MetaLine(card = card, band = band, monthStartDay = monthStartDay)
            StreakCaption(streak = card.streak, timeframe = card.limit.timeframe)
            if (card.hasHistory) {
                HistoryStrip(card = card)
            }
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

/**
 * The calm status chip. On-track and at-limit read ✓ (reaching a cap you set is not a failure, §4.1);
 * over reads "Over by N" with a bang glyph — but in the WARM amber band, never red.
 */
@Composable
private fun StatusChip(status: BuyingLimitStatus, overBy: Int, band: Color) {
    val dark = isDarkTheme()
    val bg = band.copy(alpha = if (dark) 0.24f else 0.15f)
    val fg = if (dark) lerp(band, Color.White, 0.35f) else lerp(band, Color.Black, 0.42f)
    val (icon, label) = when (status) {
        BuyingLimitStatus.ON_TRACK -> Icons.Filled.Check to stringResource(R.string.buying_limits_status_on_track)
        BuyingLimitStatus.AT_LIMIT -> Icons.Filled.Check to stringResource(R.string.buying_limits_status_at_limit)
        BuyingLimitStatus.OVER ->
            Icons.Filled.PriorityHigh to stringResource(R.string.buying_limits_status_over, overBy)
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
            text = label,
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
 * muted. Over the cap, the overflow bought pips are set apart after a small gap and drawn TRANSPARENT
 * with an inset warn ring (§4.1) — over reads warm and distinct, never red. Total = max(limit, bought).
 */
@Composable
private fun Pips(bought: Int, limit: Int, band: Color) {
    val total = maxOf(limit, bought).coerceAtLeast(1)
    val muted = MaterialTheme.colorScheme.outlineVariant
    val overRing = wellbeingWarnOn()
    val pipShape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until total) {
            // Set the overflow pips apart from the allowed ones when over the cap.
            if (i == limit && total > limit) Spacer(Modifier.width(MaterialTheme.dimens.sm))
            val overCap = i >= limit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(pipShape)
                    .then(
                        when {
                            overCap -> Modifier.border(1.6.dp, overRing, pipShape)
                            i < bought -> Modifier.background(band)
                            else -> Modifier.background(muted)
                        },
                    ),
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

/**
 * §4.2 per-limit streak caption: the shared [StreakMotif] plus quiet copy. Surfaces the current run
 * ("· N weeks/months under") when it's ≥ 2, else a muted best-run fallback ("Best run: N …") when a
 * past run was ≥ 2. Renders nothing otherwise — no loss framing, no flames.
 */
@Composable
private fun StreakCaption(streak: Streak, timeframe: BuyingLimitTimeframe) {
    val weekly = timeframe == BuyingLimitTimeframe.WEEKLY
    val current = streak.current >= StreakEngine.MIN_TO_SURFACE
    val bestRun = !current && streak.best >= StreakEngine.MIN_TO_SURFACE
    if (!current && !bestRun) return
    val n = if (current) streak.current else streak.best
    val text = when {
        current && weekly -> pluralStringResource(R.plurals.streak_weeks_under, n, n)
        current -> pluralStringResource(R.plurals.streak_months_under, n, n)
        weekly -> pluralStringResource(R.plurals.buying_limits_streak_best_weeks, n, n)
        else -> pluralStringResource(R.plurals.buying_limits_streak_best_months, n, n)
    }
    Spacer(Modifier.height(6.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        StreakMotif(filledCount = n, showLive = current && streak.liveOnTrack, muted = bestRun, maxSegments = 6)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * §4.3 history strip: under a top-border separator, the last [BuyingLimitsViewModel.HISTORY_WINDOWS]
 * closed windows as small squares — met (good), not-met (warm) or no-data (outline) — oldest on the
 * left, plus "N of the last 8 …" so a 6-of-8 user sees real progress where a bare streak shows 0.
 */
@Composable
private fun HistoryStrip(card: BuyingLimitCardUi) {
    val weekly = card.limit.timeframe == BuyingLimitTimeframe.WEEKLY
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Most-recent first from the engine → reversed so time reads left (old) to right (new).
            card.history.asReversed().forEach { HistorySquare(window = it, cap = card.limit.count) }
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = if (weekly) {
                stringResource(R.string.buying_limits_history_weeks, card.historyMet, card.history.size)
            } else {
                stringResource(R.string.buying_limits_history_months, card.historyMet, card.history.size)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistorySquare(window: LimitWindow, cap: Int) {
    val shape = RoundedCornerShape(4.dp)
    val met = window.hasData && window.count <= cap
    val modifier = when {
        !window.hasData -> Modifier.border(1.4.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        met -> Modifier.background(budgetGoodColor())
        else -> Modifier.background(budgetWarnColor())
    }
    Box(Modifier.size(11.dp).clip(shape).then(modifier))
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

/**
 * §4.4 suggestion row: a quiet informational surface — "You bought <name> N× last month — cap it?"
 * with the item name emphasised — that opens the editor pre-filled on tap, plus a dismiss ✕ that
 * remembers the rejection for good.
 */
@Composable
internal fun LimitSuggestionRow(suggestion: LimitSuggestion, onUse: () -> Unit, onDismiss: () -> Unit) {
    val prompt = stringResource(R.string.buying_limits_suggest_prompt, suggestion.name, suggestion.monthCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onUse)
            .padding(MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LimitEmojiChip(emoji = "")
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = emphasizeName(prompt, suggestion.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = stringResource(R.string.buying_limits_suggest_cap, suggestion.suggestedCap),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.buying_limits_suggest_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
    }
}

/** Bolds the item [name] inside the already-formatted [prompt], keeping the locale's word order. */
@Composable
private fun emphasizeName(prompt: String, name: String) = buildAnnotatedString {
    val idx = prompt.indexOf(name)
    if (idx < 0) {
        append(prompt)
    } else {
        append(prompt.substring(0, idx))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
            append(name)
        }
        append(prompt.substring(idx + name.length))
    }
}

@Composable
private fun BuyingLimitsEmpty(
    suggestions: List<LimitSuggestion>,
    onAdd: () -> Unit,
    onUseSuggestion: (LimitSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                // §4.4: with suggestions present, the blank-add CTA steps back to secondary so the
                // suggestions become the primary way in.
                if (suggestions.isEmpty()) {
                    PrimaryPill(
                        text = stringResource(R.string.buying_limits_add),
                        icon = Icons.Filled.Add,
                        onClick = onAdd,
                        modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
                    )
                } else {
                    OutlinedButton(
                        onClick = onAdd,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
                        )
                        Spacer(Modifier.width(MaterialTheme.dimens.sm))
                        Text(stringResource(R.string.buying_limits_add))
                    }
                }
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(R.string.buying_limits_free_hint, BuyingLimitsRepository.FREE_LIMIT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (suggestions.isNotEmpty()) {
            SuggestionsSection(
                suggestions = suggestions,
                onUseSuggestion = onUseSuggestion,
                onDismissSuggestion = onDismissSuggestion,
            )
        }
        ExplainerBlock()
    }
}

/** §4.4 discovery block on the empty state: "Most bought lately · last 60 days" + up to 3 rows. */
@Composable
private fun SuggestionsSection(
    suggestions: List<LimitSuggestion>,
    onUseSuggestion: (LimitSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
            modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
        ) {
            Text(
                text = stringResource(R.string.buying_limits_suggest_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.buying_limits_suggest_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        suggestions.forEach { suggestion ->
            LimitSuggestionRow(
                suggestion = suggestion,
                onUse = { onUseSuggestion(suggestion) },
                onDismiss = { onDismissSuggestion(suggestion.keyword) },
            )
        }
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
                    text = stringResource(R.string.buying_limits_unlock_body, BuyingLimitsRepository.FREE_LIMIT),
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
    // §4.1: at cap AND over both read warm (amber) — reaching a cap you set is not a failure.
    BuyingLimitStatus.AT_LIMIT, BuyingLimitStatus.OVER -> budgetWarnColor()
}

@Preview(showBackground = true, heightDp = 940)
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
                        streak = Streak(StreakKind.LIMIT, "Energy drinks", 3, 5, 6, true),
                        history = previewHistory("mmxmmmxm"),
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(2, "☕", "Takeaway coffee", "coffee", BuyingLimitTimeframe.WEEKLY, 5),
                        bought = 5,
                        history = previewHistory("mmmxmoom"),
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(
                            3, "🥤", "Fizzy drinks", "coke\ncola\nfanta", BuyingLimitTimeframe.MONTHLY, 3,
                        ),
                        bought = 4,
                        streak = Streak(StreakKind.LIMIT, "Fizzy drinks", 0, 4, 6, false),
                        history = previewHistory("mmxxmmxx"),
                    ),
                ),
                suggestions = listOf(
                    LimitSuggestion("Crisps", "crisps", 11, 2),
                ),
                monthStartDay = 1,
                isPremium = true,
                isLoaded = true,
            ),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _, _ -> },
            onDeleteLimit = {},
            onDismissSuggestion = {},
        )
    }
}

/** Builds a preview history from a code string: m = met, x = not-met, o = no-data (most recent last). */
private fun previewHistory(code: String): List<LimitWindow> =
    code.reversed().map { c ->
        when (c) {
            'm' -> LimitWindow(count = 1, hasData = true)
            'x' -> LimitWindow(count = 9, hasData = true)
            else -> LimitWindow(count = 0, hasData = false)
        }
    }

@Preview(showBackground = true, heightDp = 940)
@Composable
private fun BuyingLimitsEmptyPreview() {
    BudgettyTheme {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(
                items = emptyList(),
                suggestions = listOf(
                    LimitSuggestion("Coca-Cola", "coca-cola", 14, 3),
                    LimitSuggestion("Crisps", "crisps", 11, 2),
                    LimitSuggestion("Takeaway", "takeaway", 9, 2),
                ),
                isLoaded = true,
            ),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _, _ -> },
            onDeleteLimit = {},
            onDismissSuggestion = {},
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
                    BuyingLimitCardUi(
                        BuyingLimitEntity(2, "☕", "Coffee", "coffee", BuyingLimitTimeframe.WEEKLY, 5),
                        bought = 5,
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(3, "🍫", "Chocolate", "chocolate", BuyingLimitTimeframe.WEEKLY, 2),
                        bought = 3,
                    ),
                ),
                isPremium = false,
                isLoaded = true,
            ),
            onNavigateBack = {},
            onUpgrade = {},
            onSaveLimit = { _, _, _, _, _, _, _ -> },
            onDeleteLimit = {},
            onDismissSuggestion = {},
        )
    }
}
