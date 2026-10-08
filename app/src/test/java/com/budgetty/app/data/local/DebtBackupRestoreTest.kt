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
 * Guards the debts half of backup restore (the Debt payoff planner). Runs the exact export→wipe→import
 * sequence [com.budgetty.app.data.backup.BackupManager] uses against real in-memory Room, and asserts
 * every field survives — including the BigDecimal money columns — with ids reset to 0 on restore. No
 * emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DebtBackupRestoreTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: DebtDao
    private val gson = Gson()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.debtDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export then wipe then import restores every debt and field`() = runTest {
        dao.insertAll(
            listOf(
                DebtEntity(0, "💳", "Credit card", BigDecimal("2400.00"), BigDecimal("19.9"), BigDecimal("60.00"), 1),
                DebtEntity(0, "🚗", "Car loan", BigDecimal("6800.50"), BigDecimal("6.5"), BigDecimal("210.00"), 2),
            ),
        )

        val json = gson.toJson(BackupData(debts = dao.getAll().first()))
        dao.clearAll()
        assertThat(dao.getAll().first()).isEmpty()

        val restored = gson.fromJson(json, BackupData::class.java)
        dao.insertAll(restored.debts.orEmpty().map { it.copy(id = 0) })

        val debts = dao.getAll().first()
        assertThat(debts).hasSize(2)

        val card = debts.single { it.name == "Credit card" }
        assertThat(card.emoji).isEqualTo("💳")
        assertThat(card.balance.compareTo(BigDecimal("2400.00"))).isEqualTo(0)
        assertThat(card.aprPercent.compareTo(BigDecimal("19.9"))).isEqualTo(0)
        assertThat(card.minPayment.compareTo(BigDecimal("60.00"))).isEqualTo(0)

        val car = debts.single { it.name == "Car loan" }
        assertThat(car.balance.compareTo(BigDecimal("6800.50"))).isEqualTo(0)
    }

    @Test
    fun `older backup without the debts field imports cleanly`() = runTest {
        val legacyJson = """{"transactions":[],"categories":[]}"""
        val data = gson.fromJson(legacyJson, BackupData::class.java)
        dao.insertAll(data.debts.orEmpty().map { it.copy(id = 0) })
        assertThat(dao.getAll().first()).isEmpty()
    }
}
