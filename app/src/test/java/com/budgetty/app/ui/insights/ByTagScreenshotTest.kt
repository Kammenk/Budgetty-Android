package com.budgetty.app.ui.insights

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
 * Screenshot goldens for the Insights → Spending "By tag" card (Roborazzi, JVM, no emulator). The card
 * lists the period's top tags by spend as outlined #pills with primary-tinted bars relative to the top
 * tag, a "Top tags by spend · {period}" sub-label, and the footnote that tags can overlap. Captured in
 * both themes to lock the outline-pill styling (the key visual separation from the filled category rows).
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*ByTagScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*ByTagScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ByTagScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    private val tags = listOf(
        TagSpend("groceries", BigDecimal("184.20")),
        TagSpend("work", BigDecimal("96.50")),
        TagSpend("tokyo-trip", BigDecimal("72.00")),
        TagSpend("gifts", BigDecimal("38.90")),
        TagSpend("coffee", BigDecimal("11.40")),
    )

    private fun capture(dark: Boolean = false) {
        AppFormats.currencySymbol = "€"
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    InsightCard(modifier = Modifier.width(360.dp).padding(16.dp)) {
                        ByTagContent(tags = tags, periodLabel = "This month", onTagClick = {})
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun byTag_light() = capture()

    @Test fun byTag_dark() = capture(dark = true)
}
