package com.budgetty.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Flags a vertically-scrollable container written directly inside a bottom-sheet lambda
 * (`AdaptiveSheet { … }` / `ModalBottomSheet { … }`) that is NOT height-capped with
 * `Modifier.weight(1f, fill = false)`.
 *
 * Why this exists: on a phone [ModalBottomSheet], a scroll region whose height isn't pinned to the
 * sheet's available height drives the sheet height instead of scrolling within it — so the sheet
 * jitters/oscillates ("jumps up and down") once the content overflows. The project's fix is
 * `Modifier.weight(1f, fill = false)` on the scroll region (see AllCategoriesSheet,
 * CategoryTransactionsSheet, BudgetScreen). This rule stops the next new sheet from regressing so we
 * don't have to re-discover it one sheet at a time.
 *
 * Detects vertical scrollers: `LazyColumn`, `LazyVerticalGrid`, or any call whose Modifier chain uses
 * `.verticalScroll(...)` (a `Column`/`Box`). `LazyRow`/horizontal scrollers can't overflow the sheet
 * vertically, so they're ignored. The cap it looks for is `weight(..., fill = false)`; a fixed
 * `heightIn(max = …dp)` is deliberately NOT accepted, because the codebase's own history shows that
 * overflows and jitters on short screens.
 *
 * Scope/limitation: PSI-only (no type resolution), so it matches scrollers written INLINE in the
 * sheet lambda — which is the common case and how every real occurrence in this app is written. A
 * scroller factored into a separate composable that the sheet merely calls is not seen; that
 * indirection is rare and its author owns the cap there (e.g. ReceiptDetailContent).
 */
class SheetScrollNotCapped(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SheetScrollNotCapped",
        severity = Severity.Defect,
        description = "A scrollable container inside a bottom sheet must be capped with " +
            "Modifier.weight(1f, fill = false) so it scrolls within the sheet instead of driving the " +
            "sheet height (which makes the sheet jitter/oscillate when the content overflows).",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return

        // Non-lambda arguments only — never read the content block when deciding whether this call is
        // a scroller or whether it's capped.
        val modifierArgs = expression.valueArguments
            .mapNotNull { it.getArgumentExpression() }
            .filter { it !is KtLambdaExpression }
            .map { it.text }

        val isVerticalScroller = callee in LAZY_VERTICAL || modifierArgs.any { it.contains(VERTICAL_SCROLL) }
        if (!isVerticalScroller) return

        if (!isInsideSheet(expression)) return

        val capped = modifierArgs.any { CAP.containsMatchIn(it) }
        if (capped) return

        report(
            CodeSmell(
                issue,
                Entity.from(expression),
                "`$callee` inside a bottom sheet isn't capped with Modifier.weight(1f, fill = false); " +
                    "an overflowing sheet list makes the sheet jump. Add .weight(1f, fill = false) to " +
                    "its Modifier (as in AllCategoriesSheet / CategoryTransactionsSheet).",
            ),
        )
    }

    /** True when [expression] is lexically nested inside an AdaptiveSheet/ModalBottomSheet call. */
    private fun isInsideSheet(expression: KtCallExpression): Boolean {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtCallExpression && parent.calleeExpression?.text in SHEET_CALLEES) return true
            parent = parent.parent
        }
        return false
    }

    private companion object {
        val LAZY_VERTICAL = setOf("LazyColumn", "LazyVerticalGrid")
        val SHEET_CALLEES = setOf("AdaptiveSheet", "ModalBottomSheet")
        const val VERTICAL_SCROLL = "verticalScroll("
        val CAP = Regex("""weight\s*\(\s*[^)]*fill\s*=\s*false""")
    }
}

/** Registers Budgetty's custom detekt rules. Wired via META-INF/services + `detektPlugins`. */
class BudgettyRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "budgetty"
    override fun instance(config: Config): RuleSet =
        RuleSet(ruleSetId, listOf(SheetScrollNotCapped(config)))
}
