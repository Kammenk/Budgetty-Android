package com.budgetty.app.data.backup

import com.budgetty.app.data.local.BudgetDao
import com.budgetty.app.data.local.CategoryDao
import com.budgetty.app.data.local.CategoryRuleDao
import com.budgetty.app.data.local.ReceiptDao
import com.budgetty.app.data.local.RecurringDao
import com.budgetty.app.data.local.TransactionDao
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

/** Exports all local data to a JSON backup and restores it (merge or full replace). */
class BackupManager(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val receiptDao: ReceiptDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val recurringDao: RecurringDao,
) {
    private val gson = Gson()

    /** Serializes the entire local dataset to a JSON string. */
    suspend fun exportJson(): String {
        val data = BackupData(
            transactions = transactionDao.getAll().first(),
            categories = categoryDao.getAll().first(),
            budgets = budgetDao.getAll().first(),
            receipts = receiptDao.getAll().first(),
            rules = categoryRuleDao.getAll().first(),
            recurring = recurringDao.getAll().first(),
        )
        return gson.toJson(data)
    }

    /**
     * Restores a JSON backup. When [replace] is true the current data is wiped first; otherwise the
     * backup is merged on top — transactions/receipts are added, and existing categories/budgets are
     * kept (only missing ones are filled in). Throws [IllegalArgumentException] on invalid JSON.
     */
    suspend fun import(json: String, replace: Boolean) {
        val data = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid Budgetty backup file", e)
        } ?: throw IllegalArgumentException("Empty backup file")

        if (replace) {
            transactionDao.clearAll()
            categoryDao.clearAll()
            budgetDao.clearAll()
            receiptDao.clearAll()
            categoryRuleDao.clearAll()
            recurringDao.clearAll()
        }
        // New ids so a merge never collides with existing transactions.
        transactionDao.insertAll(data.transactions.map { it.copy(id = 0) })
        // .orZero() tolerates older backups without receipts.tax (pre-v15) or receipts.extraCharges
        // (pre-v17) — Gson leaves the non-null column null, which would otherwise fail the insert.
        receiptDao.insertAll(data.receipts.map { it.copy(tax = it.tax.orZero(), extraCharges = it.extraCharges.orZero()) })
        categoryDao.insertOrIgnore(data.categories)
        budgetDao.insertOrIgnore(data.budgets)
        categoryRuleDao.insertOrIgnore(data.rules)
        // New ids so a merge never collides; .orEmpty() tolerates pre-v14 backups without this field.
        recurringDao.insertAll(data.recurring.orEmpty().map { it.copy(id = 0) })
    }

    /**
     * Erases the user's local financial data — used by account deletion. Categories are kept on
     * purpose: they are the app's classification taxonomy (seeded once at DB creation), not personal
     * records, and clearing them would leave the app with no categories to assign.
     */
    suspend fun wipeUserData() {
        transactionDao.clearAll()
        receiptDao.clearAll()
        budgetDao.clearAll()
        categoryRuleDao.clearAll()
        recurringDao.clearAll()
    }
}

/** Treats a money figure missing from an older backup (deserialized as null by Gson) as zero. */
private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
