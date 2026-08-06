package com.budgetty.app.ui.savings

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.SavingsProgress
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

/** Screenshot goldens for the Budget-tab [SavingsSection]: empty / multiple (Premium) / locked-at-cap. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SavingsSectionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun millis(d: LocalDate) = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun card(
        id: Long, emoji: String, name: String, target: String, saved: String,
        date: LocalDate?, rate: String?, behind: Boolean = false,
    ): SavingsGoalCardUi {
        val t = BigDecimal(target)
        val s = BigDecimal(saved)
        val remaining = (t - s).max(BigDecimal.ZERO)
        return SavingsGoalCardUi(
            goal = SavingsGoalEntity(
                id = id, name = name, emoji = emoji, targetAmount = t, targetDate = date?.let(::millis),
            ),
            progress = SavingsProgress(
                saved = s,
                remaining = remaining,
                fraction = (s.toDouble() / t.toDouble()).toFloat().coerceIn(0f, 1f),
                reached = s >= t,
                monthlyRate = rate?.let(::BigDecimal),
                behind = behind,
            ),
        )
    }

    private val laptop get() = card(1, "💻", "New laptop", "1200", "480", LocalDate.of(2026, 12, 31), "144.00")
    private val fund get() = card(2, "🛟", "Emergency fund", "3000", "830", null, null)
    private val japan get() =
        card(3, "✈️", "Japan trip", "2500", "200", LocalDate.of(2027, 3, 31), "288.00", behind = true)

    @Test fun empty_light() = capture(emptyList(), isPremium = false, dark = false)
    @Test fun multiple_premium_light() = capture(listOf(laptop, fund, japan), isPremium = true, dark = false)
    @Test fun locked_free_light() = capture(listOf(laptop), isPremium = false, dark = false)
    @Test fun multiple_premium_dark() = capture(listOf(laptop, fund, japan), isPremium = true, dark = true)

    @Test fun detail_in_progress_light() = captureDetail(reached = false, dark = false)
    @Test fun detail_reached_light() = captureDetail(reached = true, dark = false)

    private fun contribution(id: Long, amount: String, note: String, date: LocalDate) =
        SavingsContributionEntity(id = id, goalId = 1, amount = BigDecimal(amount), note = note, date = millis(date))

    private fun captureDetail(reached: Boolean, dark: Boolean) {
        AppFormats.currencySymbol = "€"
        val target = BigDecimal("1200")
        val saved = if (reached) target else BigDecimal("480")
        val goal = SavingsGoalEntity(
            id = 1, name = "New laptop", emoji = "💻", targetAmount = target,
            targetDate = millis(LocalDate.of(2026, 12, 31)),
        )
        val progress = SavingsProgress(
            saved = saved,
            remaining = (target - saved).max(BigDecimal.ZERO),
            fraction = (saved.toDouble() / target.toDouble()).toFloat().coerceIn(0f, 1f),
            reached = reached,
            monthlyRate = if (reached) null else BigDecimal("144.00"),
            behind = false,
        )
        val contribs = listOf(
            contribution(1, "200.00", "Salary top-up", LocalDate.of(2026, 7, 15)),
            contribution(2, "150.00", "", LocalDate.of(2026, 6, 30)),
            contribution(3, "-50.00", "Needed for tyres", LocalDate.of(2026, 6, 12)),
            contribution(4, "180.00", "", LocalDate.of(2026, 6, 2)),
        )
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                SavingsGoalDetailContent(goal, contribs, progress, {}, {}, {}, {}, {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private fun capture(goals: List<SavingsGoalCardUi>, isPremium: Boolean, dark: Boolean) {
        AppFormats.currencySymbol = "€"
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface {
                    SavingsSection(
                        goals = goals,
                        isPremium = isPremium,
                        onGoalClick = {},
                        onNewGoal = {},
                        onUpgrade = {},
                        modifier = Modifier.width(360.dp).padding(16.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
