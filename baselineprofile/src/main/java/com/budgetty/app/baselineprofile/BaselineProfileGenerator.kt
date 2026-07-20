package com.budgetty.app.baselineprofile

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the Baseline Profile: the classes and methods ART should compile ahead of time at install
 * so the covered paths run compiled from the very first launch (~30% faster startup per the docs).
 * The [BaselineProfileRule] records everything touched inside each `collect` block; the
 * androidx.baselineprofile plugin merges the results into `app/src/release/generated/baselineProfiles/`
 * and they ship in the release AAB, applied on-device by profileinstaller.
 *
 * Two journeys:
 * - [startup] — cold process start to first frame. `includeInStartupProfile` also emits the
 *   dexlayout-oriented startup profile.
 * - [homeJourney] — launches straight into the signed-in app via the SKIP_AUTH test hook, then
 *   scrolls Home and opens Insights, so those composables, their ViewModels, and the Room read path
 *   land in the profile too. Without the hook the app stops at the login screen (Firebase-gated),
 *   which is why this depends on [com.budgetty.app.debug.DebugAuth]; the hook is live only in the
 *   nonMinifiedRelease build this runs against, never in the shipped release.
 *
 * # (Re)generate — needs a rooted emulator or an API 33+ device (a Google APIs, not Google Play,
 * emulator image roots with `adb root`):
 *
 *     ./gradlew :app:generateReleaseBaselineProfile
 *
 * Regenerate when startup or the Home/Insights render paths change materially.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), CONTENT_TIMEOUT_MS)
    }

    @Test
    fun homeJourney() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        // Custom launch intent carrying the SKIP_AUTH extra, so the run lands on Home instead of the
        // login screen. Explicit component so it resolves without relying on the launcher category.
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.MainActivity")
                putExtra(EXTRA_SKIP_AUTH, true)
            },
        )
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), CONTENT_TIMEOUT_MS)

        // Scroll the Home feed to compile its list/section render paths, then open Insights and scroll
        // that. Best-effort: a missing element must not fail generation — the paths reached so far are
        // still recorded.
        scrollFirstScrollable()
        device.findObject(By.text("Insights"))?.click()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), CONTENT_TIMEOUT_MS)
        scrollFirstScrollable()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.scrollFirstScrollable() {
        val scrollable = device.findObject(By.scrollable(true)) ?: return
        scrollable.setGestureMargin(device.displayWidth / 5) // keep flings off the system gesture edges
        scrollable.fling(Direction.DOWN)
        scrollable.fling(Direction.UP)
    }

    private companion object {
        const val PACKAGE_NAME = "com.budgetty.app"
        const val EXTRA_SKIP_AUTH = "com.budgetty.app.SKIP_AUTH"
        const val CONTENT_TIMEOUT_MS = 5_000L
    }
}
