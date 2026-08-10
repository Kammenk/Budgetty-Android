package com.budgetty.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Root-cause + fix guard for the case-only category rename bug. The `categories` primary key (`name`)
 * has no NOCASE collation, so "Coffee" and "COFFEE" are DISTINCT rows — which is why a case-only
 * rename that upserts the new name without deleting the old one left a duplicate (and let a free user
 * slip past the custom cap). The fix (ManageCategoriesViewModel / UploadViewModel compare names
 * exactly, so a case-only change still runs delete-old): this verifies delete-then-insert leaves
 * exactly one row. In-memory Room, no emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CategoryCaseRenameDaoTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.categoryDao()
    }

    @After
    fun tearDown() = db.close()

    private fun category(name: String) =
        CategoryEntity(name = name, colorArgb = 1, icon = "☕", isCustom = true)

    @Test
    fun `the categories primary key is case-sensitive`() = runTest {
        // Root cause: without NOCASE a case variant is a separate row, so upserting the new name does
        // NOT replace the old one.
        dao.upsert(category("Coffee"))
        dao.upsert(category("COFFEE"))
        dao.getAll().test {
            assertThat(awaitItem().map { it.name }).containsExactly("Coffee", "COFFEE")
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `a case-only rename that deletes the old name leaves exactly one row`() = runTest {
        // What the fixed ViewModels now do on any name change (incl. case-only): upsert new + delete old.
        dao.upsert(category("Coffee"))
        dao.upsert(category("COFFEE"))
        dao.deleteByName("Coffee")
        dao.getAll().test {
            assertThat(awaitItem().map { it.name }).containsExactly("COFFEE")
            cancelAndConsumeRemainingEvents()
        }
    }
}
