package com.budgetty.app.ui.savings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import com.budgetty.app.R
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import androidx.compose.ui.res.stringResource
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Savings-goals section on the Budget tab: header + honesty subtitle, then the goals (each a ring
 * card) with a "New goal" affordance — or the empty prompt, or the 1-goal free cap's locked row.
 */
@Composable
fun SavingsSection(
    goals: List<SavingsGoalCardUi>,
    isPremium: Boolean,
    onGoalClick: (Long) -> Unit,
    onNewGoal: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atCap = !isPremium && goals.size >= SavingsRepository.FREE_GOAL_LIMIT
    val totalSaved = goals.fold(BigDecimal.ZERO) { acc, g -> acc + g.progress.saved }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.savings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (goals.size >= 2) {
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = goals.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.savings_total_saved, totalSaved.formatMoney()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = savingsProgressColor(behind = false),
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(R.string.savings_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))

        if (goals.isEmpty()) {
            SavingsEmptyCard(onNewGoal = onNewGoal)
        } else {
            goals.forEach { card ->
                SavingsGoalCard(card = card, onClick = { onGoalClick(card.goal.id) })
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            }
            if (atCap) {
                SavingsLockedRow(onUpgrade = onUpgrade)
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                SavingsUnlockCard(onUpgrade = onUpgrade)
            } else {
                SavingsAddRow(onClick = onNewGoal)
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(card: SavingsGoalCardUi, onClick: () -> Unit) {
    val progress = card.progress
    val color = savingsProgressColor(progress.behind)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SavingsRing(
                fraction = progress.fraction,
                color = color,
                strokeWidth = 4.2.dp,
                modifier = Modifier.size(44.dp),
            ) {
                Text(
                    text = "${(progress.fraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiChip(card.goal.emoji, size = 22.dp)
                    Spacer(Modifier.width(MaterialTheme.dimens.xs))
                    Text(
                        text = card.goal.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.savings_saved_of_target,
                        progress.saved.formatMoney(),
                        card.goal.targetAmount.formatMoney(),
                        progress.remaining.formatMoney(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SavingsPaceLine(card.goal.targetDate, progress, color)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The card's third line: "reached", or a monthly pace + on-track/behind hint, or nothing. */
@Composable
private fun SavingsPaceLine(targetDate: Long?, progress: com.budgetty.app.ui.util.SavingsProgress, color: androidx.compose.ui.graphics.Color) {
    when {
        progress.reached -> {
            Text(
                text = stringResource(R.string.savings_reached),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = savingsProgressColor(behind = false),
            )
        }
        progress.monthlyRate != null && targetDate != null -> {
            val month = Instant.ofEpochMilli(targetDate).atZone(ZoneId.systemDefault()).toLocalDate()
                .month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            Row {
                Text(
                    text = stringResource(R.string.savings_rate, progress.monthlyRate.formatMoney(), month) + " · ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        if (progress.behind) R.string.savings_behind else R.string.savings_on_track,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun EmojiChip(emoji: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SavingsEmptyCard(onNewGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmojiChip("🎯", size = 44.dp)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.savings_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.savings_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            PrimaryPill(text = stringResource(R.string.savings_new_goal), icon = Icons.Filled.Add, onClick = onNewGoal)
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.savings_free_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavingsAddRow(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.savings_new_goal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SavingsLockedRow(onUpgrade: () -> Unit) {
    Card(
        onClick = onUpgrade,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.savings_new_goal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.tier_premium),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavingsUnlockCard(onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.md)) {
            Text(
                text = stringResource(R.string.savings_unlock_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = stringResource(R.string.savings_unlock_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            PrimaryPill(text = stringResource(R.string.go_premium), onClick = onUpgrade, fillWidth = true)
        }
    }
}

/** A filled primary pill button matching the app's fully-rounded action-button convention. */
@Composable
internal fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    fillWidth: Boolean = false,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.dimens.xs))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}
