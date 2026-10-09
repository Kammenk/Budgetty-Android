package com.budgetty.app.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * Material 3's DatePicker reads and returns a calendar day as UTC-midnight millis, while the app stores
 * local-time millis. Passing one straight into the other shifts the day for anyone off UTC — opening a
 * warranty's date picker and tapping OK moved its date back a day in every European time zone. Convert
 * at the picker boundary with these two.
 */

/** Local-time millis → the UTC-midnight millis the date picker uses to highlight that calendar day. */
fun Long.toDatePickerMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The date picker's UTC-midnight selection → local noon on that calendar day (stable for bucketing). */
fun Long.fromDatePickerMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
        .atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
