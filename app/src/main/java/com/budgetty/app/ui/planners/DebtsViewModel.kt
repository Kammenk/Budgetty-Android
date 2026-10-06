package com.budgetty.app.ui.planners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.data.repository.DebtRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Debt payoff planner: the user's debts (persisted) plus add/edit/delete. The payoff
 * simulation itself is pure ([DebtPayoffSimulator]) and runs in the screen off the live list, so the
 * strategy and extra-payment controls stay responsive without round-tripping through the view model.
 */
class DebtsViewModel(
    private val repository: DebtRepository,
) : ViewModel() {

    val debts: StateFlow<List<DebtEntity>> = repository.debts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun save(debt: DebtEntity) {
        viewModelScope.launch { repository.upsert(debt) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
