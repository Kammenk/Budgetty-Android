package com.budgetty.app.ui.wellbeing

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.components.ScoreRing
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetGreatColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.wellbeingBadContainer
import com.budgetty.app.ui.theme.wellbeingGoodContainer
import com.budgetty.app.ui.theme.wellbeingWarnContainer
import com.budgetty.app.ui.theme.wellbeingWarnOn
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.formatMonth
import com.budgetty.app.ui.util.isExpandedWidth
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Deep-link targets a Wellbeing tip / component can jump to. */
data class WellbeingNav(
    val toBudget: () -> Unit,
    val toSubscriptions: () -> Unit,
    val toGoals: () -> Unit,
    val toInsights: () -> Unit,
    val toHistory: () -> Unit,
    val addReceipt: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellbeingScreen(
    onNavigateBack: () -> Unit,
    nav: WellbeingNav,
    viewModel: WellbeingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(WellbeingMode.MONTHLY) }
    var open by rememberSaveable { mutableStateOf<WellbeingComponentKey?>(null) }
    val periodLabel = state.summary?.let {
        if (mode == WellbeingMode.WEEKLY && it.hasScore) "${it.weekStart.formatDayMonth()} – ${it.weekEnd.formatDayMonth()}"
        else if (it.hasScore) it.monthYear.formatMonth() else ""
    }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wellbeing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (periodLabel.isNotEmpty()) {
                        Text(
                            periodLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = MaterialTheme.dimens.lg),
                        )
                    }
                },
                // MainScaffold already applies the status-bar inset to this route's content.
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        WellbeingContent(
            state = state,
            mode = mode,
            onModeChange = { mode = it },
            open = open,
            onToggle = { key -> open = if (open == key) null else key },
            onDismiss = viewModel::dismiss,
            nav = nav,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun WellbeingContent(
    state: WellbeingUiState,
    mode: WellbeingMode,
    onModeChange: (WellbeingMode) -> Unit,
    open: WellbeingComponentKey?,
    onToggle: (WellbeingComponentKey) -> Unit,
    onDismiss: (String) -> Unit,
    nav: WellbeingNav,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary ?: return
    val d = MaterialTheme.dimens
    // On tablet the content is a single centred column capped at SinglePaneMaxWidth (per the tablet
    // design's portrait layout); the score card goes two-up and tips run in two columns.
    val wide = isExpandedWidth()
    val cols = if (wide) 2 else 1
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.md)
                .padding(top = d.xs, bottom = d.xxxl),
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            val firstRun = !summary.hasScore
            val weekly = mode == WellbeingMode.WEEKLY

            if (!firstRun) {
                SegmentedToggle(
                    options = listOf(stringResource(R.string.wellbeing_weekly), stringResource(R.string.wellbeing_monthly)),
                    selectedIndex = if (weekly) 0 else 1,
                    onSelect = { onModeChange(if (it == 0) WellbeingMode.WEEKLY else WellbeingMode.MONTHLY) },
                )
            }

            when {
                firstRun -> {
                    FirstRunCard(summary, onSetBudget = nav.toBudget, onAddReceipt = nav.addReceipt)
                    TipsFeed(state.monthlyTips, weekly = false, onDismiss = onDismiss, nav = nav, columns = cols)
                }
                weekly -> {
                    WeeklyPaceCard(summary.weekly)
                    SecondaryScoreRow(summary)
                    TipsFeed(state.weeklyTips, weekly = true, onDismiss = onDismiss, nav = nav, columns = cols)
                }
                else -> {
                    ScoreCard(summary, open, onToggle, nav, twoUp = wide)
                    TipsFeed(state.monthlyTips, weekly = false, onDismiss = onDismiss, nav = nav, columns = cols)
                    if (summary.wins.isNotEmpty()) WinsStrip(summary.wins)
                    FocusCard(summary)
                }
            }

            Text(
                stringResource(R.string.wellbeing_privacy_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = d.xs, vertical = d.sm),
            )
        }
    }
}

// ── Score card (monthly) ───────────────────────────────────────────────────────

