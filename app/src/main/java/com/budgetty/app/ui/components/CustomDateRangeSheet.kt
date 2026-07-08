package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.ceil

/**
 * A bottom sheet (centered dialog on tablets) for picking a start/end day. Unlike Material's
 * [androidx.compose.material3.DateRangePicker] — which shows every month in one long scroll — this
 * shows a single month at a time with `<`/`>` navigation, matching the design handoff. "Apply" is
 * enabled only once both ends are chosen, and reports the inclusive [LocalDate] range through
 * [onConfirm].
 *
 * Tapping begins a new selection, the next tap completes it (order-safe: a tap before the current
 * start swaps the ends), and a tap once a full range exists starts over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeSheet(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = Locale.getDefault()

    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var visibleMonth by remember {
        mutableStateOf(YearMonth.from(initialStart ?: initialEnd ?: LocalDate.now()))
    }
    val canApply = start != null && end != null

    val headerText = when {
        start != null && end != null -> formatDateRange(start!!, end!!)
        start != null -> start!!.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
        else -> stringResource(R.string.date_range_select_hint)
    }

    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl),
        ) {
            // ── Selected-range header ──
            Text(
                text = stringResource(R.string.date_range_selected_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = headerText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))

            // ── Month navigation ──
            val arrowColors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                    colors = arrowColors,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_prev_month),
                    )
                }
                Text(
                    text = visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                    colors = arrowColors,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_month),
                    )
                }
            }

            // ── Weekday header ──
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    Text(
                        text = firstDayOfWeek.plus(i.toLong())
                            .getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xs))

            // ── Calendar grid ──
            val onDayClick: (LocalDate) -> Unit = { date ->
                val s = start
                val e = end
                when {
                    s == null || e != null -> { start = date; end = null }
                    date.isBefore(s) -> { start = date; end = s }
                    else -> { end = date }
                }
            }
            MonthGrid(
                month = visibleMonth,
                firstDayOfWeek = firstDayOfWeek,
                start = start,
                end = end,
                onDayClick = onDayClick,
            )

            Spacer(Modifier.height(MaterialTheme.dimens.xl))

            // ── Actions ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Button(
                    enabled = canApply,
                    onClick = {
                        val s = start
                        val e = end
                        if (s != null && e != null) onConfirm(s, e)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(
                        stringResource(R.string.action_apply),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: java.time.DayOfWeek,
    start: LocalDate?,
    end: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstOfMonth = month.atDay(1)
    // Leading days from the previous month needed to align the 1st under its weekday column.
    val lead = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val gridStart = firstOfMonth.minusDays(lead.toLong())
    val weeks = ceil((lead + month.lengthOfMonth()) / 7f).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + dow).toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        start = start,
                        end = end,
                        onClick = onDayClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    inMonth: Boolean,
    start: LocalDate?,
    end: LocalDate?,
    onClick: (LocalDate) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isStart = start != null && date == start
    val isEnd = end != null && date == end
    val isEndpoint = isStart || isEnd
    val inRange = start != null && end != null &&
        !date.isBefore(start) && !date.isAfter(end)

    // Connecting band drawn behind the day so adjacent in-range cells merge into one bar.
    val fillFull = inRange && !isStart && !isEnd
    val fillTowardEnd = isStart && end != null && date != end
    val fillTowardStart = isEnd && start != null && date != start
    val bandColor = cs.secondaryContainer
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val textColor = when {
        isEndpoint -> cs.onPrimary
        inRange -> cs.onSecondaryContainer
        inMonth -> cs.onSurface
        else -> cs.onSurfaceVariant.copy(alpha = 0.38f)
    }
    val today = LocalDate.now()

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .drawBehind {
                if (!fillFull && !fillTowardEnd && !fillTowardStart) return@drawBehind
                val inset = 2.dp.toPx()
                val top = inset
                val h = size.height - inset * 2
                val half = size.width / 2f
                when {
                    fillFull -> drawRect(bandColor, Offset(0f, top), Size(size.width, h))
                    // Toward the "next day": right in LTR, left in RTL.
                    fillTowardEnd -> if (isRtl) {
                        drawRect(bandColor, Offset(0f, top), Size(half, h))
                    } else {
                        drawRect(bandColor, Offset(half, top), Size(half, h))
                    }
                    fillTowardStart -> if (isRtl) {
                        drawRect(bandColor, Offset(half, top), Size(half, h))
                    } else {
                        drawRect(bandColor, Offset(0f, top), Size(half, h))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val circleModifier = when {
            isEndpoint -> Modifier.background(cs.primary, CircleShape)
            date == today -> Modifier.border(1.dp, cs.primary, CircleShape)
            else -> Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
                .then(circleModifier)
                .clickable { onClick(date) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isEndpoint) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/** "1 Jun – 25 Jun 2026"; the start drops its year when both dates fall in the same year. */
fun formatDateRange(start: LocalDate, end: LocalDate): String {
    val endFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    val startFormatter = DateTimeFormatter.ofPattern(
        if (start.year == end.year) "d MMM" else "d MMM yyyy",
    )
    return "${start.format(startFormatter)} – ${end.format(endFormatter)}"
}
