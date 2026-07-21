package com.budgetty.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Re-renders every Budgetty widget. The one place that knows the full set of widget types. */
object WidgetRefresh {

    // Its own scope rather than the receiver's `goAsync()`: GlanceAppWidgetReceiver already claims
    // the broadcast's PendingResult, and goAsync() hands out the *only* one — taking it a second
    // time would return null and strand Glance's own work.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget refresh, for callers on a broadcast thread that can't suspend. */
    fun requestAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch { refreshAll(appContext) }
    }

    suspend fun refreshAll(context: Context) {
        BudgetWidget().updateAll(context)
        SummaryWidget().updateAll(context)
        ThisWeekWidget().updateAll(context)
        ScanWidget().updateAll(context)
        TopCategoriesWidget().updateAll(context)
    }
}
