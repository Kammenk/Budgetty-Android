package com.budgetty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetty.app.category.Categories

@Database(
    entities = [
        TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, ReceiptEntity::class,
        CategoryRuleEntity::class, RecurringEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgettyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun recurringDao(): RecurringDao
}

/** v2 adds the [TransactionEntity.category] column, defaulting existing rows to "Groceries". */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'Groceries'",
        )
    }
}

/** v3 adds the categories table and seeds the predefined categories with their colors. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS categories " +
                "(name TEXT NOT NULL, colorArgb INTEGER NOT NULL, PRIMARY KEY(name))",
        )
        seedCategories(db)
    }
}

/** v4: expands the category taxonomy — seeds the new categories and remaps two old names. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        seedCategories(db)
        db.execSQL("UPDATE transactions SET category = 'Groceries' WHERE category = 'Eggs'")
        db.execSQL("UPDATE transactions SET category = 'Beverages' WHERE category = 'Drinks'")
        db.execSQL("DELETE FROM categories WHERE name IN ('Eggs', 'Drinks')")
    }
}

/** v5 adds the budgets table (monthly / weekly / per-category limits). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS budgets " +
                "(budgetKey TEXT NOT NULL, amount TEXT NOT NULL, PRIMARY KEY(budgetKey))",
        )
    }
}

/** v6 adds the receipts table (store / date / discount), keyed by upload timestamp. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS receipts " +
                "(timestamp INTEGER NOT NULL, store TEXT NOT NULL, date INTEGER NOT NULL, " +
                "discount TEXT NOT NULL, PRIMARY KEY(timestamp))",
        )
    }
}

/** v7: adds transactions.receiptId (groups one upload's items) so timestamp can hold the receipt date. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN receiptId INTEGER NOT NULL DEFAULT 0")
        // Existing rows: group by their original upload timestamp.
        db.execSQL("UPDATE transactions SET receiptId = timestamp")
    }
}

/** v8: adds receipts.imagePath (nullable) so a captured receipt photo can be re-opened later. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN imagePath TEXT")
    }
}

/**
 * Re-applies the canonical predefined category colors to existing rows. Seeding uses
 * INSERT OR IGNORE (never overwrites), so a color-palette change only reaches already-installed
 * apps through a migration that calls this. Updates by name, leaving custom user categories alone.
 */
private fun reapplyPredefinedColors(db: SupportSQLiteDatabase) {
    Categories.predefined.forEach { category ->
        db.execSQL(
            "UPDATE categories SET colorArgb = ? WHERE name = ?",
            arrayOf<Any>(category.colorArgb, category.name),
        )
    }
}

/**
 * Re-applies the predefined categories' emoji icons by name (custom rows are left untouched). Only
 * valid once the categories.icon column exists (v13+), so this is called from [MIGRATION_12_13] and
 * the onOpen callback — never from the older color-refresh migrations.
 */
private fun reapplyPredefinedIcons(db: SupportSQLiteDatabase) {
    Categories.predefined.forEach { category ->
        db.execSQL(
            "UPDATE categories SET icon = ? WHERE name = ?",
            arrayOf<Any>(category.emoji, category.name),
        )
    }
}

/** v9: first move to the muted palette (deeper, fully opaque tones instead of washed-out pastels). */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) = reapplyPredefinedColors(db)
}

/** v10: re-applies colors again after switching to mockup-matched hues with per-family sub-shades. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) = reapplyPredefinedColors(db)
}

/**
 * v11: the app no longer stores receipt images. Drops receipts.imagePath and adds isManual,
 * recreating the table (SQLite can't drop a column pre-3.35). Existing image-less rows are treated
 * as manual, preserving the old "no image = manual" signal that drove the edit "Add receipt" action.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE receipts_new (timestamp INTEGER NOT NULL, store TEXT NOT NULL, " +
                "date INTEGER NOT NULL, discount TEXT NOT NULL, isManual INTEGER NOT NULL, " +
                "PRIMARY KEY(timestamp))",
        )
        db.execSQL(
            "INSERT INTO receipts_new (timestamp, store, date, discount, isManual) " +
                "SELECT timestamp, store, date, discount, " +
                "CASE WHEN imagePath IS NULL THEN 1 ELSE 0 END FROM receipts",
        )
        db.execSQL("DROP TABLE receipts")
        db.execSQL("ALTER TABLE receipts_new RENAME TO receipts")
    }
}

/** v12 adds the category_rules table (learned item-name → category preferences). */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS category_rules " +
                "(name TEXT NOT NULL, category TEXT NOT NULL, PRIMARY KEY(name))",
        )
    }
}

