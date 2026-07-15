package com.budgetty.app.ui.quiz

import androidx.annotation.StringRes
import com.budgetty.app.R
import com.budgetty.app.ui.insights.InsightsSection
import java.math.BigDecimal

/** One selectable answer of a quiz question. [id] is persisted, so existing values must stay stable. */
data class QuizOption(
    val id: String,
    val emoji: String,
    @param:StringRes val labelRes: Int,
)

/**
 * An optional inline amount input on a question: picking the option with [optionId] reveals the
 * field and a Continue button instead of auto-advancing (every other option still auto-advances).
 * The amount is always optional — Continue works with the field left blank.
 */
data class QuizAmountField(
    val optionId: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val helperRes: Int,
)

/** One step of the setup quiz. [id] keys the answer map and is persisted; must stay stable. */
data class QuizQuestion(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val options: List<QuizOption>,
    val amountField: QuizAmountField? = null,
)

/** A row of the closing step's "what got tailored" summary card; [arg] fills the label's placeholder when present. */
data class QuizSummaryLine(val emoji: String, @param:StringRes val labelRes: Int, val arg: String? = null)

/**
 * The one-time post-signup setup questionnaire: the fixed question set plus the deterministic
 * mapping from answers to the Insights customization settings. The quiz only ever pre-fills the
 * same hidden-sections / section-order values the Customize-sections menu edits, so every outcome
 * stays reversible there — and a core set of sections is never hidden, so Insights never looks
 * broken however the questions are answered.
 */
object InsightsQuiz {

    private const val GOAL = "goal"
    const val INCOME = "income"
    const val BILLS = "bills"
    const val BUDGET = "budget"
    private const val DETAIL = "detail"
    private const val ENTRY = "entry"

    /** Answer-map key of the currency step (not a [QuizQuestion]; the stored value is the currency code). */
    const val CURRENCY = "currency"

    /** Index of the currency step. It sits right after the goal question, before the income step, so the amount fields can show the chosen symbol. */
    const val CURRENCY_STEP = 1

    /** Total steps before the closing one: the six questions plus the currency step. */
    val stepCount: Int get() = questions.size + 1

    /** The question shown at [step], or null when [step] is the currency step. */
    fun questionAt(step: Int): QuizQuestion? = when {
        step == CURRENCY_STEP -> null
        step < CURRENCY_STEP -> questions[step]
        else -> questions[step - 1]
    }

    /** Parses an amount field's text into a positive amount (comma decimals ok), or null if blank/invalid. */
    fun amountOf(text: String): BigDecimal? =
        text.replace(',', '.').trim().toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

    /** The income-source amount to seed on finish — only when income tracking was chosen. */
    fun incomeSeed(answers: Map<String, String>, amountTexts: Map<String, String>): BigDecimal? =
        if (answers[INCOME] == "yes") amountOf(amountTexts[INCOME].orEmpty()) else null

    /** The monthly-budget amount to seed on finish — only when a budget was planned. */
    fun budgetSeed(answers: Map<String, String>, amountTexts: Map<String, String>): BigDecimal? =
        if (answers[BUDGET] == "yes") amountOf(amountTexts[BUDGET].orEmpty()) else null

    val questions: List<QuizQuestion> = listOf(
        QuizQuestion(
            GOAL, R.string.quiz_q_goal, R.string.quiz_q_goal_sub,
            listOf(
                QuizOption("see", "🔍", R.string.quiz_goal_see),
                QuizOption("budget", "🎯", R.string.quiz_goal_budget),
                QuizOption("bills", "📅", R.string.quiz_goal_bills),
                QuizOption("savings", "🪙", R.string.quiz_goal_savings),
            ),
        ),
        QuizQuestion(
            INCOME, R.string.quiz_q_income, R.string.quiz_q_income_sub,
            listOf(
                QuizOption("yes", "💰", R.string.quiz_income_yes),
                QuizOption("no", "🧾", R.string.quiz_income_no),
            ),
            amountField = QuizAmountField(
                optionId = "yes",
                labelRes = R.string.quiz_income_field_label,
                helperRes = R.string.quiz_income_field_helper,
            ),
        ),
        QuizQuestion(
            BILLS, R.string.quiz_q_bills, R.string.quiz_q_bills_sub,
            listOf(
                QuizOption("yes", "🔁", R.string.quiz_bills_yes),
                QuizOption("no", "✨", R.string.quiz_bills_no),
            ),
        ),
        QuizQuestion(
            BUDGET, R.string.quiz_q_budget, R.string.quiz_q_budget_sub,
            listOf(
                QuizOption("yes", "✅", R.string.quiz_budget_yes),
                QuizOption("later", "🤔", R.string.quiz_budget_later),
                QuizOption("no", "❌", R.string.quiz_budget_no),
            ),
            amountField = QuizAmountField(
                optionId = "yes",
                labelRes = R.string.quiz_budget_field_label,
                helperRes = R.string.quiz_budget_field_helper,
            ),
        ),
        QuizQuestion(
            DETAIL, R.string.quiz_q_detail, R.string.quiz_q_detail_sub,
            listOf(
                QuizOption("big", "🌅", R.string.quiz_detail_big),
                QuizOption("full", "🔬", R.string.quiz_detail_full),
            ),
        ),
        // Informational only for now: stored with the other answers, mapped to nothing.
        QuizQuestion(
            ENTRY, R.string.quiz_q_entry, R.string.quiz_q_entry_sub,
            listOf(
                QuizOption("scan", "📷", R.string.quiz_entry_scan),
                QuizOption("manual", "⌨️", R.string.quiz_entry_manual),
                QuizOption("both", "🤝", R.string.quiz_entry_both),
            ),
        ),
    )

