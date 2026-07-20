package com.budgetty.app.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time with and without the Baseline Profile, so the win from
 * [BaselineProfileGenerator] is a number rather than a claim.
 *
 * Run both variants and compare `timeToInitialDisplayMs`:
 *
 *     ./gradlew :baselineprofile:connectedReleaseAndroidTest
 *
 * [none] is the floor (JIT only); [baselineProfile] is what ships. The gap is the startup improvement.
 *
 * ⚠️ Measure on a physical device (the Pixel 9 Pro is the on-device target). Emulator startup numbers
 * are not trustworthy for this comparison — the emulator's compilation behaviour does not match a
 * real device's, so an emulator run can under- or over-state the gain or show none at all. Treat
 * emulator output as a smoke test that the harness runs, not as the performance result.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = measure(CompilationMode.None())

    @Test
    fun startupBaselineProfile() =
        measure(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun measure(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), FIRST_CONTENT_TIMEOUT_MS)
    }

    private companion object {
        const val PACKAGE_NAME = "com.budgetty.app"
        const val ITERATIONS = 10
        const val FIRST_CONTENT_TIMEOUT_MS = 5_000L
    }
}
