package com.budgetty.app.data.backup

import com.budgetty.app.data.local.BudgetEntity
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.WellbeingScoreEntity

/**
 * The full local dataset, serialized to/from a JSON backup file. Collections added after the first
 * backup format (recurring, savings, buying limits, wellbeing history) are read back with `.orEmpty()`
 * in [BackupManager] — Gson leaves a field absent from an older backup as null despite the default here.
 */
data class BackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList(),
    val rules: List<CategoryRuleEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val savingsContributions: List<SavingsContributionEntity> = emptyList(),
    val buyingLimits: List<BuyingLimitEntity> = emptyList(),
    val wellbeingScores: List<WellbeingScoreEntity> = emptyList(),
    /** User preferences (see [BackupSettings]); null in backups written before this field existed. */
    val settings: BackupSettings? = null,
)

/**
 * The curated subset of the user's preferences carried in a backup: display and data-interpretation
 * settings, so a full restore reproduces the account faithfully on a new device. [currency] matters
 * most — the app appends a currency symbol rather than converting amounts, so the same numbers must
 * keep their symbol on restore; [monthStartDay] and [budgetRolloverEnabled] change how the restored
 * data is bucketed into periods and budgets, so they travel with it too.
 *
 * Deliberately EXCLUDES, and must keep excluding:
 *  - security — the app-lock PIN hash, biometric flag and auto-lock delay. Never write a PIN into a
 *    file the user can share or store in the clear.
 *  - consent — crash-reporting and analytics opt-in. These are device/person-scoped (not reset on
 *    sign-out), so restoring a backup must never silently flip a device's consent choice.
 *  - transient gate / timing state — recap last-shown markers, onboarding-seen, the Insights-quiz
 *    gate, dismissed nudges/tips, remembered sort/search. Stale values would suppress a due recap or
 *    re-trigger a one-time gate.
 *
 * Every field is nullable: a backup that predates this block (or one from iOS that omits a field)
 * leaves it null and [BackupManager] keeps the current on-device value. Enums are stored by their
 * `name`; an unrecognized value is skipped, never applied. Applied only on a full replace restore,
 * never on a merge — see [BackupManager.import].
 */
data class BackupSettings(
    val currency: String? = null,
    val dateFormat: String? = null,
    val language: String? = null,
    val themeMode: String? = null,
    val accent: String? = null,
    val monthStartDay: Int? = null,
    val budgetRolloverEnabled: Boolean? = null,
    val hiddenHomeSections: List<String>? = null,
    val hiddenInsightsSections: List<String>? = null,
    val homeSectionOrder: List<String>? = null,
    val insightsSectionOrder: List<String>? = null,
    val customInsightsSections: List<String>? = null,
    val recapEnabled: Boolean? = null,
    val recapFrequency: String? = null,
)
