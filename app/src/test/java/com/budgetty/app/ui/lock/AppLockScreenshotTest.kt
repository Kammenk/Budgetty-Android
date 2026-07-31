package com.budgetty.app.ui.lock

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetty.app.ui.theme.BudgettyTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures the app-lock PIN pad in its unlock and set-PIN states (default, error, confirm). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AppLockScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test fun lock_default() {
        composeRule.setContent {
            BudgettyTheme {
                LockScreenContent(
                    filled = 2, isError = false, errorNonce = 0, showBiometric = true,
                    onDigit = {}, onBackspace = {}, onBiometric = {}, onForgot = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun lock_error() {
        composeRule.setContent {
            BudgettyTheme {
                LockScreenContent(
                    filled = 4, isError = true, errorNonce = 0, showBiometric = true,
                    onDigit = {}, onBackspace = {}, onBiometric = {}, onForgot = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun set_pin_enter() {
        composeRule.setContent {
            BudgettyTheme {
                SetPinContent(
                    confirming = false, filled = 3, isError = false, errorNonce = 0,
                    onBack = {}, onDigit = {}, onBackspace = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun set_pin_confirm() {
        composeRule.setContent {
            BudgettyTheme {
                SetPinContent(
                    confirming = true, filled = 4, isError = false, errorNonce = 0,
                    onBack = {}, onDigit = {}, onBackspace = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
