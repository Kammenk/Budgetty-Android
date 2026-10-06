package com.budgetty.app.ui.trips

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Golden for the Trips (Travel mode) screen: the active trip's hero + day strip, the amber "ahead of
 * pace" budget bar (tick behind the fill), top categories, and past trips with their budget-result
 * chips. The timezone is pinned to UTC so the dates render the same wherever it records.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TripsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun trips_light() = capture(dark = false)

    @Test
    fun trips_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 10, 14)
        fun millis(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()
        fun card(
            id: Long, name: String, tag: String, spent: String, budget: String?,
            start: Long?, end: Long?, active: Boolean, ended: Long?,
            cats: List<TripCategoryStat>, count: Int,
        ): TripCard {
            val trip = TripEntity(
                id = id, name = name, tag = tag, startDate = start, endDate = end,
                budgetAmount = budget?.let(::BigDecimal), active = active, createdAt = start ?: 0, endedAt = ended,
            )
            return TripCard(trip, BigDecimal(spent), count, TripStats.compute(trip, BigDecimal(spent), today, zone), cats)
        }
        val cats = listOf(
            TripCategoryStat("Lodging", "🏨", 0xFF5B6CD9.toInt(), BigDecimal("312"), 1f),
            TripCategoryStat("Restaurants", "🍽️", 0xFFD9763C.toInt(), BigDecimal("118"), 0.38f),
            TripCategoryStat("Transport", "🚕", 0xFFE0A030.toInt(), BigDecimal("46"), 0.15f),
        )
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TripsContent(
                        state = TripsUiState(
                            isLoaded = true,
                            active = card(
                                1, "Lisbon", "lisbon-2026", "512", "900",
                                millis(2026, 10, 12), millis(2026, 10, 20), true, null, cats, 14,
                            ),
                            past = listOf(
                                card(
                                    2, "Porto weekend", "porto-weekend-2026", "296", null,
                                    millis(2026, 6, 19), millis(2026, 6, 21), false, millis(2026, 6, 21), emptyList(), 8,
                                ),
                            ),
                        ),
                        transactionTimestamps = emptyList(),
                        onStart = { _, _, _, _, _ -> }, onEnd = {}, onDelete = {},
                        onOpenHistory = {}, onNavigateBack = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
