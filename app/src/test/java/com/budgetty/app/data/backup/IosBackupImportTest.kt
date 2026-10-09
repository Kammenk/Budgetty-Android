package com.budgetty.app.data.backup

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budgetty.app.category.Categories
import com.budgetty.app.category.CategoryBucket
import com.budgetty.app.data.local.BudgettyDatabase
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.TransactionTagEntity
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.Language
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.data.settings.ThemeMode
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

/**
 * Restores real iOS backup files (test resources shaped exactly like `Backup.swift` writes them) through
 * the production [BackupManager.import] into in-memory Room, and checks every table landed in its
 * Android shape. Also pins that an Android backup still restores as before and that a file in neither
 * format still fails without touching the account.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IosBackupImportTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var settingsStore: SettingsStore
    private lateinit var manager: BackupManager
    private val gson = Gson()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, BudgettyDatabase::class.java).allowMainThreadQueries().build()
        settingsStore = SettingsStore(app)
        manager = BackupManager({ db }, settingsStore)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `receipts restore with their line items stamped by the printed date`() = runTest {
        manager.import(fixture("ios_backup.json"), replace = true)

        val receipts = db.receiptDao().getAll().first().associateBy { it.store }
        assertThat(receipts.keys).containsExactly("Lidl", "Corner Café", "Shell")
        val lidl = receipts.getValue("Lidl")
        assertThat(lidl.timestamp).isEqualTo(ms("2026-10-01T09:30:00Z"))
        assertThat(lidl.date).isEqualTo(ms("2026-09-30T00:00:00Z"))
        assertThat(lidl.discount).isEqualToIgnoringScale("0.5")
        assertThat(lidl.tax).isEqualToIgnoringScale("1.23")
        assertThat(lidl.taxOnTop).isFalse()
        assertThat(lidl.isManual).isFalse()
        // Created in the same second as Lidl: bumped 1 ms rather than REPLACing it on the primary key.
        val shell = receipts.getValue("Shell")
        assertThat(shell.timestamp).isEqualTo(lidl.timestamp + 1)
        assertThat(shell.taxOnTop).isTrue()
        assertThat(shell.extraCharges).isEqualToIgnoringScale("1.5")
        val cafe = receipts.getValue("Corner Café")
        assertThat(cafe.isManual).isTrue()

        val txns = db.transactionDao().getAll().first().associateBy { it.name }
        assertThat(txns.keys).containsExactly("Milk 1L", "Sourdough", "Coffee beans", "Flat white", "Diesel")
        val milk = txns.getValue("Milk 1L")
        // Bucketed by the receipt's printed date (as finalizeUpload saves), not the item's createdAt.
        assertThat(milk.timestamp).isEqualTo(lidl.date)
        assertThat(milk.receiptId).isEqualTo(lidl.timestamp)
        assertThat(milk.price).isEqualToIgnoringScale("1.89")
        assertThat(milk.quantity).isEqualTo(2)
        assertThat(milk.category).isEqualTo("Dairy")
        assertThat(txns.getValue("Diesel").receiptId).isEqualTo(shell.timestamp)
        assertThat(txns.getValue("Diesel").timestamp).isEqualTo(shell.date)
        assertThat(txns.getValue("Flat white").receiptId).isEqualTo(cafe.timestamp)
    }

    @Test
    fun `item tags become catalog rows and links, and every trip keeps its tag`() = runTest {
        manager.import(fixture("ios_backup.json"), replace = true)

        val nameById = db.transactionDao().getAll().first().associate { it.id to it.name }
        assertThat(db.tagDao().allLinks().first().map { nameById[it.transactionId] to it.tagName }).containsExactly(
            "Milk 1L" to "lisbon-2026",
            "Milk 1L" to "groceries",
            "Sourdough" to "groceries",
            "Diesel" to "lisbon-2026",
        )
        val tags = db.tagDao().allTags().first().associate { it.name to it.createdAt }
        // "gifts" is carried by nothing and "ski-week" only by a past trip — both still restored.
        assertThat(tags.keys).containsExactly("gifts", "lisbon-2026", "groceries", "ski-week")
        assertThat(tags["lisbon-2026"]).isEqualTo(ms("2026-09-28T07:00:00Z")) // iOS's catalog time wins
        assertThat(tags["groceries"]).isEqualTo(ms("2026-10-01T09:30:00Z")) // first item carrying it
        assertThat(tags["ski-week"]).isEqualTo(ms("2026-05-01T07:00:00Z")) // its trip's createdAt

        val trips = db.tripDao().getAllOnce().associateBy { it.name }
        val lisbon = trips.getValue("Lisbon")
        assertThat(lisbon.tag).isEqualTo("lisbon-2026")
        assertThat(lisbon.active).isTrue()
        assertThat(lisbon.budgetAmount).isEqualToIgnoringScale("900")
        assertThat(lisbon.startDate).isEqualTo(ms("2026-09-29T00:00:00Z"))
        assertThat(lisbon.endedAt).isNull()
        val ski = trips.getValue("Ski week")
        assertThat(ski.active).isFalse()
        assertThat(ski.hasDates).isFalse()
        assertThat(ski.endedAt).isEqualTo(ms("2026-05-10T18:00:00Z"))
    }

    @Test
    fun `replace keeps every built-in category next to the iOS custom ones`() = runTest {
        db.categoryDao().upsert(CategoryEntity("Device only", colorArgb = 1, isCustom = true))

        manager.import(fixture("ios_backup.json"), replace = true)

        val categories = db.categoryDao().getAll().first().associateBy { it.name }
        // iOS exports only its custom categories; the replace wiped the table, so the converter must
        // have put the whole built-in set back — and the device's own custom row is gone.
        assertThat(categories.keys)
            .containsExactlyElementsIn(Categories.predefined.map { it.name } + "Specialty Coffee")
        val custom = categories.getValue("Specialty Coffee")
        assertThat(custom.isCustom).isTrue()
        assertThat(custom.colorArgb).isEqualTo(0xFF8D6E63.toInt())
        assertThat(custom.icon).isEqualTo("☕️")
        assertThat(custom.parent).isEqualTo("Groceries")
        assertThat(custom.bucket).isEqualTo(CategoryBucket.WANT)
        assertThat(custom.createdAt).isEqualTo(ms("2026-07-15T10:00:00Z"))
        val groceries = categories.getValue("Groceries")
        assertThat(groceries.isCustom).isFalse()
        assertThat(groceries.icon).isEqualTo("🧺")
        assertThat(groceries.parent).isNull()
        assertThat(groceries.bucket).isNull()
        // Built-in overrides from iOS's categoryOverrides.
        assertThat(categories.getValue("Coffee & Cafés").bucket).isEqualTo(CategoryBucket.NEED)
        assertThat(categories.getValue("Fuel").parent).isEqualTo("Bills & Finance")
    }

    @Test
    fun `budgets, bills, savings, limits and planners land in their Android shape`() = runTest {
        manager.import(fixture("ios_backup.json"), replace = true)

        assertThat(db.budgetDao().getAll().first().associate { it.budgetKey to it.amount.toPlainString() })
            .containsExactly("MONTHLY", "1500", "CAT:Groceries", "250.5")
        assertThat(db.categoryRuleDao().getAllOnce().map { it.name to it.category })
            .containsExactly("milk 1l" to "Dairy")

        val recurring = db.recurringDao().getAll().first().associateBy { it.label }
        val rent = recurring.getValue("Rent")
        assertThat(rent.autoPay).isTrue()
        assertThat(rent.lastPosted).isEqualTo(ms("2026-10-01T07:00:00Z"))
        assertThat(rent.cadence).isEqualTo(RecurringEntity.Cadence.MONTHLY)
        assertThat(rent.createdAt).isEqualTo(ms("2026-08-01T08:00:00Z"))
        assertThat(recurring.getValue("Salary").isIncome).isTrue()
        assertThat(recurring.getValue("Salary").autoPay).isFalse() // absent key
        assertThat(recurring.getValue("Gym").cadence).isEqualTo(RecurringEntity.Cadence.WEEKLY)
        assertThat(recurring.getValue("Gym").dueDay).isEqualTo(5)

        val goals = db.savingsDao().getGoals().first().associateBy { it.name }
        val holiday = goals.getValue("Holiday")
        assertThat(holiday.targetDate).isEqualTo(ms("2027-06-01T00:00:00Z"))
        assertThat(goals.getValue("Car").targetDate).isNull()
        val contributions = db.savingsDao().getAllContributions().first()
        assertThat(contributions.map { it.goalId }.distinct()).containsExactly(holiday.id)
        assertThat(contributions.sumOf { it.amount }).isEqualToIgnoringScale("174.5")

        val limit = db.buyingLimitDao().getAll().first().single()
        assertThat(limit.keywordList).containsExactly("coke", "pepsi max").inOrder()
        assertThat(limit.timeframe).isEqualTo(BuyingLimitTimeframe.WEEKLY)
        assertThat(limit.count).isEqualTo(2)

        val envelopes = db.budgetEnvelopeDao().getAllOnce().associateBy { it.name }
        assertThat(envelopes.getValue("Eating out").categoryList())
            .containsExactly("Restaurant & Dining", "Coffee & Cafés").inOrder()
        assertThat(envelopes.getValue("Lisbon spending").isAllSpending).isTrue()

        // The warranty points at the café receipt by its whole-second createdAt; it resolves to that
        // receipt's Android id (millis).
        val cafe = db.receiptDao().getAll().first().single { it.store == "Corner Café" }
        val warranties = db.warrantyDao().getAllOnce().associateBy { it.name }
        assertThat(warranties.getValue("Headphones").receiptId).isEqualTo(cafe.timestamp)
        assertThat(warranties.getValue("Headphones").durationMonths).isEqualTo(24)
        assertThat(warranties.getValue("Kettle").receiptId).isEqualTo(0L)

        val debt = db.debtDao().getAll().first().single()
        assertThat(debt.balance).isEqualToIgnoringScale("2450.75")
        assertThat(debt.aprPercent).isEqualToIgnoringScale("19.9")
        assertThat(db.templateDao().getAll().first().single().store).isEqualTo("Corner Café")
        val score = db.wellbeingScoreDao().getAll().first().single()
        assertThat(score.periodId).isEqualTo("2026-08")
        assertThat(score.band).isEqualTo("HEALTHY")
        assertThat(db.ignoredSubscriptionDao().getAll().first().single().merchant).isEqualTo("spotify")
    }

    @Test
    fun `replace applies the iOS settings translated to Android values`() = runTest {
        manager.import(fixture("ios_backup.json"), replace = true)

        val s = settingsStore.settings.value
        assertThat(s.currency).isEqualTo(Currency.GBP)
        assertThat(s.dateFormat).isEqualTo(DateFormatOption.DMY_SLASH)
        assertThat(s.language).isEqualTo(Language.BULGARIAN)
        assertThat(s.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(s.accent).isEqualTo(AccentTheme.OCEAN)
        assertThat(s.monthStartDay).isEqualTo(25)
        assertThat(s.budgetRolloverEnabled).isTrue()
        assertThat(s.budgetCadence).isEqualTo("FORTNIGHTLY")
        assertThat(s.fortnightAnchorEpochDay).isEqualTo(20725L)
        // iOS's week comparison has no Android section, so it drops out of both lists.
        assertThat(s.hiddenHomeSections).containsExactly("receipts")
        assertThat(s.homeSectionOrder)
            .containsExactly("budgets", "total_spent", "upcoming_bills", "wellbeing", "receipts").inOrder()
        assertThat(s.hiddenInsightsSections).containsExactly("summary", "income_spending")
        assertThat(s.insightsSectionOrder).containsExactly("breakdown", "trend", "period_comparison").inOrder()
        assertThat(s.customInsightsSections).containsExactly("breakdown", "top_categories", "by_tag").inOrder()
        assertThat(s.recapEnabled).isFalse()
        assertThat(s.recapFrequency).isEqualTo(RecapFrequency.MONTHLY)
        assertThat(s.hideAmounts).isTrue()
    }

    @Test
    fun `merge adds the iOS data on top and leaves the device settings alone`() = runTest {
        manager.import(gson.toJson(androidDataset()), replace = false)
        settingsStore.setCurrency(Currency.CHF)

        manager.import(fixture("ios_backup.json"), replace = false)

        assertThat(db.receiptDao().getAll().first().map { it.store })
            .containsExactly("Android store", "Lidl", "Corner Café", "Shell")
        assertThat(db.transactionDao().getAll().first()).hasSize(6)
        assertThat(db.tagDao().allTags().first().map { it.name }).contains("android-tag")
        assertThat(settingsStore.settings.value.currency).isEqualTo(Currency.CHF)
        assertThat(settingsStore.settings.value.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `an older iOS backup without the later optional fields restores`() = runTest {
        settingsStore.setCurrency(Currency.CHF)

        manager.import(fixture("ios_backup_legacy.json"), replace = true)

        val txn = db.transactionDao().getAll().first().single()
        assertThat(txn.name).isEqualTo("Guitar strings")
        assertThat(txn.category).isEqualTo("Music")
        assertThat(db.receiptDao().getAll().first().single().tax).isEqualToIgnoringScale("2.5")
        val music = db.categoryDao().getAll().first().single { it.name == "Music" }
        assertThat(music.colorArgb).isEqualTo(0xFFE91E63.toInt())
        assertThat(music.parent).isNull()
        assertThat(db.categoryDao().getAll().first()).hasSize(Categories.predefined.size + 1)
        val bill = db.recurringDao().getAll().first().single()
        assertThat(bill.autoPay).isFalse()
        assertThat(bill.lastPosted).isEqualTo(0L)
        assertThat(db.tagDao().allTags().first()).isEmpty()
        assertThat(db.tripDao().getAllOnce()).isEmpty()
        assertThat(db.buyingLimitDao().getAll().first()).isEmpty()
        // No settings block: the device keeps its own preferences even on a replace.
        assertThat(settingsStore.settings.value.currency).isEqualTo(Currency.CHF)
    }

    @Test
    fun `an Android backup still restores exactly as before`() = runTest {
        manager.import(gson.toJson(androidDataset()), replace = true)
        val exported = manager.exportJson()
        assertThat(IosBackupConverter.isIosBackup(JsonParser.parseString(exported))).isFalse()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val fresh = Room.inMemoryDatabaseBuilder(app, BudgettyDatabase::class.java).allowMainThreadQueries().build()
        try {
            val freshManager = BackupManager({ fresh }, settingsStore)
            freshManager.import(exported, replace = true)
            assertThat(freshManager.exportJson()).isEqualTo(exported)
        } finally {
            fresh.close()
        }
    }

    @Test
    fun `a file in neither format still fails and leaves the account untouched`() = runTest {
        manager.import(gson.toJson(androidDataset()), replace = false)

        // A JSON object that is neither format used to read as an EMPTY Android backup (Gson fills the
        // defaults), so "Replace all" wiped the account and reported success. Now every case is an
        // IllegalArgumentException, which the Account screen shows as its "import failed" message.
        for (junk in listOf("this is not json {", """{"hello":"world"}""", "{}", "[]", "42")) {
            val failure = runCatching { manager.import(junk, replace = true) }.exceptionOrNull()
            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(db.receiptDao().getAll().first().map { it.store }).containsExactly("Android store")
        assertThat(db.transactionDao().getAll().first()).hasSize(1)
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("backup/$name")) { "missing fixture $name" }.readText()

    private fun ms(iso: String): Long = Instant.parse(iso).toEpochMilli()

    /** A small backup in Android's own format: one tagged line item on one receipt. */
    private fun androidDataset() = BackupData(
        transactions = listOf(
            TransactionEntity(id = 5, name = "Android item", timestamp = 1_000L, price = BigDecimal("4.20"),
                quantity = 1, category = "Groceries", receiptId = 1_000L),
        ),
        categories = listOf(CategoryEntity("Groceries", colorArgb = 0)),
        receipts = listOf(ReceiptEntity(1_000L, "Android store", 1_000L, BigDecimal.ZERO)),
        tags = listOf(TagEntity("android-tag")),
        transactionTags = listOf(TransactionTagEntity(transactionId = 5, tagName = "android-tag")),
    )
}
