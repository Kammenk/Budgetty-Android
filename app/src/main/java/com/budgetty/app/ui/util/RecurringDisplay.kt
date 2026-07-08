package com.budgetty.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.data.local.RecurringEntity
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Shared list-subtitle formatting for income & recurring payments, used by both the Budget screen
 * and the History "Budgets" tab so the two read identically.
 */

private fun cadenceLabelRes(cadence: String): Int = when (cadence) {
    RecurringEntity.Cadence.WEEKLY -> R.string.recurring_weekly
    RecurringEntity.Cadence.YEARLY -> R.string.recurring_yearly
    RecurringEntity.Cadence.ONCE -> R.string.recurring_once
    else -> R.string.recurring_monthly
}

/** When the entry lands, formatted for a list subtitle: weekday name (weekly) or day number. */
private fun dayLabel(item: RecurringEntity): String =
    if (item.cadence == RecurringEntity.Cadence.WEEKLY) {
        DayOfWeek.of(item.dueDay.coerceIn(1, 7)).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        item.dueDay.coerceIn(1, 31).toString()
    }

/**
 * "Category · Monthly · 1" for a bill, "Monthly · 25" for income (no category). A one-time entry has
 * no recurring day, so it shows the date it was added instead — "Once · 24 Jun".
 */
@Composable
fun recurringSubtitle(item: RecurringEntity, includeCategory: Boolean): String {
    val parts = buildList {
        if (includeCategory) add(categoryDisplayName(item.category))
        add(stringResource(cadenceLabelRes(item.cadence)))
        if (item.cadence == RecurringEntity.Cadence.ONCE) {
            if (item.createdAt > 0L) add(item.createdAt.formatDayMonth())
        } else {
            add(dayLabel(item))
        }
    }
    return parts.joinToString(" · ")
}
