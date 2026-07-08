package com.budgetty.app.data.ingest

import android.net.Uri

/** Turns a single uploaded receipt ([Uri]) into an editable [ParsedReceipt]. */
interface ReceiptExtractor {
    suspend fun extract(uri: Uri): ParsedReceipt
}

/**
 * Thrown when the receipt itself couldn't be read: the photo is too poor to extract, or the model
 * abstained / would have confabulated (see [HaikuReceiptExtractor.validateExtraction]). Kept
 * distinct from a backend/service failure (an HTTP or network error from the Cloud Function proxy,
 * e.g. an Anthropic outage or exhausted credits) so the UI can tell the user to retake the photo
 * rather than to try again later — the two failures have different fixes.
 */
class ReceiptUnreadableException(message: String) : Exception(message)
