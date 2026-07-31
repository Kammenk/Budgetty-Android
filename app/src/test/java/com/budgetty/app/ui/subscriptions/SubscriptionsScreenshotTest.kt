package com.budgetty.app.ui.subscriptions

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.DetectedSubscription
import com.budgetty.app.ui.util.PriceHike
import com.budgetty.app.ui.util.SubCharge
import com.budgetty.app.ui.util.SubscriptionCadence
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SubscriptionsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun ms(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun sub(
        merchant: String, emoji: String, amount: String, dueDay: Int,
        hike: PriceHike? = null, charges: List<SubCharge> = emptyList(),
    ): DetectedSubscription {
        val amt = BigDecimal(amount)
        return DetectedSubscription(
            merchant = merchant, emoji = emoji, cadence = SubscriptionCadence.MONTHLY,
            amount = amt, monthlyEquivalent = amt,
            lastChargeMillis = ms(2026, 7, dueDay), nextExpectedMillis = ms(2026, 8, dueDay),
            dueDay = dueDay, charges = charges, priceHike = hike,
        )
    }

    private val netflix = sub(
        "Netflix", "🎬", "13.99", 14,
        hike = PriceHike(BigDecimal("11.99"), BigDecimal("13.99"), ms(2026, 3, 14)),
        charges = listOf(
            SubCharge(BigDecimal("13.99"), ms(2026, 7, 14)),
            SubCharge(BigDecimal("13.99"), ms(2026, 6, 14)),
            SubCharge(BigDecimal("13.99"), ms(2026, 5, 14)),
            SubCharge(BigDecimal("13.99"), ms(2026, 4, 14)),
            SubCharge(BigDecimal("13.99"), ms(2026, 3, 14)),
            SubCharge(BigDecimal("11.99"), ms(2026, 2, 14)),
        ),
    )
    private val gym = sub("FitPark Gym", "🏋️", "24.90", 1)
    private val spotify = sub("Spotify", "🎵", "10.99", 3)

    private fun state(premium: Boolean) = SubscriptionsUiState(
        loaded = true,
        isPremium = premium,
        detected = listOf(gym, netflix, spotify),
        monthlyTotal = BigDecimal("49.88"),
        yearlyTotal = BigDecimal("598.56"),
        receiptCount = 34,
    )

    @Test fun entry_premium() = capture { SubscriptionsEntryCard(state(true), {}, {}) }
    @Test fun entry_teaser() = capture { SubscriptionsEntryCard(state(false), {}, {}) }
    @Test fun list() = capture { Column { ListContent(state(true), {}, {}) } }
    @Test fun detail_with_hike() = capture { Column { DetailContent(netflix, {}, {}) } }

    private fun capture(content: @androidx.compose.runtime.Composable () -> Unit) {
        AppFormats.currencySymbol = "€"
        AppFormats.dayMonthPattern = "d MMM"
        composeRule.setContent {
            BudgettyTheme {
                Surface {
                    Column(Modifier.width(380.dp).padding(16.dp)) { content() }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
