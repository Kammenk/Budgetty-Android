package com.budgetty.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row on the Category rules screen: a saved rule plus how many saved transactions it matches. */
data class CategoryRuleRow(
    /** Normalized match key (lower-cased) — the stable list key and the delete handle. */
    val key: String,
    /** Key with its first letter capitalized for display ("bananas" → "Bananas", "банани" → "Банани"). */
    val displayName: String,
    val category: String,
    val categoryColor: Int,
    val transactionCount: Int,
)

data class CategoryRulesUiState(
    val rules: List<CategoryRuleRow> = emptyList(),
    /** False until the first DB emission, so the empty state doesn't flash on cold start. */
    val isLoaded: Boolean = false,
)

/**
 * Backs the "Category rules" management screen (Account → Category rules): the learned name →
 * category rules, each annotated with the number of saved transactions it currently matches, plus
 * a delete action. Counts are computed in Kotlin on the same normalized key the rules use so they
 * fold case + Cyrillic exactly as auto-apply does on scan.
 */
class CategoryRulesViewModel(
    private val ruleRepository: CategoryRuleRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoryRulesUiState> = combine(
        ruleRepository.rules,
        transactionRepository.getAll(),
        categoryRepository.categories,
    ) { rules, transactions, categories ->
        val counts = transactions.groupingBy { CategoryRuleEntity.key(it.name) }.eachCount()
        val savedColors = categories.associate { it.name to it.colorArgb }
        val rows = rules
            .map { rule ->
                CategoryRuleRow(
                    key = rule.name,
                    displayName = rule.name.replaceFirstChar { it.uppercase() },
                    category = rule.category,
                    categoryColor = savedColors[rule.category] ?: Categories.colorOf(rule.category),
                    transactionCount = counts[rule.name] ?: 0,
                )
            }
            .sortedBy { it.displayName.lowercase() }
        CategoryRulesUiState(rules = rows, isLoaded = true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryRulesUiState(),
    )

    /** Forgets a rule so future scans stop auto-filing that item. Saved transactions are untouched. */
    fun deleteRule(key: String) {
        viewModelScope.launch { ruleRepository.removeRule(key) }
    }
}
