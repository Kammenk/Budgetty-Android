package com.budgetty.app.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives Google Play's In-App Updates flow so eligible, opted-in users are actively prompted to move
 * to the latest build instead of relying on silent background auto-update.
 *
 * IMPORTANT: this only surfaces updates Play already considers available for this user on the track
 * they're eligible for. It does NOT bypass tester opt-in, Google-account, or install-source
 * eligibility — a user Play doesn't consider eligible will simply see no prompt.
 *
 * The default flow is [AppUpdateType.FLEXIBLE]: the update downloads in the background while the app
 * stays usable, then [updateDownloaded] flips true so the UI can offer a "restart to install"
 * prompt. Switch [UPDATE_TYPE] to [AppUpdateType.IMMEDIATE] for a blocking, forced-update experience.
 *
 * In-app updates only work for builds installed from Play, so this is a no-op in debug/sideloaded
 * builds.
 */
class InAppUpdateManager(activity: ComponentActivity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    private val _updateDownloaded = MutableStateFlow(false)

    /** True once a FLEXIBLE update has finished downloading and is waiting for a restart to install. */
    val updateDownloaded: StateFlow<Boolean> = _updateDownloaded.asStateFlow()

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _updateDownloaded.value = true
        }
    }

    // Must be registered during activity init (before STARTED), per the Activity Result API.
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // No-op: a declined or failed update just leaves the app on the current version.
        }

    init {
        if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installListener)
        }
    }

    /** Ask Play whether a newer build is available for this user and, if so, start the update flow. */
    fun checkForUpdate() {
        val options = AppUpdateOptions.newBuilder(UPDATE_TYPE).build()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val availability = info.updateAvailability()
            val shouldStart = (availability == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(options)) ||
                // Resume an IMMEDIATE update that was interrupted (e.g. the app was backgrounded).
                (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    UPDATE_TYPE == AppUpdateType.IMMEDIATE)
            if (shouldStart) {
                appUpdateManager.startUpdateFlowForResult(info, updateLauncher, options)
            }
            // A FLEXIBLE download may have completed while we were away — surface the prompt.
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _updateDownloaded.value = true
            }
        }
    }

    /** Finish a downloaded FLEXIBLE update. This restarts the app and installs the new version. */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /** Re-check on resume so interrupted IMMEDIATE updates resume and finished downloads surface. */
    fun onResume() = checkForUpdate()

    /** Unregister the install listener when the owning activity is destroyed. */
    fun dispose() {
        if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installListener)
        }
    }

    private companion object {
        /** FLEXIBLE = background download + restart prompt. IMMEDIATE = blocking forced update. */
        val UPDATE_TYPE = AppUpdateType.FLEXIBLE
    }
}
