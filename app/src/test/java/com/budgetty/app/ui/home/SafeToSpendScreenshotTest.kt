package com.budgetty.app.ui.home

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
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
 * Screenshot goldens for [SafeToSpendCard] (the current-cycle "safe to spend" Home card) via
 * Roborazzi — renders on the JVM with no emulator. Covers the four states the card can take:
 * healthy / getting-low / overspent / setup-no-income, plus a dark rendering.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*SafeToSpendScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*SafeToSpendScreenshotTest"   # fail on drift
 *
 * Amounts and the reset date are fixed (not `now()`), so goldens stay deterministic across runs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SafeToSpendScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // A fixed payday so the "resets …" date never drifts with the wall clock.
    private val payday: LocalDate = LocalDate.of(2026, 8, 25)

    // Numbers reconcile to income: spent (incl. paid bills) + safe-to-spend + bills-still-due = income,
    // i.e. safe = 2400 − 712.40 − 1067.60 − 182.40 = 437.60. Paid bills are part of "spent", so they're
    // subtracted from safe just like receipts — the card's figures add up to the cycle income.
    private fun healthy() = HomeUiState(
        isLoaded = true,
        monthlySpent = BigDecimal("712.40"),
        cycleIncome = BigDecimal("2400.00"),
        billsStillDue = BigDecimal("1067.60"),
        billsPaid = BigDecimal("182.40"),
        safeToSpend = BigDecimal("437.60"),
        daysUntilPayday = 15,
        nextPayday = payday,
    )

    // safe = 2400 − 1110.00 − 1067.60 − 182.40 = 40.00 (≤ 10% of income ⇒ the "getting low" state).
    private fun low() = healthy().copy(
        monthlySpent = BigDecimal("1110.00"),
        safeToSpend = BigDecimal("40.00"),
        daysUntilPayday = 9,
    )

    // safe = 2400 − 1215.00 − 1067.60 − 182.40 = −65.00 (negative ⇒ the "overspent" state).
    private fun over() = healthy().copy(
        monthlySpent = BigDecimal("1215.00"),
        safeToSpend = BigDecimal("-65.00"),
        daysUntilPayday = 9,
    )

    private fun setup() = HomeUiState(
        isLoaded = true,
        monthlySpent = BigDecimal("712.40"),
        billsStillDue = BigDecimal("485.00"),
    )

    @Test fun healthy_light() = capture(healthy(), dark = false)
    @Test fun healthy_dark() = capture(healthy(), dark = true)
    @Test fun low_light() = capture(low(), dark = false)
    @Test fun over_light() = capture(over(), dark = false)
    @Test fun setup_light() = capture(setup(), dark = false)

    // Tablet reuses the same card with the period pill suppressed (the pill lives in the tablet
    // header). Portrait-tablet width and the narrow landscape left column (0.4f) both need to hold up.
    @Test fun tablet_portrait() = captureTablet(healthy(), widthDp = 520)
    @Test fun tablet_widecolumn() = captureTablet(healthy(), widthDp = 320)

    private fun captureTablet(state: HomeUiState, widthDp: Int) {
        AppFormats.currencySymbol = "€"
        AppFormats.dayMonthPattern = "d MMM"
        composeRule.setContent {
            BudgettyTheme(darkTheme = false) {
                Surface {
                    SafeToSpendCard(
                        state = state,
                        showPeriodPill = false,
                        modifier = Modifier.width(widthDp.dp).padding(16.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private fun capture(state: HomeUiState, dark: Boolean) {
        AppFormats.currencySymbol = "€"
        AppFormats.dayMonthPattern = "d MMM"
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    SafeToSpendCard(state = state, modifier = Modifier.width(360.dp).padding(16.dp))
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
