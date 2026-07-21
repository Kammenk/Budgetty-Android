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

    fun setEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
    }
}
