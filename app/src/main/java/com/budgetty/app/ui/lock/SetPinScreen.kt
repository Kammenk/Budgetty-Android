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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Two-step PIN setup: choose a 4-digit PIN, then re-enter to confirm. A mismatch shakes and restarts
 * from step one. On a match the PIN is saved (which turns app lock on) and [onDone] fires.
 */
@Composable
fun SetPinScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AppLockViewModel = koinViewModel(),
) {
    var confirming by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorNonce by remember { mutableIntStateOf(0) }

    // On a mismatch, let the shake play then restart from the first entry.
    LaunchedEffect(errorNonce) {
        if (errorNonce > 0) {
            delay(450)
            confirming = false
            firstPin = ""
            entered = ""
            isError = false
        }
    }

    fun onDigit(d: Int) {
        if (entered.length >= PIN_LENGTH) return
        isError = false
        entered += d.toString()
        if (entered.length < PIN_LENGTH) return
        if (!confirming) {
            firstPin = entered
            entered = ""
            confirming = true
        } else if (entered == firstPin) {
            viewModel.setPin(firstPin)
            onDone()
        } else {
            isError = true
            errorNonce++
        }
    }

    SetPinContent(
        confirming = confirming,
        filled = entered.length,
        isError = isError,
        errorNonce = errorNonce,
        onBack = onBack,
        onDigit = ::onDigit,
        onBackspace = { isError = false; if (entered.isNotEmpty()) entered = entered.dropLast(1) },
    )
}

@Composable
internal fun SetPinContent(
    confirming: Boolean,
    filled: Int,
    isError: Boolean,
    errorNonce: Int,
    onBack: () -> Unit,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
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
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LockMark()
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(if (confirming) R.string.app_lock_confirm_title else R.string.app_lock_set_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (confirming) R.string.app_lock_confirm_hint else R.string.app_lock_set_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
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
                            stringResource(R.string.app_lock_pin_mismatch),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                PinKeypad(onDigit = onDigit, onBackspace = onBackspace, onBiometric = null)
            }
        }
    }
}
