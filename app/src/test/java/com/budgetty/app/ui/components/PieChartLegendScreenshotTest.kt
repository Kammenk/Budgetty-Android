package com.budgetty.app.ui.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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

/**
 * Screenshot guard for the [PieChart] breakdown: the enriched legend (colored emoji tile + name +
 * percentage over a muted amount) and the on-ring percentage labels that must stay drawn. Same
 * harness as [EmptyStateScreenshotTest]; light and dark are separate goldens.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*PieChartLegendScreenshotTest*"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class PieChartLegendScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pieLegend_light() = capture(dark = false)

    @Test
    fun pieLegend_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .width(300.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(16.dp),
                    ) {
                        Sample()
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sample() {
        val slices = listOf(
            PieSlice("Groceries", BigDecimal("242"), Color(0xFF4FA85A)),
            PieSlice("Dining & Entertainment", BigDecimal("114"), Color(0xFFE0795B)),
            PieSlice("Fuel", BigDecimal("90"), Color(0xFFD08A4A)),
            PieSlice("Household & Personal", BigDecimal("85"), Color(0xFFC77DB0)),
            PieSlice("Health & Wellness", BigDecimal("64"), Color(0xFF5BB6A6)),
            PieSlice("Other", BigDecimal("117"), Color(0xFF9A93A6)),
        )
        PieChart(
            slices = slices,
            total = BigDecimal("712"),
            chartSize = 200.dp,
            periodLabel = "This month",
        )
    }
}
