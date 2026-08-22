package com.budgetty.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Flags a Material3 top app bar (`TopAppBar` / `CenterAlignedTopAppBar` / `MediumTopAppBar` /
 * `LargeTopAppBar`) that does NOT zero its window insets with `windowInsets = WindowInsets(0, …)`.
 *
 * Why this exists: the app is global edge-to-edge and every screen renders inside `MainScaffold`'s
 * NavHost, which ALREADY pads its content by the system bars (see ui/navigation/BudgettyApp.kt). A
 * top app bar's default `windowInsets` then add the status-bar inset a SECOND time, leaving a visible
 * gap above the toolbar. The convention (see the edge-to-edge notes; BudgetScreen, UploadScreen,
 * SubscriptionsScreen, …) is `windowInsets = WindowInsets(0, 0, 0, 0)` on every NavHost-hosted app
 * bar. This exact gap has shipped repeatedly (SavingsGoalDetail, Subscriptions, ManageCategories);
 * this rule stops the next new screen from regressing so we don't re-discover it one screen at a time.
 *
 * Exemption: a top app bar inside a `Dialog` (or `AlertDialog` / `BasicAlertDialog`) lives in its OWN
 * window, not the NavHost, so it MUST keep its default insets. A `Dialog` that lexically encloses the
 * bar (same function) is skipped automatically. When the `Dialog` is in a *caller* instead — e.g. the
 * full-bleed CategoryPicker, whose CategoryPickerScreen wraps CategoryPickerContent's bar — this
 * PSI-only walk can't see it, so annotate that function with `@Suppress("TopAppBarInsetNotZeroed")`
 * and a note (as would a genuinely-immersive full-screen bar that intentionally wants the inset).
 *
 * Scope/limitation: PSI-only (no type resolution). It matches by callee name and by the literal
 * `WindowInsets(0…)` idiom the codebase uses uniformly; an unusual zero form (e.g.
 * `WindowInsets(top = 0)`) is flagged — rewrite it to the canonical form or suppress.
 */
class TopAppBarInsetNotZeroed(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "TopAppBarInsetNotZeroed",
        severity = Severity.Defect,
        description = "A NavHost-hosted Material3 top app bar must set windowInsets = " +
            "WindowInsets(0, 0, 0, 0); the nav Scaffold already applies the top inset, so the bar's " +
            "default insets double it and leave a gap above the toolbar.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return
        if (callee !in TOP_APP_BARS) return

        // A dialog-hosted app bar lives in its own window and correctly keeps its default insets.
        if (isInsideDialog(expression)) return

        val zeroed = expression.valueArguments.any { arg ->
            arg.getArgumentName()?.asName?.asString() == "windowInsets" &&
                arg.getArgumentExpression()?.text?.let { ZERO_INSETS.containsMatchIn(it) } == true
        }
        if (zeroed) return

        report(
            CodeSmell(
                issue,
                Entity.from(expression),
                "`$callee` isn't given windowInsets = WindowInsets(0, 0, 0, 0). The NavHost already " +
                    "applies the top inset, so this doubles it and leaves a gap above the toolbar. Add " +
                    "windowInsets = WindowInsets(0, 0, 0, 0) (as in BudgetScreen / UploadScreen). If it's " +
                    "inside a Dialog or an immersive full-screen flow, @Suppress this rule with a note.",
            ),
        )
    }

    /** True when [expression] is lexically nested inside a Dialog / AlertDialog call. */
    private fun isInsideDialog(expression: KtCallExpression): Boolean {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtCallExpression && parent.calleeExpression?.text in DIALOG_CALLEES) return true
            parent = parent.parent
        }
        return false
    }

    private companion object {
        val TOP_APP_BARS = setOf(
            "TopAppBar", "CenterAlignedTopAppBar", "MediumTopAppBar", "LargeTopAppBar",
        )
        val DIALOG_CALLEES = setOf("Dialog", "AlertDialog", "BasicAlertDialog")
        val ZERO_INSETS = Regex("""WindowInsets\s*\(\s*0""")
    }
}
