package com.budgetty.app.ui.templates

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.local.TemplateEntity
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

/**
 * Golden for the Manage-templates screen (Account → Templates): the saved regulars with their amount
 * and category, an "ask amount" template, and the "New template" row. Roborazzi, JVM, no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TemplatesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedSymbol = "€"

    @Before fun saveFormats() { savedSymbol = AppFormats.currencySymbol; AppFormats.currencySymbol = "€" }

    @After fun restoreFormats() { AppFormats.currencySymbol = savedSymbol }

    private val templates = listOf(
        TemplateEntity(1, "☕", "Coffee", BigDecimal("3.20"), "Food", "Café Delta"),
        TemplateEntity(2, "🚆", "Metro", BigDecimal("1.60"), "Transport", "Metro"),
        TemplateEntity(3, "🔑", "Rent", BigDecimal("1200"), "Housing", "Landlord"),
        TemplateEntity(4, "🛒", "Groceries", BigDecimal.ZERO, "Groceries", "", askAmount = true),
    )

    private fun capture(templates: List<TemplateEntity>, dark: Boolean = false) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                TemplatesContent(templates = templates, onNavigateBack = {}, onSave = {}, onDelete = {})
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun manage_light() = capture(templates)

    @Test fun manage_dark() = capture(templates, dark = true)

    @Test fun manage_empty() = capture(emptyList())
}
