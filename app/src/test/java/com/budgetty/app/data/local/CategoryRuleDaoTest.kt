package com.budgetty.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [CategoryRuleDao] against a real (in-memory) Room database on the JVM via Robolectric —
 * no emulator. This is also the reference for the test stack added alongside it: Robolectric runs the
 * Android/SQLite layer, `runTest` drives the coroutines, and Turbine asserts on the reactive [getAll]
 * Flow. Real SQL, so it catches query and conflict-strategy mistakes a mock never would.
 */
// Use a bare Application, NOT the manifest's BudgettyApplication — the real one starts Koin and
// Firebase in onCreate(), which has no place in a DAO unit test and throws without a Firebase config.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CategoryRuleDaoTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: CategoryRuleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.categoryRuleDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `upsert then read back the mapped category`() = runTest {
        dao.upsert(CategoryRuleEntity(CategoryRuleEntity.key("Milk"), "Groceries"))

        assertThat(dao.getAllOnce()).containsExactly(
            CategoryRuleEntity("milk", "Groceries"),
        )
    }

    @Test
    fun `upsert on the same key REPLACEs rather than duplicating`() = runTest {
        val key = CategoryRuleEntity.key("Milk")
        dao.upsert(CategoryRuleEntity(key, "Groceries"))
        dao.upsert(CategoryRuleEntity(key, "Household"))

        // OnConflictStrategy.REPLACE means the second write wins and there is still exactly one row —
        // the behaviour the "remember this category" flow depends on.
        assertThat(dao.getAllOnce()).containsExactly(CategoryRuleEntity(key, "Household"))
    }

    @Test
    fun `getAll emits the current rule set and re-emits on write`() = runTest {
        dao.getAll().test {
            assertThat(awaitItem()).isEmpty()

            dao.upsert(CategoryRuleEntity("bread", "Groceries"))
            assertThat(awaitItem()).containsExactly(CategoryRuleEntity("bread", "Groceries"))

            dao.delete("bread")
            assertThat(awaitItem()).isEmpty()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `insertOrIgnore keeps the existing rule on key collision`() = runTest {
        dao.upsert(CategoryRuleEntity("bread", "Groceries"))
        // IGNORE strategy: a bulk insert must not clobber a category the user set by hand.
        dao.insertOrIgnore(listOf(CategoryRuleEntity("bread", "Something Else")))

        assertThat(dao.getAllOnce()).containsExactly(CategoryRuleEntity("bread", "Groceries"))
    }

    @Test
    fun `reassignCategory re-points only the matching rules`() = runTest {
        dao.upsert(CategoryRuleEntity("bread", "Old"))
        dao.upsert(CategoryRuleEntity("milk", "Old"))
        dao.upsert(CategoryRuleEntity("soap", "Household"))

        dao.reassignCategory(from = "Old", to = "Groceries")

        assertThat(dao.getAllOnce()).containsExactly(
            CategoryRuleEntity("bread", "Groceries"),
            CategoryRuleEntity("milk", "Groceries"),
            CategoryRuleEntity("soap", "Household"),
        )
    }
}
