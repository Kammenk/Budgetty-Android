package com.budgetty.app.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds attest with the Firebase debug provider. Emulators and developer devices can't pass
 * Play Integrity, so they present a locally generated debug token instead. The token is printed to
 * Logcat on first run (tag "DebugAppCheckProvider"); register it once under Firebase Console →
 * App Check → apps → Manage debug tokens.
 *
 * This factory is compiled only into the debug variant: firebase-appcheck-debug is a
 * debugImplementation dependency, so neither the artifact nor this provider ever ships in release.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
