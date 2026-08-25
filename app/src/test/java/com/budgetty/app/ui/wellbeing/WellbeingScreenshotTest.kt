package com.budgetty.app.ui.wellbeing

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.streaks.Streak
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
 * Roborazzi goldens (JVM, no emulator) for the §3 Wellbeing meta-progression UI: the trend sparkline
 * (light + dark), the band-up nudge, the budget-streak evidence under the Budget component, the
 * thin-data state where the sparkline renders nothing, and a tip with vs. without the "+N to your
 * score" pill.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*WellbeingScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*WellbeingScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class WellbeingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    private val nav = WellbeingNav({}, {}, {}, {}, {}, {})

    private val comps = listOf(
        WellbeingComponent(WellbeingComponentKey.SAVINGS, WellbeingEngine.W_SAVINGS, 78),
        WellbeingComponent(WellbeingComponentKey.BUDGET, WellbeingEngine.W_BUDGET, 55),
        WellbeingComponent(WellbeingComponentKey.TREND, WellbeingEngine.W_TREND, 62),
        WellbeingComponent(WellbeingComponentKey.SUBSCRIPTIONS, WellbeingEngine.W_SUBSCRIPTIONS, 45),
        WellbeingComponent(WellbeingComponentKey.GOALS, WellbeingEngine.W_GOALS, 50),
    )

    private fun summary(
        withTrend: Boolean,
        streaks: List<Streak>,
    ) = WellbeingSummary(
        score = WellbeingScore(57, WellbeingBand.GETTING_THERE, comps, trendDeltaVsPrevious = 4),
        monthlyTips = emptyList(),
        weekly = WeeklyPace(BigDecimal("210"), BigDecimal("300"), 0.7f, 0.71f, BigDecimal("90"), 2, -12, true),
        weeklyTips = emptyList(),
        wins = emptyList(),
        detail = emptyMap(),
        receiptsLogged = 18, hasBudget = true, periodId = "2026-09",
        monthYear = YearMonth.of(2026, 9), weekStart = LocalDate.of(2026, 9, 21), weekEnd = LocalDate.of(2026, 9, 27),
        trend = if (withTrend) {
            WellbeingTrend(
                closed = listOf(49, 51, 52, 54, 55, 56)
                    .mapIndexed { i, s -> WellbeingTrendPoint(YearMonth.of(2026, 3 + i), s) },
                liveScore = 57, deltaSinceFirst = 8, firstMonth = YearMonth.of(2026, 3),
            )
        } else {
            null
        },
        bandUp = BandUp(3, WellbeingBand.HEALTHY),
        budgetStreaks = streaks,
    )

    private val streaks = listOf(
        Streak(StreakKind.BUDGET_MONTH, "Groceries", current = 4, best = 5, periodsChecked = 6, liveOnTrack = true),
        Streak(StreakKind.BUDGET_MONTH, "Household", current = 2, best = 3, periodsChecked = 6, liveOnTrack = true),
    )

    private fun capture(dark: Boolean, content: @Composable () -> Unit) {
        AppFormats.currencySymbol = "€"
        composeRule.setContent { BudgettyTheme(darkTheme = dark) { Surface { content() } } }
        composeRule.onRoot().captureRoboImage()
    }

    private fun scoreCard(dark: Boolean = false, withTrend: Boolean, streaks: List<Streak>) = capture(dark) {
        Box(Modifier.width(320.dp).padding(12.dp)) {
            ScoreCard(summary(withTrend, streaks), open = null, onToggle = {}, nav = nav, twoUp = false)
        }
    }

    // Sparkline (light + dark) + band-up nudge + streak evidence under Budget, all in one card.
    @Test fun score_card_proposed_light() = scoreCard(withTrend = true, streaks = streaks)

    @Test fun score_card_proposed_dark() = scoreCard(dark = true, withTrend = true, streaks = streaks)

    // Thin data: no stored trend → the sparkline area renders nothing at all (no placeholder).
    @Test fun score_card_thin_light() = scoreCard(withTrend = false, streaks = emptyList())

    // ── Tip: with vs. without the "+N to your score" pill ────────────────────────────

    private fun tipCard(tip: WellbeingTip) = capture(dark = false) {
        Box(Modifier.width(320.dp).padding(12.dp)) {
            TipCard(tip, onDismiss = {}, onTipAct = {}, nav = nav)
        }
    }

    @Test fun tip_with_pill_light() = tipCard(
        WellbeingTip(
            TipType.MISSING_BUDGET, "missing_budget:Dining", TipTone.OPPORTUNITY,
            amount = BigDecimal("180"), label = "Dining", projectedGain = 6,
        ),
    )

    @Test fun tip_without_pill_light() = tipCard(
        // Gain below the floor → the pill is suppressed even though the tip is actionable.
        WellbeingTip(
            TipType.SUBSCRIPTION_COST, "subs_cost", TipTone.CAUTION,
            amount = BigDecimal("22"), count = 2, percent = 9, projectedGain = 1,
        ),
    )
}
