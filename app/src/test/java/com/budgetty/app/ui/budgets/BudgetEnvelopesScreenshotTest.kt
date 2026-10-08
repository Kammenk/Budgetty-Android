package com.budgetty.app.ui.budgets

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Golden for the multiple-budgets list — each envelope's pace bar (fill vs the "Today" tick) in its
 * on-pace / over-pace states, plus the free-tier "New budget" button. A free user below the cap.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class BudgetEnvelopesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_light() = capture(dark = false)

    @Test
    fun list_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        val today = LocalDate.of(2026, 10, 22)
        fun millis(d: LocalDate) = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun card(emoji: String, name: String, spent: String, limit: String, start: LocalDate, end: LocalDate): EnvelopeCardUi {
            val e = BudgetEnvelopeEntity(
                name = name, emoji = emoji, limitAmount = BigDecimal(limit),
                startDate = millis(start), endDate = millis(end),
            )
            return EnvelopeCardUi(
                e, BigDecimal(spent), start, end,
                BudgetPace.compute(BigDecimal(spent), BigDecimal(limit), start, end, today),
            )
        }
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EnvelopesContent(
                        state = EnvelopesUiState(
                            isLoaded = true, isPremium = false,
                            envelopes = listOf(
                                card("🧾", "Everyday", "842", "1200", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                                card("✈️", "Lisbon trip", "512", "600", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 27)),
                            ),
                        ),
                        categories = emptyList(),
                        onNavigateBack = {}, onNavigateToPaywall = {},
                        onSave = { _, _, _, _, _, _, _ -> }, onDelete = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
