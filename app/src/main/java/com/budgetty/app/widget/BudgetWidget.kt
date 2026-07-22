package com.budgetty.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
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
import com.budgetty.app.ui.util.budgetRatio
import com.budgetty.app.ui.util.monthlyToWeekly
import com.budgetty.app.ui.util.weeklyToMonthly
import java.math.BigDecimal
import kotlin.math.roundToInt

private val CompactSize = DpSize(150.dp, 150.dp)
private val LargeSize = DpSize(250.dp, 120.dp)

/**
 * The Budget widget: one adaptive widget mirroring the Home budget block. It shows the period the
 * user actually set (Monthly, or Weekly when that alone is set) with a status bar, and on the Large
 * size a "≈ X / week|month" equivalent for the other cadence. Both sizes (Compact 2×2 / Large 4×2)
 * render from one [WidgetData] snapshot, chosen by [LocalSize].
 */
class BudgetWidget : BudgettyGlanceWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))

    @Composable
    override fun Content(data: WidgetData) = BudgetWidgetContent(data)
}

@Composable
private fun BudgetWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val large = LocalSize.current.width >= 200.dp
    val monthly = data.budgetIsMonthly

    val budget = if (monthly) data.monthlyBudget else data.weeklyBudget
    val spent = if (monthly) data.monthlySpent else data.weeklySpent
    val iconRes = if (monthly) R.drawable.widget_ic_monthly else R.drawable.widget_ic_weekly
    val periodLabel =
        if (monthly) data.monthLabel else context.getString(R.string.widget_this_week)

    WidgetScaffold(dest = Routes.BUDGET, padding = if (large) 16.dp else 14.dp) {
        // Header: icon chip + "Budget", with the active-period label pushed to the trailing edge.
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(iconRes, size = if (large) 28.dp else 24.dp)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.widget_budget),
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (large) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = periodLabel,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
            )
        }

        if (budget.signum() <= 0) {
            // No top-level budget set anywhere → invite the user to set one.
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = context.getString(R.string.widget_set_budget_title),
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (large) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (large) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = context.getString(R.string.widget_set_budget_body),
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            return@WidgetScaffold
        }

        val ratio = budgetRatio(spent, budget)
        val pct = (ratio * 100).roundToInt()
        val remaining = (budget - spent).max(BigDecimal.ZERO)
        val status = statusColor(ratio)
        val symbol = data.currencySymbol
        val leftText = context.getString(R.string.widget_remaining, remaining.widgetWhole(symbol))

        // Hero spend figure, vertically centred between the header and the footer.
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = spent.widgetWhole(symbol),
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = if (large) 34.sp else 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))
        // "of <budget>" on both sizes; the Large size appends the derived "≈ X / week|month" hint.
        val ofBudget = context.getString(R.string.widget_of_budget, budget.widgetWhole(symbol))
        val subText = if (large) {
            val equivalent = if (monthly) monthlyToWeekly(budget) else weeklyToMonthly(budget)
            val approxRes = if (monthly) R.string.widget_approx_weekly else R.string.widget_approx_monthly
            "$ofBudget  ·  ${context.getString(approxRes, equivalent.widgetWhole(symbol))}"
        } else {
            ofBudget
        }
        Text(
            text = subText,
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurfaceVariant,
                fontSize = if (large) 13.sp else 11.sp,
            ),
        )
        Spacer(GlanceModifier.defaultWeight())

        // Footer: Large shows "% spent" + remaining above the bar; Compact leads with the bar, then
        // a "% · remaining" line — mirroring the design's per-size emphasis.
        if (large) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = context.getString(R.string.widget_pct_spent, pct),
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = leftText,
                    maxLines = 1,
                    style = TextStyle(color = status, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            WidgetBar(ratio = ratio, color = status, modifier = GlanceModifier.fillMaxWidth(), height = 8.dp)
        } else {
            WidgetBar(ratio = ratio, color = status, modifier = GlanceModifier.fillMaxWidth(), height = 7.dp)
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$pct%",
                    style = TextStyle(color = status, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = leftText,
                    maxLines = 1,
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
                )
            }
        }
    }
}
