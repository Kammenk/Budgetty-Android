package com.budgetty.app.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.UpcomingBill
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal

/**
 * Home "Upcoming bills" card: the recurring payments due within a 7- or 30-day window, soonest first,
 * with their combined total. Moved here from Insights so what's due next sits alongside the budget on
 * Home. [bills] is already filtered to unpaid, date-scheduled bills (see
 * `List<RecurringEntity>.upcomingBills`); [onGoToBudget] opens the Budget screen to add/edit bills.
 */
@Composable
fun UpcomingBillsCard(
    bills: List<UpcomingBill>,
    onGoToBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.xl)) {
            var window by remember { mutableStateOf(7) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.insights_upcoming_bills),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    WindowChip(stringResource(R.string.insights_upcoming_7days), window == 7) { window = 7 }
                    WindowChip(stringResource(R.string.insights_upcoming_30days), window == 30) { window = 30 }
                }
            }
            val visible = bills.filter { it.daysUntil <= window }
            if (visible.isEmpty()) {
                UpcomingBillsEmpty(onGoToBudget)
            } else {
                val total = visible.fold(BigDecimal.ZERO) { acc, b -> acc + b.entity.amount }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(total.formatMoney(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.insights_upcoming_due, window),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                visible.take(3).forEach { UpcomingBillRow(it) }
                if (visible.size > 3) {
                    Text(
                        text = stringResource(R.string.insights_upcoming_more, visible.size - 3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun UpcomingBillRow(bill: UpcomingBill) {
    val entity = bill.entity
    val whenLabel = when (bill.daysUntil) {
        0 -> stringResource(R.string.insights_upcoming_today)
        1 -> stringResource(R.string.insights_upcoming_tomorrow)
        else -> stringResource(R.string.insights_upcoming_in_days, bill.daysUntil)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(Categories.colorOf(entity.category)).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(Categories.emojiOf(entity.category), fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                entity.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                whenLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            entity.amount.formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Shown when no bill falls inside the selected window: a nudge to add recurring payments in Budget. */
@Composable
private fun UpcomingBillsEmpty(onGoToBudget: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🗓️", fontSize = 28.sp)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.insights_upcoming_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.insights_upcoming_empty_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = "${stringResource(R.string.insights_go_to_budget)} →",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onGoToBudget)
                .padding(horizontal = MaterialTheme.dimens.sm, vertical = 4.dp),
        )
    }
}
