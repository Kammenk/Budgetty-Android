package com.budgetty.app.ui.lock

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Wraps the whole app. When app lock is on it shows [LockScreen] over [content]:
 *  - **cold start** always locks (a fresh process has no saved `locked` state, so it re-inits to on);
 *  - **resume** re-locks only once the app has been backgrounded past the auto-lock delay
 *    (`autoLockMinutes == 0` = immediately).
 * Turning the lock off (or "Forgot PIN") clears the gate so the user is never stranded.
 */
@Composable
fun AppLockGate(
    viewModel: AppLockViewModel = koinViewModel(),
    content: @Composable () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val enabled = settings.appLockEnabled && settings.pinHash.isNotEmpty()
    val autoLockMinutes = settings.autoLockMinutes

    var locked by rememberSaveable { mutableStateOf(enabled) }
    var backgroundedAt by remember { mutableLongStateOf(0L) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, enabled, autoLockMinutes) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> if (enabled && backgroundedAt > 0L) {
                    val idleMs = SystemClock.elapsedRealtime() - backgroundedAt
                    if (autoLockMinutes == 0 || idleMs >= autoLockMinutes * 60_000L) locked = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Drop the gate if the lock gets turned off (Account toggle or forgot-PIN sign-out).
    LaunchedEffect(enabled) { if (!enabled) locked = false }

    content()
    if (locked && enabled) {
        LockScreen(onUnlocked = { locked = false }, viewModel = viewModel)
    }
}
