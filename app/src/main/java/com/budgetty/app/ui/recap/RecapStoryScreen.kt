package com.budgetty.app.ui.recap

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.ScoreRing
import com.budgetty.app.ui.streaks.StreakEngine
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.streaks.StreakMotif
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetGreatColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.wellbeingGoodContainer
import com.budgetty.app.ui.theme.wellbeingGreatContainer
import com.budgetty.app.ui.theme.wellbeingWarnContainer
import com.budgetty.app.ui.theme.wellbeingWarnOn
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.wellbeing.WellbeingBand
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** The score ring diameter — matches the Wellbeing hero ring's raw-dp sizing (a design-specific size). */
private val RecapRingSize = 170.dp

/** Caps the story content column so it stays a comfortable reading width, centred on tablets. */
private val RecapContentMaxWidth = 460.dp

private val BigFigureSize = 54.sp
private val CoverTitleSize = 34.sp
private val ScoreNumberSize = 46.sp
private val MoverNameSize = 22.sp
private val BudgetTitleSize = 28.sp
private val FocusTitleSize = 26.sp

/**
 * The full-screen Wrapped-style recap story: a swipeable [HorizontalPager] of one-figure cards on
 * tonal band backdrops, with a segmented progress bar, a ✕ that exits any time, and — only on the
 * final card — the Done / See details actions. Tap the right two-thirds to advance, the left third to
 * go back (swipe does the same). No auto-advance: a recap is read, not watched.
 *
 * Stateless: it renders a prebuilt [RecapStory] and calls back on exit. Used both as the on-open
 * interstitial (via the gate in `BudgettyApp`) and re-opened from Insights ([RecapReopenScreen]).
 */
@Composable
fun RecapStoryScreen(
    story: RecapStory,
    onClose: () -> Unit,
    onSeeDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onShown: (RecapKind) -> Unit = {},
    onCompleted: (kind: RecapKind, cardsViewed: Int) -> Unit = { _, _ -> },
    onStreakSurfaced: (StreakKind, Int) -> Unit = { _, _ -> },
    recapEnabled: Boolean = true,
    recapFrequency: RecapFrequency = RecapFrequency.BOTH,
    onRecapFrequencyChange: (enabled: Boolean, frequency: RecapFrequency) -> Unit = { _, _ -> },
) {
    val cards = story.cards
    if (cards.isEmpty()) {
        RecapLoadingBackdrop(modifier)
        return
    }
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage

    // Analytics: count the story as shown once it appears, and — on any exit path (✕, Done, See
    // details, back, or the gate un-mounting it after markShown) — report how far the user got as the
    // highest card reached + 1, so a bare cover read (1) is distinguishable from a full read. Any
    // surfaced streak card also fires streak_surfaced once here (§0 event set).
    val highestCard = remember { mutableIntStateOf(0) }
    LaunchedEffect(current) { if (current > highestCard.intValue) highestCard.intValue = current }
    DisposableEffect(Unit) {
        onShown(story.kind)
        cards.forEach { card -> streakSurfaceOf(card)?.let { (kind, length) -> onStreakSurfaced(kind, length) } }
        onDispose { onCompleted(story.kind, highestCard.intValue + 1) }
    }

    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            RecapCardPage(
                story = story,
                card = cards[page],
                recapEnabled = recapEnabled,
                recapFrequency = recapFrequency,
                onRecapFrequencyChange = onRecapFrequencyChange,
            )
        }

        // Tap zones: left third goes back, right two-thirds advances. Ripple-less; horizontal drags
        // fall through to the pager beneath, so swiping still works.
        Row(modifier = Modifier.fillMaxSize()) {
            TapZone(Modifier.weight(1f)) {
                if (current > 0) scope.launch { pagerState.animateScrollToPage(current - 1) }
            }
            TapZone(Modifier.weight(2f)) {
                if (current < cards.lastIndex) scope.launch { pagerState.animateScrollToPage(current + 1) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
        ) {
            RecapProgressBar(count = cards.size, current = current)
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            RecapHeader(story = story, onClose = onClose)
        }

        if (current == cards.lastIndex) {
            RecapActions(
                onDone = onClose,
                onSeeDetails = onSeeDetails,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.lg),
            )
        }
    }
}

