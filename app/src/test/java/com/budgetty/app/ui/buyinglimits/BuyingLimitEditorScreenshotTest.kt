package com.budgetty.app.ui.buyinglimits

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.CountableItem
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot goldens for the buying-limit editor body ([BuyingLimitEditorBody]) — verifying it matches
 * `BuyingLimitEditor.dc.html`: UPPERCASE section labels, the compact label field aligned with the emoji
 * chip, the carded "currently matches" read-out, and the full-width count stepper.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
class BuyingLimitEditorScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // "now" so every seeded item falls inside the current weekly/monthly window regardless of run date.
    private val now = System.currentTimeMillis()
    private fun item(name: String, qty: Int = 1) = CountableItem(name = name, quantity = qty, timestamp = now)

    private val fizzy = BuyingLimitEntity(
        id = 1, emoji = "🥤", label = "Fizzy drinks",
        keywords = BuyingLimitEntity.joinKeywords(listOf("coke", "cola", "кока-кола")),
        timeframe = BuyingLimitTimeframe.MONTHLY, count = 3,
    )
    private val fizzyItems = listOf(
        item("Coca-Cola 500ml"), item("Coke Zero 330ml"), item("Кока-Кола 2л"), item("Pepsi Cola 1L"),
    )

    // "ice" is short and catches a lot (juice, rice, nice…) → the amber "catching a lot" state.
    private val iceItems = listOf(
        item("Orange juice 1L"), item("Iced tea"), item("Ice cubes 2kg"), item("Vanilla ice cream"),
        item("Rice 1kg"), item("Juice apple 250ml"), item("Nice biscuits"),
    )

    @Test fun editMatchesLight() = capture(fizzy, fizzyItems, dark = false)

    @Test fun editMatchesDark() = capture(fizzy, fizzyItems, dark = true)

    @Test fun noMatchLight() = capture(
        BuyingLimitEntity(
            emoji = "☕", label = "Takeaway coffee",
            keywords = BuyingLimitEntity.joinKeywords(listOf("flat white")),
            timeframe = BuyingLimitTimeframe.WEEKLY, count = 2,
        ),
        emptyList(), dark = false,
    )

    @Test fun tooBroadDark() = capture(
        BuyingLimitEntity(
            emoji = "🍦", label = "Ice cream",
            keywords = BuyingLimitEntity.joinKeywords(listOf("ice")),
            timeframe = BuyingLimitTimeframe.WEEKLY, count = 2,
        ),
        iceItems, dark = true,
    )

    private fun capture(initial: BuyingLimitEntity, items: List<CountableItem>, dark: Boolean) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    Column(Modifier.width(360.dp).heightIn(max = 800.dp)) {
                        BuyingLimitEditorBody(
                            initial = initial,
                            items = items,
                            monthStartDay = 1,
                            onSave = { _, _, _, _, _ -> },
                            onDelete = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
