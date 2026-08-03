package com.budgetty.app.ui.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetGreatColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.wellbeingBadContainer
import com.budgetty.app.ui.theme.wellbeingGoodContainer
import com.budgetty.app.ui.theme.wellbeingWarnContainer

/**
 * The slim one-line Insights entry: a band-tinted score chip, "Wellbeing score", the band word, a
 * chevron. Pinned above Breakdown — a door into the Wellbeing screen that costs the tab one row, not
 * a thirteenth content block. Falls to a muted "Set up" chip before there's a score.
 */
@Composable
fun WellbeingInsightsRow(summary: WellbeingSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val firstRun = !summary.hasScore
    val band = summary.score.band
    val chipBg = if (firstRun) MaterialTheme.colorScheme.surfaceContainerHigh else chipContainer(band)
    val chipFg = if (firstRun) MaterialTheme.colorScheme.onSurfaceVariant else bandColorOf(band)

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .defaultMinSize(minWidth = 32.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(chipBg)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (firstRun) "—" else "${summary.score.score}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = chipFg,
            )
        }
        Text(
            stringResource(R.string.wellbeing_entry_score),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (firstRun) stringResource(R.string.wellbeing_entry_setup) else bandWordOf(band),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (firstRun) MaterialTheme.colorScheme.primary else bandColorOf(band),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun chipContainer(band: WellbeingBand?): Color = when (band) {
    WellbeingBand.NEEDS_WORK -> wellbeingBadContainer()
    WellbeingBand.GETTING_THERE -> wellbeingWarnContainer()
    else -> wellbeingGoodContainer()
}

@Composable
private fun bandColorOf(band: WellbeingBand?): Color = when (band) {
    WellbeingBand.NEEDS_WORK -> budgetBadColor()
    WellbeingBand.GETTING_THERE -> budgetWarnColor()
    WellbeingBand.HEALTHY -> budgetGoodColor()
    WellbeingBand.THRIVING -> budgetGreatColor()
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun bandWordOf(band: WellbeingBand?): String = stringResource(
    when (band) {
        WellbeingBand.NEEDS_WORK -> R.string.wellbeing_band_needs_work
        WellbeingBand.GETTING_THERE -> R.string.wellbeing_band_getting_there
        WellbeingBand.HEALTHY -> R.string.wellbeing_band_healthy
        WellbeingBand.THRIVING -> R.string.wellbeing_band_thriving
        null -> R.string.wellbeing_not_scored_yet
    },
)
