package com.budgetty.app.ui.recap

import com.budgetty.app.data.settings.RecapFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pins the end-of-period recap trigger: due/not-due per frequency against the last-shown keys and a
 * period boundary (monthly via the pay-cycle, weekly via the locale first-day-of-week), the
 * both-due-same-open rule, and the first-run / not-enough-data guard. This is the contract the iOS
 * port must mirror — the story UI is downstream of it.
 */
class RecapSchedulerTest {

    private val monday = DayOfWeek.MONDAY

    // ── Period-id helpers ────────────────────────────────────────────────────────

    @Test
    fun justClosedMonthId_calendarCycle_isPreviousCalendarMonth() {
        // 1 Aug, calendar cycle → the month that just closed is July.
        assertEquals("2026-07", RecapScheduler.justClosedMonthId(LocalDate.of(2026, 8, 1), monthStartDay = 1))
        // Mid-August is still inside August's cycle → July is still the just-closed one.
        assertEquals("2026-07", RecapScheduler.justClosedMonthId(LocalDate.of(2026, 8, 20), monthStartDay = 1))
    }

    @Test
    fun justClosedMonthId_payCycle_followsStartDay() {
        // Pay day = 25th. On 24 Aug we're still in the cycle that opened 25 Jul, so the just-closed
        // cycle is the one that opened 25 Jun ("2026-06").
        assertEquals("2026-06", RecapScheduler.justClosedMonthId(LocalDate.of(2026, 8, 24), monthStartDay = 25))
        // On 25 Aug the new cycle opens, so the just-closed cycle is the one that opened 25 Jul.
        assertEquals("2026-07", RecapScheduler.justClosedMonthId(LocalDate.of(2026, 8, 25), monthStartDay = 25))
    }

    @Test
    fun justClosedWeekId_isPreviousWeekStart() {
        // Wed 22 Jul 2026 → this week started Mon 20 Jul → just-closed week started Mon 13 Jul.
        assertEquals("2026-07-13", RecapScheduler.justClosedWeekId(LocalDate.of(2026, 7, 22), monday))
        // Sunday belongs to the week that started the previous Monday (13 Jul) → just-closed = 6 Jul.
        assertEquals("2026-07-06", RecapScheduler.justClosedWeekId(LocalDate.of(2026, 7, 19), monday))
    }

    // ── Monthly cadence ──────────────────────────────────────────────────────────

