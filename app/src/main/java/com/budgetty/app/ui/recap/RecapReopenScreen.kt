package com.budgetty.app.ui.recap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.theme.dimens
import org.koin.androidx.compose.koinViewModel

/**
 * Re-opens the user's last recap from Insights, recomputed on demand (a quick dismiss never loses it).
 * The settings footnote promises this door. ✕ / Done / See details all just pop back — nothing is
 * re-stamped, since this isn't the once-per-period interstitial.
 */
@Composable
fun RecapReopenScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecapViewModel = koinViewModel(),
) {
    val story by viewModel.reopen.collectAsStateWithLifecycle()
    val prefs by viewModel.recapPrefs.collectAsStateWithLifecycle()
    val current = story
    if (current == null) {
        // Loading, or (defensively) none stored — Insights only offers the entry when one exists.
        RecapReopenEmpty(onNavigateBack)
    } else {
        RecapStoryScreen(
            story = current,
            onClose = onNavigateBack,
            onSeeDetails = onNavigateBack,
            recapEnabled = prefs.enabled,
            recapFrequency = prefs.frequency,
            onRecapFrequencyChange = viewModel::setRecapFrequencyChoice,
        )
    }
}

/**
 * The slim Insights entry that re-opens the last recap — a sibling of the Wellbeing row, pinned just
 * below it. Shown only once a recap has been generated for a closed period.
 */
@Composable
fun RecapReopenRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.avatar)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.recap_reopen_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.recap_reopen_sub),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
    }
}

@Composable
private fun RecapReopenEmpty(onNavigateBack: () -> Unit) {
    BackHandler(onBack = onNavigateBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(MaterialTheme.dimens.sm),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(MaterialTheme.dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.recap_reopen_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
