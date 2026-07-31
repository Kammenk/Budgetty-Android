package com.budgetty.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/** One exported line (per receipt): date, store, its dominant category, and the paid total. */
data class ExportRow(
    val dateMillis: Long,
    val dateLabel: String,
    val store: String,
    val category: String,
    val colorArgb: Int,
    val amount: BigDecimal,
)

/** A category's share of the period's spend, for the statement's summary. */
data class ExportCategory(
    val name: String,
    val emoji: String,
    val colorArgb: Int,
    val total: BigDecimal,
    val pct: Int,
)

/** Everything a CSV or PDF export needs, already resolved to display strings. */
data class ExportData(
    val periodLabel: String,
    val generatedLabel: String,
    val currencySymbol: String,
    val rows: List<ExportRow>,
    val totalSpent: BigDecimal,
    val income: BigDecimal,
    val net: BigDecimal,
    val byCategory: List<ExportCategory>,
    val totalRowLabel: String,
)

/**
 * Turns [ExportData] into a CSV spreadsheet or a one-page-plus branded PDF statement — both built on
 * device with no network. The PDF is drawn with [PdfDocument] (native, no libraries) and paginates the
 * transaction table across A4 pages.
 */
object DataExporter {

    // ── PDF ──

    private const val PAGE_W = 595 // A4 at 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private val PRIMARY = Color.rgb(0x66, 0x50, 0xA4)
    private val GOOD = Color.rgb(0x2E, 0x7D, 0x32)
    private val INK = Color.rgb(0x1D, 0x1B, 0x20)
    private val MUTED = Color.rgb(0x6E, 0x68, 0x78)
    private val TILE_BG = Color.rgb(0xF7, 0xF2, 0xFA)
    private val LINE = Color.rgb(0xE3, 0xDD, 0xEA)
    private val ZEBRA = Color.rgb(0xFA, 0xF8, 0xFC)

