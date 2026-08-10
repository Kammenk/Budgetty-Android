package com.budgetty.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.category.Categories
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Manage Categories screen renders: the full category list (built-ins + customs) and
 *  the Premium flag that gates the custom-category cap. */
data class ManageCategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isPremium: Boolean = false,
)

/**
 * Backs the standalone **Manage categories** screen (reached from Account). It exposes the category
 * list plus the same create / edit / delete / re-home operations the receipt picker offers — the
 * screen re-presents that CRUD as a management surface. The mutation logic mirrors
 * [com.budgetty.app.ui.upload.UploadViewModel] (kept in lockstep): a rename cascades the new name
 * across transactions, rules and the per-category budget; a delete re-files its transactions to
 * "Other"; giving a category a parent releases its own children (two levels only).
 */
class ManageCategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val budgetRepository: BudgetRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageCategoriesUiState())
    val uiState: StateFlow<ManageCategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.categories.collect { cats -> _uiState.update { it.copy(categories = cats) } }
        }
        viewModelScope.launch {
            billingManager.isPremium.collect { premium -> _uiState.update { it.copy(isPremium = premium) } }
        }
    }

    /** Creates a new custom category, or updates / renames an existing one ([original] = its current
     *  name when editing, null when creating). No-ops on invalid or duplicate input, or past the cap. */
    fun saveCustomCategory(original: String?, rawName: String, icon: String, colorArgb: Int, parent: String?) {
        val name = rawName.trim()
        if (name.isEmpty() || icon.isEmpty() || isDuplicateName(name, original)) return
        viewModelScope.launch {
            val existing = _uiState.value.categories
            if (original == null) {
                val customCount = existing.count { it.isCustom }
                val cap = if (_uiState.value.isPremium) Categories.MAX_CUSTOM_LIMIT else Categories.FREE_CUSTOM_LIMIT
                if (customCount >= cap) return@launch
                categoryRepository.upsert(
                    CategoryEntity(
                        name = name,
                        colorArgb = colorArgb,
                        icon = icon,
                        isCustom = true,
                        createdAt = System.currentTimeMillis(),
                        parent = parent,
                    ),
                )
            } else {
                val createdAt = existing.firstOrNull { it.name == original }?.createdAt
                    ?: System.currentTimeMillis()
                categoryRepository.upsert(
                    CategoryEntity(name, colorArgb, icon, isCustom = true, createdAt = createdAt, parent = parent),
                )
                // Compare EXACTLY (not case-insensitively): a case-only rename (e.g. "Coffee" →
                // "COFFEE") is still a rename. The categories primary key is case-sensitive, so
                // skipping the cascade + delete would leave the old-cased row behind as a duplicate
                // (rendering with the wrong colour) and let a free user slip past the custom cap.
                if (name != original) {
                    transactionRepository.reassignCategory(original, name)
                    categoryRuleRepository.reassignCategory(original, name)
                    budgetRepository.renameCategoryBudget(original, name)
                    // Keep this category's own children attached across the rename.
                    categoryRepository.reparentChildren(original, name)
                    categoryRepository.deleteByName(original)
                }
                // Two levels only: a category given a parent can't also keep children — release them.
                if (parent != null) categoryRepository.promoteChildrenOf(name)
            }
        }
    }

    /** Re-homes any category — built-in or custom — under [parent] (null = top-level). Backs the
     *  built-in rows' "Move to group" and the custom form's group field. */
    fun setCategoryParent(name: String, parent: String?) {
        viewModelScope.launch {
            categoryRepository.setParent(name, parent)
            if (parent != null) categoryRepository.promoteChildrenOf(name)
        }
    }

    /** Deletes a custom category: its transactions re-file to "Other", its rules and per-category
     *  budget are dropped, and its own children are promoted to top-level. */
    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.promoteChildrenOf(name)
            transactionRepository.reassignCategory(name, Categories.OTHER)
            categoryRuleRepository.removeRulesForCategory(name)
            budgetRepository.clearCategoryBudget(name)
            categoryRepository.deleteByName(name)
        }
    }

    /** How many saved transactions currently sit in [name] — shown in the delete confirmation. */
    suspend fun transactionCount(name: String): Int = transactionRepository.countByCategory(name)

    /** True when [name] (trimmed, case-insensitive) already names a predefined or existing custom
     *  category other than [original]. Kept in lockstep with UploadViewModel.isDuplicateName. */
    private fun isDuplicateName(name: String, original: String?): Boolean {
        val key = name.trim().lowercase()
        if (original != null && key == original.trim().lowercase()) return false
        return _uiState.value.categories.any {
            it.name.trim().lowercase() == key && (it.isCustom || Categories.isPredefined(it.name))
        }
    }
}
