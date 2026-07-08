package com.budgetty.app.data.ingest

import android.content.Context
import android.net.Uri

/**
 * Routes an uploaded receipt to the Haiku-backed extractor, which handles both images
 * and PDFs via the Cloud Function proxy.
 *
 * [PdfReceiptExtractor] (text-layer PDF parsing) is kept on disk but is not wired in here.
 */
class ReceiptIngestManager(
    private val context: Context,
    private val haikuExtractor: HaikuReceiptExtractor,
) {
    class UnsupportedReceiptException(mimeType: String?) :
        Exception("Unsupported receipt type: ${mimeType ?: "unknown"}")

    suspend fun extract(uri: Uri): ParsedReceipt {
        val mimeType = context.contentResolver.getType(uri)

        return when {
            mimeType == "application/pdf" -> haikuExtractor.extract(uri)
            mimeType?.startsWith("image/") == true -> haikuExtractor.extract(uri)
            else -> throw UnsupportedReceiptException(mimeType)
        }
    }
}
