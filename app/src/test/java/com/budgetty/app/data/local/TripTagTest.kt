package com.budgetty.app.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The trip tag is the name, normalized and stamped with the year — once. */
class TripTagTest {

    @Test
    fun `stamps the year onto a plain name`() {
        assertThat(TripEntity.tagFor("Lisbon", 2026)).isEqualTo("lisbon-2026")
        assertThat(TripEntity.tagFor("Work Trip", 2026)).isEqualTo("work-trip-2026")
    }

    @Test
    fun `a name that already ends in the year is not stamped twice`() {
        assertThat(TripEntity.tagFor("Lisbon 2026", 2026)).isEqualTo("lisbon-2026")
        assertThat(TripEntity.tagFor("#Lisbon-2026", 2026)).isEqualTo("lisbon-2026")
    }

    @Test
    fun `a different year in the name still gets the trip's year`() {
        assertThat(TripEntity.tagFor("Lisbon 2025", 2026)).isEqualTo("lisbon-2025-2026")
        assertThat(TripEntity.tagFor("Expo12026", 2026)).isEqualTo("expo12026-2026")
    }

    @Test
    fun `a blank name falls back to trip`() {
        assertThat(TripEntity.tagFor("  ", 2026)).isEqualTo("trip-2026")
    }
}
