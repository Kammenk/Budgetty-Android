package com.budgetty.app.ui.insights

import com.budgetty.app.ui.theme.dimens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.ui.components.formatDateRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Insights period control: `‹ [pill] ›`. The centre is an elevated "refined pill" — the active
 * unit as a small uppercase eyebrow over the bold period value (a calendar glyph fronts the eyebrow
 * for a custom range) — and tapping it opens a dropdown to pick the stepping unit or a custom range.
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
    fillWidth: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "stepperChevron")

    // Eyebrow over the value: the active unit, or "CUSTOM" for a custom range, shown uppercased.
    val eyebrow = when {
        customSelected -> stringResource(R.string.period_unit_custom)
        selectedUnit != null -> stringResource(selectedUnit.labelRes)
        else -> ""
    }.uppercase(Locale.getDefault())

    // Same background as the Insights cards (InsightCard uses surfaceContainer) in both themes.
    val pillColor = MaterialTheme.colorScheme.surfaceContainer
    val pillShape = RoundedCornerShape(MaterialTheme.dimens.radiusLg)
    val arrowColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onStepBackward, enabled = steppable && canStepBackward, colors = arrowColors) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_period_previous),
            )
        }
        Box(modifier = if (fillWidth) Modifier.fillMaxWidth(0.5f) else Modifier) {
            Row(
                modifier = Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .clip(pillShape)
                    .background(pillColor)
                    .clickable { expanded = true }
                    .padding(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.md, top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
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
                            text = eyebrow,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                if (fillWidth) Spacer(Modifier.weight(1f)) else Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PeriodUnit.entries.forEach { unit ->
                    val isSelected = unit == selectedUnit
                    DropdownMenuItem(
                        modifier = if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        } else {
                            Modifier
                        },
                        text = {
                            Text(
                                text = stringResource(unit.labelRes),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onUnitSelected(unit)
                            expanded = false
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Box(Modifier.size(MaterialTheme.dimens.icon))
                            }
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.period_custom_range),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        onCustomClick()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = if (customSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
        IconButton(onClick = onStepForward, enabled = steppable && canStepForward, colors = arrowColors) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_period_next),
            )
        }
    }
}

/**
 * The stepper's friendly label for [period], reused by the Breakdown sub-label and the category
 * sheet: relative ("This month", "Last week") near the present, absolute ("April 2025", "Q2 2026")
 * further out, and a plain date span for weeks and custom ranges.
 */
@Composable
fun periodFriendlyLabel(period: InsightsPeriod, today: LocalDate = LocalDate.now()): String =
    when (period) {
        is InsightsPeriod.Custom -> formatDateRange(period.start, period.end)
        is InsightsPeriod.Stepped -> {
            val (start, end) = period.bounds(today)
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
