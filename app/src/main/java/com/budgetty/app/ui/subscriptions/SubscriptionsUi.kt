package com.budgetty.app.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.savings.EmojiChip
import com.budgetty.app.ui.savings.PrimaryPill
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.DetectedSubscription
import com.budgetty.app.ui.util.PriceHike
import com.budgetty.app.ui.util.SubscriptionCadence
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel

/** Self-contained Insights entry: fetches its own detection VM and renders the entry card. */
@Composable
fun SubscriptionsInsightsCard(onSeeAll: () -> Unit, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val vm: SubscriptionsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    SubscriptionsEntryCard(state, onSeeAll, onUnlock, modifier)
}

/** "↑ €2.00" pill beside a subscription's amount — amber, or red for a bigger jump. */
@Composable
fun PriceHikePill(hike: PriceHike, red: Boolean = false) {
    val color = if (red) budgetBadColor() else budgetWarnColor()
    val delta = (hike.newAmount - hike.oldAmount)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = "↑ " + delta.formatMoney(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun cadenceLabel(cadence: SubscriptionCadence): String = stringResource(
    if (cadence == SubscriptionCadence.YEARLY) R.string.sub_cadence_yearly else R.string.sub_cadence_monthly,
)

/** "Monthly · next ~14 Aug" */
@Composable
fun cadenceNextLabel(sub: DetectedSubscription): String =
    cadenceLabel(sub.cadence) + " · " + stringResource(R.string.sub_next, sub.nextExpectedMillis.formatDayMonth())

/**
 * The Insights entry card: a Premium summary (count, per-month/year, top merchants) or a free teaser
 * with a real count + total but redacted rows. Renders nothing when nothing's been detected.
 */
@Composable
fun SubscriptionsEntryCard(
    state: SubscriptionsUiState,
    onSeeAll: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.detected.isEmpty()) return
    if (state.isPremium) PremiumEntryCard(state, onSeeAll, modifier) else TeaserEntryCard(state, onUnlock, modifier)
}

@Composable
private fun PremiumEntryCard(state: SubscriptionsUiState, onSeeAll: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onSeeAll,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sub_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = pluralStringResource(R.plurals.sub_found_count, state.detected.size, state.detected.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                StatTile(stringResource(R.string.sub_per_month), state.monthlyTotal.formatMoney(), Modifier.weight(1f))
                StatTile(stringResource(R.string.sub_per_year), state.yearlyTotal.formatMoney(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            state.detected.take(3).forEach { sub ->
                PreviewRow(sub)
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            }
            Text(
                text = stringResource(R.string.sub_see_all, state.detected.size),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TeaserEntryCard(state: SubscriptionsUiState, onUnlock: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sub_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                LockBadge()
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = pluralStringResource(R.plurals.sub_teaser_count, state.detected.size, state.detected.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.sub_teaser_body, state.monthlyTotal.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            state.detected.take(3).forEach { sub ->
                RedactedRow(sub.emoji)
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            PrimaryPill(text = stringResource(R.string.sub_unlock), onClick = onUnlock, fillWidth = true)
            Text(
                text = stringResource(R.string.sub_unlock_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.sm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(MaterialTheme.dimens.md),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A merchant + name + cadence + amount row, with a price-hike pill where one applies. */
@Composable
fun PreviewRow(sub: DetectedSubscription, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EmojiChip(sub.emoji, size = 34.dp)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sub.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (sub.priceHike != null) {
                    Spacer(Modifier.width(6.dp))
                    PriceHikePill(sub.priceHike)
                }
            }
            Text(cadenceNextLabel(sub), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(sub.amount.formatMoney(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RedactedRow(emoji: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EmojiChip(emoji, size = 34.dp)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column(Modifier.weight(1f)) {
            Bar(width = 0.55f, height = 11.dp)
            Spacer(Modifier.height(5.dp))
            Bar(width = 0.4f, height = 8.dp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Bar(width = 0.14f, height = 11.dp)
    }
}

@Composable
private fun Bar(width: Float, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
internal fun LockBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text(stringResource(R.string.tier_premium), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
