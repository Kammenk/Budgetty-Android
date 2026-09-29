package com.budgetty.app.ui.insights

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Screenshot goldens for the Insights → Overview hero's "With bills" line (Roborazzi, JVM, no
 * emulator). "Total spent" stays actual spend; the new hatch-swatch "With bills" line rolls in the
 * period's planned recurring bills (`total + periodBills`) and renders only when `hasBills` is true —
 * so the three goldens are: has-bills (€820 + €420 = €1,240), bills-but-nothing-spent-yet
 * (€0 + €420), and no-bills (the line is absent — the shipped hero).
 *
 * A minimal state is used so only the hero card renders: no slices/highlights (Top spending / Worth
 * knowing are skipped) and `isLoaded = false` (the setup checklist is skipped).
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*OverviewWithBillsScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*OverviewWithBillsScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class OverviewWithBillsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    private val noopControls = OverviewControls(
        dismissedSetup = emptySet(),
        overlayNudgeDismissed = true,
        onNavigateToBudget = {},
        onNavigateToManageCategories = {},
        onToggleIncludeRecurringBills = {},
        onDismissOverlayNudge = {},
        onDismissSetupItem = {},
    )

    /** Actual spend AND recurring bills: €820 spent + €420 planned ⇒ "With bills €1,240". */
    private val withBills = InsightsUiState(
        total = BigDecimal("820"),
        periodBills = BigDecimal("420"),
        hasBills = true,
        receiptCount = 18,
        totalSaved = BigDecimal("12"),
        avgPerDay = BigDecimal("27"),
        isLoaded = false,
    )

    /** Bills exist but nothing scanned yet: "Total spent €0" + "With bills €420" (all planned). */
    private val billsOnly = withBills.copy(
        total = BigDecimal.ZERO,
        receiptCount = 0,
        totalSaved = BigDecimal.ZERO,
        avgPerDay = BigDecimal.ZERO,
    )

    /** No recurring bills at all: the "With bills" line must be absent (the shipped hero). */
    private val noBills = withBills.copy(hasBills = false, periodBills = BigDecimal.ZERO)

    private fun capture(state: InsightsUiState, dark: Boolean = false) {
        AppFormats.currencySymbol = "€"
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    Column(
                        modifier = Modifier.width(360.dp).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OverviewTabContent(
                            state = state,
                            onGoToTab = {},
                            onSliceClick = {},
                            controls = noopControls,
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun withBills_light() = capture(withBills)

    @Test fun withBills_dark() = capture(withBills, dark = true)

    @Test fun billsOnly_light() = capture(billsOnly)

    @Test fun noBills_light() = capture(noBills)
}
