package com.budgetty.app.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * The one place that touches the Crashlytics SDK, so the rest of the app depends on this small
 * interface rather than Firebase directly.
 *
 * Collection is default-on with an opt-out: [com.budgetty.app.data.settings.AppSettings.crashReportingEnabled]
 * defaults to true and the Account screen exposes a toggle. The stored preference is the source of
 * truth — [setEnabled] is applied at startup ([com.budgetty.app.BudgettyApplication]) and again on
 * every toggle change, so the SDK state always follows the user's choice.
 *
 * [setCrashlyticsCollectionEnabled] persists inside Crashlytics and survives process death, so a user
 * who opts out stays opted out even before startup re-applies the preference.
 *
 * ⚠️ Shipping this also requires a Play Data-safety update (declare Crash logs / Diagnostics, Shared =
 * No, not ephemeral) and a privacy-policy disclosure — those are Console/policy tasks, not code.
 */
class CrashReporting {

    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    fun setEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    /** Static context set once at startup: the Room schema version a crash occurred on. */
    fun setDatabaseVersion(version: Int) = crashlytics.setCustomKey(KEY_DB_VERSION, version)

    /** The user's premium tier; updated as the subscription state changes. */
    fun setPremium(premium: Boolean) = crashlytics.setCustomKey(KEY_PREMIUM, premium)

    /** The screen currently visible; updated on every navigation so a crash names where it happened. */
    fun setCurrentScreen(route: String) = crashlytics.setCustomKey(KEY_SCREEN, route)

    /** Leave a breadcrumb in the next crash report's log (e.g. a navigation or a key user action). */
    fun leaveBreadcrumb(message: String) = crashlytics.log(message)

    /**
     * Record a caught, non-fatal [throwable] so it surfaces in Crashlytics instead of being swallowed.
     * [context] is logged as a breadcrumb first, so the report carries what the app was doing.
     */
    fun recordException(throwable: Throwable, context: String? = null) {
        if (context != null) crashlytics.log(context)
        crashlytics.recordException(throwable)
    }

    private companion object {
        const val KEY_DB_VERSION = "db_version"
        const val KEY_PREMIUM = "premium"
        const val KEY_SCREEN = "screen"
    }
}
