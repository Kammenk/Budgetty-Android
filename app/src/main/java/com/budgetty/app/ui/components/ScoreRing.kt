package com.budgetty.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A 0–1 ring gauge: a full track with a rounded arc filling [fraction] of it, starting at 12 o'clock,
 * with [content] centered inside. Reuses the Insights Breakdown donut's ring language (thin ring,
 * rounded caps) so the Wellbeing score reads as a sibling of the pie. [strokeRatio] is the stroke
 * width as a fraction of the ring's diameter (the design's 4.2/42 ≈ 0.10 for the big ring). A
 * [dashedTrack] renders the track as a dotted outline for the not-yet-scored first-run state.
 */
@Composable
fun ScoreRing(
    fraction: Float,
    arcColor: Color,
    modifier: Modifier = Modifier,
    strokeRatio: Float = 4.2f / 42f,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    dashedTrack: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * strokeRatio
            val diameter = size.minDimension - strokeW
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val trackStroke = if (dashedTrack) {
                Stroke(width = strokeW, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeW * 0.5f, strokeW * 1.4f)))
            } else {
                Stroke(width = strokeW)
            }
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = trackStroke)
            if (fraction > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