@Composable
private fun TapZone(modifier: Modifier, onTap: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
    )
}

@Composable
private fun RecapProgressBar(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(MaterialTheme.dimens.xs)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun RecapHeader(story: RecapStory, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose, modifier = Modifier.size(MaterialTheme.dimens.touchTarget)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column {
            Text(
                text = recapBarTitle(story),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(recapTagRes(story.kind)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecapActions(onDone: () -> Unit, onSeeDetails: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onDone,
            shape = ButtonDefaults.filledTonalShape,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = RecapContentMaxWidth)
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onSeeDetails) {
            Text(stringResource(R.string.recap_action_details) + " ›")
        }
    }
}

@Composable
internal fun RecapCardPage(
    story: RecapStory,
    card: RecapCard,
    recapEnabled: Boolean = true,
    recapFrequency: RecapFrequency = RecapFrequency.BOTH,
    onRecapFrequencyChange: (enabled: Boolean, frequency: RecapFrequency) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bandBackground(card.band)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = RecapContentMaxWidth)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.dimens.xxl, vertical = RecapPageVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (card) {
                is RecapCard.Cover -> CoverCardBody(story, card)
                is RecapCard.Total -> TotalCardBody(story, card)
                is RecapCard.Score -> ScoreCardBody(card)
                is RecapCard.Mover -> MoverCardBody(card)
                is RecapCard.BudgetStreak -> BudgetStreakCardBody(story, card)
                is RecapCard.Streak -> StreakCardBody(card)
                is RecapCard.Limits -> LimitsCardBody(card)
                is RecapCard.Pace -> PaceCardBody(card)
                is RecapCard.Focus -> FocusCardBody(
                    story = story, card = card, recapEnabled = recapEnabled,
                    recapFrequency = recapFrequency, onRecapFrequencyChange = onRecapFrequencyChange,
                )
            }
        }
    }
}

// ── Card bodies ────────────────────────────────────────────────────────────────

@Composable
private fun CoverCardBody(story: RecapStory, card: RecapCard.Cover) {
    val on = bandOnColor(card.band)
    Kicker(text = stringResource(recapTagRes(story.kind)), color = on)
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        text = if (story.kind == RecapKind.MONTHLY) {
            stringResource(R.string.recap_cover_title_month, monthName(story.monthYear))
        } else {
            stringResource(R.string.recap_cover_title_week)
        },
        fontSize = CoverTitleSize,
        lineHeight = CoverTitleSize,
        fontWeight = FontWeight.ExtraBold,
        color = on,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Text(
        text = if (story.kind == RecapKind.MONTHLY) {
            stringResource(R.string.recap_cover_sub_month)
        } else {
            "${story.weekStart.formatDayMonth()} – ${story.weekEnd.formatDayMonth()}"
        },
        style = MaterialTheme.typography.bodyLarge,
        color = on.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.xxl))
    Text(
        text = stringResource(R.string.recap_cover_hint),
        style = MaterialTheme.typography.labelMedium,
        color = on.copy(alpha = 0.6f),
    )
}

