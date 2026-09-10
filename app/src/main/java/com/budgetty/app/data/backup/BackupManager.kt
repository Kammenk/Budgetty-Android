package com.budgetty.app.data.backup

import androidx.room.withTransaction
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.Language
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.data.settings.ThemeMode
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

/** Exports the active account's local data to a JSON backup and restores it (merge or full replace). */
class BackupManager(
    private val db: UserDatabaseManager,
    private val settingsStore: SettingsStore,
) {
    private val transactionDao get() = db.database.transactionDao()
    private val categoryDao get() = db.database.categoryDao()
    private val budgetDao get() = db.database.budgetDao()
    private val receiptDao get() = db.database.receiptDao()
    private val categoryRuleDao get() = db.database.categoryRuleDao()
    private val recurringDao get() = db.database.recurringDao()
    private val savingsDao get() = db.database.savingsDao()
    private val buyingLimitDao get() = db.database.buyingLimitDao()
    private val wellbeingScoreDao get() = db.database.wellbeingScoreDao()

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
            savingsGoals = savingsDao.getGoals().first(),
            savingsContributions = savingsDao.getAllContributions().first(),
            buyingLimits = buyingLimitDao.getAll().first(),
            wellbeingScores = wellbeingScoreDao.getAll().first(),
            settings = currentBackupSettings(),
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

        // One transaction so a failure or process death mid-restore can't leave the account
        // half-wiped (matters most under replace=true, which clears first).
        db.database.withTransaction {
            if (replace) {
                transactionDao.clearAll()
                categoryDao.clearAll()
                budgetDao.clearAll()
                receiptDao.clearAll()
                categoryRuleDao.clearAll()
                // Child before parent (the goal→contribution CASCADE would cover it too).
                savingsDao.clearContributions()
                savingsDao.clearGoals()
                buyingLimitDao.clearAll()
                wellbeingScoreDao.clearAll()
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
            // Savings: insert goals with fresh ids, then remap each contribution's goalId onto the
            // freshly-minted goal id (insertAllGoals returns the new ids in input order) so the
            // goal↔contribution link survives. .orEmpty() tolerates pre-savings backups; a
            // contribution whose goal is missing from the backup is dropped.
            val goals = data.savingsGoals.orEmpty()
            val newGoalIds = savingsDao.insertAllGoals(goals.map { it.copy(id = 0) })
            val goalIdMap = goals.zip(newGoalIds).associate { (old, newId) -> old.id to newId }
            savingsDao.insertAllContributions(
                data.savingsContributions.orEmpty().mapNotNull { contribution ->
                    goalIdMap[contribution.goalId]?.let { contribution.copy(id = 0, goalId = it) }
                },
            )
            // Buying limits: fresh ids so a merge never collides; .orEmpty() tolerates pre-v25 backups
            // (Gson leaves the absent field null). Limits carry no child rows, so no id remap is needed.
            buyingLimitDao.insertAll(data.buyingLimits.orEmpty().map { it.copy(id = 0) })
            // Wellbeing history: keyed by periodId (a natural key, no id to remap); .orEmpty() tolerates
            // pre-v26 backups. insertAll IGNOREs a periodId clash, so a merge keeps the on-device
            // snapshot rather than letting the backup rewrite a month's finalized score (§3.1).
            wellbeingScoreDao.insertAll(data.wellbeingScores.orEmpty())
        }

        // Preferences live outside Room (a device-global SharedPreferences store), so they're applied
        // after the data transaction commits — and only on a full replace. A merge adds a backup's
        // data on top of the current account, so it must not overwrite the device's current display
        // preferences. A pre-settings backup has settings == null and skips this entirely.
        if (replace) data.settings?.let { applySettings(it) }
    }

    /** Snapshots the current display / data-interpretation preferences for an export. */
    private fun currentBackupSettings(): BackupSettings {
        val s = settingsStore.settings.value
        return BackupSettings(
            currency = s.currency.name,
            dateFormat = s.dateFormat.name,
            language = s.language.name,
            themeMode = s.themeMode.name,
            accent = s.accent.name,
            monthStartDay = s.monthStartDay,
            budgetRolloverEnabled = s.budgetRolloverEnabled,
            hiddenHomeSections = s.hiddenHomeSections.toList(),
            hiddenInsightsSections = s.hiddenInsightsSections.toList(),
            homeSectionOrder = s.homeSectionOrder,
            insightsSectionOrder = s.insightsSectionOrder,
            recapEnabled = s.recapEnabled,
            recapFrequency = s.recapFrequency.name,
        )
    }

    /**
     * Applies the preferences from a restored backup. Every field is optional (an older or
     * cross-platform backup may omit some) and an unrecognized enum name is skipped, so the current
     * on-device value is kept rather than reset to a default. Security, consent and transient-gate
     * settings are intentionally absent from [BackupSettings] and so can never be restored.
     */
    private fun applySettings(s: BackupSettings) {
        applyEnum<Currency>(s.currency, settingsStore::setCurrency)
        applyEnum<DateFormatOption>(s.dateFormat, settingsStore::setDateFormat)
        applyEnum<Language>(s.language, settingsStore::setLanguage)
        applyEnum<ThemeMode>(s.themeMode, settingsStore::setThemeMode)
        applyEnum<AccentTheme>(s.accent, settingsStore::setAccent)
        applyEnum<RecapFrequency>(s.recapFrequency, settingsStore::setRecapFrequency)
        s.monthStartDay?.let(settingsStore::setMonthStartDay)
        s.budgetRolloverEnabled?.let(settingsStore::setBudgetRolloverEnabled)
        s.recapEnabled?.let(settingsStore::setRecapEnabled)
        s.hiddenHomeSections?.let { settingsStore.setHiddenHomeSections(it.toSet()) }
        s.hiddenInsightsSections?.let { settingsStore.setHiddenInsightsSections(it.toSet()) }
        s.homeSectionOrder?.let(settingsStore::setHomeSectionOrder)
        s.insightsSectionOrder?.let(settingsStore::setInsightsSectionOrder)
    }

    /** Parses [name] to an enum of type [T] and applies it via [set]; a null or unknown name is a no-op. */
    private inline fun <reified T : Enum<T>> applyEnum(name: String?, set: (T) -> Unit) {
        val value = name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: return
        set(value)
    }
}

/** Treats a money figure missing from an older backup (deserialized as null by Gson) as zero. */
private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
