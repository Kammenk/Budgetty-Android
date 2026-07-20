package com.budgetty.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.backup.BackupManager
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.review.ReviewTracker
import com.budgetty.app.data.repository.AuthRepository
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.Language
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.data.settings.ThemeMode
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Outcome of an account-deletion attempt, surfaced to the UI. */
enum class DeleteAccountResult { SUCCESS, REQUIRES_REAUTH, ERROR }

class AccountViewModel(
    private val backupManager: BackupManager,
    private val settingsStore: SettingsStore,
    private val authRepository: AuthRepository,
    private val scanQuota: ScanQuota,
    private val appScope: CoroutineScope,
    private val billingManager: BillingManager,
    private val databaseManager: UserDatabaseManager,
    private val reviewTracker: ReviewTracker,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings
    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    /** Tester-only Premium unlock, triggered by the hidden 11-tap gesture on the version label. */
    fun unlockTesterPremium() = billingManager.unlockTesterPremium()

    fun setThemeMode(value: ThemeMode) = settingsStore.setThemeMode(value)
    fun setAccent(value: AccentTheme) = settingsStore.setAccent(value)
    fun setCurrency(value: Currency) = settingsStore.setCurrency(value)
    fun setDateFormat(value: DateFormatOption) = settingsStore.setDateFormat(value)
    fun setLanguage(value: Language) = settingsStore.setLanguage(value)
    fun setDisplayName(value: String) = settingsStore.setDisplayName(value.trim())

    /** Builds the JSON backup of all local data. */
    suspend fun buildBackupJson(): String = backupManager.exportJson()

    /** Imports a backup (merge on top, or full replace); reports success via [onResult]. */
    fun importBackup(json: String, replace: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try {
                backupManager.import(json, replace)
                true
            } catch (e: Exception) {
                false
            }
            onResult(ok)
        }
    }

    /**
     * Deletes the Firebase account, then erases that account's local database file. Runs on
     * [appScope] (not [viewModelScope]) so the local wipe still completes after deletion flips the
     * auth state and navigates away from this screen, clearing this ViewModel. [onResult] is
     * delivered on the main thread. The Firebase user is deleted first, so a
     * [FirebaseAuthRecentLoginRequiredException] leaves local data untouched.
     */
    fun deleteAccount(onResult: (DeleteAccountResult) -> Unit) {
        appScope.launch {
            val result = try {
                // Capture the uid up front: successful deletion signs the user out, after which the
                // active database is no longer theirs — the wipe must target the captured account.
                val uid = databaseManager.activeUid.value
                authRepository.deleteAccount()
                uid?.let { databaseManager.deleteDataFor(it) }
                scanQuota.reset()
                reviewTracker.reset()
                DeleteAccountResult.SUCCESS
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                DeleteAccountResult.REQUIRES_REAUTH
            } catch (e: Exception) {
                DeleteAccountResult.ERROR
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }
}
