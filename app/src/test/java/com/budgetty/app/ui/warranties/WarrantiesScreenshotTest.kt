package com.budgetty.app.ui.warranties

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.local.WarrantyEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

/**
 * Golden for the warranties list — the status groups (expiring soon / active / expired) with their
 * elapsed rings and countdown chips, plus the free-tier counter + add button. A free user below the
 * cap. Light + dark are separate goldens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class WarrantiesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_light() = capture(dark = false)

    @Test
    fun list_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        val today = LocalDate.of(2026, 10, 6)
        fun millis(d: LocalDate) = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun card(id: Long, name: String, emoji: String, store: String, purchase: LocalDate, months: Int): WarrantyCardUi {
            val e = WarrantyEntity(
                id = id, name = name, emoji = emoji, store = store,
                purchaseDate = millis(purchase), durationMonths = months,
            )
            return WarrantyCardUi(e, Warranties.status(purchase, months, today))
        }
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WarrantiesContent(
                        state = WarrantiesUiState(
                            isLoaded = true,
                            isPremium = false,
                            expiringSoon = listOf(card(1, "Bosch dishwasher", "🍽️", "Saturn", LocalDate.of(2024, 10, 29), 24)),
                            active = listOf(
                                card(2, "MacBook Air 13″", "💻", "MediaMarkt", LocalDate.of(2026, 3, 6), 24),
                                card(3, "Samsung TV 55″", "📺", "MediaMarkt", LocalDate.of(2025, 11, 20), 36),
                            ),
                            expired = listOf(card(4, "Sony WH-1000XM5", "🎧", "Amazon", LocalDate.of(2024, 8, 14), 12)),
                        ),
                        onNavigateBack = {}, onNavigateToPaywall = {},
                        onSave = { _, _, _, _, _, _, _, _ -> }, onDelete = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
