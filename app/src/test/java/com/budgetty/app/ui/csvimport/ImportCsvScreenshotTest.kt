package com.budgetty.app.ui.csvimport

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.data.csvimport.CsvField
import com.budgetty.app.data.csvimport.CsvTable
import com.budgetty.app.ui.theme.BudgettyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden for the CSV-import wizard's Map step — the column→field mapping with its live 3-row preview,
 * the signature screen of the feature. Light + dark are separate goldens so a theme break is caught.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ImportCsvScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapStep_light() = capture(dark = false)

    @Test
    fun mapStep_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        val table = CsvTable(
            headers = listOf("Booking date", "Payee", "Amount", "Category"),
            rows = listOf(
                listOf("12/10/2026", "LIDL SAGT DANKE", "-42.80", "Groceries"),
                listOf("11/10/2026", "SPOTIFY", "-10.99", "Subscriptions"),
                listOf("10/10/2026", "ACME GMBH", "2450.00", "Income"),
            ),
        )
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ImportCsvContent(
                        state = ImportUiState(
                            step = ImportStep.MAP,
                            fileName = "statement_oct.csv",
                            table = table,
                            mapping = listOf(CsvField.DATE, CsvField.STORE, CsvField.AMOUNT, CsvField.CATEGORY),
                        ),
                        onNavigateBack = {}, onNavigateToHistory = {}, onPick = {}, onClearFile = {},
                        onGoToMap = {}, onCycleColumn = {}, onGoToReview = {}, onBack = {},
                        onSetDateFormat = {}, onSetSign = {}, onSetSkipDuplicates = {}, onImport = {}, onUndo = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
