package com.budgetty.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.budgetty.app.data.backup.BackupData
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
 * Guards the templates half of backup restore (transaction templates). Runs the export→wipe→import
 * sequence [com.budgetty.app.data.backup.BackupManager] uses against real in-memory Room, asserting
 * every field survives — the BigDecimal amount, the askAmount flag and the optional store — with ids
 * reset to 0 on restore. No emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TemplateBackupRestoreTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: TemplateDao
    private val gson = Gson()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.templateDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export then wipe then import restores every template and field`() = runTest {
        dao.insertAll(
            listOf(
                TemplateEntity(0, "☕", "Coffee", BigDecimal("3.20"), "Food", "Café Delta", false, 1),
                TemplateEntity(0, "🛒", "Groceries", BigDecimal.ZERO, "Groceries", "", true, 2),
            ),
        )

        val json = gson.toJson(BackupData(templates = dao.getAll().first()))
        dao.clearAll()
        assertThat(dao.getAll().first()).isEmpty()

        val restored = gson.fromJson(json, BackupData::class.java)
        dao.insertAll(restored.templates.orEmpty().map { it.copy(id = 0) })

        val templates = dao.getAll().first()
        assertThat(templates).hasSize(2)

        val coffee = templates.single { it.name == "Coffee" }
        assertThat(coffee.emoji).isEqualTo("☕")
        assertThat(coffee.amount.compareTo(BigDecimal("3.20"))).isEqualTo(0)
        assertThat(coffee.category).isEqualTo("Food")
        assertThat(coffee.store).isEqualTo("Café Delta")
        assertThat(coffee.askAmount).isFalse()

        val groceries = templates.single { it.name == "Groceries" }
        assertThat(groceries.askAmount).isTrue()
        assertThat(groceries.store).isEmpty()
    }

    @Test
    fun `older backup without the templates field imports cleanly`() = runTest {
        val legacyJson = """{"transactions":[],"categories":[]}"""
        val data = gson.fromJson(legacyJson, BackupData::class.java)
        dao.insertAll(data.templates.orEmpty().map { it.copy(id = 0) })
        assertThat(dao.getAll().first()).isEmpty()
    }
}
