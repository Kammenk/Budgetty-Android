package com.budgetty.app.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Opening a date picker and tapping OK without touching it must keep the same calendar day. Feeding
 * local-midnight millis straight to the M3 picker (which works in UTC midnight) moved a warranty's
 * purchase date back one day per open in every zone east of UTC.
 */
class DatePickerMillisTest {

    private val zones = listOf("Europe/Sofia", "Europe/Lisbon", "America/New_York", "Pacific/Auckland", "UTC")
        .map(ZoneId::of)

    @Test
    fun `the picker highlights the stored local day`() {
        for (zone in zones) {
            val localMidnight = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val lateEvening = LocalDate.of(2026, 10, 1).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
            val expected = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            assertThat(localMidnight.toDatePickerMillis(zone)).isEqualTo(expected)
            assertThat(lateEvening.toDatePickerMillis(zone)).isEqualTo(expected)
        }
    }

    @Test
    fun `confirming the picker unchanged keeps the date, however many times`() {
        for (zone in zones) {
            var stored = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            repeat(3) { stored = stored.toDatePickerMillis(zone).fromDatePickerMillis(zone) }

            assertThat(Instant.ofEpochMilli(stored).atZone(zone).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 1))
        }
    }

    @Test
    fun `a picked day lands on that local day`() {
        val picked = LocalDate.of(2026, 2, 28).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        for (zone in zones) {
            val stored = picked.fromDatePickerMillis(zone)
            assertThat(Instant.ofEpochMilli(stored).atZone(zone).toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 28))
        }
    }
}