    /**
     * Sections hidden for [answers]. Never touches the core set (Breakdown, Summary, Highlights,
     * Trend, Top categories), so the screen stays coherent even in the most minimal outcome.
     */
    fun hiddenSections(answers: Map<String, String>): Set<String> = buildSet {
        if (answers[INCOME] == "no") {
            add(InsightsSection.INCOME_SPENDING.key)
            add(InsightsSection.SAVINGS_RATE.key)
            add(InsightsSection.INCOME_BY_SOURCE.key)
        }
        if (answers[BILLS] == "no") {
            add(InsightsSection.UPCOMING_BILLS.key)
            add(InsightsSection.FIXED_FLEXIBLE.key)
        }
        if (answers[BUDGET] == "no") add(InsightsSection.BUDGET.key)
        if (answers[DETAIL] == "big") {
            add(InsightsSection.TOP_STORES.key)
            add(InsightsSection.BIGGEST_PURCHASES.key)
            add(InsightsSection.PERIOD_COMPARISON.key)
        }
    }

    /**
     * Section order for [answers]: the main-goal sections move up to sit right after the Breakdown
     * hero (which stays first — it also carries the empty state a brand-new user sees). Empty — the
     * default order — when the goal is the plain spending overview. A section can be both boosted
     * and hidden (savings goal + "just spending"); hiding wins at render time, so that's harmless.
     */
    fun sectionOrder(answers: Map<String, String>): List<String> {
        val boosted = when (answers[GOAL]) {
            "budget" -> listOf(InsightsSection.BUDGET, InsightsSection.PERIOD_COMPARISON)
            "bills" -> listOf(InsightsSection.UPCOMING_BILLS, InsightsSection.FIXED_FLEXIBLE)
            "savings" -> listOf(InsightsSection.SAVINGS_RATE, InsightsSection.INCOME_SPENDING)
            else -> return emptyList()
        }.map { it.key }
        val head = listOf(InsightsSection.BREAKDOWN.key) + boosted
        return head + InsightsSection.entries.map { it.key }.filterNot { it in head }
    }

    /**
     * The closing step's summary rows. [incomeAmount] / [budgetAmount] are pre-formatted display
     * amounts (null when the field was left blank) — they upgrade the plain income line and add the
     * budget line. A bills-yes answer is deliberately absent: it surfaces as the "add your bills"
     * hand-off hint under the CTA instead of a summary row.
     */
    fun summary(
        answers: Map<String, String>,
        incomeAmount: String? = null,
        budgetAmount: String? = null,
    ): List<QuizSummaryLine> = listOfNotNull(
        when (answers[GOAL]) {
            "see" -> QuizSummaryLine("🔍", R.string.quiz_sum_goal_see)
            "budget" -> QuizSummaryLine("🎯", R.string.quiz_sum_goal_budget)
            "bills" -> QuizSummaryLine("📅", R.string.quiz_sum_goal_bills)
            "savings" -> QuizSummaryLine("🪙", R.string.quiz_sum_goal_savings)
            else -> null
        },
        answers[CURRENCY]?.let { QuizSummaryLine("💱", R.string.quiz_sum_currency, it) },
        when {
            answers[INCOME] == "yes" && incomeAmount != null ->
                QuizSummaryLine("💰", R.string.quiz_sum_income_amount, incomeAmount)
            answers[INCOME] == "yes" -> QuizSummaryLine("💰", R.string.quiz_sum_income_yes)
            answers[INCOME] == "no" -> QuizSummaryLine("🧾", R.string.quiz_sum_income_no)
            else -> null
        },
        if (answers[BUDGET] == "yes" && budgetAmount != null) {
            QuizSummaryLine("✅", R.string.quiz_sum_budget_amount, budgetAmount)
        } else {
            null
        },
        if (answers[BILLS] == "no") QuizSummaryLine("✨", R.string.quiz_sum_bills_no) else null,
        when (answers[DETAIL]) {
            "big" -> QuizSummaryLine("🌅", R.string.quiz_sum_detail_big)
            "full" -> QuizSummaryLine("🔬", R.string.quiz_sum_detail_full)
            else -> null
        },
    )

    /** True when the closing step should show the "add your recurring bills" hand-off hint. */
    fun showsBillsHint(answers: Map<String, String>): Boolean = answers[BILLS] == "yes"

    /** Answers encoded for persistence ("goal=budget,currency=EUR,…") in step order, kept for future re-tuning. */
    fun encode(answers: Map<String, String>): String {
        val ids = listOf(questions.first().id, CURRENCY) + questions.drop(1).map { it.id }
        return ids.mapNotNull { id -> answers[id]?.let { "$id=$it" } }.joinToString(",")
    }
}
