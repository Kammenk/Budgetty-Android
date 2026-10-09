package com.budgetty.app.ui.components

import com.budgetty.app.category.CategorySuggester
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Habit-based suggestions for the category picker's "Suggested for you" row: the user's categories
 * ranked by recent use (or generic "Common picks" before there's enough history), plus the learned
 * name → category rules so the picker can float a match for the item being categorised. Recomputed
 * live as transactions or rules change; see [CategorySuggester]. Shared by every screen that opens
 * the picker, so they all suggest the same thing.
 */
fun categorySuggestionsFlow(
    transactions: TransactionRepository,
    rules: CategoryRuleRepository,
): Flow<CategorySuggestions> = combine(
    transactions.recentCategoryStamps(),
    rules.rules,
) { stamps, ruleRows ->
    val ranked = CategorySuggester.rank(
        stamps.map { it.category to it.timestamp },
        System.currentTimeMillis(),
    )
    CategorySuggestions(
        ranked = ranked.categories,
        personalized = ranked.personalized,
        rulesByName = ruleRows.associate { it.name to it.category },
    )
}
