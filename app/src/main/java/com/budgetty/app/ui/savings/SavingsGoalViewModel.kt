package com.budgetty.app.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.ui.util.SavingsMath
import com.budgetty.app.ui.util.SavingsProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** One savings goal card: the goal plus its derived progress (saved / %, pace, reached). */
data class SavingsGoalCardUi(
    val goal: SavingsGoalEntity,
    val progress: SavingsProgress,
)

data class SavingsGoalDetailUiState(
    val loaded: Boolean = false,
    /** True once loaded and the goal no longer exists (e.g. just deleted) — the screen pops. */
    val missing: Boolean = false,
    val goal: SavingsGoalEntity? = null,
    val contributions: List<SavingsContributionEntity> = emptyList(),
    val progress: SavingsProgress? = null,
)

/** Backs one savings-goal detail screen (goal + its contribution history + edit/delete/contribute). */
class SavingsGoalViewModel(
    private val savingsRepository: SavingsRepository,
    billingManager: BillingManager,
    private val goalId: Long,
) : ViewModel() {

    val uiState: StateFlow<SavingsGoalDetailUiState> =
        combine(
            savingsRepository.goal(goalId),
            savingsRepository.contributions(goalId),
        ) { goal, contributions ->
            if (goal == null) {
                SavingsGoalDetailUiState(loaded = true, missing = true)
            } else {
                SavingsGoalDetailUiState(
                    loaded = true,
                    goal = goal,
                    contributions = contributions,
                    progress = SavingsMath.progress(goal, contributions),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsGoalDetailUiState())

    /** Logs a contribution: [amount] positive to add to savings, negative to withdraw. */
    fun addContribution(amount: BigDecimal, note: String, date: Long) {
        viewModelScope.launch {
            savingsRepository.addContribution(
                SavingsContributionEntity(goalId = goalId, amount = amount, note = note, date = date),
            )
        }
    }

    fun updateGoal(name: String, emoji: String, target: BigDecimal, targetDate: Long?) {
        viewModelScope.launch {
            val current = savingsRepository.goal(goalId).first() ?: return@launch
            savingsRepository.upsertGoal(
                current.copy(name = name, emoji = emoji, targetAmount = target, targetDate = targetDate),
            )
        }
    }

    fun deleteGoal(onDeleted: () -> Unit) {
        viewModelScope.launch {
            savingsRepository.deleteGoal(goalId)
            onDeleted()
        }
    }
}
