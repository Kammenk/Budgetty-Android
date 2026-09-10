package com.budgetty.app.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release and every release-derived build type (benchmarkRelease, nonMinifiedRelease — all of which
 * inherit this src/release source set) attest with Play Integrity: the app proves to Google Play
 * that it is the genuine, Play-installed binary before Firebase issues an App Check token.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
