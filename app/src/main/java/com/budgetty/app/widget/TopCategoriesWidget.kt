package com.budgetty.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.navigation.Routes
import java.math.BigDecimal
import kotlin.math.roundToInt

private val CompactSize = DpSize(150.dp, 150.dp)
private val LargeSize = DpSize(250.dp, 120.dp)

/** How many real categories the donut + legend show before rolling the rest into "Other". */
private const val TOP_CATEGORY_SLICES = 4

/**
 * The Top Categories widget: this month's spend split across the biggest categories. The Large size
 * pairs a donut ring (drawn to a [Bitmap], since Glance can't render arcs) with a ranked legend;
 * Compact shows the ranked rows alone.
 */
class TopCategoriesWidget : BudgettyGlanceWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))

    @Composable
    override fun Content(data: WidgetData) = TopCategoriesWidgetContent(data)
}

@Composable
private fun TopCategoriesWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val large = LocalSize.current.width >= 200.dp
    val symbol = data.currencySymbol

    WidgetScaffold(dest = Routes.INSIGHTS, padding = if (large) 16.dp else 14.dp) {
        // Header: pie chip + "Top categories", with the month trailing.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(R.drawable.widget_ic_pie, size = if (large) 26.dp else 24.dp)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.widget_topcat),
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (large) 13.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = data.monthLabel,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
            )
        }

        if (data.topCategories.isEmpty()) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = context.getString(R.string.widget_no_spending),
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
            )
            Spacer(GlanceModifier.defaultWeight())
            return@WidgetScaffold
        }

        // Top categories + an "Other" remainder, so the slices always add up to the month total.
        val total = data.monthTotal.toFloat().coerceAtLeast(0.0001f)
        val shown = data.topCategories.take(TOP_CATEGORY_SLICES)
        val shownSum = shown.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
        val other = (data.monthTotal - shownSum).coerceAtLeast(BigDecimal.ZERO)
        val legend = buildList {
            addAll(shown)
            if (other.signum() > 0) add(WidgetCategory(context.getString(R.string.widget_other), other, Categories.colorOf("Other")))
        }

        Spacer(GlanceModifier.height(if (large) 10.dp else 8.dp))
        if (large) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val px = (104 * context.resources.displayMetrics.density).roundToInt()
                Image(
                    provider = ImageProvider(donutBitmap(legend, px)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(104.dp),
                )
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    legend.forEachIndexed { index, cat ->
                        if (index > 0) Spacer(GlanceModifier.height(6.dp))
                        LegendRow(cat, total, symbol, showPercent = true)
                    }
                }
            }
        } else {
            data.topCategories.take(3).forEachIndexed { index, cat ->
                if (index > 0) Spacer(GlanceModifier.height(7.dp))
                LegendRow(cat, total, symbol, showPercent = false)
            }
        }
    }
}

/** One legend row: color dot, name, optional %, a proportional bar, and the amount. */
@Composable
private fun LegendRow(cat: WidgetCategory, total: Float, symbol: String, showPercent: Boolean) {
    val ratio = (cat.amount.toFloat() / total).coerceIn(0f, 1f)
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Dot(cat.colorArgb, size = 7.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = cat.name,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetColors.onSurface, fontSize = 11.sp),
        )
        if (showPercent) {
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = "${(ratio * 100).roundToInt()}%",
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 10.sp),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        WidgetBar(
            ratio = ratio,
            color = ColorProvider(Color(cat.colorArgb)),
            modifier = GlanceModifier.width(44.dp),
            height = 4.dp,
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = cat.amount.widgetWhole(symbol),
            maxLines = 1,
            style = TextStyle(color = WidgetColors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * Draws the category split as a donut ring to a square bitmap. Arcs use the categories' own colors
 * over a neutral semi-transparent track (so the same bitmap reads on light and dark). No center
 * text — that would need theme-resolved colors the bitmap can't reliably pick.
 */
private fun donutBitmap(legend: List<WidgetCategory>, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val stroke = sizePx * 0.17f
    val pad = stroke / 2f + 1f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.BUTT
    }
    val total = legend.fold(0f) { acc, c -> acc + c.amount.toFloat() }.coerceAtLeast(0.0001f)
    paint.color = 0x33888888.toInt() // neutral track, legible on both themes
    canvas.drawArc(rect, 0f, 360f, false, paint)
    var start = -90f
    legend.forEach { cat ->
        val sweep = cat.amount.toFloat() / total * 360f
        paint.color = cat.colorArgb
        canvas.drawArc(rect, start, sweep, false, paint)
        start += sweep
    }
    return bmp
}
