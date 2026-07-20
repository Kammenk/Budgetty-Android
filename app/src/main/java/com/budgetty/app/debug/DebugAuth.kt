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
 * only when `BuildConfig.DEBUG` is true and the SKIP_AUTH launch extra is present. In a release build
 * nothing sets it, so [skipAuth] stays false and the real Firebase gate is the only way in — there is
 * no code path that turns this on for a shipped app.
 *
 * Signed-out access already falls back to an empty scratch database (see
 * [com.budgetty.app.data.local.UserDatabaseManager]), so the bypassed app renders a real, empty Home
 * instead of crashing for want of a Firebase uid.
 */
object DebugAuth {
    @Volatile
    var skipAuth: Boolean = false
}
