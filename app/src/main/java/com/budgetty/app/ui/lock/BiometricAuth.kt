package com.budgetty.app.ui.lock

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager as FwBiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Thin wrapper over the framework [BiometricPrompt] (available since API 28, our minSdk) so app lock
 * can offer fingerprint / face unlock without pulling in `androidx.biometric`. Availability uses
 * [FwBiometricManager] on API 29+ and the fingerprint feature flag on API 28 (where BiometricManager
 * doesn't exist yet); either way a missing/unenrolled sensor surfaces as an error and we fall back to
 * the PIN. The lock is a convenience gate, so we intentionally accept any enrolled biometric class.
 */
object BiometricAuth {

    /** Whether the device has usable biometric hardware the user has enrolled. */
    fun isAvailable(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            val mgr = context.getSystemService(FwBiometricManager::class.java)
            @Suppress("DEPRECATION")
            mgr?.canAuthenticate() == FwBiometricManager.BIOMETRIC_SUCCESS
        }
        else -> context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    /**
     * Shows the system biometric prompt. [onError] fires for any non-success outcome (cancel, the
     * "Use PIN" negative button, lockout, no hardware) — the caller then leaves the PIN pad up.
     */
    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        negativeLabel: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeLabel, executor) { _, _ -> onError() }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onError()
                // onAuthenticationFailed (a single non-match) is transient; the prompt keeps listening.
            },
        )
    }
}
