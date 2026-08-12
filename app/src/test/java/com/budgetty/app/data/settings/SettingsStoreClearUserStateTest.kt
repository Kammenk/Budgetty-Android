package com.budgetty.app.data.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression test for the cross-account leak: [SettingsStore] is a single device-global
 * SharedPreferences, so [SettingsStore.clearUserState] (called on sign-out and account deletion)
 * must wipe every user-scoped setting — identity, search history, the app-lock PIN/biometric, the
 * questionnaire, dismissed tips, section layout — while keeping device/app display preferences.
 * Verified against real Robolectric SharedPreferences, no emulator.
 */
// Bare Application, NOT BudgettyApplication (its onCreate starts Koin + Firebase).
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsStoreClearUserStateTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private fun seedUserState(store: SettingsStore) {
        store.setDisplayName("Alice")
        store.addRecentSearch("Pharmacy")
        store.addRecentSearch("Rent")
        store.setPin("1234") // sets pinHash + appLockEnabled
        store.setBiometricEnabled(true)
        store.setInsightsQuizPending(true)
        store.setHomeSectionHidden("safe_to_spend", hidden = true)
        store.setHomeSectionOrder(listOf("budget", "safe_to_spend"))
        store.dismissWellbeingTip("2026-08|spend_spike")
    }

    @Test
    fun `clearUserState wipes every user-scoped setting`() {
        val store = SettingsStore(context)
        seedUserState(store)

        store.clearUserState()

        val s = store.settings.value
        assertThat(s.displayName).isEmpty()
        assertThat(s.recentSearches).isEmpty()
        assertThat(s.pinHash).isEmpty()
        assertThat(s.appLockEnabled).isFalse()
        assertThat(s.biometricEnabled).isFalse()
        assertThat(s.insightsQuizPending).isFalse()
        assertThat(s.dismissedWellbeingTips).isEmpty()
        assertThat(s.hiddenHomeSections).isEmpty()
        assertThat(s.homeSectionOrder).isEmpty()
        // The old PIN must no longer unlock the app.
        assertThat(store.verifyPin("1234")).isFalse()
    }

    @Test
    fun `clearUserState keeps device and app display preferences`() {
        val store = SettingsStore(context)
        store.setThemeMode(ThemeMode.DARK)
        store.setMonthStartDay(15)
        store.setDisplayName("Alice")

        store.clearUserState()

        val s = store.settings.value
        assertThat(s.themeMode).isEqualTo(ThemeMode.DARK) // retained
        assertThat(s.monthStartDay).isEqualTo(15) // retained
        assertThat(s.displayName).isEmpty() // cleared
    }

    @Test
    fun `clearUserState persists to prefs, not just the in-memory flow`() {
        SettingsStore(context).apply {
            setThemeMode(ThemeMode.DARK)
            seedUserState(this)
            clearUserState()
        }

        // A fresh instance re-reads the same device-global prefs from disk.
        val reloaded = SettingsStore(context).settings.value
        assertThat(reloaded.displayName).isEmpty()
        assertThat(reloaded.recentSearches).isEmpty()
        assertThat(reloaded.pinHash).isEmpty()
        assertThat(reloaded.appLockEnabled).isFalse()
        assertThat(reloaded.biometricEnabled).isFalse()
        assertThat(reloaded.hiddenHomeSections).isEmpty()
        assertThat(reloaded.themeMode).isEqualTo(ThemeMode.DARK) // device pref survived
    }
}
