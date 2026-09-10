package com.budgetty.app.appcheck

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

/**
 * Firebase App Check bootstrap — client side, "monitor mode".
 *
 * App Check lets the shared Firebase project (budgetty-96a3d) tell traffic from our genuine,
 * unmodified, Play-installed app apart from bots and scraped API keys. The SDK attaches a
 * short-lived attestation token to every Firebase backend request; auto-refresh keeps it current.
 *
 * This wiring is purely additive: tokens are ATTACHED but nothing is rejected until per-service
 * enforcement is switched on in the Firebase console. Until then the app behaves exactly as before,
 * so it is safe to ship ahead of any console change ("monitor mode").
 *
 * The attestation provider is chosen per build type by [appCheckProviderFactory]: Play Integrity for
 * release (and the release-derived benchmark build types), and the Firebase debug provider for local
 * / emulator builds. The provider lives in the debug/release source sets because the debug artifact
 * ships only in debug builds and cannot be referenced from release-compiled code.
 */
fun installAppCheck(context: Context) {
    // FirebaseApp is already brought up by Firebase's FirebaseInitProvider (a ContentProvider that
    // runs before Application.onCreate). initializeApp(context) is idempotent — it just returns that
    // same default instance — and marks the documented anchor point for wiring App Check.
    FirebaseApp.initializeApp(context)
    FirebaseAppCheck.getInstance().apply {
        // Silently refresh the token in the background so requests always carry a fresh one.
        setTokenAutoRefreshEnabled(true)
        installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