    fun renderPdf(data: ExportData, out: File) {
        val doc = PdfDocument()
        val rowsPerLaterPage = 46
        // Rows that fit on page 1 (after the header/tiles/category section) vs later full pages.
        val firstPageRows = 16
        val pages = paginate(data.rows, firstPageRows, rowsPerLaterPage)
        pages.forEachIndexed { index, pageRows ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, index + 1).create())
            drawPage(page.canvas, data, pageRows, index == 0, index + 1, pages.size)
            doc.finishPage(page)
        }
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    /** Renders page 1 of the statement to a bitmap (for previews / screenshot tests). */
    fun renderBitmap(data: ExportData): Bitmap {
        val bmp = Bitmap.createBitmap(PAGE_W, PAGE_H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val pages = paginate(data.rows, 16, 46)
        drawPage(c, data, pages.first(), firstPage = true, pageNo = 1, pageCount = pages.size)
        return bmp
    }

    private fun paginate(rows: List<ExportRow>, first: Int, later: Int): List<List<ExportRow>> {
        if (rows.isEmpty()) return listOf(emptyList())
        val out = mutableListOf<List<ExportRow>>()
        var i = 0
        out.add(rows.take(first)); i = minOf(first, rows.size)
        while (i < rows.size) { out.add(rows.subList(i, minOf(i + later, rows.size))); i += later }
        return out
    }

    private fun drawPage(c: Canvas, data: ExportData, pageRows: List<ExportRow>, firstPage: Boolean, pageNo: Int, pageCount: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val reg = Typeface.SANS_SERIF
        var y = MARGIN

        if (firstPage) {
            // Header
            p.typeface = bold; p.textSize = 19f; p.color = INK
            c.drawText("Budgetty", MARGIN, y + 16f, p)
            p.typeface = reg; p.textSize = 9f; p.color = MUTED
            c.drawText("Spending statement", MARGIN, y + 29f, p)
            p.typeface = bold; p.textSize = 15f; p.color = INK; p.textAlign = Paint.Align.RIGHT
            c.drawText(data.periodLabel, PAGE_W - MARGIN, y + 14f, p)
            p.typeface = reg; p.textSize = 9f; p.color = MUTED
            c.drawText(data.generatedLabel, PAGE_W - MARGIN, y + 27f, p)
            p.textAlign = Paint.Align.LEFT
            y += 44f
            p.color = PRIMARY
            c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 3f, p)
            y += 18f

            // Three tiles
            val tileW = (PAGE_W - 2 * MARGIN - 2 * 12f) / 3f
            val tiles = listOf(
                Triple("TOTAL SPENT", money(data.totalSpent, data.currencySymbol), INK),
                Triple("INCOME", money(data.income, data.currencySymbol), INK),
                Triple("NET", (if (data.net.signum() >= 0) "+" else "") + money(data.net, data.currencySymbol), if (data.net.signum() >= 0) GOOD else INK),
            )
            tiles.forEachIndexed { i, (label, value, col) ->
                val x = MARGIN + i * (tileW + 12f)
                p.color = TILE_BG
                c.drawRoundRect(RectF(x, y, x + tileW, y + 56f), 10f, 10f, p)
                p.typeface = bold; p.textSize = 8f; p.color = MUTED
                c.drawText(label, x + 13f, y + 20f, p)
                p.typeface = bold; p.textSize = 20f; p.color = col
                c.drawText(value, x + 13f, y + 44f, p)
            }
            y += 56f + 24f

            // By category
            sectionHeader(c, p, "BY CATEGORY", y); y += 20f
            data.byCategory.forEach { cat ->
                p.color = cat.colorArgb; c.drawCircle(MARGIN + 5f, y + 4f, 4.5f, p)
                p.typeface = reg; p.textSize = 11f; p.color = INK
                c.drawText(cat.emoji, MARGIN + 16f, y + 8f, p)
                p.typeface = bold
                c.drawText(cat.name, MARGIN + 34f, y + 8f, p)
                // bar
                val barX = MARGIN + 150f; val barW = PAGE_W - MARGIN - 150f - 110f
                p.color = Color.rgb(0xF1, 0xEC, 0xF6); c.drawRoundRect(RectF(barX, y + 1f, barX + barW, y + 7f), 3f, 3f, p)
                p.color = cat.colorArgb; c.drawRoundRect(RectF(barX, y + 1f, barX + barW * (cat.pct.coerceIn(0, 100) / 100f), y + 7f), 3f, 3f, p)
                p.typeface = reg; p.textSize = 10f; p.color = MUTED; p.textAlign = Paint.Align.RIGHT
                c.drawText("${cat.pct}%", PAGE_W - MARGIN - 72f, y + 8f, p)
                p.typeface = bold; p.textSize = 11f; p.color = INK
                c.drawText(money(cat.total, data.currencySymbol), PAGE_W - MARGIN, y + 8f, p)
                p.textAlign = Paint.Align.LEFT
                y += 17f
                p.color = Color.rgb(0xEF, 0xEA, 0xF4); c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 0.5f, p)
                y += 5f
            }
            y += 18f
            sectionHeader(c, p, "TRANSACTIONS", y)
            p.typeface = reg; p.textSize = 9f; p.color = MUTED; p.textAlign = Paint.Align.RIGHT
            c.drawText("${data.rows.size} in this period", PAGE_W - MARGIN, y + 4f, p)
            p.textAlign = Paint.Align.LEFT
            y += 20f
        } else {
            p.typeface = bold; p.textSize = 11f; p.color = INK
            c.drawText("TRANSACTIONS (cont.)", MARGIN, y + 10f, p)
            y += 24f
        }

        // Transaction table header
        p.typeface = bold; p.textSize = 8f; p.color = MUTED
        c.drawText("DATE", MARGIN + 8f, y, p)
        c.drawText("STORE", MARGIN + 70f, y, p)
        c.drawText("CATEGORY", PAGE_W - MARGIN - 180f, y, p)
        p.textAlign = Paint.Align.RIGHT; c.drawText("AMOUNT", PAGE_W - MARGIN - 8f, y, p); p.textAlign = Paint.Align.LEFT
        y += 6f

        pageRows.forEachIndexed { i, r ->
            if (i % 2 == 1) { p.color = ZEBRA; c.drawRoundRect(RectF(MARGIN, y - 2f, PAGE_W - MARGIN, y + 12f), 4f, 4f, p) }
            p.typeface = reg; p.textSize = 10f; p.color = Color.rgb(0x49, 0x45, 0x4F)
            c.drawText(r.dateLabel, MARGIN + 8f, y + 9f, p)
            p.typeface = bold; p.textSize = 11f; p.color = INK
            c.drawText(ellipsize(r.store, p, PAGE_W - MARGIN - 180f - (MARGIN + 70f) - 6f), MARGIN + 70f, y + 9f, p)
            p.color = r.colorArgb; c.drawCircle(PAGE_W - MARGIN - 180f + 3f, y + 5f, 3.5f, p)
            p.typeface = reg; p.textSize = 10f; p.color = Color.rgb(0x49, 0x45, 0x4F)
            c.drawText(ellipsize(r.category, p, 80f), PAGE_W - MARGIN - 180f + 12f, y + 9f, p)
            p.typeface = bold; p.textSize = 11f; p.color = INK; p.textAlign = Paint.Align.RIGHT
            c.drawText(money(r.amount, data.currencySymbol), PAGE_W - MARGIN - 8f, y + 9f, p); p.textAlign = Paint.Align.LEFT
            y += 15f
        }

        // Total row (last page only)
        if (pageNo == pageCount) {
            p.color = PRIMARY; c.drawRect(MARGIN, y + 4f, PAGE_W - MARGIN, y + 6f, p)
            y += 16f
            p.typeface = bold; p.textSize = 12f; p.color = INK
            c.drawText(data.totalRowLabel, MARGIN + 8f, y + 8f, p)
            p.textAlign = Paint.Align.RIGHT; p.textSize = 13f
            c.drawText(money(data.totalSpent, data.currencySymbol), PAGE_W - MARGIN - 8f, y + 8f, p); p.textAlign = Paint.Align.LEFT
        }

        // Footer
        p.color = LINE; c.drawRect(MARGIN, PAGE_H - MARGIN - 22f, PAGE_W - MARGIN, PAGE_H - MARGIN - 21.5f, p)
        p.typeface = reg; p.textSize = 8f; p.color = Color.rgb(0x8A, 0x84, 0x96)
        c.drawText("Created on device by Budgetty from your own receipts. Not a tax document or a bank statement.", MARGIN, PAGE_H - MARGIN - 8f, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("Page $pageNo of $pageCount", PAGE_W - MARGIN, PAGE_H - MARGIN - 8f, p)
        p.textAlign = Paint.Align.LEFT
    }

    private fun sectionHeader(c: Canvas, p: Paint, text: String, y: Float) {
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); p.textSize = 11f; p.color = INK
        c.drawText(text, MARGIN, y + 4f, p)
        val w = p.measureText(text)
        p.color = LINE; c.drawRect(MARGIN + w + 10f, y + 1f, PAGE_W - MARGIN, y + 2f, p)
    }

    private fun ellipsize(text: String, p: Paint, maxW: Float): String {
        if (p.measureText(text) <= maxW) return text
        var s = text
        while (s.isNotEmpty() && p.measureText("$s…") > maxW) s = s.dropLast(1)
        return "$s…"
    }

    private fun money(v: BigDecimal, symbol: String): String {
        val n = String.format("%,.2f", v.setScale(2, RoundingMode.HALF_UP))
        return "$n $symbol"
    }
}
