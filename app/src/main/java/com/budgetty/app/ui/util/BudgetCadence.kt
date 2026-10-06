package com.budgetty.app.ui.util

import androidx.annotation.StringRes
import com.budgetty.app.R
import com.budgetty.app.data.repository.BudgetRepository
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The period the single top-level budget runs on — the app's governing cadence. The amount is stored
 * under [budgetKey] in [BudgetRepository]; [cardLabelRes] titles the amount card ("Fortnightly budget")
 * and [toggleLabelRes] names the segment in the three-up selector ("Fortnightly").
 *
 * [MONTHLY] is the user's pay-cycle month (unchanged). [WEEKLY] is the Mon–Sun week. [FORTNIGHTLY] is
 * a continuous 14-day window anchored on a reference pay day (see [PayCycle.fortnight]); monthly
 * figures prorate to it × 12 ÷ 26 so 26 fortnights sum to a year (see [monthlyToFortnightly]).
 *
 * The enum order is the toggle order: Weekly · Fortnightly · Monthly. Persisted by [name], so values
 * must stay stable.
 */
enum class BudgetCadence(
    val budgetKey: String,
    @param:StringRes val cardLabelRes: Int,
    @param:StringRes val toggleLabelRes: Int,
) {
    WEEKLY(BudgetRepository.WEEKLY, R.string.budget_weekly, R.string.budget_period_weekly),
    FORTNIGHTLY(BudgetRepository.FORTNIGHTLY, R.string.budget_fortnightly, R.string.budget_period_fortnightly),
    MONTHLY(BudgetRepository.MONTHLY, R.string.budget_monthly, R.string.budget_period_monthly);

    companion object {
        /**
         * Parses a persisted cadence [name]; blank or unknown → null so callers fall back (e.g. derive
         * the legacy period from which budget key is set, preserving pre-fortnightly behaviour).
         */
        fun fromName(name: String?): BudgetCadence? =
            name?.takeIf { it.isNotBlank() }?.let { runCatching { valueOf(it) }.getOrNull() }

        /**
         * The active cadence for a user who has never explicitly chosen one (blank pref): derived from
         * which legacy key is stored — Weekly only when it alone is set, Monthly otherwise. Mirrors the
         * pre-fortnightly Budget screen so an existing Weekly-only user isn't silently switched to an
         * empty Monthly budget on upgrade. Fortnightly is never auto-derived (it needs an anchor).
         */
        fun resolve(prefName: String?, budgets: Map<String, BigDecimal>): BudgetCadence =
            fromName(prefName) ?: deriveFromKeys(budgets)

        private fun deriveFromKeys(budgets: Map<String, BigDecimal>): BudgetCadence {
            val hasMonthly = budgets[BudgetRepository.MONTHLY] != null
            val hasWeekly = budgets[BudgetRepository.WEEKLY] != null
            return if (hasWeekly && !hasMonthly) WEEKLY else MONTHLY
        }
    }
}

/** The inclusive [start, end] calendar dates of this cadence's window, [offset] windows from today's. */
fun BudgetCadence.windowDates(
    today: LocalDate,
    monthStartDay: Int,
    fortnightAnchorEpochDay: Long,
    offset: Int = 0,
): Pair<LocalDate, LocalDate> = when (this) {
    BudgetCadence.MONTHLY -> PayCycle.month(today, monthStartDay, offset)
    BudgetCadence.WEEKLY -> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
        weekStart to weekStart.plusDays(6)
    }
    BudgetCadence.FORTNIGHTLY -> {
        val anchor = fortnightAnchorEpochDay.takeIf { it > 0L }
            ?: PayCycle.defaultFortnightAnchor(today, monthStartDay)
        PayCycle.fortnight(today, anchor, offset)
    }
}

/** This cadence's window as an inclusive [start, end] epoch-millis pair, for transaction queries. */
fun BudgetCadence.windowRange(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
    fortnightAnchorEpochDay: Long = 0L,
    offset: Int = 0,
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<Long, Long> {
    val (start, end) = windowDates(today, monthStartDay, fortnightAnchorEpochDay, offset)
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return startMillis to endMillis
}
