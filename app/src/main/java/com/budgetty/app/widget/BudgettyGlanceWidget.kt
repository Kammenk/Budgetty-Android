package com.budgetty.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import com.budgetty.app.data.billing.BillingManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base for every Budgetty home-screen widget. It owns the one thing they all share: the free-tier
 * cap check ([WidgetQuota]). An instance past the cap renders [LockedWidgetContent]; everything
 * within it renders its own [Content] from a freshly loaded [WidgetData].
 *
 * Subclasses supply [Content] and their own `sizeMode`; [provideGlance] is final so a new widget
 * type can't accidentally ship uncapped.
 */
abstract class BudgettyGlanceWidget : GlanceAppWidget(), KoinComponent {

    private val dataProvider: WidgetDataProvider by inject()
    private val billingManager: BillingManager by inject()

    /** The widget's own UI. Only called for instances within the cap. */
    @Composable
    protected abstract fun Content(data: WidgetData)

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        // Checked before loading: a locked widget shows no data, so there's nothing to read for it.
        if (WidgetQuota.isLocked(context, appWidgetId, billingManager.isPremium.value)) {
            provideContent { LockedWidgetContent() }
            return
        }
        val data = dataProvider.load()
        provideContent { Content(data) }
    }
}
