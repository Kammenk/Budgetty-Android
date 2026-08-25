package com.budgetty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgetty.app.R

/**
 * Budgetty's shared "planned, not yet real" visual language — one hatch texture, so a diagonal-hatch
 * fill reads as "planned bills, not money I actually spent" identically on Home (the spent-vs-planned
 * strip / [com.budgetty.app.ui.home] `SpentPlannedBar`) and in the Insights recurring-bills overlay.
 *
 * The hatch is deliberately drawn on a **neutral muted token** (`outlineVariant`), never a category
 * hue and never a status colour, so "planned" never reads as "spent", "good" or "over".
 */

/** Fills the current draw bounds with thin diagonal stripes — the shared planned texture. Callers set
 *  the [color] (typically `outlineVariant`), the [spacing] between stripes and the [stroke] width. */
fun DrawScope.drawPlannedHatch(color: Color, spacing: Dp, stroke: Dp) {
    val step = spacing.toPx()
    val strokePx = stroke.toPx()
    val h = size.height
    var x = -h
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, h),
            end = Offset(x + h, 0f),
            strokeWidth = strokePx,
        )
        x += step
    }
}

/** A small square legend key: a solid accent square for real (receipt-backed) spend, or the planned
 *  hatch in a muted outline for planned bills. 10dp by default. */
@Composable
fun PlannedSwatch(hatched: Boolean, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(3.dp)
    if (hatched) {
        Box(
            modifier
                .size(size)
                .clip(shape)
                .drawBehind { drawPlannedHatch(outline, spacing = 3.dp, stroke = 1.dp) }
                .border(1.dp, outline, shape),
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(shape)
                .background(primary),
        )
    }
}

/**
 * The quiet, tappable "Planned" badge shown in an Insights section header (Breakdown / Summary /
 * Trend) when the recurring-bills overlay is on. It doubles as the overlay's legend — a hatch swatch
 * plus a one-word label — and as the entry point to that section's read-only explanatory dialog.
 *
 * Deliberately styled as a status chip, **not** a button (design "option A"): the same muted fill and
 * text as the period chip beside it, so it reads honestly as a legend rather than out-shouting the
 * real controls. The ink is small, so a full [minimumInteractiveComponentSize] touch target is
 * reserved around it — a mark this quiet needs its 48dp target to do the work the visual doesn't.
 */
@Composable
fun PlannedBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.insights_planned_badge)
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlannedSwatch(hatched = true, size = 9.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
