package com.budgetty.app.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moneyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/** App-wide formatting prefs, refreshed from SettingsStore at the top of the UI tree. */
object AppFormats {
    var currencySymbol: String = "лв"
    var datePattern: String = "d MMM yyyy"
    /** Year-less short form (day/month order follows the same preference as [datePattern]). */
    var dayMonthPattern: String = "d MMM"
}

/** Formats a monetary amount as e.g. "12.50 лв" (currency symbol from settings). */
fun BigDecimal.formatMoney(): String =
    "${moneyFormat.format(setScale(2, RoundingMode.HALF_UP))} ${AppFormats.currencySymbol}"

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Formats an epoch-millis timestamp as a short date (pattern from settings). */
fun Long.formatDate(): String =
    DateTimeFormatter.ofPattern(AppFormats.datePattern, Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** Formats an epoch-millis timestamp as day + month with no year, e.g. "24 Jun" — order follows
 *  the user's date-format preference ([AppFormats.dayMonthPattern]). */
fun Long.formatDayMonth(): String =
    DateTimeFormatter.ofPattern(AppFormats.dayMonthPattern, Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** Formats a [YearMonth] as e.g. "June 2026". */
fun YearMonth.formatMonth(): String = monthFormatter.format(this)

/** Formats a [LocalDate] as abbreviated weekday + the preferred day/month short form, e.g.
 *  "Wed, 25 Jun" — the day/month part follows the user's date-format preference. */
fun LocalDate.formatDayHeader(): String =
    DateTimeFormatter.ofPattern("EEE, ${AppFormats.dayMonthPattern}", Locale.getDefault())
        .format(this)
