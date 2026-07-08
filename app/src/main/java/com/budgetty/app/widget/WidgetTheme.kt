package com.budgetty.app.widget

import androidx.glance.unit.ColorProvider
import com.budgetty.app.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Resource-backed day/night colors for the widgets. Each `ColorProvider(resId)` resolves the right
 * tone automatically via `res/values` + `res/values-night`, mirroring the app palette in
 * `ui/theme/Color.kt`.
 */
object WidgetColors {
    val card = ColorProvider(R.color.widget_card)
    val onSurface = ColorProvider(R.color.widget_on_surface)
    val onSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
    val brand = ColorProvider(R.color.widget_brand)
    val onBrand = ColorProvider(R.color.widget_on_brand)
    val track = ColorProvider(R.color.widget_track)
    val good = ColorProvider(R.color.widget_good)
    val warn = ColorProvider(R.color.widget_warn)
    val bad = ColorProvider(R.color.widget_bad)
}

/**
 * Traffic-light status color for a budget [ratio], using the same thresholds as
 * [com.budgetty.app.ui.util.budgetColor]: green under 50%, amber 50–74%, red 75%+.
 */
fun statusColor(ratio: Float): ColorProvider = when {
    ratio >= 0.75f -> WidgetColors.bad
    ratio >= 0.50f -> WidgetColors.warn
    else -> WidgetColors.good
}

private val wholeFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 0
}
private val centsFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** Compact whole amount + currency symbol, e.g. "1,200 лв" (symbol as a suffix, like the app). */
fun BigDecimal.widgetWhole(symbol: String): String = "${wholeFormat.format(this)} $symbol"

/** Amount with cents + currency symbol, e.g. "712.40 лв" — the summary widget's headline figure. */
fun BigDecimal.widgetCents(symbol: String): String = "${centsFormat.format(this)} $symbol"
