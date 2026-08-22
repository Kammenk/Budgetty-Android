package com.budgetty.app.ui.buyinglimits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.formatDayMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The save-time buying-limit nudge: a non-blocking floating card shown over live Home right after a
 * receipt push a keyword limit to/over its cap. No scrim — the receipt is already saved. "Got it"
 * dismisses; "View limits" opens the Buying limits screen. In-app only.
 */
@Composable
fun BuyingLimitNudgeCard(
    nudge: BuyingLimitNudge,
    monthStartDay: Int,
    onDismiss: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warn = budgetWarnColor()
    val period = stringResource(
        if (nudge.timeframe == BuyingLimitTimeframe.WEEKLY) {
            R.string.buying_limits_this_week
        } else {
            R.string.buying_limits_this_month
        },
    )
    val reset = resetText(nudge.timeframe, monthStartDay)
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXxl),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(modifier = Modifier.padding(MaterialTheme.dimens.lg), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(warn.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                if (nudge.emoji.isNotEmpty()) {
                    Text(nudge.emoji, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = warn,
                        modifier = Modifier.size(MaterialTheme.dimens.icon),
                    )
                }
            }
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.buying_limit_nudge_title,
                        nudge.countAfter,
                        nudge.title,
                        period,
                        nudge.limitCount,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.buying_limit_nudge_body, nudge.title, reset),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(MaterialTheme.dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.buying_limit_nudge_dismiss))
                    }
                    Button(
                        onClick = onView,
                        shape = RoundedCornerShape(50),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) {
                        Text(stringResource(R.string.buying_limit_nudge_view), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** "Mon" for weekly, "1 Sep" for monthly — the date the current window rolls over. */
@Composable
private fun resetText(timeframe: BuyingLimitTimeframe, monthStartDay: Int): String {
    val next = BuyingLimitCounter.nextReset(timeframe, LocalDate.now(), monthStartDay)
    return if (timeframe == BuyingLimitTimeframe.WEEKLY) {
        next.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        next.formatDayMonth()
    }
}
