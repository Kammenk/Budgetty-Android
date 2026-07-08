package com.budgetty.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** The widget types the in-app picker offers. */
enum class WidgetKind { BUDGET, SUMMARY, THIS_WEEK, SCAN, TOP_CATEGORIES }

/**
 * Wraps the launcher's "pin a widget" flow (`requestPinAppWidget`, API 26+). The picker calls
 * [request] for the chosen type + size; [isSupported] gates the button for launchers that don't
 * support programmatic pinning (where the user must long-press the home screen instead).
 */
object WidgetPinning {

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /**
     * Whether at least one instance of the [kind]/[large] widget is currently placed on a home
     * screen. Lets the picker reflect the real launcher state (including widgets added by dragging)
     * instead of an optimistic in-memory flag, so the button is correct across reopens.
     */
    fun isPlaced(context: Context, kind: WidgetKind, large: Boolean): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, receiverClass(kind, large))
        return manager.getAppWidgetIds(provider).isNotEmpty()
    }

    /** Asks the launcher to pin the [kind] widget at the [large] (4×2) or compact (2×2) preset. */
    fun request(context: Context, kind: WidgetKind, large: Boolean): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, receiverClass(kind, large))
        val requested = manager.requestPinAppWidget(provider, null, null)
        // Remember the size the user just chose so the picker reopens on it (the launcher's own pin
        // dialog is what actually places it, but this is the size they asked for).
        if (requested) rememberChosenSize(context, kind, large)
        return requested
    }

    /** Records the size ([large]) the user last chose to add for [kind]. */
    fun rememberChosenSize(context: Context, kind: WidgetKind, large: Boolean) {
        prefs(context).edit().putBoolean(sizeKey(kind), large).apply()
    }

    /**
     * The size ([large] = true) the user last added for [kind], or null if they never have — lets the
     * picker default each card's Large/Compact toggle to whatever was last added instead of always Large.
     */
    fun lastChosenLarge(context: Context, kind: WidgetKind): Boolean? {
        val store = prefs(context)
        return if (store.contains(sizeKey(kind))) store.getBoolean(sizeKey(kind), true) else null
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun sizeKey(kind: WidgetKind) = "last_large_${kind.name}"

    private const val PREFS = "widget_picker"

    private fun receiverClass(kind: WidgetKind, large: Boolean): Class<*> = when (kind) {
        WidgetKind.BUDGET ->
            if (large) BudgetLargeReceiver::class.java else BudgetCompactReceiver::class.java
        WidgetKind.SUMMARY ->
            if (large) SummaryLargeReceiver::class.java else SummaryCompactReceiver::class.java
        WidgetKind.THIS_WEEK ->
            if (large) ThisWeekLargeReceiver::class.java else ThisWeekCompactReceiver::class.java
        WidgetKind.SCAN ->
            if (large) ScanLargeReceiver::class.java else ScanCompactReceiver::class.java
        WidgetKind.TOP_CATEGORIES ->
            if (large) TopCategoriesLargeReceiver::class.java else TopCategoriesCompactReceiver::class.java
    }
}
