package com.budgetty.app.ui.insights

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.components.PieSlice
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.MatchedBillLine
import com.budgetty.app.ui.util.PlannedBillLine
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
import java.time.YearMonth

/**
 * Screenshot goldens for the Insights planned recurring-bills overlay via Roborazzi (JVM, no
 * emulator) — the OFF (shipped) vs ON (overlay applied) comparison in the rent-heavy stress month:
 * €950 actual spend against €967 recurring bills. Covers the donut's hatched "Bills · planned" wedge
 * + "+ €967 bills" centre subline, the trend's hatched planned caps, and the Customize "Layers" switch.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*InsightsOverlayScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*InsightsOverlayScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class InsightsOverlayScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // AppFormats is a process-global; save and restore it so this test can't leak its currency symbol
    // into other screenshot tests that rely on the ambient value (test isolation).
    private var savedSymbol = "€"
    private var savedPattern = "d MMM"

    @Before fun saveFormats() {
        savedSymbol = AppFormats.currencySymbol
        savedPattern = AppFormats.dayMonthPattern
    }

    @After fun restoreFormats() {
        AppFormats.currencySymbol = savedSymbol
        AppFormats.dayMonthPattern = savedPattern
    }

    private val slices = listOf(
        PieSlice("Groceries", BigDecimal("418"), Color(0xFF52B770)),
        PieSlice("Dining", BigDecimal("152"), Color(0xFFB77052)),
        PieSlice("Fuel", BigDecimal("118"), Color(0xFFB79552)),
        PieSlice("Household", BigDecimal("104"), Color(0xFFB75285)),
        PieSlice("Health", BigDecimal("72"), Color(0xFF52B7B4)),
        PieSlice("Other", BigDecimal("86"), Color(0xFF9B97A1)),
    )
    private val spend = BigDecimal("950")

    // Six planned bills summing to €967, plus two hidden as already-matched to a receipt (dedup).
    private val overlay = PlannedOverlay(
        plannedTotal = BigDecimal("967.00"),
        bills = listOf(
            planned("Rent", "780"),
            planned("Insurance", "75"),
            planned("Internet", "39"),
            planned("Gym", "35"),
            planned("Phone", "25"),
            planned("Netflix", "13"),
        ),
        matched = listOf(
            MatchedBillLine("Spotify", BigDecimal("10.99"), dateMillis = 1_718_000_000_000L),
            MatchedBillLine("Water", BigDecimal("18.40"), dateMillis = 1_717_000_000_000L),
        ),
    )

    private fun planned(label: String, amount: String) =
        PlannedBillLine(label = label, category = "", amount = BigDecimal(amount), matchAmount = BigDecimal(amount))

    // Dec 2025 → Jun 2026: bills were created in February, so Dec/Jan carry no planned cap
    // (no back-projection); Feb–Jun each carry the flat €967 monthly rate.
    private val trend = TrendData(
        bucketing = TrendBucketing.MONTHLY,
        buckets = listOf(
            month(2025, 12, "Dec", "890", planned = "0"),
            month(2026, 1, "Jan", "1020", planned = "0"),
            month(2026, 2, "Feb", "760", planned = "967"),
            month(2026, 3, "Mar", "980", planned = "967"),
            month(2026, 4, "Apr", "845", planned = "967"),
            month(2026, 5, "May", "910", planned = "967"),
            month(2026, 6, "Jun", "950", planned = "967", current = true),
        ),
    )

    private fun month(y: Int, m: Int, axis: String, total: String, planned: String, current: Boolean = false) =
        TrendBucket(
            axisLabel = axis,
            fullLabel = axis,
            total = BigDecimal(total),
            isCurrent = current,
            planned = BigDecimal(planned),
            monthKey = YearMonth.of(y, m),
        )

    private fun capture(dark: Boolean = false, content: @Composable () -> Unit) {
        AppFormats.currencySymbol = "€"
        AppFormats.dayMonthPattern = "d MMM"
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface { content() }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private fun breakdown(includeBills: Boolean, dark: Boolean = false) = capture(dark = dark) {
        BreakdownCard(
            slices = slices,
            total = spend,
            periodLabel = "This month",
            onSliceClick = {},
            includeBills = includeBills,
            plannedOverlay = if (includeBills) overlay else PlannedOverlay.EMPTY,
            modifier = Modifier.width(360.dp).padding(16.dp),
        )
    }

    private fun trendCard(includeBills: Boolean) = capture {
        androidx.compose.foundation.layout.Column(modifier = Modifier.width(360.dp).padding(16.dp)) {
            TrendCardContent(trend = trend, includeBills = includeBills)
        }
    }

    @Test fun breakdown_off_light() = breakdown(includeBills = false)

    @Test fun breakdown_on_light() = breakdown(includeBills = true)

    @Test fun breakdown_on_dark() = breakdown(includeBills = true, dark = true)

    @Test fun trend_off_light() = trendCard(includeBills = false)

    @Test fun trend_on_light() = trendCard(includeBills = true)

    @Test fun layers_toggle_light() = capture {
        androidx.compose.foundation.layout.Column(modifier = Modifier.width(360.dp).padding(16.dp)) {
            InsightsLayersToggle(checked = true, onCheckedChange = {})
        }
    }
}
