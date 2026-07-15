package com.budgetty.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.SettingsStore
import java.math.BigDecimal
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Backs the setup quiz's side effects. Beyond the Insights customization the quiz has always
 * written, the v2 steps pre-fill real app state: the currency setting (immediately on pick), one
 * monthly income source, and the overall monthly budget (both on finish). Everything lands where
 * its normal editor lives — Account for currency, the Budget tab for income and budget — so every
 * choice stays reversible there.
 */
class InsightsQuizViewModel(
    private val settingsStore: SettingsStore,
    private val recurringRepository: RecurringRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings

    /**
     * Applies a currency pick right away — it's the same setting Account edits, and applying it
     * immediately lets the later amount fields show the right symbol. An explicit pick sticking
     * even if the quiz is skipped afterwards is intended.
     */
    fun selectCurrency(currency: Currency) = settingsStore.setCurrency(currency)

    fun skip() = settingsStore.setInsightsQuizPending(false)

    /**
     * Finishes the quiz: seeds the optional income source and monthly budget, then applies the
     * derived Insights customization (which also drops the gate). The seeding is best-effort — a
     * storage hiccup must never trap the user behind the quiz.
     */
    fun finish(
        answers: Map<String, String>,
        amountTexts: Map<String, String>,
        incomeLabel: String,
    ) {
        val incomeAmount: BigDecimal? = InsightsQuiz.incomeSeed(answers, amountTexts)
        val budgetAmount: BigDecimal? = InsightsQuiz.budgetSeed(answers, amountTexts)
        viewModelScope.launch {
            runCatching {
                incomeAmount?.let {
                    recurringRepository.upsert(
                        RecurringEntity(
                            label = incomeLabel,
                            amount = it,
                            isIncome = true,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                budgetAmount?.let { budgetRepository.setBudget(BudgetRepository.MONTHLY, it) }
            }
            settingsStore.applyInsightsQuizResult(
                hidden = InsightsQuiz.hiddenSections(answers),
                order = InsightsQuiz.sectionOrder(answers),
                encodedAnswers = InsightsQuiz.encode(answers),
            )
        }
    }
}
