package com.budgetty.app.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.TemplateRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.ui.components.CategorySuggestions
import com.budgetty.app.ui.components.categorySuggestionsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Manage-templates screen and the Add-sheet strip: the user's templates + add/edit/delete. */
class TemplatesViewModel(
    private val repository: TemplateRepository,
    transactionRepository: TransactionRepository,
    categoryRuleRepository: CategoryRuleRepository,
) : ViewModel() {

    val templates: StateFlow<List<TemplateEntity>> = repository.templates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The editor's category picker suggests the same habit-ranked categories as the add screen. */
    val categorySuggestions: StateFlow<CategorySuggestions> =
        categorySuggestionsFlow(transactionRepository, categoryRuleRepository)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategorySuggestions())

    fun save(template: TemplateEntity) {
        viewModelScope.launch {
            // New templates (id 0) get a createdAt so the list/strip order is stable (oldest first).
            val toSave = if (template.id == 0L && template.createdAt == 0L) {
                template.copy(createdAt = System.currentTimeMillis())
            } else {
                template
            }
            repository.upsert(toSave)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
