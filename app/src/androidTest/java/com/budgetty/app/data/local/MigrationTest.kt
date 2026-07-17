package com.budgetty.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real [ALL_MIGRATIONS] chain against real SQLite.
 *
 * Room validates the post-migration schema against the compiled entities when it opens a database,
 * so simply reaching an open [BudgettyDatabase] from an older file asserts that the migrations
 * produce exactly the schema the entities declare — a missed column throws rather than surfacing
 * later as a crash on someone's phone. The assertions on top of that cover what schema validation
 * cannot see: whether the *data* a migration rewrites survives with the right values.
 *
 * These start from hand-written historical schemas rather than Room's exported JSONs because the
 * schema predates this repository — the initial commit already declared version 17, so there is no
 * commit to recover a 1.json..16.json from. That reconstruction is self-checking: an inaccurate
 * starting schema propagates to the end and fails Room's validation, so a passing test means v1 plus
 * the 16 migrations really does land on the v17 entity schema. From 17 on the exported JSONs exist,
 * so a future migration can use `MigrationTestHelper` and drop the hand-written SQL.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DB)
    }

    /**
     * The whole chain on a database that predates every migration. Also pins [MIGRATION_6_7]'s
     * backfill, which is the one migration that invents data for existing rows: pre-v7 receipts were
     * grouped by upload timestamp, so it seeds receiptId from timestamp to keep that grouping intact.
     */
    @Test
    fun migratesFromV1PreservingTransactions() {
        openRawAtV1().use { db ->
            db.execSQL(
                "INSERT INTO transactions (name, timestamp, price, quantity) VALUES (?, ?, ?, ?)",
                arrayOf<Any>("Milk", 1_700_000_000_000L, "2.49", 2),
            )
        }

        openWithRoom().useSqlite { db ->
            db.query("SELECT name, timestamp, price, quantity, category, receiptId FROM transactions")
                .use { c ->
                    assertTrue("the v1 row must survive to v17", c.moveToFirst())
                    assertEquals("Milk", c.getString(0))
                    assertEquals(1_700_000_000_000L, c.getLong(1))
                    assertEquals("2.49", c.getString(2))
                    assertEquals(2, c.getInt(3))
                    assertEquals("MIGRATION_1_2 backfills the category default", "Groceries", c.getString(4))
                    assertEquals("MIGRATION_6_7 groups pre-v7 rows by their upload timestamp", 1_700_000_000_000L, c.getLong(5))
                    assertEquals(1, c.count)
                }

            // The tables each later migration introduces should all be present and queryable.
            for (table in listOf("categories", "budgets", "receipts", "category_rules", "recurring")) {
                db.query("SELECT COUNT(*) FROM $table").use { c ->
                    assertTrue("$table must exist after migrating from v1", c.moveToFirst())
                }
            }
        }
    }

    /**
     * [MIGRATION_10_11] is the only migration that drops a column, which SQLite pre-3.35 forces it to
     * do by recreating the table and copying rows across — the one place a mistake silently loses
     * every receipt. It also carries a semantic conversion: the app stopped storing receipt images, so
     * "had no image" is what now marks a receipt as manually entered.
     */
    @Test
    fun migration10To11RecreatesReceiptsAndDerivesIsManual() {
        openRawAtV1().use { db ->
            // Walk the real migrations up to v8, where receipts still has the imagePath column.
            ALL_MIGRATIONS.filter { it.endVersion <= 8 }.forEach { it.migrate(db) }
            db.execSQL(
                "INSERT INTO receipts (timestamp, store, date, discount, imagePath) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(1L, "Penny", 1_600_000_000_000L, "1.10", "/data/img.jpg"),
            )
            db.execSQL(
                "INSERT INTO receipts (timestamp, store, date, discount, imagePath) VALUES (?, ?, ?, ?, NULL)",
                arrayOf<Any>(2L, "Edeka", 1_600_000_001_000L, "0"),
            )
            db.version = 8
        }

        openWithRoom().useSqlite { db ->
            db.query("SELECT timestamp, store, date, discount, isManual, tax, taxOnTop, extraCharges FROM receipts ORDER BY timestamp")
                .use { c ->
                    assertEquals("both receipts must survive the table recreation", 2, c.count)

                    assertTrue(c.moveToFirst())
                    assertEquals(1L, c.getLong(0))
                    assertEquals("Penny", c.getString(1))
                    assertEquals(1_600_000_000_000L, c.getLong(2))
                    assertEquals("1.10", c.getString(3))
                    assertEquals("a scanned receipt (it had an image) is not manual", 0, c.getInt(4))

                    assertTrue(c.moveToNext())
                    assertEquals(2L, c.getLong(0))
                    assertEquals("Edeka", c.getString(1))
                    assertEquals("a receipt with no image was entered by hand", 1, c.getInt(4))

                    // v15/v16/v17 defaults: an old receipt reports no tax and no extra charges, and
                    // its prices are tax-inclusive.
                    assertEquals("0", c.getString(5))
                    assertEquals(0, c.getInt(6))
                    assertEquals("0", c.getString(7))
                }
        }
    }

    /**
     * A fresh install skips the migrations entirely — Room creates v17 from the entities and
     * [categorySeedCallback] seeds it. Worth pinning because [seedCategories] only writes name and
     * colorArgb: the other columns rely on the entity defaults matching what the migrations set, and
     * a mismatch would only show up on a clean install, not on any upgrade path.
     */
    @Test
    fun freshInstallSeedsCategories() {
        openWithRoom().useSqlite { db ->
            db.query("SELECT COUNT(*) FROM categories").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(
                    "onCreate must seed every predefined category",
                    com.budgetty.app.category.Categories.predefined.size,
                    c.getInt(0),
                )
            }
            db.query("SELECT icon, isCustom, createdAt FROM categories WHERE name = 'Groceries'").use { c ->
                assertTrue(c.moveToFirst())
                assertTrue("the seed leaves icon to the callback's refresh", c.getString(0).isNotEmpty())
                assertEquals("a seeded category is not user-created", 0, c.getInt(1))
                assertEquals(0L, c.getLong(2))
            }
        }
    }

    /**
     * Opens [TEST_DB] with the schema as it stood at v1: transactions only, before
     * [MIGRATION_1_2] added category and [MIGRATION_6_7] added receiptId.
     */
    private fun openRawAtV1(): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `price` TEXT NOT NULL, `quantity` INTEGER NOT NULL)",
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    /**
     * Opens the database the way [UserDatabaseManager] does — same migration list, same callback — so
     * the test exercises the production upgrade path rather than a lookalike of it.
     */
    private fun openWithRoom(): BudgettyDatabase =
        Room.databaseBuilder(context, BudgettyDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(categorySeedCallback)
            .build()

    /** Room opens (and therefore migrates and validates) lazily; touching the helper forces it. */
    private inline fun BudgettyDatabase.useSqlite(block: (SupportSQLiteDatabase) -> Unit) {
        try {
            block(openHelper.writableDatabase)
        } finally {
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
