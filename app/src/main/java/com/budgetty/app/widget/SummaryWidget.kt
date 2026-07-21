package com.budgetty.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.budgetty.app.R
import com.budgetty.app.ui.navigation.Routes
import java.math.BigDecimal

private val CompactSize = DpSize(150.dp, 150.dp)
private val LargeSize = DpSize(250.dp, 120.dp)

/** "This month" total + vs-last-month + the biggest categories, in both Compact and Large sizes. */
class SummaryWidget : BudgettyGlanceWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))

    @Composable
    override fun Content(data: WidgetData) = SummaryWidgetContent(data)
}

@Composable
private fun SummaryWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val large = LocalSize.current.width >= 200.dp
    val symbol = data.currencySymbol

    WidgetScaffold(dest = Routes.INSIGHTS, padding = if (large) 16.dp else 14.dp) {
        // Header: "This month" + receipt count.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.widget_this_month),
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (large) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            val receiptsText =
                if (large) context.getString(R.string.widget_receipts, data.monthReceiptCount)
                else data.monthReceiptCount.toString()
            Text(
                text = receiptsText,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
            )
        }

        Spacer(GlanceModifier.height(if (large) 10.dp else 6.dp))
        Text(
            text = data.monthTotal.widgetCents(symbol),
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = if (large) 30.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        data.vsLastMonthPercent?.let { pct ->
            val signed = (if (pct > 0) "+" else "") + "$pct%"
            val vsColor = when {
                pct > 0 -> WidgetColors.bad
                pct < 0 -> WidgetColors.good
                else -> WidgetColors.onSurfaceVariant
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (large) context.getString(R.string.widget_vs_last_month, signed) else signed,
                style = TextStyle(color = vsColor, fontSize = if (large) 12.sp else 11.sp),
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

        Spacer(GlanceModifier.height(if (large) 12.dp else 8.dp))
        val topAmount = data.topCategories.first().amount
        val rows = if (large) data.topCategories.take(3) else data.topCategories.take(2)
        // Grouped in a nested Column so the rows + spacers count as ONE child of the scaffold:
        // Glance caps a container at 10 elements (Spacers included), which the flat layout could hit.
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            rows.forEachIndexed { index, category ->
                if (index > 0) Spacer(GlanceModifier.height(if (large) 8.dp else 6.dp))
                CategoryRow(category, topAmount, symbol, large)
            }
        }
    }
}

/** One "top category" row: color dot, name, a proportional bar, and the amount. */
@Composable
private fun CategoryRow(
    category: WidgetCategory,
    topAmount: BigDecimal,
    symbol: String,
    large: Boolean,
) {
    val ratio = if (topAmount.signum() > 0) {
        (category.amount.toDouble() / topAmount.toDouble()).toFloat()
    } else {
        0f
    }
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Dot(category.colorArgb)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = category.name,
            maxLines = 1,
            modifier = GlanceModifier.width(if (large) 72.dp else 60.dp),
            style = TextStyle(color = WidgetColors.onSurface, fontSize = if (large) 12.sp else 11.sp),
        )
        Spacer(GlanceModifier.width(8.dp))
        WidgetBar(
            ratio = ratio,
            color = ColorProvider(Color(category.colorArgb)),
            modifier = GlanceModifier.defaultWeight(),
            height = 6.dp,
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = category.amount.widgetWhole(symbol),
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = if (large) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
