package com.budgetty.app.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Full-screen unlock gate: PIN pad plus optional biometric. Verifies against the salted hash in
 * settings; a wrong PIN shakes the dots and clears. "Forgot PIN?" signs the user out (see
 * [AppLockViewModel.forgotPin]) so they're never locked out of their own device.
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember(settings.biometricEnabled) {
        settings.biometricEnabled && BiometricAuth.isAvailable(context)
    }

    var entered by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorNonce by remember { mutableIntStateOf(0) }

    val promptTitle = stringResource(R.string.app_lock_title)
    val promptSubtitle = stringResource(R.string.app_lock_biometric_subtitle)
    val usePin = stringResource(R.string.app_lock_use_pin)

    fun launchBiometric() = BiometricAuth.authenticate(
        context = context,
        title = promptTitle,
        subtitle = promptSubtitle,
        negativeLabel = usePin,
        onSuccess = onUnlocked,
        onError = {},
    )

    // Offer biometric once as the screen appears.
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) launchBiometric()
    }

    // After a wrong PIN, let the shake play then reset for another try.
    LaunchedEffect(errorNonce) {
        if (errorNonce > 0) {
            delay(450)
            entered = ""
            isError = false
        }
    }

    fun onDigit(d: Int) {
        if (entered.length >= PIN_LENGTH) return
        isError = false
        entered += d.toString()
        if (entered.length == PIN_LENGTH) {
            if (viewModel.verifyPin(entered)) onUnlocked() else { isError = true; errorNonce++ }
        }
    }

    LockScreenContent(
        filled = entered.length,
        isError = isError,
        errorNonce = errorNonce,
        showBiometric = biometricAvailable,
        onDigit = ::onDigit,
        onBackspace = { isError = false; if (entered.isNotEmpty()) entered = entered.dropLast(1) },
        onBiometric = ::launchBiometric,
        onForgot = viewModel::forgotPin,
    )
}

@Composable
internal fun LockScreenContent(
    filled: Int,
    isError: Boolean,
    errorNonce: Int,
    showBiometric: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit,
    onForgot: () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorNonce) {
        if (errorNonce > 0) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -12f at 50; 12f at 100; -10f at 160; 10f at 220; -5f at 300; 0f at 400
                },
            )
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LockMark()
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_lock_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.app_lock_enter_pin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            PinDots(
                filled = filled,
                isError = isError,
                modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            )
            Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                if (isError) {
                    Text(
                        stringResource(R.string.app_lock_wrong_pin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PinKeypad(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onBiometric = if (showBiometric) onBiometric else null,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onForgot) {
                Text(stringResource(R.string.app_lock_forgot))
            }
        }
    }
}
