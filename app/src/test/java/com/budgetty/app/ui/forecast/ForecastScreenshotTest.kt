package com.budgetty.app.ui.forecast

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.forecast.CashEvent
import com.budgetty.app.data.forecast.CashFlowForecast
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

/**
 * Golden for the cash-flow forecast's ready (premium) state — the projected-balance chart with its
 * trough callout, the month-by-month list and the assumptions row. A healthy scenario (the trough
 * stays above the comfort line, so the callout is green). Light + dark are separate goldens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ForecastScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ready_light() = capture(dark = false)

    @Test
    fun ready_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        val today = LocalDate.of(2026, 10, 6)
        val result = CashFlowForecast.project(
            startBalance = BigDecimal("1200"),
            today = today,
            horizonMonths = 3,
            events = listOf(
                CashEvent(LocalDate.of(2026, 10, 25), BigDecimal("2600")),
                CashEvent(LocalDate.of(2026, 11, 25), BigDecimal("2600")),
                CashEvent(LocalDate.of(2026, 11, 15), BigDecimal("-640")),
                CashEvent(LocalDate.of(2026, 11, 1), BigDecimal("-850")),
                CashEvent(LocalDate.of(2026, 12, 1), BigDecimal("-850")),
            ),
            monthlyDiscretionary = BigDecimal("780"),
            comfortThreshold = BigDecimal("300"),
        )
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ForecastContent(
                        state = ForecastUiState(
                            isLoaded = true, isPremium = true, hasIncome = true, hasBills = true,
                            startBalance = BigDecimal("1200"), derivedDiscretionary = BigDecimal("780"),
                            discretionary = BigDecimal("780"), comfortThreshold = BigDecimal("300"),
                            horizonMonths = 3, result = result,
                        ),
                        onNavigateBack = {}, onNavigateToPaywall = {}, onNavigateToBudget = {},
                        onCycleHorizon = {}, onSetStartBalance = {}, onSetComfort = {},
                        onSetDiscretionary = {}, onResetDiscretionary = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
