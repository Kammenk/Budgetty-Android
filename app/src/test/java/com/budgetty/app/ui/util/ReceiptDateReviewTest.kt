package com.budgetty.app.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * [receiptDateNeedsReview] flags a scanned receipt's date for confirmation when it can't plausibly be a
 * recent purchase — a future date, or one older than [RECEIPT_DATE_REVIEW_WINDOW_DAYS] — so a misread
 * month or year is caught before saving. The two "flagged" cases below are the tester receipts that
 * regressed: 08.09.2026 misread as 8 Apr 2026 (wrong month, right year — which the earlier year-only
 * check missed) and 01.09.2026 misread as 1 Sep 2020 (wrong year).
 */
class ReceiptDateReviewTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 9, 9)

    private fun millis(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun needsReview(date: LocalDate) = receiptDateNeedsReview(millis(date), today, zone)

    @Test
    fun `today is not flagged`() {
        assertFalse(needsReview(today))
    }

    @Test
    fun `a recent receipt within the window is not flagged`() {
        assertFalse(needsReview(LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `the far edge of the window is still accepted`() {
        assertFalse(needsReview(today.minusDays(RECEIPT_DATE_REVIEW_WINDOW_DAYS)))
    }

    @Test
    fun `just past the window is flagged`() {
        assertTrue(needsReview(today.minusDays(RECEIPT_DATE_REVIEW_WINDOW_DAYS + 1)))
    }

    @Test
    fun `wrong month within the current year is flagged`() {
        assertTrue(needsReview(LocalDate.of(2026, 4, 8)))
    }

    @Test
    fun `wrong year is flagged`() {
        assertTrue(needsReview(LocalDate.of(2020, 9, 1)))
    }

    @Test
    fun `a future date is flagged`() {
        assertTrue(needsReview(LocalDate.of(2026, 12, 25)))
    }
}
