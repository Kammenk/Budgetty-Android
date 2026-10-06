package com.budgetty.app.ui.planners

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/**
 * Above-the-fold goldens for the Planners feature (Roborazzi, JVM, no emulator): the Planners hub, the
 * Loan calculator (inputs + the monthly-payment hero + split), and the Debt payoff planner in its
 * populated (strategy toggle, result, compare, payoff chart) and empty states.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*PlannersScreenshotTest"
 *   ./gradlew :app:verifyRoborazziDebug --tests "*PlannersScreenshotTest"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class PlannersScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol; AppFormats.currencySymbol = "€" }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    private val debts = listOf(
        DebtEntity(1, "💳", "Credit card", BigDecimal("2400"), BigDecimal("19.9"), BigDecimal("60")),
        DebtEntity(2, "🚗", "Car loan", BigDecimal("6800"), BigDecimal("6.5"), BigDecimal("210")),
        DebtEntity(3, "🛍️", "Store card", BigDecimal("650"), BigDecimal("24.9"), BigDecimal("25")),
    )

    @Test fun plannersHub() {
        composeRule.setContent {
            BudgettyTheme { PlannersScreen(onNavigateBack = {}, onOpenDebtPayoff = {}, onOpenLoanCalculator = {}) }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun loanCalculator() {
        composeRule.setContent {
            BudgettyTheme { LoanCalculatorScreen(onNavigateBack = {}) }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun debtPayoff_populated() {
        composeRule.setContent {
            BudgettyTheme {
                DebtPayoffContent(debts = debts, onNavigateBack = {}, onSaveDebt = {}, onDeleteDebt = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun debtPayoff_empty() {
        composeRule.setContent {
            BudgettyTheme {
                DebtPayoffContent(debts = emptyList(), onNavigateBack = {}, onSaveDebt = {}, onDeleteDebt = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