    @Test
    fun monthly_due_whenJustClosedMonthNotYetShown() {
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.MONTHLY,
            lastShownWeek = "", lastShownMonth = "2026-06",
            today = LocalDate.of(2026, 8, 1), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.MONTHLY, due?.show)
        assertEquals("2026-07", due?.markMonth)
        // Weekly isn't part of MONTHLY frequency, so no week is marked.
        assertNull(due?.markWeek)
    }

    @Test
    fun monthly_notDue_whenAlreadyShownThisCycle() {
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.MONTHLY,
            lastShownWeek = "", lastShownMonth = "2026-07",
            today = LocalDate.of(2026, 8, 10), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertNull(due)
    }

    @Test
    fun monthly_frequency_neverFiresWeekly() {
        // A new week has closed, but a Monthly-only user gets no weekly recap. The month that closed
        // (July, via the 1 Aug boundary) is already stamped, so nothing is due at all.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.MONTHLY,
            lastShownWeek = "", lastShownMonth = "2026-07",
            today = LocalDate.of(2026, 8, 5), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertNull(due)
    }

    // ── Weekly cadence ───────────────────────────────────────────────────────────

    @Test
    fun weekly_due_whenJustClosedWeekNotYetShown() {
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.WEEKLY,
            lastShownWeek = "2026-07-06", lastShownMonth = "",
            today = LocalDate.of(2026, 7, 22), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.WEEKLY, due?.show)
        assertEquals("2026-07-13", due?.markWeek)
        assertNull(due?.markMonth)
    }

    @Test
    fun weekly_notDue_whenAlreadyShownThisWeek() {
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.WEEKLY,
            lastShownWeek = "2026-07-13", lastShownMonth = "",
            today = LocalDate.of(2026, 7, 22), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertNull(due)
    }

    @Test
    fun weekly_frequency_neverFiresMonthly() {
        // A month closed (1 Aug boundary), but a Weekly-only user gets no monthly recap. The just-closed
        // week as of 5 Aug (Mon 27 Jul) is already stamped, so nothing is due.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.WEEKLY,
            lastShownWeek = "2026-07-27", lastShownMonth = "",
            today = LocalDate.of(2026, 8, 5), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertNull(due)
    }

    // ── Both cadences ────────────────────────────────────────────────────────────

    @Test
    fun both_dueSameOpen_showsMonthlyAndMarksBoth() {
        // 1 Aug: a month AND a week have closed and neither is shown → show Monthly, mark both.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.BOTH,
            lastShownWeek = "2026-07-06", lastShownMonth = "2026-06",
            today = LocalDate.of(2026, 8, 1), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.MONTHLY, due?.show)
        assertEquals("2026-07", due?.markMonth)
        // The just-closed week as of 1 Aug (Sat) started Mon 20 Jul.
        assertEquals("2026-07-20", due?.markWeek)
    }

    @Test
    fun both_onlyWeeklyDue_showsWeekly() {
        // The just-closed month (June, as of 22 Jul) is already shown, but a fresh week has closed → weekly only.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.BOTH,
            lastShownWeek = "2026-07-06", lastShownMonth = "2026-06",
            today = LocalDate.of(2026, 7, 22), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.WEEKLY, due?.show)
        assertEquals("2026-07-13", due?.markWeek)
        assertNull(due?.markMonth)
    }

    @Test
    fun both_onlyMonthlyDue_marksOnlyMonth() {
        // Week already shown, month fresh → monthly only, week not re-marked.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.BOTH,
            lastShownWeek = "2026-07-20", lastShownMonth = "2026-06",
            today = LocalDate.of(2026, 8, 1), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.MONTHLY, due?.show)
        assertEquals("2026-07", due?.markMonth)
        assertNull(due?.markWeek)
    }

    // ── Enabled flag ─────────────────────────────────────────────────────────────

    @Test
    fun disabled_neverDue() {
        val due = RecapScheduler.due(
            enabled = false, frequency = RecapFrequency.BOTH,
            lastShownWeek = "", lastShownMonth = "",
            today = LocalDate.of(2026, 8, 1), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertNull(due)
    }

    @Test
    fun freshInstall_bothEmpty_isDue_soTheGuardCanSkipAndStamp() {
        // Empty last-shown keys mean the current just-closed periods look "due"; the data guard then
        // skips a first-run user and stamps them, so no empty recap ever shows.
        val due = RecapScheduler.due(
            enabled = true, frequency = RecapFrequency.MONTHLY,
            lastShownWeek = "", lastShownMonth = "",
            today = LocalDate.of(2026, 8, 1), monthStartDay = 1, firstDayOfWeek = monday,
        )
        assertEquals(RecapKind.MONTHLY, due?.show)
        assertEquals("2026-07", due?.markMonth)
    }

    // ── Data guard ───────────────────────────────────────────────────────────────

    @Test
    fun guard_underReceiptFloor_skips() {
        assertEquals(
            RecapGuard.Skip,
            RecapDataGuard.evaluate(totalReceipts = 4, periodHasSpend = true, priorPeriodHasSpend = true),
        )
    }

    @Test
    fun guard_periodHadNoSpend_skips() {
        assertEquals(
            RecapGuard.Skip,
            RecapDataGuard.evaluate(totalReceipts = 30, periodHasSpend = false, priorPeriodHasSpend = true),
        )
    }

    @Test
    fun guard_noPriorPeriod_showsPartial() {
        assertEquals(
            RecapGuard.Show(withComparison = false),
            RecapDataGuard.evaluate(totalReceipts = 8, periodHasSpend = true, priorPeriodHasSpend = false),
        )
    }

    @Test
    fun guard_enoughDataWithPrior_showsFull() {
        assertEquals(
            RecapGuard.Show(withComparison = true),
            RecapDataGuard.evaluate(totalReceipts = 40, periodHasSpend = true, priorPeriodHasSpend = true),
        )
    }

    @Test
    fun guard_floorMatchesWellbeing() {
        // Exactly at the floor is enough (5 = MIN_RECEIPTS).
        assertEquals(
            RecapGuard.Show(withComparison = true),
            RecapDataGuard.evaluate(
                totalReceipts = RecapDataGuard.MIN_RECEIPTS,
                periodHasSpend = true,
                priorPeriodHasSpend = true,
            ),
        )
    }
}