/**
 * v13: user-created categories. Adds icon (emoji), isCustom, and createdAt to the categories table,
 * and backfills the predefined categories' emoji icons (custom rows keep their stored icon).
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE categories ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE categories ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        reapplyPredefinedIcons(db)
    }
}

/**
 * v14 adds the recurring table (income sources + recurring payments). Planning-only for now; the
 * nextDue/lastPosted/active columns are stored ahead of the later auto-posting phase. Column order,
 * types, and defaults mirror [RecurringEntity] so Room's schema validation passes.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS recurring (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "label TEXT NOT NULL, amount TEXT NOT NULL, isIncome INTEGER NOT NULL, " +
                "category TEXT NOT NULL DEFAULT '', cadence TEXT NOT NULL DEFAULT 'MONTHLY', " +
                "dueDay INTEGER NOT NULL DEFAULT 1, createdAt INTEGER NOT NULL DEFAULT 0, " +
                "nextDue INTEGER NOT NULL DEFAULT 0, lastPosted INTEGER NOT NULL DEFAULT 0, " +
                "active INTEGER NOT NULL DEFAULT 1)",
        )
    }
}

/**
 * v15 adds receipts.tax — the tax/VAT contained in the receipt total, shown as an "incl. VAT" line.
 * Stored as TEXT like [ReceiptEntity.discount] (BigDecimal via [Converters]); existing rows default
 * to '0' (no tax shown), matching pre-v15 receipts that had no tax figure.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN tax TEXT NOT NULL DEFAULT '0'")
    }
}

/**
 * v16 adds receipts.taxOnTop — whether the receipt's [ReceiptEntity.tax] is added on top of the net
 * line prices (tax-exclusive: US sales tax, EU net invoice) rather than contained within them.
 * Existing rows default to 0 (contained), which is correct for every pre-v16 receipt: tax-inclusive
 * receipts always had their tax inside the prices, and the handful of tax-exclusive ones were grossed
 * up at ingest, so their item sum already equals what was paid and must not have tax added again.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN taxOnTop INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v17 adds receipts.extraCharges — money paid beyond the line items, order discount and on-top tax
 * (delivery & service fees + a courier tip on a delivery-app order, an uncaptured deposit, …). Stored
 * as TEXT like [ReceiptEntity.discount] (BigDecimal via [Converters]); existing rows default to '0'
 * (ordinary receipts whose items already reconcile to their total have no such gap).
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN extraCharges TEXT NOT NULL DEFAULT '0'")
    }
}

/** Inserts the predefined categories. Idempotent — never overwrites an existing row. */
fun seedCategories(db: SupportSQLiteDatabase) {
    Categories.predefined.forEach { category ->
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, colorArgb) VALUES (?, ?)",
            arrayOf<Any>(category.name, category.colorArgb),
        )
    }
}

/**
 * Seeds predefined categories on first creation, and on every open re-seeds (insert-or-ignore) then
 * refreshes their colors and icons from [Categories] — so adding a category or tweaking a color/emoji
 * in code shows up on the next launch without a migration or version bump. The re-seed inserts any
 * newly-added predefined categories on existing installs; colors and icons aren't user-customizable
 * so there's nothing to preserve, and custom category names (not in the predefined set) are left
 * untouched by all three steps.
 */
val categorySeedCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        seedCategories(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        seedCategories(db)
        reapplyPredefinedColors(db)
        reapplyPredefinedIcons(db)
    }
}

/**
 * Every migration, in order. Both [UserDatabaseManager] and the migration tests build from this one
 * list — a test that assembled its own copy could pass while the app shipped without a migration.
 * Append here when adding one; a gap makes Room fall back to destructive recreation on upgrade.
 *
 * Declared last on purpose: top-level properties initialize in file order, so referencing the
 * migrations from any earlier point would capture them as null.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
    MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
)
