package com.budgetty.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * The free tier's home-screen widget cap.
 *
 * Everything here is derived live from [AppWidgetManager] rather than stored: the set of placed
 * widgets *is* the state. That's what makes the cap self-healing — removing a widget from the home
 * screen frees its slot immediately, with nothing to keep in sync and nothing to migrate.
 *
 * Slots are counted per placed **instance**, across every type and size: two Budget widgets on two
 * home screens use both free slots.
 *
 * Android has no way to refuse a placement (the system widget picker bypasses the app entirely), so
 * the cap is enforced at *render* time — an instance past the cap draws the locked card from
 * [com.budgetty.app.widget.LockedWidgetContent] instead of its data. The in-app picker's button is a
 * courtesy gate on top of that, not the enforcement.
 */
object WidgetQuota {

    /** Widget instances a free user may have placed at once. Premium is uncapped. */
    const val FREE_LIMIT = 2

    /** Every provider a Budgetty widget can be placed as — 5 types × the Large/Compact presets. */
    private val PROVIDERS: List<Class<*>> = listOf(
        BudgetLargeReceiver::class.java,
        BudgetCompactReceiver::class.java,
        SummaryLargeReceiver::class.java,
        SummaryCompactReceiver::class.java,
        ThisWeekLargeReceiver::class.java,
        ThisWeekCompactReceiver::class.java,
        ScanLargeReceiver::class.java,
        ScanCompactReceiver::class.java,
        TopCategoriesLargeReceiver::class.java,
        TopCategoriesCompactReceiver::class.java,
    )

    /**
     * Every currently-placed widget id, oldest first. Ids are allocated by an incrementing system
     * counter, so ascending order is placement order — which is what makes the cap keep a user's
     * *existing* widgets working when they add one too many, rather than breaking an old one.
     */
    fun placedIds(context: Context): List<Int> {
        val manager = AppWidgetManager.getInstance(context)
        return PROVIDERS
            .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }
            .sorted()
    }

    /** How many widgets are on home screens right now, across all types and sizes. */
    fun placedCount(context: Context): Int = placedIds(context).size

    /** Free slots left, or null when [isPremium] (uncapped — callers show an "unlimited" state). */
    fun remaining(context: Context, isPremium: Boolean): Int? =
        if (isPremium) null else (FREE_LIMIT - placedCount(context)).coerceAtLeast(0)

    /** Whether another widget may be added right now. */
    fun canAddAnother(context: Context, isPremium: Boolean): Boolean =
        isPremium || placedCount(context) < FREE_LIMIT

    /**
     * Whether this instance is past the free cap and must render locked. False for Premium, and
     * false whenever [appWidgetId] isn't (yet) known to [AppWidgetManager] — a widget mid-placement
     * can render before the system registers its id, and locking on that half-known state would
     * flash a lock card on a perfectly legitimate first widget. The next update settles it.
     */
    fun isLocked(context: Context, appWidgetId: Int, isPremium: Boolean): Boolean {
        if (isPremium || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val index = placedIds(context).indexOf(appWidgetId)
        if (index < 0) return false
        return index >= FREE_LIMIT
    }
}
