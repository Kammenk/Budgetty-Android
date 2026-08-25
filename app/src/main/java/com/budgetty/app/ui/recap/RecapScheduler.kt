package com.budgetty.app.ui.recap

import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.ui.util.PayCycle
import com.budgetty.app.ui.wellbeing.WellbeingEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Which cadence a recap covers. Monthly is the full report card; weekly is a lighter momentum check. */
enum class RecapKind { MONTHLY, WEEKLY }

/**
 * A recap that is due on this app open. [show] is the one to display — Monthly wins when both are due,
 * because it's the fuller report — while [markWeek]/[markMonth] are the period ids to stamp as shown.
 * Both are stamped even when only one screen appears, so the other cadence doesn't stack a second
 * interstitial in the same open.
 */
data class RecapDue(
    val show: RecapKind,
    val markWeek: String?,
    val markMonth: String?,
)

/**
 * Pure scheduler for the end-of-period recap: decides which recap (if any) is due on an app open, from
 * the user's cadence + the last-shown period keys + the clock. No Android, no DB, no Compose — so the
 * whole "when does it fire" contract is unit-testable on the JVM and ports 1:1 to iOS.
 *
 * It is deliberately split from the data check: this decides the *boundary* has been crossed;
 * [RecapDataGuard] decides — once the DB has loaded — whether there's enough data worth showing.
 * The interstitial is modelled on the onboarding/quiz gate + a [com.budgetty.app.review.ReviewTracker]
 * -style last-shown key: shown once per completed period, on first open on/after the boundary.
 */
object RecapScheduler {

    private val MONTH_ID: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /**
     * The pay-cycle month that has just closed as of [today] — the one a monthly recap is about. This
     * is offset −1 from the cycle containing today, so on/after the pay-cycle boundary it names the
     * month that ended (e.g. opening on 1 Aug with a calendar cycle → "2026-07").
     */
    fun justClosedMonthId(today: LocalDate, monthStartDay: Int): String {
        val (start, _) = PayCycle.month(today, monthStartDay, offset = -1)
        return YearMonth.from(start).format(MONTH_ID)
    }

    /**
     * Start date (ISO yyyy-MM-dd) of the week that has just closed as of [today] — the week before the
     * one containing today, anchored on the locale's [firstDayOfWeek]. Its stability across a whole
     * week is what makes it a per-week key.
     */
    fun justClosedWeekId(today: LocalDate, firstDayOfWeek: DayOfWeek): String {
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return currentWeekStart.minusWeeks(1).toString()
    }

    /**
     * The recap due on this open, or null when none is (disabled, or the just-closed period's recap
     * was already shown). When both cadences are due, [RecapDue.show] is [RecapKind.MONTHLY] but BOTH
     * ids are returned to mark, so the weekly doesn't also fire in the same open.
     */
    fun due(
        enabled: Boolean,
        frequency: RecapFrequency,
        lastShownWeek: String,
        lastShownMonth: String,
        today: LocalDate,
        monthStartDay: Int,
        firstDayOfWeek: DayOfWeek,
    ): RecapDue? {
        if (!enabled) return null
        val monthId = justClosedMonthId(today, monthStartDay)
        val weekId = justClosedWeekId(today, firstDayOfWeek)
        val monthlyDue = frequency != RecapFrequency.WEEKLY && lastShownMonth != monthId
        val weeklyDue = frequency != RecapFrequency.MONTHLY && lastShownWeek != weekId
        return when {
            monthlyDue -> RecapDue(
                show = RecapKind.MONTHLY,
                markWeek = weekId.takeIf { weeklyDue },
                markMonth = monthId,
            )
            weeklyDue -> RecapDue(show = RecapKind.WEEKLY, markWeek = weekId, markMonth = null)
            else -> null
        }
    }
}

/** Outcome of the data check: skip entirely, or show — with or without the vs-previous comparison. */
sealed interface RecapGuard {
    /** Not enough data to be worth showing (under the scoring floor, or the period had no spend). */
    data object Skip : RecapGuard

    /** Enough to show. [withComparison] is false when there's no prior period to compare against, so
     *  the story drops the comparison-dependent cards rather than showing an empty comparison. */
    data class Show(val withComparison: Boolean) : RecapGuard
}

/**
 * Pure first-run / not-enough-data guard, applied once the DB has loaded. Under the wellbeing scoring
 * floor ([WellbeingEngine.MIN_RECEIPTS_TO_SCORE] = 5 receipts), or when the just-closed period itself
 * had no spend, the recap is skipped (and marked shown so it isn't re-checked every open). With data
 * but no prior period to compare, a partial recap is shown that drops the comparison cards.
 */
object RecapDataGuard {

    /** Kept in step with the wellbeing score's first-run floor so the two features agree. */
    const val MIN_RECEIPTS: Int = WellbeingEngine.MIN_RECEIPTS_TO_SCORE

    fun evaluate(totalReceipts: Int, periodHasSpend: Boolean, priorPeriodHasSpend: Boolean): RecapGuard {
        if (totalReceipts < MIN_RECEIPTS || !periodHasSpend) return RecapGuard.Skip
        return RecapGuard.Show(withComparison = priorPeriodHasSpend)
    }
}
