package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Shared money-flow math for income & recurring payments, so the Budget screen and the History
 * "Budgets" tab compute identical monthly totals (and treat one-time entries the same way).
 */

/**
 * Inclusive [start, end] epoch-millis window for [today]'s pay-cycle month, which starts on
 * [monthStartDay] (1 = the ordinary calendar month). Shifting it moves the monthly budget and the
 * recurring plan's "current month" onto the user's pay day. See [PayCycle].
 */
fun currentMonthRange(today: LocalDate = LocalDate.now(), monthStartDay: Int = 1): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val (start, end) = PayCycle.month(today, monthStartDay)
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return startMillis to endMillis
}

/**
 * Whether this bill has been marked paid for its CURRENT occurrence — i.e. [RecurringEntity.lastPosted]
 * (set to "now" when the user taps Paid) falls inside the window of the occurrence that contains
 * [today]: the pay-cycle month for a monthly bill, the Mon–Sun week for a weekly one, the calendar
 * year for a yearly one. It therefore resets on its own when the next occurrence begins — last cycle's
 * timestamp lands outside the new window — with no scheduled job. A one-time entry stays paid once set.
 */
fun RecurringEntity.isPaidThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    if (lastPosted <= 0L) return false
    val (start, end) = when (cadence) {
        RecurringEntity.Cadence.ONCE -> return true
        RecurringEntity.Cadence.WEEKLY -> {
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            weekStart to weekStart.plusDays(6)
        }
        RecurringEntity.Cadence.YEARLY -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        else -> PayCycle.month(today, monthStartDay) // MONTHLY
    }
    val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return lastPosted in startMs..endMs
}

/** The date within [today]'s pay-cycle month whose day-of-month is [day] (clamped to the month). */
private fun dueDateThisCycle(today: LocalDate, monthStartDay: Int, day: Int): LocalDate {
    val (start, end) = PayCycle.month(today, monthStartDay)
    val d = day.coerceIn(1, 31)
    val candidate = clampDay(YearMonth.from(start), d)
    return if (!candidate.isBefore(start) && !candidate.isAfter(end)) candidate
    else clampDay(YearMonth.from(end), d)
}

/**
 * Whether this bill's due date within its current occurrence has already arrived (today on/after it):
 * the [RecurringEntity.dueDay]-th of the pay-cycle month for a monthly bill, the [dueDay] weekday of
 * this Mon–Sun week for a weekly one. Yearly (stores no month) and one-offs return false — they have
 * no computable due date to auto-mark against.
 */
fun RecurringEntity.isDuePassedThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): Boolean = when (cadence) {
    RecurringEntity.Cadence.MONTHLY -> !today.isBefore(dueDateThisCycle(today, monthStartDay, dueDay))
    RecurringEntity.Cadence.WEEKLY -> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        !today.isBefore(weekStart.plusDays((dueDay.coerceIn(1, 7) - 1).toLong()))
    }
    else -> false
}

/**
 * Whether a bill counts as paid for this cycle: either manually marked ([isPaidThisCycle]) or, when
 * [RecurringEntity.autoPay] is on, its due date has already passed ([isDuePassedThisCycle]). Auto-pay
 * only fills paid in once the day arrives; it never clears a manual payment.
 */
fun RecurringEntity.isEffectivelyPaidThisCycle(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): Boolean = isPaidThisCycle(today, monthStartDay) ||
    (autoPay && isDuePassedThisCycle(today, monthStartDay))

/**
 * Whether autopay can apply to this entry at all — independent of the [RecurringEntity.autoPay]
 * switch. Bills only (never income), and only monthly or weekly cadences: yearly stores no month and
 * one-offs don't recur, so neither has a due date to auto-mark against.
 */
fun RecurringEntity.autoPayEligible(): Boolean =
    !isIncome &&
        (cadence == RecurringEntity.Cadence.MONTHLY || cadence == RecurringEntity.Cadence.WEEKLY)

