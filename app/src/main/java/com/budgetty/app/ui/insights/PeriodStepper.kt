package com.budgetty.app.ui.insights

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.ui.components.formatDateRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Insights period control: one fully-rounded pill holding `‹ [label] ›` — the step arrows at its
 * edges and the bold period value centred (a calendar glyph fronts a custom range). Tapping the label
 * opens a dropdown to pick the stepping unit or a custom range.
 * The arrows walk an [InsightsPeriod.Stepped] window one [unit] at a time; they're disabled while a
 * custom range is active ([steppable] = false), the forward arrow is disabled at the current period
 * ([canStepForward] = false), and the back arrow is disabled once the earliest recorded data is
 * reached ([canStepBackward] = false). The pill opens the menu in every state, so a custom range can
 * always switch back to a unit.
 */
@Composable
fun PeriodStepper(
    label: String,
    steppable: Boolean,
    canStepForward: Boolean,
    canStepBackward: Boolean,
    selectedUnit: PeriodUnit?,
    customSelected: Boolean,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onUnitSelected: (PeriodUnit) -> Unit,
    onCustomClick: () -> Unit,
    onAllTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // One fully-rounded pill (same surface as the Insights cards) holding both step arrows and the
    // centre label; tapping the label opens the unit / custom-range menu, matching the Hybrid mockup.
    val pillColor = MaterialTheme.colorScheme.surfaceContainer
    val pillShape = RoundedCornerShape(percent = 50)
    val arrowColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(pillColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onStepBackward,
            enabled = steppable && canStepBackward,
            colors = arrowColors,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_period_previous),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .clickable { expanded = true }
                .padding(vertical = MaterialTheme.dimens.sm),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (customSelected) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(MaterialTheme.dimens.md),
                    )
                    Spacer(Modifier.width(MaterialTheme.dimens.xs))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            PeriodMenu(
                expanded = expanded,
                selectedUnit = selectedUnit,
                customSelected = customSelected,
                onUnitSelected = onUnitSelected,
                onAllTimeClick = onAllTimeClick,
                onCustomClick = onCustomClick,
                onDismiss = { expanded = false },
            )
        }
        IconButton(
            onClick = onStepForward,
            enabled = steppable && canStepForward,
            colors = arrowColors,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_period_next),
            )
        }
    }
}

/** The period control's dropdown: the stepping units, then All-time and a custom range. */
@Composable
private fun PeriodMenu(
    expanded: Boolean,
    selectedUnit: PeriodUnit?,
    customSelected: Boolean,
    onUnitSelected: (PeriodUnit) -> Unit,
    onAllTimeClick: () -> Unit,
    onCustomClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PeriodUnit.entries.forEach { unit ->
            val isSelected = unit == selectedUnit
            DropdownMenuItem(
                modifier = if (isSelected) Modifier.background(primary.copy(alpha = 0.10f)) else Modifier,
                text = {
                    Text(
                        text = stringResource(unit.labelRes),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                onClick = { onUnitSelected(unit); onDismiss() },
                leadingIcon = {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = primary)
                    } else {
                        Box(Modifier.size(MaterialTheme.dimens.icon))
                    }
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.period_all_time), color = primary, fontWeight = FontWeight.SemiBold)
            },
            onClick = { onAllTimeClick(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = primary) },
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.period_custom_range), color = primary, fontWeight = FontWeight.SemiBold)
            },
            onClick = { onCustomClick(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = primary) },
            trailingIcon = if (customSelected) {
                { Icon(Icons.Filled.Check, contentDescription = null, tint = primary) }
            } else {
                null
            },
        )
    }
}

/**
 * The stepper's friendly label for [period], reused by the Breakdown sub-label and the category
 * sheet: relative ("This month", "Last week") near the present, absolute ("April 2025", "Q2 2026")
 * further out, and a plain date span for weeks and custom ranges.
 */
@Composable
fun periodFriendlyLabel(period: InsightsPeriod, today: LocalDate = LocalDate.now(), monthStartDay: Int = 1): String =
    when (period) {
        is InsightsPeriod.Custom -> formatDateRange(period.start, period.end)
        is InsightsPeriod.Stepped -> {
            val (start, end) = period.bounds(today, monthStartDay = monthStartDay)
            when (period.unit) {
                PeriodUnit.WEEK -> when (period.offset) {
                    0 -> stringResource(R.string.period_this_week)
                    -1 -> stringResource(R.string.period_last_week)
                    else -> formatDateRange(start, end)
                }
                PeriodUnit.MONTH -> when (period.offset) {
                    0 -> stringResource(R.string.period_this_month)
                    -1 -> stringResource(R.string.period_last_month)
                    else -> monthYearLabel(start, today.year)
                }
                PeriodUnit.QUARTER -> when (period.offset) {
                    0 -> stringResource(R.string.period_this_quarter)
                    -1 -> stringResource(R.string.period_last_quarter)
                    else -> stringResource(R.string.period_quarter_format, quarterOf(start.monthValue), start.year)
                }
                PeriodUnit.HALF_YEAR -> when (period.offset) {
                    0 -> stringResource(R.string.period_this_half)
                    -1 -> stringResource(R.string.period_last_half)
                    else -> stringResource(R.string.period_half_format, if (start.monthValue <= 6) 1 else 2, start.year)
                }
            }
        }
    }

/** Quarter number (1–4) of [month]. */
private fun quarterOf(month: Int): Int = (month - 1) / 3 + 1

/** Standalone month name, with the year appended only when it isn't [currentYear]. */
private fun monthYearLabel(date: LocalDate, currentYear: Int): String {
    val pattern = if (date.year == currentYear) "LLLL" else "LLLL yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
