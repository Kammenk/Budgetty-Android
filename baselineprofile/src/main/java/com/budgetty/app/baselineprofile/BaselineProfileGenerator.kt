package com.budgetty.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records a Baseline Profile for the app's cold-start path.
 *
 * # What this does
 * ART interprets bytecode until JIT decides a path is hot. A Baseline Profile lists the classes and
 * methods to compile ahead of time at install, so the traced path runs compiled from the very first
 * launch — the docs put that at ~30% faster startup. This test is the recording device: the
 * [BaselineProfileRule] launches [PACKAGE_NAME], records every class/method touched during
 * [BaselineProfileRule.collect]'s `profileBlock`, and the androidx.baselineprofile plugin writes the
 * result back into `app/src/release/generated/baselineProfiles/`, from which it's bundled into the
 * release AAB and applied on-device by profileinstaller.
 *
 * # Why startup only (and not a Home scroll)
 * The app gates every screen behind Firebase sign-in and has no debug auth bypass, while the plugin
 * drives a freshly-installed `nonMinifiedRelease` build that is therefore signed out. So the reliably
 * reachable journey is process-start → first frame, which lands on the login screen. That is less
 * limiting than it sounds: nearly all cold-start cost — Application.onCreate, the Koin graph, the
 * Compose runtime, theme setup, Firebase init — is on this path regardless of which screen renders
 * first, so this is where a profile pays off most. Extending coverage to Home/Insights/History
 * scrolling needs a signed-in session; see QUALITY_TOOLING_TODO notes / the follow-up on adding a
 * debug auth bypass.
 *
 * # How to (re)generate
 * Needs a rooted emulator or an API 33+ device connected (a Google APIs — not Google Play — emulator
 * image can be rooted with `adb root`):
 *
 *     ./gradlew :app:generateReleaseBaselineProfile
 *
 * Regenerate whenever startup code changes materially (new DI wiring, a new first screen, a
 * dependency that initializes eagerly). A stale profile is not wrong, just less complete.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = PACKAGE_NAME,
        // Also emit a dexlayout-oriented startup profile, not just the general one — this is the
        // journey that most benefits from it.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        // startActivityAndWait returns at the first frame; give the initial composition and the
        // AuthState.Loading -> SignedOut resolve a beat to run so their classes are captured too.
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), FIRST_CONTENT_TIMEOUT_MS)
    }

    private companion object {
        const val PACKAGE_NAME = "com.budgetty.app"
        const val FIRST_CONTENT_TIMEOUT_MS = 5_000L
    }
}
