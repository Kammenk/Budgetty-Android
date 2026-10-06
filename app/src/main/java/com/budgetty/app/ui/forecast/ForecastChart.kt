package com.budgetty.app.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import com.budgetty.app.data.forecast.ForecastResult
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import java.math.BigDecimal

/**
 * The projected-balance curve: a line through the daily balances, a dashed "warn me below" comfort
 * line, a faint zero baseline, and a dot on the lowest point (amber if it dips below comfort, else
 * green). Forward-only — the whole curve is the projection — so it reads as one estimate.
 */
@Composable
fun ForecastChart(
    result: ForecastResult,
    comfortThreshold: BigDecimal,
    modifier: Modifier = Modifier,
) {
    val curveColor = MaterialTheme.colorScheme.primary
    val warnColor = budgetWarnColor()
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val troughColor = if (result.dipsBelowComfort) warnColor else budgetGoodColor()
    val points = result.points
    if (points.size < 2) return

    val balances = points.map { it.balance.toDouble() }
    val comfort = comfortThreshold.toDouble()
    val maxV = (balances.max() * MAX_HEADROOM)
    val minV = minOf(0.0, balances.min(), comfort)
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val lastIndex = points.size - 1
    val troughIndex = points.indexOfFirst { it.date == result.troughDate }.coerceAtLeast(0)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun x(i: Int): Float = if (lastIndex == 0) 0f else w * i / lastIndex
        fun y(v: Double): Float = (h - (v - minV) / range * h).toFloat()

        // Zero baseline (if within range) and the comfort line, both faint/dashed.
        if (minV < 0.0 && maxV > 0.0) {
            drawLine(baselineColor, Offset(0f, y(0.0)), Offset(w, y(0.0)), strokeWidth = 1f)
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        drawLine(
            color = warnColor,
            start = Offset(0f, y(comfort)),
            end = Offset(w, y(comfort)),
            strokeWidth = 2f,
            pathEffect = dash,
        )

        val path = Path().apply {
            moveTo(x(0), y(balances[0]))
            for (i in 1..lastIndex) lineTo(x(i), y(balances[i]))
        }
        drawPath(path, curveColor, style = Stroke(width = 6f))

        drawCircle(
            color = troughColor,
            radius = 10f,
            center = Offset(x(troughIndex), y(result.trough.toDouble())),
        )
        drawCircle(
            color = surfaceColor,
            radius = 4f,
            center = Offset(x(troughIndex), y(result.trough.toDouble())),
        )
    }
}

private const val MAX_HEADROOM = 1.08
