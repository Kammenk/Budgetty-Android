package com.budgetty.app.ui.savings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import java.math.BigDecimal
import java.math.RoundingMode

/** Emoji offered in the goal create/edit sheet — a small, goal-flavoured curated set. */
val SAVINGS_EMOJIS: List<String> = listOf(
    "🎯", "💰", "🐷", "🛟", "💻", "📱", "✈️", "🏖️", "🚗", "🏠",
    "🎓", "💍", "🎁", "🎮", "🚴", "🛋️", "👶", "🏥", "📚", "🎸",
)

/** The ring/pace colour: amber when behind the target-date pace, positive green otherwise. */
@Composable
fun savingsProgressColor(behind: Boolean): Color =
    if (behind) budgetWarnColor() else budgetGoodColor()

/**
 * A circular progress ring (donut) at [strokeWidth], filled [fraction] of the way in [color] over a
 * muted track, with [content] centred inside (the % or a check). Sweeps clockwise from the top.
 */
@Composable
fun SavingsRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val sweep = 360f * fraction.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * Small UPPERCASE overline field label shared by the bottom sheets (savings, subscriptions, export,
 * buying limits) — matches the mockups' `text-transform:uppercase` section labels. Uppercasing is done
 * here so callers pass normal title-case strings.
 */
@Composable
fun SavingsSheetLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

val SavingsFieldShape = RoundedCornerShape(12.dp)

@Composable
fun savingsFieldColors(): TextFieldColors {
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest
    return TextFieldDefaults.colors(
        focusedContainerColor = bg,
        unfocusedContainerColor = bg,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )
}

/** Parses a user-entered positive money amount ("1,500" / "1500.50") to 2dp, or null if invalid. */
fun String.toSavingsAmount(): BigDecimal? =
    replace(',', '.').trim().toBigDecimalOrNull()
        ?.takeIf { it.signum() > 0 }
        ?.setScale(2, RoundingMode.HALF_UP)
