package com.budgetty.app.data.ingest

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts transactions from a text-layer PDF using PDFBox-Android.
 *
 * Note: scanned-image PDFs without a text layer yield no text and therefore no
 * transactions — that is an accepted v1 limitation.
 */
class PdfReceiptExtractor(
    private val context: Context,
    private val parser: ReceiptParser,
) : ReceiptExtractor {

    override suspend fun extract(uri: Uri): ParsedReceipt = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context.applicationContext)
        val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                PDFTextStripper().getText(document)
            }
        } ?: ""
        ParsedReceipt(items = parser.parse(rawText))
    }
}
