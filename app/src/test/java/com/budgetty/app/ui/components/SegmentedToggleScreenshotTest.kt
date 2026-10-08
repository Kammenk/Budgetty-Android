package com.budgetty.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/**
 * Golden for the shared [SegmentedToggle] in its three-up Budget cadence form (Weekly · Fortnightly ·
 * Monthly) with Fortnightly selected and carrying the "New" badge — the release surface for the
 * fortnightly budget cadence. Light + dark are separate goldens so a theme break is caught.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SegmentedToggleScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun budgetCadence_light() = capture(dark = false)

    @Test
    fun budgetCadence_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    Box(modifier = Modifier.width(340.dp).padding(16.dp)) {
                        Sample()
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sample() {
        SegmentedToggle(
            options = listOf("Weekly", "Fortnightly", "Monthly"),
            selectedIndex = 1,
            onSelect = {},
            badgeIndex = 1,
            badgeText = "New",
        )
    }
}