/**
 * True when autopay is both switched on and [autoPayEligible] for this entry's cadence — the single
 * source of truth for "this bill is auto-managed". Used when persisting (so a yearly/one-off entry
 * can never keep a stuck autopay flag) and when displaying (so the Budget row shows a manual paid
 * toggle, not the "Auto" chip, for any ineligible entry — including rows saved before the rule was
 * enforced).
 */
fun RecurringEntity.isAutoPayActive(): Boolean = autoPay && autoPayEligible()

/**
 * The entry's amount expressed per month (weekly ×52/12, yearly ÷12), for the totals/breakdown.
 * A one-time ([RecurringEntity.Cadence.ONCE]) entry counts its full amount only in the calendar
 * month it was added ([monthStart]..[monthEnd]) and zero afterwards, so a variable monthly wage or
 * a one-off bonus lands in that month's plan without carrying over into future months.
 */
fun RecurringEntity.monthlyAmount(monthStart: Long, monthEnd: Long): BigDecimal = when (cadence) {
    RecurringEntity.Cadence.WEEKLY ->
        amount.multiply(BigDecimal(52)).divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
    RecurringEntity.Cadence.YEARLY -> amount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
    RecurringEntity.Cadence.ONCE -> if (createdAt in monthStart..monthEnd) amount else BigDecimal.ZERO
    else -> amount
}

/**
 * The entry's contribution to the inclusive [windowStart]..[windowEnd] epoch-millis window, counting
 * a recurring cadence only from the month it was added ([RecurringEntity.createdAt]) onward. A salary
 * or bill is therefore never projected backward onto months it didn't yet exist for — a half-year
 * view no longer shows 6× a salary that was only just added — while a one-time entry still counts its
 * full amount exactly once, and only when it was added inside the window. Used by the Insights
 * money-flow cards and the History Budgets snapshot so both scale a plan to the selected range using
 * only what's actually known, rather than predicting the past.
 *
 * A window that is a whole number of monthly steps from its own start day — the calendar month, the
 * user's pay-cycle month (e.g. the 10th → the 9th) and quarters/halves — counts by months, so one
 * such period is exactly one month's rate regardless of its 28–31 day length; only genuinely partial
 * windows (a week step, a custom range) are day-scaled.
 */
