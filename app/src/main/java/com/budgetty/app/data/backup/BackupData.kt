package com.budgetty.app.data.backup

import com.budgetty.app.data.local.BudgetEntity
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity

/** The full local dataset, serialized to/from a JSON backup file. */
data class BackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList(),
    val rules: List<CategoryRuleEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
)
