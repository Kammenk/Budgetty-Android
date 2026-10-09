package com.budgetty.app.data.backup

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budgetty.app.data.local.BudgetEntity
import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.data.local.BudgetRolloverEntity
import com.budgetty.app.data.local.BudgettyDatabase
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.data.local.IgnoredSubscriptionEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.TransactionTagEntity
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.data.local.WarrantyEntity
import com.budgetty.app.data.local.WellbeingScoreEntity
import com.budgetty.app.data.settings.SettingsStore
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/**
 * Drives the real [BackupManager.import] against in-memory Room. A "Replace all" restore must leave
 * exactly the backup's rows in every table — a table the replace block forgets to clear keeps the
 * device's rows and gets the backup's copies on top (recurring bills were duplicated that way).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BackupManagerReplaceTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var manager: BackupManager
    private val gson = Gson()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, BudgettyDatabase::class.java).allowMainThreadQueries().build()
        manager = BackupManager({ db }, SettingsStore(app))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `replace leaves exactly the backup's rows in every table`() = runTest {
        manager.import(gson.toJson(dataset("Device")), replace = false)
        db.budgetRolloverDao().upsert(BudgetRolloverEntity("MONTHLY", BigDecimal("1200"), "2026-10"))
        assertThat(rowCounts().values.all { it == 1 }).isTrue()

        manager.import(gson.toJson(dataset("Backup")), replace = true)

        // budget_rollover is not backed up: the old carry-over belongs to the replaced data.
        val expected = USER_TABLES.associateWith { if (it == "budget_rollover") 0 else 1 }
        assertThat(rowCounts()).isEqualTo(expected)
        assertThat(db.recurringDao().getAll().first().map { it.label }).containsExactly("Backup salary")
        assertThat(db.ignoredSubscriptionDao().getAll().first().map { it.merchant }).containsExactly("backup tv")
    }

    @Test
    fun `restoring the same backup twice with replace does not double recurring bills`() = runTest {
        val json = gson.toJson(dataset("Backup"))
        manager.import(json, replace = true)
        manager.import(json, replace = true)

        val recurring = db.recurringDao().getAll().first()
        assertThat(recurring).hasSize(1)
        assertThat(recurring.single().autoPay).isTrue()
    }

    @Test
    fun `merge keeps a merchant dismissed on the device without duplicating it`() = runTest {
        db.ignoredSubscriptionDao().ignore(IgnoredSubscriptionEntity("backup tv", ignoredAt = 5))

        manager.import(gson.toJson(dataset("Backup")), replace = false)

        val ignored = db.ignoredSubscriptionDao().getAll().first()
        assertThat(ignored).hasSize(1)
        assertThat(ignored.single().ignoredAt).isEqualTo(5)
    }

    @Test
    fun `every user table is covered by this test`() {
        // Adding a table? Back it up (or decide it is derived) and clear it in BackupManager.import's
        // replace block, then add a row for it to dataset() and its name here.
        assertThat(rowCounts().keys).isEqualTo(USER_TABLES)
    }

    private fun rowCounts(): Map<String, Int> {
        val tables = db.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table'")
            .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
            .filterNot { it in SQLITE_INTERNAL_TABLES }
        return tables.associateWith { table ->
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`")
                .use { c -> c.moveToFirst(); c.getInt(0) }
        }
    }

    /** One row per backed-up table, labelled with [prefix] so the two datasets never collide. */
    private fun dataset(prefix: String): BackupData {
        val lower = prefix.lowercase()
        val receiptId = if (prefix == "Device") 1_000L else 2_000L
        return BackupData(
            transactions = listOf(TransactionEntity(id = 11, name = "$prefix milk", timestamp = receiptId,
                price = BigDecimal("1.20"), quantity = 1, category = "$prefix cat", receiptId = receiptId)),
            categories = listOf(CategoryEntity("$prefix cat", colorArgb = 0, isCustom = true)),
            budgets = listOf(BudgetEntity("CAT:$prefix cat", BigDecimal("100"))),
            receipts = listOf(ReceiptEntity(receiptId, "$prefix store", receiptId, BigDecimal.ZERO)),
            rules = listOf(CategoryRuleEntity("$lower milk", "$prefix cat")),
            recurring = listOf(RecurringEntity(label = "$prefix salary", amount = BigDecimal("3200"),
                isIncome = true, autoPay = true)),
            savingsGoals = listOf(SavingsGoalEntity(id = 7, name = "$prefix goal", emoji = "🎯",
                targetAmount = BigDecimal("500"))),
            savingsContributions = listOf(SavingsContributionEntity(goalId = 7, amount = BigDecimal("50"))),
            buyingLimits = listOf(BuyingLimitEntity(label = "$prefix coffee", keywords = "coffee")),
            wellbeingScores = listOf(WellbeingScoreEntity("$prefix-2026-09", 70, "good", "{}", 1L)),
            tags = listOf(TagEntity("$lower-tag")),
            transactionTags = listOf(TransactionTagEntity(transactionId = 11, tagName = "$lower-tag")),
            debts = listOf(DebtEntity(name = "$prefix card", balance = BigDecimal("900"),
                aprPercent = BigDecimal("19.9"), minPayment = BigDecimal("30"))),
            templates = listOf(TemplateEntity(name = "$prefix coffee", amount = BigDecimal("3"))),
            warranties = listOf(WarrantyEntity(name = "$prefix kettle", purchaseDate = 1L, durationMonths = 24)),
            budgetEnvelopes = listOf(BudgetEnvelopeEntity(name = "$prefix trip", limitAmount = BigDecimal("400"),
                startDate = 1L, endDate = 2L)),
            trips = listOf(TripEntity(name = "$prefix trip", tag = "$lower-tag")),
            ignoredSubscriptions = listOf(IgnoredSubscriptionEntity("$lower tv", ignoredAt = 9)),
        )
    }

    private companion object {
        val SQLITE_INTERNAL_TABLES = setOf("android_metadata", "room_master_table", "sqlite_sequence")
        val USER_TABLES = setOf(
            "transactions", "categories", "budgets", "receipts", "category_rules", "recurring",
            "budget_rollover", "savings_goals", "savings_contributions", "ignored_subscriptions",
            "buying_limits", "wellbeing_scores", "tags", "transaction_tags", "debts", "templates",
            "warranties", "budget_envelopes", "trips",
        )
    }
}
