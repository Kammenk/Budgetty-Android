package com.budgetty.app.ui.recap

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Roborazzi goldens (JVM, no emulator) for the retention recap changes: the new weekly Streak card
 * (current run + best-run fallback), the de-flamed monthly budget-streak card (current run + best-run),
 * the weekly Limits card with an at-cap warm (never red) chip, and the in-story frequency sheet.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*RecapStreakScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*RecapStreakScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class RecapStreakScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    // A minimal story just to satisfy card bodies that read dates (the monthly budget card's caption
    // uses nextMonth = the current OPEN month, "August on track so far").
    private val story = RecapStory(
        kind = RecapKind.MONTHLY,
        monthYear = YearMonth.of(2026, 7),
        nextMonth = YearMonth.of(2026, 8),
        weekStart = LocalDate.of(2026, 7, 1),
        weekEnd = LocalDate.of(2026, 7, 31),
        cards = emptyList(),
    )

    private val monthlySegments = listOf(
        RecapSegStatus.GOOD, RecapSegStatus.GOOD, RecapSegStatus.WARN,
        RecapSegStatus.GOOD, RecapSegStatus.GOOD, RecapSegStatus.BAD,
    )

    private fun card(dark: Boolean = false, card: RecapCard) = capture(dark) {
        Box(Modifier.size(360.dp, 720.dp)) {
            RecapCardPage(story = story, card = card)
        }
    }

    private fun capture(dark: Boolean, content: @Composable () -> Unit) {
        AppFormats.currencySymbol = "€"
        composeRule.setContent { BudgettyTheme(darkTheme = dark) { Surface { content() } } }
        composeRule.onRoot().captureRoboImage()
    }

    // ── Weekly Streak card ─────────────────────────────────────────────────────────

    private fun weeklyStreak(isBestRun: Boolean) = RecapCard.Streak(
        band = RecapBand.SECONDARY, kind = StreakKind.BUDGET_WEEK, scope = "Groceries",
        current = if (isBestRun) 0 else 3, best = if (isBestRun) 6 else 4,
        liveOnTrack = !isBestRun, isBestRun = isBestRun,
    )

    @Test fun weekly_streak_current_light() = card(card = weeklyStreak(isBestRun = false))

    @Test fun weekly_streak_current_dark() = card(dark = true, card = weeklyStreak(isBestRun = false))

    @Test fun weekly_streak_best_run_light() = card(card = weeklyStreak(isBestRun = true))

    // ── De-flamed monthly budget-streak card ───────────────────────────────────────

    @Test fun monthly_streak_current_light() = card(
        card = RecapCard.BudgetStreak(
            band = RecapBand.GREAT, streakMonths = 3, best = 6, liveOnTrack = true,
            underCount = 5, scopeCount = 6, segments = monthlySegments, safeToSpend = BigDecimal("60"),
        ),
    )

    @Test fun monthly_streak_current_dark() = card(
        dark = true,
        card = RecapCard.BudgetStreak(
            band = RecapBand.GREAT, streakMonths = 3, best = 6, liveOnTrack = true,
            underCount = 5, scopeCount = 6, segments = monthlySegments, safeToSpend = BigDecimal("60"),
        ),
    )

    @Test fun monthly_streak_best_run_light() = card(
        card = RecapCard.BudgetStreak(
            band = RecapBand.GREAT, streakMonths = 0, best = 6, liveOnTrack = false,
            underCount = 4, scopeCount = 6,
            segments = listOf(
                RecapSegStatus.GOOD, RecapSegStatus.GOOD, RecapSegStatus.GOOD,
                RecapSegStatus.GOOD, RecapSegStatus.WARN, RecapSegStatus.WARN,
            ),
            safeToSpend = BigDecimal("18"),
        ),
    )

    // ── Weekly Limits card (at-cap chip = warm, never red) ──────────────────────────

    @Test fun weekly_limits_at_cap_light() = card(
        card = RecapCard.Limits(
            band = RecapBand.WARN, underCount = 3, totalCount = 4,
            chips = listOf(
                RecapLimitChip("🥤", "Coke", 2, 4),
                RecapLimitChip("🍟", "Crisps", 3, 4),
                RecapLimitChip("⚡", "Energy", 1, 2),
                RecapLimitChip("🍕", "Takeaway", 4, 4),
            ),
        ),
    )

    // ── In-story frequency sheet ────────────────────────────────────────────────────

    @Test fun frequency_sheet_light() = capture(dark = false) {
        Surface(Modifier.size(360.dp, 320.dp)) {
            RecapFrequencySheetContent(
                recapEnabled = true, recapFrequency = RecapFrequency.BOTH, onSelect = { _, _ -> },
            )
        }
    }

    @Test fun frequency_sheet_dark() = capture(dark = true) {
        Surface(Modifier.size(360.dp, 320.dp)) {
            RecapFrequencySheetContent(
                recapEnabled = true, recapFrequency = RecapFrequency.BOTH, onSelect = { _, _ -> },
            )
        }
    }
}
