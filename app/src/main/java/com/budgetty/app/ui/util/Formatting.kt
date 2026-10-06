package com.budgetty.app.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var currencySymbol: String = "€"
    var datePattern: String = "d MMM yyyy"
    /** Year-less short form (day/month order follows the same preference as [datePattern]). */
    var dayMonthPattern: String = "d MMM"

    /**
     * When true, the "Hide amounts" privacy mode is on and [formatMoney] returns a fixed mask instead
     * of the figure — the one source of truth that keeps every money value private, including the ~90
     * amounts spliced into `stringResource`/string templates that a Compose wrapper can't reach.
     *
     * Backed by Compose **snapshot state**: a composable that renders an amount through [formatMoney]
     * records this read, so flipping the toggle recomposes and re-masks every amount on screen in one
     * pass — no per-call-site wiring. [MoneyText] and the app-bar eye read it too. Set from
     * SettingsStore in MainActivity. Reads off the main thread (e.g. a background flow) just return
     * the current value untracked. Display-only: data export builds its own strings and never calls
     * [formatMoney], so exported files are unaffected. The mask drops the figure's sign and magnitude
     * so neither leaks; the currency symbol stays so the slot still reads as money.
     */
    var hideAmounts: Boolean by mutableStateOf(false)
}

/** The masked stand-in for a figure while "Hide amounts" is on — fixed, so no magnitude leaks. */
private const val MONEY_MASK = "••••"

/** Formats a monetary amount as e.g. "12.50 €" (currency symbol from settings), or a privacy mask
 *  (e.g. "•••• €") while [AppFormats.hideAmounts] is on. */
fun BigDecimal.formatMoney(): String =
    if (AppFormats.hideAmounts) "$MONEY_MASK ${AppFormats.currencySymbol}"
    else "${moneyFormat.format(setScale(2, RoundingMode.HALF_UP))} ${AppFormats.currencySymbol}"

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

/** Formats a [LocalDate] as day + month with no year, e.g. "24 Jun" — order follows the user's
 *  date-format preference ([AppFormats.dayMonthPattern]). */
fun LocalDate.formatDayMonth(): String =
    DateTimeFormatter.ofPattern(AppFormats.dayMonthPattern, Locale.getDefault()).format(this)

/** Formats a [YearMonth] as e.g. "June 2026". */
fun YearMonth.formatMonth(): String = monthFormatter.format(this)

/** Formats a [LocalDate] as abbreviated weekday + the preferred day/month short form, e.g.
 *  "Wed, 25 Jun" — the day/month part follows the user's date-format preference. */
fun LocalDate.formatDayHeader(): String =
    DateTimeFormatter.ofPattern("EEE, ${AppFormats.dayMonthPattern}", Locale.getDefault())
        .format(this)