@Composable
private fun ScoreCard(
    summary: WellbeingSummary,
    open: WellbeingComponentKey?,
    onToggle: (WellbeingComponentKey) -> Unit,
    nav: WellbeingNav,
    twoUp: Boolean,
) {
    WellbeingCard {
        if (twoUp) {
            // Tablet: the grade and its reasons read together — ring on the left, components on the right.
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xl)) {
                Box(Modifier.weight(0.42f), contentAlignment = Alignment.TopCenter) {
                    ScoreRingHero(summary, ringSize = 168.dp)
                }
                Column(Modifier.weight(0.58f)) {
                    ComponentsSection(summary, open, onToggle, nav)
                }
            }
        } else {
            ScoreRingHero(summary, ringSize = 152.dp)
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            ComponentsSection(summary, open, onToggle, nav)
        }
    }
}

@Composable
private fun ScoreRingHero(summary: WellbeingSummary, ringSize: Dp) {
    val score = summary.score
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ScoreRing(
            fraction = (score.score ?: 0) / 100f,
            arcColor = bandColor(score.band),
            strokeRatio = 4.2f / 42f,
            modifier = Modifier.size(ringSize),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${score.score}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text(bandWord(score.band), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = bandColor(score.band))
                Text(stringResource(R.string.wellbeing_out_of_100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        score.trendDeltaVsPrevious?.let { delta ->
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            TrendChip(delta)
        }
    }
}

@Composable
private fun ComponentsSection(
    summary: WellbeingSummary,
    open: WellbeingComponentKey?,
    onToggle: (WellbeingComponentKey) -> Unit,
    nav: WellbeingNav,
) {
    Text(
        stringResource(R.string.wellbeing_built_from),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    summary.score.components.forEach { comp ->
        ComponentRow(comp, summary.detail[comp.key], open == comp.key, { onToggle(comp.key) }, nav)
    }
    if (summary.score.hasExcludedComponent) {
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            stringResource(R.string.wellbeing_renormalise_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComponentRow(
    comp: WellbeingComponent,
    detail: ComponentDetail?,
    open: Boolean,
    onToggle: () -> Unit,
    nav: WellbeingNav,
) {
    val excluded = comp.score == null
    val color = if (excluded) MaterialTheme.colorScheme.onSurfaceVariant else tierColor(comp.score!!)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .clickable(enabled = !excluded) { onToggle() }
            .padding(vertical = MaterialTheme.dimens.sm)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                componentName(comp.key),
                style = MaterialTheme.typography.bodyMedium,
                color = if (excluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (excluded) stringResource(R.string.wellbeing_not_counted) else "${comp.score}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (!excluded) {
                Icon(
                    if (open) Icons.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (comp.score ?: 0) / 100f },
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
        )
        if (open && !excluded) {
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(MaterialTheme.dimens.md),
            ) {
                Text(
                    componentWhy(comp.key, detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Row(
                    Modifier.clickable { componentLink(comp.key, nav) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(componentLinkLabel(comp.key), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendChip(delta: Int) {
    val up = delta >= 0
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            (if (up) "▲ " else "▼ ") + kotlin.math.abs(delta),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (up) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.wellbeing_vs_last_month), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Weekly ─────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyPaceCard(weekly: WeeklyPace) {
    WellbeingCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.wellbeing_this_week), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            weekly.deltaPercentVsLastWeek?.let {
                val down = it <= 0
                Text(
                    (if (down) "↓ " else "↑ ") + "${kotlin.math.abs(it)}% " + stringResource(R.string.wellbeing_vs_last_week),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (down) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(weekly.spent.formatMoney(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        weekly.weeklyBudget?.let {
            Text(stringResource(R.string.wellbeing_of_weekly_budget, it.formatMoney()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            LinearProgressIndicator(
                progress = { weekly.fractionUsed },
                color = if (weekly.onTrack) budgetGoodColor() else budgetWarnColor(),
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                stringResource(R.string.wellbeing_left_days, weekly.remaining.formatMoney(), weekly.daysLeft),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecondaryScoreRow(summary: WellbeingSummary) {
    val score = summary.score
    WellbeingCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
            ScoreRing(
                fraction = (score.score ?: 0) / 100f,
                arcColor = bandColor(score.band),
                strokeRatio = 5f / 42f,
                modifier = Modifier.size(52.dp),
            ) {
                Text("${score.score}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(bandWord(score.band), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = bandColor(score.band))
                Text(stringResource(R.string.wellbeing_updates_monthly), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── First run ──────────────────────────────────────────────────────────────────

@Composable
private fun FirstRunCard(summary: WellbeingSummary, onSetBudget: () -> Unit, onAddReceipt: () -> Unit) {
    WellbeingCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreRing(
                fraction = 0.22f,
                arcColor = MaterialTheme.colorScheme.outline,
                strokeRatio = 4.2f / 42f,
                dashedTrack = true,
                modifier = Modifier.size(140.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.wellbeing_not_scored_yet), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Text(stringResource(R.string.wellbeing_first_run_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            SetupStep(done = summary.receiptsLogged > 0, text = stringResource(R.string.wellbeing_setup_receipts, summary.receiptsLogged))
            SetupStep(done = summary.hasBudget, text = stringResource(R.string.wellbeing_setup_budget))
            SetupStep(done = summary.receiptsLogged >= WellbeingEngine.MIN_RECEIPTS_TO_SCORE, text = stringResource(R.string.wellbeing_setup_history))
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                FilledTonalButton(onClick = onSetBudget, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.wellbeing_cta_set_budget)) }
                FilledTonalButton(onClick = onAddReceipt, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.wellbeing_cta_add_receipt)) }
            }
        }
    }
}

@Composable
private fun SetupStep(done: Boolean, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Icon(
            if (done) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = null,
            tint = if (done) budgetGoodColor() else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Tips ───────────────────────────────────────────────────────────────────────

@Composable
private fun TipsFeed(tips: List<WellbeingTip>, weekly: Boolean, onDismiss: (String) -> Unit, nav: WellbeingNav, columns: Int = 1) {
    if (tips.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
        Row(
            Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.md, start = MaterialTheme.dimens.xs, end = MaterialTheme.dimens.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                stringResource(if (weekly) R.string.wellbeing_focus_week else R.string.wellbeing_focus_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.wellbeing_tip_count, tips.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (columns >= 2) {
            // Tablet: two-up so the wide column isn't left half-empty.
            tips.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                    pair.forEach { TipCard(it, onDismiss, nav, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            tips.forEach { TipCard(it, onDismiss, nav) }
        }
    }
}

@Composable
private fun TipCard(tip: WellbeingTip, onDismiss: (String) -> Unit, nav: WellbeingNav, modifier: Modifier = Modifier) {
    WellbeingCard(modifier = modifier, padding = MaterialTheme.dimens.md) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(toneBg(tip.tone)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(toneIcon(tip.tone), contentDescription = null, tint = toneFg(tip.tone), modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tipTitle(tip), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        tipDetail(tip)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.md, start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { tipCta(tip.type, nav) }
                            .padding(horizontal = 15.dp, vertical = 9.dp),
                    ) {
                        Text(tipCtaLabel(tip.type), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(
                        stringResource(R.string.wellbeing_got_it),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onDismiss(tip.id) }.padding(horizontal = 10.dp, vertical = 9.dp),
                    )
                }
            }
            IconButton(onClick = { onDismiss(tip.id) }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_dismiss), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Wins + focus ───────────────────────────────────────────────────────────────

@Composable
private fun WinsStrip(wins: List<WellbeingWin>) {
    Column {
        Text(stringResource(R.string.wellbeing_wins), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = MaterialTheme.dimens.md, bottom = MaterialTheme.dimens.sm, start = MaterialTheme.dimens.xs))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
            wins.forEach { win ->
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(wellbeingGoodContainer()).padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(win.emoji, style = MaterialTheme.typography.bodyMedium)
                    Text(winLabel(win), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = budgetGoodColor(), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FocusCard(summary: WellbeingSummary) {
    val lowest = summary.score.components.filter { it.score != null }.minByOrNull { it.score!! } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.xs)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(MaterialTheme.dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(19.dp))
        Column {
            Text(stringResource(R.string.wellbeing_focus_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(focusMessage(lowest.key), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ── Shared card ────────────────────────────────────────────────────────────────

@Composable
private fun WellbeingCard(modifier: Modifier = Modifier, padding: Dp = MaterialTheme.dimens.xl, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(padding)) { content() }
    }
}

// ── Localised copy + colour maps ───────────────────────────────────────────────

@Composable private fun bandColor(band: WellbeingBand?): Color = when (band) {
    WellbeingBand.NEEDS_WORK -> budgetBadColor()
    WellbeingBand.GETTING_THERE -> budgetWarnColor()
    WellbeingBand.HEALTHY -> budgetGoodColor()
    WellbeingBand.THRIVING -> budgetGreatColor()
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable private fun tierColor(score: Int): Color = when (WellbeingEngine.tier(score)) {
    WellbeingTier.GOOD -> budgetGoodColor()
    WellbeingTier.WARN -> budgetWarnColor()
    WellbeingTier.BAD -> budgetBadColor()
}

@Composable private fun bandWord(band: WellbeingBand?): String = stringResource(
    when (band) {
        WellbeingBand.NEEDS_WORK -> R.string.wellbeing_band_needs_work
        WellbeingBand.GETTING_THERE -> R.string.wellbeing_band_getting_there
        WellbeingBand.HEALTHY -> R.string.wellbeing_band_healthy
        WellbeingBand.THRIVING -> R.string.wellbeing_band_thriving
        null -> R.string.wellbeing_not_scored_yet
    },
)

@Composable private fun componentName(key: WellbeingComponentKey): String = stringResource(
    when (key) {
        WellbeingComponentKey.SAVINGS -> R.string.wellbeing_comp_savings
        WellbeingComponentKey.BUDGET -> R.string.wellbeing_comp_budget
        WellbeingComponentKey.TREND -> R.string.wellbeing_comp_trend
        WellbeingComponentKey.SUBSCRIPTIONS -> R.string.wellbeing_comp_subs
        WellbeingComponentKey.GOALS -> R.string.wellbeing_comp_goals
    },
)

@Composable private fun componentWhy(key: WellbeingComponentKey, d: ComponentDetail?): String = when (key) {
    WellbeingComponentKey.SAVINGS -> stringResource(R.string.wellbeing_why_savings, d?.savingsRatePercent ?: 0, (d?.saved ?: BigDecimal.ZERO).formatMoney(), (d?.income ?: BigDecimal.ZERO).formatMoney())
    WellbeingComponentKey.BUDGET -> if ((d?.overCount ?: 0) == 0) stringResource(R.string.wellbeing_why_budget_ok, d?.budgetedCount ?: 0)
        else stringResource(R.string.wellbeing_why_budget_over, d?.overCount ?: 0, d?.budgetedCount ?: 0, (d?.overspend ?: BigDecimal.ZERO).formatMoney())
    WellbeingComponentKey.TREND -> d?.trendPercent?.let {
        stringResource(if (it >= 0) R.string.wellbeing_why_trend_up else R.string.wellbeing_why_trend_down, kotlin.math.abs(it), (d.avgSpend ?: BigDecimal.ZERO).formatMoney())
    } ?: stringResource(R.string.wellbeing_why_trend_none)
    WellbeingComponentKey.SUBSCRIPTIONS -> stringResource(R.string.wellbeing_why_subs, (d?.subsMonthly ?: BigDecimal.ZERO).formatMoney(), d?.subsSharePercent ?: 0)
    WellbeingComponentKey.GOALS -> stringResource(R.string.wellbeing_why_goals, d?.goalsOnPace ?: 0, d?.goalsTotal ?: 0)
}

@Composable private fun componentLinkLabel(key: WellbeingComponentKey): String = stringResource(
    when (key) {
        WellbeingComponentKey.SAVINGS -> R.string.wellbeing_link_savings
        WellbeingComponentKey.BUDGET -> R.string.wellbeing_link_budget
        WellbeingComponentKey.TREND -> R.string.wellbeing_link_trend
        WellbeingComponentKey.SUBSCRIPTIONS -> R.string.wellbeing_link_subs
        WellbeingComponentKey.GOALS -> R.string.wellbeing_link_goals
    },
)

private fun componentLink(key: WellbeingComponentKey, nav: WellbeingNav) = when (key) {
    WellbeingComponentKey.SAVINGS -> nav.toInsights()
    WellbeingComponentKey.BUDGET -> nav.toBudget()
    WellbeingComponentKey.TREND -> nav.toInsights()
    WellbeingComponentKey.SUBSCRIPTIONS -> nav.toSubscriptions()
    WellbeingComponentKey.GOALS -> nav.toGoals()
}

@Composable private fun toneBg(tone: TipTone): Color = when (tone) {
    TipTone.ALERT -> wellbeingBadContainer()
    TipTone.CAUTION -> wellbeingWarnContainer()
    TipTone.OPPORTUNITY -> MaterialTheme.colorScheme.primaryContainer
    TipTone.WIN -> wellbeingGoodContainer()
}

@Composable private fun toneFg(tone: TipTone): Color = when (tone) {
    TipTone.ALERT -> budgetBadColor()
    TipTone.CAUTION -> wellbeingWarnOn()
    TipTone.OPPORTUNITY -> MaterialTheme.colorScheme.onPrimaryContainer
    TipTone.WIN -> budgetGoodColor()
}

private fun toneIcon(tone: TipTone): ImageVector = when (tone) {
    TipTone.ALERT -> Icons.Filled.Warning
    TipTone.CAUTION -> Icons.Filled.Info
    TipTone.OPPORTUNITY -> Icons.Filled.Add
    TipTone.WIN -> Icons.Filled.Check
}

@Composable private fun tipTitle(tip: WellbeingTip): String {
    val amt = (tip.amount ?: BigDecimal.ZERO).formatMoney()
    val amt2 = (tip.amount2 ?: BigDecimal.ZERO).formatMoney()
    return when (tip.type) {
        TipType.NEGATIVE_CASHFLOW -> stringResource(R.string.wellbeing_tip_cashflow_title, amt)
        TipType.OVER_BUDGET -> pluralStringResource(R.plurals.wellbeing_tip_overbudget_title, tip.count ?: 0, tip.count ?: 0, amt)
        TipType.CATEGORY_SPIKE -> stringResource(R.string.wellbeing_tip_spike_title, categoryDisplayName(tip.label.orEmpty()), tip.percent ?: 0, amt, amt2)
        TipType.SUBSCRIPTION_COST -> pluralStringResource(R.plurals.wellbeing_tip_subs_title, tip.count ?: 0, tip.count ?: 0, amt)
        TipType.MISSING_BUDGET -> stringResource(R.string.wellbeing_tip_missingbudget_title, amt, categoryDisplayName(tip.label.orEmpty()))
        TipType.NO_GOAL -> stringResource(R.string.wellbeing_tip_nogoal_title)
        TipType.GOAL_OFF_TRACK -> stringResource(R.string.wellbeing_tip_goaloff_title, tip.label.orEmpty())
        TipType.SAVINGS_WIN -> stringResource(R.string.wellbeing_tip_savingswin_title, tip.percent ?: 0)
        TipType.CATEGORY_IMPROVED -> stringResource(R.string.wellbeing_tip_improved_title, categoryDisplayName(tip.label.orEmpty()), tip.percent ?: 0)
        TipType.BUDGET_PACE -> stringResource(R.string.wellbeing_tip_pace_title, tip.percent ?: 0)
        TipType.SMALL_PURCHASE_LEAK -> pluralStringResource(R.plurals.wellbeing_tip_leak_title, tip.count ?: 0, tip.count ?: 0, categoryDisplayName(tip.label.orEmpty()))
        TipType.UNDER_PACE_WIN -> stringResource(R.string.wellbeing_tip_underpace_title)
    }
}

@Composable private fun tipDetail(tip: WellbeingTip): String? {
    val amt = (tip.amount ?: BigDecimal.ZERO).formatMoney()
    return when (tip.type) {
        TipType.SAVINGS_WIN -> stringResource(R.string.wellbeing_tip_savingswin_detail, amt)
        TipType.SUBSCRIPTION_COST -> tip.percent?.let { stringResource(R.string.wellbeing_tip_subs_detail, it) }
        TipType.BUDGET_PACE -> stringResource(R.string.wellbeing_tip_pace_detail, amt)
        TipType.SMALL_PURCHASE_LEAK -> stringResource(R.string.wellbeing_tip_leak_detail, amt)
        TipType.MISSING_BUDGET -> stringResource(R.string.wellbeing_tip_missingbudget_detail)
        TipType.NO_GOAL -> stringResource(R.string.wellbeing_tip_nogoal_detail)
        else -> null
    }
}

@Composable private fun tipCtaLabel(type: TipType): String = stringResource(
    when (type) {
        TipType.NEGATIVE_CASHFLOW -> R.string.wellbeing_cta_where
        TipType.OVER_BUDGET -> R.string.wellbeing_cta_view_budgets
        TipType.CATEGORY_SPIKE -> R.string.wellbeing_cta_set_budget
        TipType.SUBSCRIPTION_COST -> R.string.wellbeing_cta_review_subs
        TipType.MISSING_BUDGET -> R.string.wellbeing_cta_set_budget
        TipType.NO_GOAL, TipType.GOAL_OFF_TRACK -> R.string.wellbeing_cta_create_goal
        TipType.SAVINGS_WIN -> R.string.wellbeing_link_savings
        TipType.CATEGORY_IMPROVED -> R.string.wellbeing_link_trend
        TipType.BUDGET_PACE, TipType.UNDER_PACE_WIN -> R.string.wellbeing_cta_view_budgets
        TipType.SMALL_PURCHASE_LEAK -> R.string.wellbeing_cta_see_txns
    },
)

private fun tipCta(type: TipType, nav: WellbeingNav) = when (type) {
    TipType.NEGATIVE_CASHFLOW, TipType.SMALL_PURCHASE_LEAK -> nav.toHistory()
    TipType.OVER_BUDGET, TipType.CATEGORY_SPIKE, TipType.MISSING_BUDGET, TipType.BUDGET_PACE, TipType.UNDER_PACE_WIN -> nav.toBudget()
    TipType.SUBSCRIPTION_COST -> nav.toSubscriptions()
    TipType.NO_GOAL, TipType.GOAL_OFF_TRACK -> nav.toGoals()
    TipType.SAVINGS_WIN, TipType.CATEGORY_IMPROVED -> nav.toInsights()
}

@Composable private fun winLabel(win: WellbeingWin): String = when (win.type) {
    TipType.SAVINGS_WIN -> stringResource(R.string.wellbeing_win_savings, win.percent ?: 0)
    TipType.CATEGORY_IMPROVED -> stringResource(R.string.wellbeing_win_improved, categoryDisplayName(win.label.orEmpty()), win.percent ?: 0)
    TipType.UNDER_PACE_WIN -> stringResource(R.string.wellbeing_win_underpace)
    else -> stringResource(R.string.wellbeing_win_generic)
}

@Composable private fun focusMessage(key: WellbeingComponentKey): String = stringResource(
    when (key) {
        WellbeingComponentKey.SAVINGS -> R.string.wellbeing_focus_savings
        WellbeingComponentKey.BUDGET -> R.string.wellbeing_focus_budget
        WellbeingComponentKey.TREND -> R.string.wellbeing_focus_trend
        WellbeingComponentKey.SUBSCRIPTIONS -> R.string.wellbeing_focus_subs
        WellbeingComponentKey.GOALS -> R.string.wellbeing_focus_goals
    },
)

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun WellbeingHealthyPreview() {
    BudgettyTheme {
        WellbeingContent(
            state = WellbeingUiState(loaded = true, summary = previewSummary(), monthlyTips = previewTips()),
            mode = WellbeingMode.MONTHLY,
            onModeChange = {}, open = null, onToggle = {}, onDismiss = {},
            nav = previewNav(),
        )
    }
}

private fun previewNav() = WellbeingNav({}, {}, {}, {}, {}, {})

private fun previewTips() = listOf(
    WellbeingTip(TipType.CATEGORY_SPIKE, "spike:Dining", TipTone.CAUTION, amount = BigDecimal("182"), amount2 = BigDecimal("136"), percent = 34, label = "Dining"),
    WellbeingTip(TipType.SUBSCRIPTION_COST, "subs_cost", TipTone.CAUTION, amount = BigDecimal("22"), count = 2, percent = 9),
    WellbeingTip(TipType.SAVINGS_WIN, "savings_win", TipTone.WIN, percent = 18, amount = BigDecimal("432")),
)

private fun previewSummary(): WellbeingSummary {
    val comps = listOf(
        WellbeingComponent(WellbeingComponentKey.SAVINGS, 25, 78),
        WellbeingComponent(WellbeingComponentKey.BUDGET, 25, 65),
        WellbeingComponent(WellbeingComponentKey.TREND, 15, 80),
        WellbeingComponent(WellbeingComponentKey.SUBSCRIPTIONS, 15, 55),
        WellbeingComponent(WellbeingComponentKey.GOALS, 20, 70),
    )
    return WellbeingSummary(
        score = WellbeingScore(72, WellbeingBand.HEALTHY, comps, 4),
        monthlyTips = previewTips(),
        weekly = WeeklyPace(BigDecimal("210"), BigDecimal("300"), 0.7f, 0.71f, BigDecimal("90"), 2, -12, true),
        weeklyTips = emptyList(),
        wins = listOf(WellbeingWin("🔥", TipType.SAVINGS_WIN, percent = 18)),
        detail = emptyMap(),
        receiptsLogged = 18, hasBudget = true, periodId = "2026-06",
        monthYear = YearMonth.of(2026, 6), weekStart = LocalDate.of(2026, 6, 22), weekEnd = LocalDate.of(2026, 6, 28),
    )
}
