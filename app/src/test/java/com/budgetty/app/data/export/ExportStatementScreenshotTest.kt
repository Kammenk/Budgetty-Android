package com.budgetty.app.data.export

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Renders the branded PDF statement (page 1) to a bitmap and captures it — verifies the PDF layout. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ExportStatementScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun row(date: String, store: String, cat: String, color: Long, amt: String) =
        ExportRow(0, date, store, cat, color.toInt(), BigDecimal(amt))

    private fun cat(emoji: String, name: String, total: String, pct: Int, color: Long) =
        ExportCategory(name, emoji, color.toInt(), BigDecimal(total), pct)

    @Test fun statement() {
        val data = ExportData(
            periodLabel = "1 – 31 July 2026",
            generatedLabel = "Generated 30 Jul 2026 · € · 8 receipts",
            currencySymbol = "€",
            rows = listOf(
                row("02 Jul", "Kaufland", "Groceries", 0xFF4FA85A, "48.20"),
                row("03 Jul", "Café Luna", "Dining", 0xFFE0795B, "7.40"),
                row("04 Jul", "Shell", "Fuel", 0xFFD08A4A, "62.00"),
                row("06 Jul", "Lidl", "Groceries", 0xFF4FA85A, "31.40"),
                row("07 Jul", "dm drogerie", "Household", 0xFFC77DB0, "22.10"),
                row("09 Jul", "Apotheke Nord", "Health", 0xFF5BB6A6, "18.60"),
                row("12 Jul", "Kaufland", "Groceries", 0xFF4FA85A, "62.85"),
                row("20 Jul", "Media Markt", "Other", 0xFF9A93A6, "79.00"),
            ),
            totalSpent = BigDecimal("331.55"),
            income = BigDecimal("2400.00"),
            net = BigDecimal("2068.45"),
            byCategory = listOf(
                cat("🛒", "Groceries", "142.45", 43, 0xFF4FA85A),
                cat("📦", "Other", "79.00", 24, 0xFF9A93A6),
                cat("⛽", "Fuel", "62.00", 19, 0xFFD08A4A),
                cat("🏠", "Household", "22.10", 7, 0xFFC77DB0),
                cat("❤️", "Health", "18.60", 6, 0xFF5BB6A6),
                cat("🍽️", "Dining", "7.40", 2, 0xFFE0795B),
            ),
            totalRowLabel = "Total · July 2026",
        )
        val bmp = DataExporter.renderBitmap(data).asImageBitmap()
        composeRule.setContent {
            Surface {
                Image(bitmap = bmp, contentDescription = null, modifier = Modifier.width(460.dp).aspectRatio(595f / 842f))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
