package com.budgetty.app.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetty.app.R

/** Brand lock badge shown above the PIN pad. */
@Composable
fun LockMark(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** Row of [total] dots, the first [filled] solid. Turns [MaterialTheme.colorScheme.error] on a mismatch. */
@Composable
fun PinDots(filled: Int, isError: Boolean, modifier: Modifier = Modifier, total: Int = PIN_LENGTH) {
    val onColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(total) { i ->
            val dot = Modifier.size(16.dp).clip(CircleShape)
            Box(
                if (i < filled) dot.background(onColor)
                else dot.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

/**
 * The 3×4 number pad. The bottom-left slot holds the biometric key when [onBiometric] is non-null
 * (lock screen) and is blank otherwise (set-PIN), keeping the 0 and backspace aligned in both.
 */
@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { d -> DigitKey(d, onClick = { onDigit(d) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (onBiometric != null) {
                    IconKey(Icons.Filled.Fingerprint, R.string.app_lock_use_biometric, onBiometric)
                } else {
                    Spacer(Modifier.size(KEY_SIZE.dp))
                }
                DigitKey(0, onClick = { onDigit(0) })
                IconKey(Icons.AutoMirrored.Filled.Backspace, R.string.app_lock_backspace, onBackspace)
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(KEY_SIZE.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            digit.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun IconKey(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    val label = stringResource(labelRes)
    Box(
        Modifier
            .size(KEY_SIZE.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

const val PIN_LENGTH = 4
private const val KEY_SIZE = 72
