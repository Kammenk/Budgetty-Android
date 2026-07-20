package com.budgetty.app.ui.components

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
 * Screenshot test for [EmptyState] via Roborazzi — renders the composable on the JVM (no emulator)
 * and compares against a committed golden in src/test/screenshots. Catches visual regressions
 * (spacing, colour, typography, theming) that a semantics-only test never sees.
 *
 * This is also the reference for adding more: wrap a stateless composable in [BudgettyTheme], capture
 * `onRoot()`. Light and dark are separate goldens because the whole point is to catch a theme break.
 *
 *   ./gradlew :app:recordRoborazziDebug   # write/refresh goldens (review the diff before committing)
 *   ./gradlew :app:verifyRoborazziDebug   # fail on any pixel drift (also runs in the normal test task)
 *
 * NATIVE graphics mode is what makes Robolectric render real pixels instead of a blank canvas; the
 * fixed device qualifier keeps density and size deterministic across machines.
 *
 * Portability caveat: goldens are pixel-exact and font rendering differs by OS, so goldens recorded
 * on macOS will not match a Linux CI run. This is why the standard `testDebugUnitTest` (and CI) does
 * NOT verify screenshots — captureRoboImage is a no-op unless a record/verify roborazzi task drives
 * it. Run verify locally on the same OS the goldens were recorded on, or record on CI's image first.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class EmptyStateScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_light() = capture(dark = false)

    @Test
    fun emptyState_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Sample()
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sample() {
        EmptyState(
            emoji = "🧾",
            title = "No receipts yet",
            subtitle = "Scan your first receipt and it will show up here.",
        )
    }
}
