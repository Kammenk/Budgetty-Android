package com.budgetty.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.ui.components.ScoreRing
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetGreatColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.theme.wellbeingBadContainer
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.wellbeing.TipTone
import com.budgetty.app.ui.wellbeing.TipType
import com.budgetty.app.ui.wellbeing.WellbeingBand
import com.budgetty.app.ui.wellbeing.WellbeingSummary
import com.budgetty.app.ui.wellbeing.WellbeingTip
import java.math.BigDecimal

/**
 * The compact live-teaser banner into the Wellbeing screen: a 40dp score ring, one truncated tip
 * line, a chevron. The lightest card on Home — never a second hero. Takes an attention tint when the
 * score is low or the top tip is an alert; falls to first-run copy before there's a score.
 */
@Composable
fun WellbeingBanner(summary: WellbeingSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val firstRun = !summary.hasScore
    val topTip = summary.monthlyTips.firstOrNull()
    val attention = !firstRun && (summary.score.band == WellbeingBand.NEEDS_WORK || topTip?.tone == TipTone.ALERT)
    val ringColor = if (attention) budgetBadColor() else bandColorOf(summary.score.band)
    val cardBg = if (attention) wellbeingBadContainer() else MaterialTheme.colorScheme.surfaceContainer

    Row(
        modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            ScoreRing(
                fraction = if (firstRun) 0f else (summary.score.score ?: 0) / 100f,
                arcColor = ringColor,
                strokeRatio = 5.4f / 42f,
                dashedTrack = firstRun,
                modifier = Modifier.size(40.dp),
            ) {
                Text(
                    if (firstRun) "—" else "${summary.score.score}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (firstRun) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            // Unseen-tip dot — marks the tip, not the destination.
            if (!firstRun && topTip != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    stringResource(R.string.wellbeing_entry_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!firstRun) {
                    Text(
                        "· " + bandWordOf(summary.score.band),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ringColor,
                    )
                }
            }
            Text(
                bannerLine(firstRun, topTip),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun bannerLine(firstRun: Boolean, tip: WellbeingTip?): String = when {
    firstRun -> stringResource(R.string.wellbeing_banner_firstrun)
    tip == null -> stringResource(R.string.wellbeing_banner_generic)
    else -> {
        val amt = (tip.amount ?: BigDecimal.ZERO).formatMoney()
        when (tip.type) {
            TipType.CATEGORY_SPIKE -> stringResource(R.string.wellbeing_banner_spike, tip.label.orEmpty(), tip.percent ?: 0)
            TipType.NEGATIVE_CASHFLOW -> stringResource(R.string.wellbeing_banner_cashflow, amt)
            TipType.OVER_BUDGET -> stringResource(R.string.wellbeing_banner_overbudget)
            TipType.SUBSCRIPTION_COST -> stringResource(R.string.wellbeing_banner_subs, amt)
            TipType.MISSING_BUDGET -> stringResource(R.string.wellbeing_banner_missingbudget, tip.label.orEmpty())
            TipType.NO_GOAL -> stringResource(R.string.wellbeing_banner_nogoal)
            TipType.SAVINGS_WIN -> stringResource(R.string.wellbeing_banner_savingswin, tip.percent ?: 0)
            else -> stringResource(R.string.wellbeing_banner_generic)
        }
    }
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
