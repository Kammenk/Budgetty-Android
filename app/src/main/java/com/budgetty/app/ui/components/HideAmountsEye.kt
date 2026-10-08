package com.budgetty.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.AppFormats
import org.koin.compose.koinInject

/**
 * The app-bar "Hide amounts" toggle: a plain eye while amounts show, and a tonal eye-with-a-slash
 * (secondaryContainer) while they're hidden, so the active state reads at a glance — matching the
 * Hide-amounts mockup. Self-contained: it reads the live state from [AppFormats.hideAmounts] and
 * flips the shared [SettingsStore] preference, so it can be dropped into any screen's header without
 * threading a handler through that screen's ViewModel.
 */
@Composable
fun HideAmountsEye(
    modifier: Modifier = Modifier,
    settingsStore: SettingsStore = koinInject(),
) {
    val hidden = AppFormats.hideAmounts
    val description = stringResource(
        if (hidden) R.string.action_show_amounts else R.string.action_hide_amounts,
    )
    if (hidden) {
        FilledTonalIconButton(onClick = { settingsStore.toggleHideAmounts() }, modifier = modifier) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = description)
        }
    } else {
        IconButton(onClick = { settingsStore.toggleHideAmounts() }, modifier = modifier) {
            Icon(
                Icons.Filled.Visibility,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
