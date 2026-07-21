package com.budgetty.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.budgetty.app.R
import com.budgetty.app.ui.navigation.Routes
import java.math.BigDecimal
import kotlin.math.abs

private val CompactSize = DpSize(150.dp, 150.dp)
private val LargeSize = DpSize(250.dp, 120.dp)

/**
 * The This Week widget: this Mon–Sun week's spend with a ▲/▼ change vs last week, and — on the Large
 * size — a two-bar comparison of this week against last week. Mirrors the Home "This week" stat.
 */
class ThisWeekWidget : BudgettyGlanceWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))

    @Composable
    override fun Content(data: WidgetData) = ThisWeekWidgetContent(data)
}

@Composable
private fun ThisWeekWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val large = LocalSize.current.width >= 200.dp
    val symbol = data.currencySymbol
    val pct = data.vsLastWeekPercent

    WidgetScaffold(dest = Routes.INSIGHTS, padding = if (large) 16.dp else 14.dp) {
        // Header: trend chip + "This week", with the week's date range trailing.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(R.drawable.widget_ic_trend, size = if (large) 28.dp else 24.dp)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.widget_this_week),
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (large) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = data.weekLabel,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
            )
        }

        // Hero figure + change line, vertically centred.
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = data.weeklySpent.widgetWhole(symbol),
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = if (large) 34.sp else 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        if (pct != null) {
            val arrow = if (pct > 0) "▲" else if (pct < 0) "▼" else "•"
            val deltaColor = when {
                pct > 0 -> WidgetColors.bad
                pct < 0 -> WidgetColors.good
                else -> WidgetColors.onSurfaceVariant
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_vs_last_week, "$arrow ${abs(pct)}%"),
                maxLines = 1,
                style = TextStyle(color = deltaColor, fontSize = if (large) 13.sp else 11.sp, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(GlanceModifier.defaultWeight())

        // Large size: a two-bar "this week vs last week" comparison, scaled to the larger of the two.
        // Grouped in a nested Column so this block counts as ONE child of the scaffold: Glance caps a
        // container at 10 elements (Spacers included), which the flat layout would exceed here.
        if (large && pct != null) {
            val maxVal = data.weeklySpent.max(data.lastWeekTotal).toFloat().coerceAtLeast(0.0001f)
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(1.dp).cornerRadius(0.dp)
                        .background(WidgetColors.track),
                ) {}
                Spacer(GlanceModifier.height(9.dp))
                ComparisonBar(
                    label = context.getString(R.string.widget_this_week),
                    amount = data.weeklySpent.widgetWhole(symbol),
                    ratio = (data.weeklySpent.toFloat() / maxVal),
                    barColor = WidgetColors.brand,
                    emphasized = true,
                )
                Spacer(GlanceModifier.height(7.dp))
                ComparisonBar(
                    label = context.getString(R.string.widget_last_week),
                    amount = data.lastWeekTotal.widgetWhole(symbol),
                    ratio = (data.lastWeekTotal.toFloat() / maxVal),
                    barColor = WidgetColors.onSurfaceVariant,
                    emphasized = false,
                )
            }
        }
    }
}

/** One labelled comparison row: week label, a proportional bar, and the amount. */
@Composable
private fun ComparisonBar(
    label: String,
    amount: String,
    ratio: Float,
    barColor: androidx.glance.unit.ColorProvider,
    emphasized: Boolean,
) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            maxLines = 1,
            modifier = GlanceModifier.width(62.dp),
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
        )
        Spacer(GlanceModifier.width(8.dp))
        WidgetBar(ratio = ratio, color = barColor, modifier = GlanceModifier.defaultWeight(), height = 7.dp)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = amount,
            maxLines = 1,
            style = TextStyle(
                color = if (emphasized) WidgetColors.onSurface else WidgetColors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}
