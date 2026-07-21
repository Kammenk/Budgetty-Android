package com.budgetty.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Base for all ten receivers: re-renders every widget whenever one is removed. A removal frees a
 * free-tier slot ([WidgetQuota]), which can bring an over-cap widget back from its locked card —
 * and that widget won't redraw on its own, because nothing about *it* changed.
 */
abstract class BudgettyWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetRefresh.requestAll(context)
    }
}

/**
 * One receiver per widget type × size. Large and Compact for a type point at the same
 * [GlanceAppWidget]; they differ only in their `appwidget-provider` default size (res/xml), so the
 * picker can pin each as a distinct preset while the widget still renders responsively if resized.
 */
class BudgetLargeReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

class BudgetCompactReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

class SummaryLargeReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}

class SummaryCompactReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}

class ThisWeekLargeReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThisWeekWidget()
}

class ThisWeekCompactReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThisWeekWidget()
}

class ScanLargeReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScanWidget()
}

class ScanCompactReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScanWidget()
}

class TopCategoriesLargeReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopCategoriesWidget()
}

class TopCategoriesCompactReceiver : BudgettyWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopCategoriesWidget()
}