@Composable
private fun TotalCardBody(story: RecapStory, card: RecapCard.Total) {
    val on = bandOnColor(card.band)
    Kicker(stringResource(R.string.recap_total_kicker, monthName(story.monthYear).uppercase(Locale.getDefault())), on)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    BigFigure(card.spent.formatMoney(), on)
    card.deltaPercent?.let { pct ->
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        val less = pct <= 0
        RecapPill(
            text = (if (less) "↓ " else "↑ ") + "${kotlin.math.abs(pct)}% " +
                stringResource(R.string.recap_vs, monthName(card.prevMonth ?: story.monthYear.minusMonths(1))),
            tone = if (less) RecapPillTone.GOOD else RecapPillTone.WARN,
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        text = totalSubtitle(card, story),
        style = MaterialTheme.typography.bodyLarge,
        color = on.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun totalSubtitle(card: RecapCard.Total, story: RecapStory): String {
    val prevMonth = card.prevMonth
    val prevTotal = card.prevTotal
    if (card.deltaPercent == null || prevMonth == null || prevTotal == null) {
        return stringResource(R.string.recap_total_sub_none, monthName(story.monthYear))
    }
    val diff = (card.spent - prevTotal).abs().formatMoney()
    return if (card.deltaPercent <= 0) {
        stringResource(R.string.recap_total_sub_less, diff, monthName(prevMonth), prevTotal.formatMoney())
    } else {
        stringResource(R.string.recap_total_sub_more, diff, monthName(prevMonth), prevTotal.formatMoney())
    }
}

@Composable
private fun ScoreCardBody(card: RecapCard.Score) {
    val on = bandOnColor(card.band)
    val accent = scoreBandColor(card.scoreBand)
    Kicker(stringResource(R.string.recap_score_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    ScoreRing(
        fraction = card.score / 100f,
        arcColor = accent,
        trackColor = on.copy(alpha = 0.22f),
        modifier = Modifier.size(RecapRingSize),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${card.score}", fontSize = ScoreNumberSize, fontWeight = FontWeight.ExtraBold, color = on)
            Text(
                scoreBandWord(card.scoreBand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
    card.delta?.let { d ->
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        RecapPill(
            text = (if (d >= 0) "↑ " else "↓ ") + "${kotlin.math.abs(d)} " +
                stringResource(R.string.recap_vs, monthName(card.prevMonth ?: YearMonth.now())),
            tone = if (d >= 0) RecapPillTone.GOOD else RecapPillTone.WARN,
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        text = stringResource(R.string.recap_score_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = on.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MoverCardBody(card: RecapCard.Mover) {
    val on = bandOnColor(card.band)
    val drop = card.delta.signum() < 0
    val accent = if (drop) budgetGoodColor() else wellbeingWarnOn()
    Kicker(stringResource(R.string.recap_mover_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.md)
                .clip(CircleShape)
                .background(Color(card.dotColorArgb)),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(card.category, fontSize = MoverNameSize, fontWeight = FontWeight.Bold, color = on)
    }
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    BigFigure((if (drop) "−" else "+") + card.delta.abs().formatMoney(), accent)
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        text = moverSubtitle(card, drop),
        style = MaterialTheme.typography.bodyLarge,
        color = on.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun moverSubtitle(card: RecapCard.Mover, drop: Boolean): String {
    val prev = card.previousAmount.formatMoney()
    val curr = card.currentAmount.formatMoney()
    val primary = if (drop) {
        stringResource(R.string.recap_mover_sub_drop, card.category, prev, curr)
    } else {
        stringResource(R.string.recap_mover_sub_rise, card.category, prev, curr)
    }
    val second = card.second ?: return primary
    val secondText = if (second.delta.signum() < 0) {
        stringResource(R.string.recap_mover_second_down, second.category, second.delta.abs().formatMoney())
    } else {
        stringResource(R.string.recap_mover_second_up, second.category, second.delta.abs().formatMoney())
    }
    return "$primary $secondText"
}

@Composable
private fun BudgetStreakCardBody(story: RecapStory, card: RecapCard.BudgetStreak) {
    val on = bandOnColor(card.band)
    // §2.4/§2.6: de-flamed. A live current run (≥ 2) shows the motif + a "closed months · this month on
    // track" caption; when the run is 0 but a past run exists, the muted best-run fallback shows instead;
    // below that the report-card panel (under-count, segment bar, safe-to-spend) is unchanged.
    val showCurrent = card.streakMonths >= StreakEngine.MIN_TO_SURFACE
    val showBest = !showCurrent && card.best >= StreakEngine.MIN_TO_SURFACE
    Kicker(stringResource(R.string.recap_budget_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Text(
        text = when {
            showCurrent -> stringResource(R.string.recap_budget_streak_many, card.streakMonths)
            showBest -> stringResource(R.string.recap_streak_best_months, card.best)
            else -> stringResource(R.string.recap_budget_streak_one)
        },
        fontSize = BudgetTitleSize,
        lineHeight = BudgetTitleSize,
        fontWeight = FontWeight.ExtraBold,
        color = on,
        textAlign = TextAlign.Center,
    )
    if (showCurrent || showBest) {
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        StreakMotif(
            filledCount = if (showCurrent) card.streakMonths else card.best,
            showLive = showCurrent && card.liveOnTrack,
            muted = showBest,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = if (showCurrent) {
                val counted = stringResource(R.string.recap_streak_month_counted, card.streakMonths)
                if (card.liveOnTrack) {
                    counted + " · " + stringResource(R.string.recap_streak_month_live, monthName(story.nextMonth))
                } else {
                    counted
                }
            } else {
                stringResource(R.string.recap_streak_best_month_sub)
            },
            style = MaterialTheme.typography.labelMedium,
            color = on.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .padding(MaterialTheme.dimens.lg),
    ) {
        Text(
            stringResource(R.string.recap_budget_panel, card.underCount, card.scopeCount),
            style = MaterialTheme.typography.bodyMedium,
            color = on,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        SegmentBar(card.segments)
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            stringResource(R.string.recap_budget_safe),
            style = MaterialTheme.typography.labelMedium,
            color = on.copy(alpha = 0.7f),
        )
        val safePositive = card.safeToSpend.signum() >= 0
        Text(
            text = (if (safePositive) "+" else "−") + card.safeToSpend.abs().formatMoney(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (safePositive) budgetGoodColor() else wellbeingWarnOn(),
        )
    }
}

@Composable
private fun StreakCardBody(card: RecapCard.Streak) {
    val on = bandOnColor(card.band)
    Kicker(stringResource(R.string.recap_streak_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Text(
        text = streakTitle(card),
        fontSize = BudgetTitleSize,
        lineHeight = BudgetTitleSize,
        fontWeight = FontWeight.ExtraBold,
        color = on,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    StreakMotif(
        filledCount = if (card.isBestRun) card.best else card.current,
        showLive = !card.isBestRun && card.liveOnTrack,
        muted = card.isBestRun,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    Text(
        text = streakSub(card),
        style = MaterialTheme.typography.bodyLarge,
        color = on.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun streakTitle(card: RecapCard.Streak): String {
    val weeks = card.kind == StreakKind.BUDGET_WEEK
    return when {
        card.isBestRun -> stringResource(
            if (weeks) R.string.recap_streak_best_weeks else R.string.recap_streak_best_months, card.best,
        )
        weeks && card.scope != null -> stringResource(R.string.recap_streak_week_scope, card.current, card.scope)
        weeks -> stringResource(R.string.recap_streak_week_all, card.current)
        else -> stringResource(R.string.recap_budget_streak_many, card.current)
    }
}

@Composable
private fun streakSub(card: RecapCard.Streak): String {
    val weeks = card.kind == StreakKind.BUDGET_WEEK
    return when {
        card.isBestRun && weeks && card.scope != null ->
            stringResource(R.string.recap_streak_best_week_scope_sub, card.scope)
        card.isBestRun && weeks -> stringResource(R.string.recap_streak_best_week_sub)
        card.isBestRun -> stringResource(R.string.recap_streak_best_month_sub)
        weeks -> {
            val counted = stringResource(R.string.recap_streak_week_counted, card.current)
            if (card.liveOnTrack) counted + " " + stringResource(R.string.recap_streak_week_live) else counted
        }
        else -> stringResource(R.string.recap_streak_month_counted, card.current)
    }
}

@Composable
private fun LimitsCardBody(card: RecapCard.Limits) {
    val on = bandOnColor(card.band)
    Kicker(stringResource(R.string.recap_limits_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    BigFigure(stringResource(R.string.recap_of, card.underCount, card.totalCount), on)
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        stringResource(R.string.recap_limits_sub, card.underCount, card.totalCount),
        style = MaterialTheme.typography.bodyLarge,
        color = on.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        card.chips.forEach { chip -> LimitChipRow(chip) }
    }
}

@Composable
private fun LimitChipRow(chip: RecapLimitChip) {
    // §1.3 / no-loss-framing: under cap = good (green); AT or over cap = warm amber, never red — the
    // state is "reached", not "failed" (the user set this number themselves). A neutral chip surface
    // keeps it legible on the WARN band, with the good/warn accent carried by the label.
    val accent = if (chip.under) budgetGoodColor() else budgetWarnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(chip.emoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            stringResource(R.string.recap_limits_chip, chip.label, chip.bought, chip.cap),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

@Composable
private fun PaceCardBody(card: RecapCard.Pace) {
    val on = bandOnColor(card.band)
    Kicker(stringResource(R.string.recap_pace_kicker), on)
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    BigFigure(card.spent.formatMoney(), on)
    card.deltaPercent?.let { pct ->
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        val less = pct <= 0
        val arrow = if (less) "↓ " else "↑ "
        RecapPill(
            text = arrow + "${kotlin.math.abs(pct)}% " + stringResource(R.string.recap_vs_last_week),
            tone = if (less) RecapPillTone.GOOD else RecapPillTone.WARN,
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .padding(MaterialTheme.dimens.lg),
    ) {
        val budget = card.weeklyBudget
        if (budget != null && budget.signum() > 0) {
            Text(
                stringResource(R.string.recap_pace_budget, card.spent.formatMoney(), budget.formatMoney()),
                style = MaterialTheme.typography.bodyMedium,
                color = on,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            PaceBar(fraction = card.fractionUsed, over = card.spent > budget)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            val over = card.spent > budget
            Text(
                text = if (over) {
                    stringResource(R.string.recap_pace_over, (card.spent - budget).formatMoney())
                } else {
                    stringResource(R.string.recap_pace_left, card.remaining.formatMoney())
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (over) wellbeingWarnOn() else budgetGoodColor(),
            )
        } else {
            Text(
                stringResource(R.string.recap_pace_nobudget),
                style = MaterialTheme.typography.bodyMedium,
                color = on.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun FocusCardBody(
    story: RecapStory,
    card: RecapCard.Focus,
    recapEnabled: Boolean = true,
    recapFrequency: RecapFrequency = RecapFrequency.BOTH,
    onRecapFrequencyChange: (enabled: Boolean, frequency: RecapFrequency) -> Unit = { _, _ -> },
) {
    val on = bandOnColor(card.band)
    Kicker(
        text = if (card.isWeekly) {
            stringResource(R.string.recap_focus_kicker_week)
        } else {
            stringResource(R.string.recap_focus_kicker_month, monthName(story.nextMonth).uppercase(Locale.getDefault()))
        },
        color = on,
    )
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Text(
        text = focusTitle(card.focus),
        fontSize = FocusTitleSize,
        lineHeight = FocusTitleSize,
        fontWeight = FontWeight.ExtraBold,
        color = on,
        textAlign = TextAlign.Center,
    )
    if (card.isWeekly) {
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            stringResource(R.string.recap_focus_week_note),
            style = MaterialTheme.typography.bodyMedium,
            color = on.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        // §1.4: the in-story off-switch that makes weekly-by-default safe — low-emphasis, weekly only,
        // NEVER on the monthly story. Writes settings immediately (see [RecapFrequencyRow]).
        Spacer(Modifier.height(MaterialTheme.dimens.xl))
        RecapFrequencyRow(
            onColor = on,
            recapEnabled = recapEnabled,
            recapFrequency = recapFrequency,
            onChange = onRecapFrequencyChange,
        )
    }
    // Leaves room above the fixed Done / See details actions.
    Spacer(Modifier.height(MaterialTheme.dimens.xxxl))
}

/**
 * §1.4 in-story frequency control: a low-emphasis "Weekly recaps · Change" row on the weekly Focus
 * card. Tapping it opens the [AdaptiveSheet] frequency picker (a centred dialog on tablet). Choices
 * are written immediately via [onChange]; the gate latches the current story so this can't tear it
 * down mid-read. Discoverable but never shouty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecapFrequencyRow(
    onColor: Color,
    recapEnabled: Boolean,
    recapFrequency: RecapFrequency,
    onChange: (enabled: Boolean, frequency: RecapFrequency) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
            .clickable { open = true }
            .padding(horizontal = MaterialTheme.dimens.sm, vertical = MaterialTheme.dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.recap_freq_row_label),
            style = MaterialTheme.typography.labelLarge,
            color = onColor.copy(alpha = 0.6f),
        )
        Text(
            " · ",
            style = MaterialTheme.typography.labelLarge,
            color = onColor.copy(alpha = 0.35f),
        )
        Text(
            stringResource(R.string.recap_freq_change),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = onColor.copy(alpha = 0.9f),
            textDecoration = TextDecoration.Underline,
        )
    }
    if (open) {
        AdaptiveSheet(onDismiss = { open = false }) {
            RecapFrequencySheetContent(
                recapEnabled = recapEnabled,
                recapFrequency = recapFrequency,
                onSelect = { enabled, frequency ->
                    onChange(enabled, frequency)
                    open = false
                },
            )
        }
    }
}

/**
 * The four-option recap-frequency picker body (Weekly / Monthly / Both / Off), shown inside an
 * [AdaptiveSheet]. Stateless: [recapEnabled] + [recapFrequency] mark the current selection (Off when
 * disabled), and [onSelect] hands back the (enabled, frequency) to persist — Off keeps the remembered
 * cadence and just turns the recap off. Extracted so it renders standalone for screenshot goldens.
 */
@Composable
internal fun RecapFrequencySheetContent(
    recapEnabled: Boolean,
    recapFrequency: RecapFrequency,
    onSelect: (enabled: Boolean, frequency: RecapFrequency) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
    ) {
        Text(
            stringResource(R.string.recap_freq_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            stringResource(R.string.recap_freq_sheet_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        RecapFrequencyOption(
            label = stringResource(R.string.budget_period_weekly),
            selected = recapEnabled && recapFrequency == RecapFrequency.WEEKLY,
            onClick = { onSelect(true, RecapFrequency.WEEKLY) },
        )
        RecapFrequencyOption(
            label = stringResource(R.string.budget_period_monthly),
            selected = recapEnabled && recapFrequency == RecapFrequency.MONTHLY,
            onClick = { onSelect(true, RecapFrequency.MONTHLY) },
        )
        RecapFrequencyOption(
            label = stringResource(R.string.recap_freq_both),
            selected = recapEnabled && recapFrequency == RecapFrequency.BOTH,
            onClick = { onSelect(true, RecapFrequency.BOTH) },
        )
        RecapFrequencyOption(
            label = stringResource(R.string.recap_freq_off),
            selected = !recapEnabled,
            onClick = { onSelect(false, recapFrequency) },
        )
    }
}

@Composable
private fun RecapFrequencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun focusTitle(focus: RecapFocus): String = when (focus) {
    is RecapFocus.CapCategory -> stringResource(R.string.recap_focus_cap, focus.category, focus.amount.formatMoney())
    is RecapFocus.WatchCategory -> stringResource(R.string.recap_focus_watch, focus.category)
    is RecapFocus.SetBudget -> stringResource(R.string.recap_focus_setbudget, focus.category)
    RecapFocus.KeepItUp -> stringResource(R.string.recap_focus_keep)
}

// ── Shared bits ───────────────────────────────────────────────────────────────

@Composable
private fun Kicker(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = color.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BigFigure(text: String, color: Color) {
    Text(
        text = text,
        fontSize = BigFigureSize,
        lineHeight = BigFigureSize,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RecapPill(text: String, tone: RecapPillTone) {
    val accent = when (tone) {
        RecapPillTone.GOOD -> budgetGoodColor()
        RecapPillTone.WARN -> wellbeingWarnOn()
        RecapPillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun SegmentBar(segments: List<RecapSegStatus>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.progressHeight),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        segments.forEach { seg ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(segColor(seg)),
            )
        }
    }
}

@Composable
private fun segColor(status: RecapSegStatus): Color = when (status) {
    RecapSegStatus.GOOD -> budgetGoodColor()
    RecapSegStatus.WARN -> budgetWarnColor()
    RecapSegStatus.BAD -> wellbeingWarnOn()
}

@Composable
private fun PaceBar(fraction: Float, over: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.progressHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(if (over) wellbeingWarnOn() else budgetGoodColor()),
        )
    }
}

@Composable
private fun bandBackground(band: RecapBand): Color = when (band) {
    RecapBand.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    RecapBand.GOOD -> wellbeingGoodContainer()
    RecapBand.WARN -> wellbeingWarnContainer()
    RecapBand.GREAT -> wellbeingGreatContainer()
    RecapBand.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    RecapBand.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
private fun bandOnColor(band: RecapBand): Color = when (band) {
    RecapBand.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    RecapBand.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun scoreBandColor(band: WellbeingBand): Color = when (band) {
    WellbeingBand.NEEDS_WORK -> wellbeingWarnOn()
    WellbeingBand.GETTING_THERE -> budgetWarnColor()
    WellbeingBand.HEALTHY -> budgetGoodColor()
    WellbeingBand.THRIVING -> budgetGreatColor()
}

@Composable
private fun scoreBandWord(band: WellbeingBand): String = stringResource(
    when (band) {
        WellbeingBand.NEEDS_WORK -> R.string.wellbeing_band_needs_work
        WellbeingBand.GETTING_THERE -> R.string.wellbeing_band_getting_there
        WellbeingBand.HEALTHY -> R.string.wellbeing_band_healthy
        WellbeingBand.THRIVING -> R.string.wellbeing_band_thriving
    },
)

@Composable
private fun recapBarTitle(story: RecapStory): String =
    if (story.kind == RecapKind.MONTHLY) monthName(story.monthYear) else stringResource(R.string.recap_title_week)

@StringRes
private fun recapTagRes(kind: RecapKind): Int =
    if (kind == RecapKind.MONTHLY) R.string.recap_tag_monthly else R.string.recap_tag_weekly

private fun monthName(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

/**
 * The (kind, length) to log for a surfaced streak card, or null when the card isn't a streak surfacing.
 * The weekly [RecapCard.Streak] always counts (its best when it's a best-run fallback); the monthly
 * [RecapCard.BudgetStreak] counts only once its run — or its personal best — clears the ≥2 bar (§2.7).
 */
private fun streakSurfaceOf(card: RecapCard): Pair<StreakKind, Int>? = when (card) {
    is RecapCard.Streak -> card.kind to if (card.isBestRun) card.best else card.current
    is RecapCard.BudgetStreak -> when {
        card.streakMonths >= StreakEngine.MIN_TO_SURFACE -> StreakKind.BUDGET_MONTH to card.streakMonths
        card.best >= StreakEngine.MIN_TO_SURFACE -> StreakKind.BUDGET_MONTH to card.best
        else -> null
    }
    else -> null
}

/** Neutral full-screen hold shown by the gate while the story loads, so Home never flashes first. */
@Composable
fun RecapLoadingBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

/** Padding above/below the centred card content, leaving room for the top chrome and bottom actions. */
private val RecapPageVerticalPadding = 96.dp

// ── Previews ──────────────────────────────────────────────────────────────────

private fun previewMonthly(): RecapStory = RecapStory(
    kind = RecapKind.MONTHLY,
    monthYear = YearMonth.of(2026, 7),
    nextMonth = YearMonth.of(2026, 8),
    weekStart = java.time.LocalDate.of(2026, 7, 1),
    weekEnd = java.time.LocalDate.of(2026, 7, 31),
    cards = listOf(
        RecapCard.Cover(RecapBand.PRIMARY),
        RecapCard.Total(
            RecapBand.GOOD, BigDecimal("1240"), -12,
            YearMonth.of(2026, 6), BigDecimal("1409"), improved = true,
        ),
        RecapCard.Score(RecapBand.SECONDARY, 72, WellbeingBand.HEALTHY, 4, YearMonth.of(2026, 6)),
        RecapCard.Mover(
            RecapBand.NEUTRAL, "Dining", 0xFFEF5350.toInt(), BigDecimal("-40"),
            BigDecimal("120"), BigDecimal("80"), RecapSecondMover("Groceries", BigDecimal("25")),
        ),
        RecapCard.BudgetStreak(
            band = RecapBand.GREAT, streakMonths = 3, best = 6, liveOnTrack = true,
            underCount = 5, scopeCount = 6,
            segments = listOf(
                RecapSegStatus.GOOD, RecapSegStatus.GOOD, RecapSegStatus.WARN,
                RecapSegStatus.GOOD, RecapSegStatus.GOOD, RecapSegStatus.BAD,
            ),
            safeToSpend = BigDecimal("60"),
        ),
        RecapCard.Limits(
            RecapBand.WARN, 3, 4,
            listOf(RecapLimitChip("🥤", "Coke", 2, 4), RecapLimitChip("🍫", "Chocolate", 5, 3)),
        ),
        RecapCard.Focus(RecapBand.PRIMARY, RecapFocus.CapCategory("Dining", BigDecimal("150")), isWeekly = false),
    ),
)

@Preview(name = "Recap · Monthly cover", widthDp = 360, heightDp = 740)
@Composable
private fun RecapMonthlyPreview() {
    BudgettyTheme { RecapStoryScreen(story = previewMonthly(), onClose = {}, onSeeDetails = {}) }
}

@Preview(name = "Recap · Monthly (dark)", widthDp = 360, heightDp = 740)
@Composable
private fun RecapMonthlyDarkPreview() {
    BudgettyTheme(darkTheme = true) { RecapStoryScreen(story = previewMonthly(), onClose = {}, onSeeDetails = {}) }
}

@Preview(name = "Recap · Weekly", widthDp = 360, heightDp = 740)
@Composable
private fun RecapWeeklyPreview() {
    val story = RecapStory(
        kind = RecapKind.WEEKLY,
        monthYear = YearMonth.of(2026, 7),
        nextMonth = YearMonth.of(2026, 7),
        weekStart = java.time.LocalDate.of(2026, 7, 21),
        weekEnd = java.time.LocalDate.of(2026, 7, 27),
        cards = listOf(
            RecapCard.Cover(RecapBand.PRIMARY),
            RecapCard.Pace(RecapBand.GOOD, BigDecimal("280"), BigDecimal("300"), 0.93f, 1f, BigDecimal("20"), -8),
            RecapCard.Limits(
                RecapBand.WARN, 3, 4,
                listOf(RecapLimitChip("🥤", "Coke", 2, 4), RecapLimitChip("🍕", "Takeaway", 4, 4)),
            ),
            RecapCard.Streak(
                band = RecapBand.SECONDARY, kind = StreakKind.BUDGET_WEEK, scope = "Groceries",
                current = 3, best = 4, liveOnTrack = true, isBestRun = false,
            ),
            RecapCard.Focus(RecapBand.PRIMARY, RecapFocus.WatchCategory("Dining"), isWeekly = true),
        ),
    )
    BudgettyTheme { RecapStoryScreen(story = story, onClose = {}, onSeeDetails = {}) }
}

@Preview(name = "Recap · tablet", widthDp = 840, heightDp = 720)
@Composable
private fun RecapTabletPreview() {
    BudgettyTheme { RecapStoryScreen(story = previewMonthly(), onClose = {}, onSeeDetails = {}) }
}
