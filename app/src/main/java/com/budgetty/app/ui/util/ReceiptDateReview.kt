package com.budgetty.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * How recent a just-scanned receipt's purchase date is expected to be. A date more than this many days
 * before "today" — or any date in the future — is treated as a likely misread and surfaced for review
 * instead of being saved silently. A soft nudge, not a hard limit.
 */
const val RECEIPT_DATE_REVIEW_WINDOW_DAYS = 45L

/**
 * Whether the extracted receipt [dateMillis] looks implausible for a receipt scanned around [today] and
 * should be flagged for the user to confirm before saving.
 *
 * A freshly scanned receipt is almost always at most a few weeks old, so a date in the FUTURE or more
 * than [RECEIPT_DATE_REVIEW_WINDOW_DAYS] in the past is usually the extractor misreading the printed
 * date — a wrong month (8 Sep read as 8 Apr), or an unrelated date/year lifted off the receipt
 * (copyright, loyalty, card expiry, registration). Callers only highlight the date on the strength of
 * this; it is never auto-corrected, so a genuine old receipt still saves with the date the user picks.
 *
 * Pure — [today] and [zone] are injected — so the boundary behaviour can be unit-tested.
 */
fun receiptDateNeedsReview(
    dateMillis: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    val day = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
    return day.isAfter(today) || day.isBefore(today.minusDays(RECEIPT_DATE_REVIEW_WINDOW_DAYS))
}
