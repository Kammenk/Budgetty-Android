package com.budgetty.app.ui.tags

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.components.TagField
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
 * Screenshot goldens for the free-form tags UI via Roborazzi (JVM, no emulator) — the Manage-tags
 * screen (list + empty) and the Review-screen Tags field, in light and dark. The tag input sheet is
 * a ModalBottomSheet, which a plain Robolectric capture can't render, so it's verified on-device.
 *
 *   ./gradlew :app:recordRoborazziDebug --tests "*TagsScreenshotTest"   # write goldens
 *   ./gradlew :app:verifyRoborazziDebug --tests "*TagsScreenshotTest"   # fail on drift
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TagsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleTags = listOf(
        TagRow("lisbon-2026", 14),
        TagRow("work", 9),
        TagRow("reimbursable", 6),
        TagRow("tax-deductible", 4),
        TagRow("дача", 2),
    )

    private fun capture(dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            BudgettyTheme(darkTheme = dark) {
                Surface { content() }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun manage_populated_light() = capture {
        TagsContent(
            tags = sampleTags,
            isLoaded = true,
            onRename = { _, _ -> },
            onMerge = { _, _ -> },
            onDelete = {},
            onNavigateBack = {},
        )
    }

    @Test fun manage_populated_dark() = capture(dark = true) {
        TagsContent(
            tags = sampleTags,
            isLoaded = true,
            onRename = { _, _ -> },
            onMerge = { _, _ -> },
            onDelete = {},
            onNavigateBack = {},
        )
    }

    @Test fun manage_empty_light() = capture {
        TagsContent(
            tags = emptyList(),
            isLoaded = true,
            onRename = { _, _ -> },
            onMerge = { _, _ -> },
            onDelete = {},
            onNavigateBack = {},
        )
    }

    @Test fun review_tag_field_light() = reviewField(dark = false)

    @Test fun review_tag_field_dark() = reviewField(dark = true)

    private fun reviewField(dark: Boolean) = capture(dark = dark) {
        Column(
            modifier = Modifier.width(360.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TagField(clientId = "a", tags = listOf("reimbursable", "work"), modifier = Modifier)
            TagField(clientId = "b", tags = emptyList(), modifier = Modifier)
        }
    }
}
