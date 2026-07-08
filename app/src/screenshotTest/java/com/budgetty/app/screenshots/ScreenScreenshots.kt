package com.budgetty.app.screenshots

import androidx.compose.runtime.Composable
import com.budgetty.app.ui.account.AccountScreenTabletPreview
import com.budgetty.app.ui.auth.LoginScreenTabletPreview
import com.budgetty.app.ui.budget.BudgetScreenTabletPreview
import com.budgetty.app.ui.history.HistoryGroupedByDayPreview
import com.budgetty.app.ui.home.HomeScreenTabletPreview
import com.budgetty.app.ui.insights.InsightsScreenTabletPreview
import com.budgetty.app.ui.onboarding.OnboardingScreenTabletPreview
import com.budgetty.app.ui.paywall.PaywallScreenLandscapePreview
import com.budgetty.app.ui.upload.UploadErrorPreview
import com.budgetty.app.ui.upload.UploadScreenPreview

/**
 * Screenshot-test entry points. Each function is the canonical populated preview of a top-level
 * screen (defined in `src/main`, made `internal` + width-adaptive) re-exposed under [BudgettyScreens]
 * so it renders across every window size and font scale. Adding a screen here = a new row of golden
 * images; record with `./gradlew updateDebugScreenshotTest`, gate with `validateDebugScreenshotTest`.
 *
 * These delegate to the main previews so the sample data lives in one place and the IDE preview
 * pane and the screenshot matrix can never drift apart.
 */

@BudgettyScreens @Composable internal fun HomeScreenshots() = HomeScreenTabletPreview()

@BudgettyScreens @Composable internal fun InsightsScreenshots() = InsightsScreenTabletPreview()

@BudgettyScreens @Composable internal fun HistoryScreenshots() = HistoryGroupedByDayPreview()

@BudgettyScreens @Composable internal fun BudgetScreenshots() = BudgetScreenTabletPreview()

@BudgettyScreens @Composable internal fun AccountScreenshots() = AccountScreenTabletPreview()

@BudgettyScreens @Composable internal fun PaywallScreenshots() = PaywallScreenLandscapePreview()

@BudgettyScreens @Composable internal fun UploadReviewScreenshots() = UploadScreenPreview()

@BudgettyScreens @Composable internal fun UploadErrorScreenshots() = UploadErrorPreview()

@BudgettyScreens @Composable internal fun LoginScreenshots() = LoginScreenTabletPreview()

@BudgettyScreens @Composable internal fun OnboardingScreenshots() = OnboardingScreenTabletPreview()