fun RecurringEntity.windowAmount(
    windowStart: Long,
    windowEnd: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): BigDecimal {
    if (cadence == RecurringEntity.Cadence.ONCE) {
        return if (createdAt in windowStart..windowEnd) amount else BigDecimal.ZERO
    }
    // Recurring cadences ignore the month bounds passed to monthlyAmount — it returns the per-month rate.
    val rate = monthlyAmount(windowStart, windowEnd)
    if (rate.signum() == 0) return BigDecimal.ZERO

    val startDate = Instant.ofEpochMilli(windowStart).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(windowEnd).atZone(zone).toLocalDate()
    val createdDate = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
    val createdMonth = YearMonth.from(createdDate)
    // Clip the window to the part on/after the month the entry was added; nothing left ⇒ no contribution.
    val activeStart = maxOf(startDate, createdMonth.atDay(1))
    if (activeStart.isAfter(endDate)) return BigDecimal.ZERO

    // Whole-month window: an exact integer number of monthly steps from the window's own start day.
    // This covers the calendar month (1st → next 1st) AND the user's pay-cycle month (e.g. the 10th →
    // the 9th), as well as quarters/halves — so a single such period counts as exactly one month's rate
    // whether it spans 28 or 31 days, instead of being day-scaled. [months] == 0 ⇒ a genuine partial
    // window (a week step or a custom range). A pay day clamped into a short month (29–31) can leave a
    // cycle boundary that isn't a clean month step; that rare case falls back to day-scaling below.
    val endExclusive = endDate.plusDays(1)
    val months = ChronoUnit.MONTHS.between(startDate, endExclusive)
    val wholeMonths = months >= 1 && startDate.plusMonths(months) == endExclusive
    return if (wholeMonths) {
        // Count only the month-steps whose occurrence began on/after the entry was added, so a plan is
        // never projected back onto cycles before it existed (matches the calendar-month clip exactly).
        val eligible = (0L until months).count { step -> createdDate.isBefore(startDate.plusMonths(step + 1)) }
        rate.multiply(BigDecimal(eligible)).setScale(2, RoundingMode.HALF_UP)
    } else {
        // Partial window (a week step or a custom range): scale by active days over an average month,
        // matching the Insights custom-range factor.
        val activeDays = ChronoUnit.DAYS.between(activeStart, endDate) + 1
        rate.multiply(BigDecimal(activeDays)).divide(BigDecimal("30.4375"), 2, RoundingMode.HALF_UP)
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────────────
// Planned-bills overlay (Insights): projecting recurring bills onto a period as a distinct "planned"
// layer, and de-duplicating a bill that the user also logged/scanned so it's never double-counted.
// Pure Kotlin (no Android/Compose deps) so the dedup rule is unit-testable on the host.
// ─────────────────────────────────────────────────────────────────────────────────────────────────

/** One recurring bill projected onto the selected Insights window, for the planned overlay. */
data class PlannedBillLine(
    val label: String,
    val category: String,
    /** The bill's contribution to the whole selected window ([windowAmount]) — what the overlay
     *  draws and sums (e.g. one month's rent for a month view). */
    val amount: BigDecimal,
    /** The bill's single-occurrence amount ([RecurringEntity.amount]) — what one real receipt for it
     *  would show, used only for dedup matching (never for display). */
    val matchAmount: BigDecimal,
)

/** The receipt-side signal the dedup matcher compares bills against: one per receipt in the window,
 *  its normalized [merchant], paid [amount] (total) and [dateMillis]. Mirrors the per-receipt "charge"
 *  the subscription detector already builds, so bill↔receipt matching is consistent across features. */
data class ReceiptCharge(
    val merchant: String,
    val amount: BigDecimal,
    val dateMillis: Long,
)

/** A planned bill excluded from the overlay because it already matches a real receipt in the window —
 *  so it's counted once, in spend, not twice. Surfaced in the Breakdown dialog's dedup note as
 *  "{label} matched · {date} {amount}" (the matched receipt's actual total and date). */
data class MatchedBillLine(
    val label: String,
    val amount: BigDecimal,
    val dateMillis: Long,
)

/** The split of a period's recurring bills into the [visible] planned layer (largest first) and the
 *  [matched] bills hidden as already-counted-in-spend. */
data class PlannedBillsSplit(
    val visible: List<PlannedBillLine>,
    val matched: List<MatchedBillLine>,
)

/**
 * Splits [bills] into the planned layer the overlay draws and the bills already represented by a real
 * receipt in the same window ([PlannedBillsSplit.matched]) — so a bill the user both *planned* and
 * *logged/scanned* is counted once (in spend), never twice.
 *
 * A bill matches a [charge] when **both** hold: their names align (each [normalizeMerchant]-d, one
 * containing the other as a substring of at least [MIN_MATCH_NAME_LEN] chars — "Spotify" ↔ "Spotify")
 * and the charge total is within [amountTolerance] of the bill's single-occurrence [matchAmount] (the
 * printed examples: Spotify €10.99, Water €18.40). Each charge is consumed by at most one bill (the
 * closest by amount), so two same-name bills can't both claim one receipt.
 *
 * Deliberately conservative: an unmatched bill stays *visible* rather than risk hiding a genuinely
 * unpaid plan and understating the planned story — the dedup note is a trust anchor, so a false hide
 * costs more than a missed one. Bills with a non-positive window [amount] (e.g. a plan not yet created
 * for this window — no back-projection) are dropped entirely.
 */
fun splitPlannedBills(bills: List<PlannedBillLine>, charges: List<ReceiptCharge>): PlannedBillsSplit {
    val present = bills.filter { it.amount.signum() > 0 }
    val visible = mutableListOf<PlannedBillLine>()
    val matched = mutableListOf<MatchedBillLine>()
    val available = charges.toMutableList()
    for (bill in present.sortedByDescending { it.amount }) {
        val billName = normalizeMerchant(bill.label)
        val hit = available
            .filter { charge ->
                namesAlign(billName, normalizeMerchant(charge.merchant)) &&
                    amountsClose(charge.amount, bill.matchAmount)
            }
            .minByOrNull { charge -> (charge.amount - bill.matchAmount).abs() }
        if (hit != null) {
            available.remove(hit)
            matched += MatchedBillLine(label = bill.label, amount = hit.amount, dateMillis = hit.dateMillis)
        } else {
            visible += bill
        }
    }
    return PlannedBillsSplit(
        visible = visible.sortedByDescending { it.amount },
        matched = matched.sortedByDescending { it.dateMillis },
    )
}

/** Shortest normalized name length that may match, so 1–2 char noise ("dm" aside) can't false-match. */
private const val MIN_MATCH_NAME_LEN = 3

/** Lowercased, brand-canonicalized merchant/label for comparison (so "Netflix" ↔ a Netflix receipt). */
private fun normalizeMerchant(raw: String): String =
    com.budgetty.app.store.StoreNormalizer.normalize(raw).lowercase().trim()

/** Whether two normalized names refer to the same merchant: equal, or one contains the other where the
 *  shorter is at least [MIN_MATCH_NAME_LEN] chars (so "spotify" ↔ "spotify premium" match). */
private fun namesAlign(a: String, b: String): Boolean {
    if (a.length < MIN_MATCH_NAME_LEN || b.length < MIN_MATCH_NAME_LEN) return a == b && a.isNotEmpty()
    return a == b || a.contains(b) || b.contains(a)
}

/** Whether a receipt total is close enough to a bill's occurrence amount to be the same payment:
 *  within the greater of €2 or 15% of the bill (covers variable utilities without over-matching). */
private fun amountsClose(chargeTotal: BigDecimal, billAmount: BigDecimal): Boolean {
    val tolerance = billAmount.abs().multiply(BigDecimal("0.15")).max(BigDecimal("2.00"))
    return (chargeTotal - billAmount).abs() <= tolerance
}

/** A recurring bill paired with the whole days until its next occurrence (0 = today), for the Home
 *  "Upcoming bills" card. */
data class UpcomingBill(
    val entity: RecurringEntity,
    val daysUntil: Int,
)

/**
 * The recurring bills (non-income) due soon, soonest first: each bill not already marked paid for
 * this cycle, paired with the whole days until its next occurrence. Only monthly and weekly bills
 * have a computable next date; yearly bills (which store no month) and one-offs are omitted.
 */
fun List<RecurringEntity>.upcomingBills(
    today: LocalDate = LocalDate.now(),
    monthStartDay: Int = 1,
): List<UpcomingBill> =
    filterNot { it.isIncome }
        // A bill paid for this cycle (manually or via autopay) drops off "upcoming" until next time.
        .filterNot { it.isEffectivelyPaidThisCycle(today, monthStartDay) }
        .mapNotNull { bill -> bill.nextOccurrenceDays(today)?.let { UpcomingBill(bill, it) } }
        .sortedBy { it.daysUntil }

/** Days from [today] to this bill's next occurrence, or null when it has no monthly/weekly schedule
 *  (yearly bills store no month, one-offs don't recur). */
fun RecurringEntity.nextOccurrenceDays(today: LocalDate = LocalDate.now()): Int? = when (cadence) {
    RecurringEntity.Cadence.MONTHLY -> {
        val day = dueDay.coerceIn(1, 31)
        var date = clampDay(YearMonth.from(today), day)
        if (date.isBefore(today)) date = clampDay(YearMonth.from(today).plusMonths(1), day)
        ChronoUnit.DAYS.between(today, date).toInt()
    }
    RecurringEntity.Cadence.WEEKLY -> {
        val target = dueDay.coerceIn(1, 7) // 1=Mon … 7=Sun
        var date = today
        while (date.dayOfWeek.value != target) date = date.plusDays(1)
        ChronoUnit.DAYS.between(today, date).toInt()
    }
    else -> null
}

private fun clampDay(month: YearMonth, day: Int): LocalDate =
    month.atDay(day.coerceAtMost(month.lengthOfMonth()))
