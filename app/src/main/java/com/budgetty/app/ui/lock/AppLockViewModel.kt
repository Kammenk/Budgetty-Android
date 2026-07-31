package com.budgetty.app.ui.lock

import androidx.lifecycle.ViewModel
import com.budgetty.app.data.repository.AuthRepository
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.StateFlow

/**
 * Backs the app-lock gate, lock screen, set-PIN flow and the Account security rows. All PIN storage
 * lives in [SettingsStore] (salted hash); this just exposes the settings stream and the operations.
 */
class AppLockViewModel(
    private val settingsStore: SettingsStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings

    fun setPin(pin: String) = settingsStore.setPin(pin)
    fun verifyPin(pin: String): Boolean = settingsStore.verifyPin(pin)
    fun setBiometricEnabled(value: Boolean) = settingsStore.setBiometricEnabled(value)
    fun setAutoLockMinutes(value: Int) = settingsStore.setAutoLockMinutes(value)
    fun disableLock() = settingsStore.disableAppLock()

    /** "Forgot PIN": clear the lock and sign out so the user re-authenticates instead of being stuck. */
    fun forgotPin() {
        settingsStore.disableAppLock()
        authRepository.signOut()
    }
}
