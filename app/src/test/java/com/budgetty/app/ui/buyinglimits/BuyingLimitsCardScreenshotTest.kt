package com.budgetty.app.ui.buyinglimits

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.ui.streaks.LimitWindow
import com.budgetty.app.ui.streaks.Streak
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.LimitSuggestion
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi goldens (JVM, no emulator) for the §4 buying-limits changes: a card with the under-cap
 * streak caption + 8-window history strip, the "Over by N" warm (never red) state, a dismissible
 * suggestion row, the suggestions empty state, and the locked 3-of-3 free-cap state.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*BuyingLimitsCardScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*BuyingLimitsCardScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class BuyingLimitsCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** History from a code string: m = met, x = not-met, o = no-data (oldest → newest, most recent last). */
    private fun history(code: String): List<LimitWindow> = code.reversed().map {
        when (it) {
            'm' -> LimitWindow(count = 1, hasData = true)
            'x' -> LimitWindow(count = 9, hasData = true)
            else -> LimitWindow(count = 0, hasData = false)
        }
    }

    private val streakCard = BuyingLimitCardUi(
        limit = BuyingLimitEntity(1, "⚡", "Energy drinks", "red bull\nmonster", BuyingLimitTimeframe.WEEKLY, 2),
        bought = 1,
        streak = Streak(
            StreakKind.LIMIT, "Energy drinks", current = 3, best = 5, periodsChecked = 8, liveOnTrack = true,
        ),
        history = history("mmxmmmxm"),
    )

    private val overCard = BuyingLimitCardUi(
        limit = BuyingLimitEntity(2, "🥤", "Fizzy drinks", "coke\ncola", BuyingLimitTimeframe.MONTHLY, 3),
        bought = 4,
        streak = Streak(
            StreakKind.LIMIT, "Fizzy drinks", current = 0, best = 4, periodsChecked = 8, liveOnTrack = false,
        ),
        history = history("mmxxmmxx"),
    )

    private val suggestion =
        LimitSuggestion(name = "Coca-Cola", keyword = "coca-cola", monthCount = 14, suggestedCap = 3)

    private fun card(dark: Boolean, content: @Composable () -> Unit) = capture(dark) {
        Column(Modifier.width(360.dp).padding(16.dp)) { content() }
    }

    private fun capture(dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent { BudgettyTheme(darkTheme = dark) { Surface { content() } } }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun streakAndHistory_light() = card(dark = false) { BuyingLimitCard(streakCard, monthStartDay = 1) {} }

    @Test fun streakAndHistory_dark() = card(dark = true) { BuyingLimitCard(streakCard, monthStartDay = 1) {} }

    @Test fun overByN_warm_light() = card(dark = false) { BuyingLimitCard(overCard, monthStartDay = 1) {} }

    @Test fun overByN_warm_dark() = card(dark = true) { BuyingLimitCard(overCard, monthStartDay = 1) {} }

    @Test fun suggestionRow_light() = card(dark = false) { LimitSuggestionRow(suggestion, onUse = {}, onDismiss = {}) }

    @Test fun suggestionsEmptyState_light() = capture(dark = false) {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(
                items = emptyList(),
                suggestions = listOf(
                    suggestion,
                    LimitSuggestion("Crisps", "crisps", 11, 2),
                    LimitSuggestion("Takeaway", "takeaway", 9, 2),
                ),
                isLoaded = true,
            ),
            onNavigateBack = {}, onUpgrade = {}, onSaveLimit = { _, _, _, _, _, _, _ -> },
            onDeleteLimit = {}, onDismissSuggestion = {},
        )
    }

    @Test fun lockedThreeOfThree_light() = capture(dark = false) {
        BuyingLimitsContent(
            state = BuyingLimitsUiState(
                limits = listOf(
                    BuyingLimitCardUi(
                        BuyingLimitEntity(1, "🥤", "Fizzy drinks", "coke", BuyingLimitTimeframe.MONTHLY, 3),
                        bought = 2,
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(2, "☕", "Coffee", "coffee", BuyingLimitTimeframe.WEEKLY, 5),
                        bought = 5,
                    ),
                    BuyingLimitCardUi(
                        BuyingLimitEntity(3, "🍫", "Chocolate", "chocolate", BuyingLimitTimeframe.WEEKLY, 2),
                        bought = 3,
                    ),
                ),
                isPremium = false,
                isLoaded = true,
            ),
            onNavigateBack = {}, onUpgrade = {}, onSaveLimit = { _, _, _, _, _, _, _ -> },
            onDeleteLimit = {}, onDismissSuggestion = {},
        )
    }
}
