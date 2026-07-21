package com.budgetty.app.debug

/**
 * Debug-only switch that lets test harnesses land straight on the main app, skipping the onboarding →
 * Firebase-login → setup-quiz gates. Mirrors the iOS `SKIP_AUTH` launch flag so one Maestro flow can
 * drive both platforms.
 *
 * Consumers: Baseline Profile generation (a signed-out build otherwise only reaches the login screen),
 * Maestro E2E, and screenshot tests.
 *
 * Release-safe by construction: the only writer is [com.budgetty.app.MainActivity], and it writes
 * only when `BuildConfig.TEST_HOOKS_ENABLED` is true and the SKIP_AUTH launch extra is present. That
 * flag is true for debug and the Baseline-Profile variants but false for the shipped `release`, so
 * nothing sets [skipAuth] in production — the real Firebase gate is the only way in.
 *
 * Signed-out access already falls back to an empty scratch database (see
 * [com.budgetty.app.data.local.UserDatabaseManager]), so the bypassed app renders a real, empty Home
 * instead of crashing for want of a Firebase uid.
 */
object DebugAuth {
    @Volatile
    var skipAuth: Boolean = false
}
