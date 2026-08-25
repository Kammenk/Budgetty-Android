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

/**
 * Guards the buying-limits half of backup restore — the "restore silently dropped my buying limits"
 * gap. Runs the exact export→wipe→import sequence [com.budgetty.app.data.backup.BackupManager] uses
 * (Gson round-trip of [BackupData] + the DAO restore with ids reset to 0), against real in-memory
 * Room, and asserts every field survives — including the [BuyingLimitTimeframe] enum and the
 * newline-joined (Cyrillic-safe) keywords. No emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BuyingLimitsBackupRestoreTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: BuyingLimitDao
    private val gson = Gson()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.buyingLimitDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export then wipe then import restores every limit and field`() = runTest {
        dao.insertAll(
            listOf(
                BuyingLimitEntity(
                    id = 0,
                    emoji = "🥤",
                    label = "Fizzy drinks",
                    keywords = BuyingLimitEntity.joinKeywords(listOf("Coke", "cola")),
                    timeframe = BuyingLimitTimeframe.MONTHLY,
                    count = 3,
                    createdAt = 1,
                ),
                BuyingLimitEntity(
                    id = 0,
                    emoji = "",
                    label = "",
                    keywords = BuyingLimitEntity.joinKeywords(listOf("кока")),
                    timeframe = BuyingLimitTimeframe.WEEKLY,
                    count = 1,
                    createdAt = 2,
                ),
            ),
        )

        // Export exactly as BackupManager does, then wipe and restore from the JSON.
        val json = gson.toJson(BackupData(buyingLimits = dao.getAll().first()))
        dao.clearAll()
        assertThat(dao.getAll().first()).isEmpty()

        val restored = gson.fromJson(json, BackupData::class.java)
        dao.insertAll(restored.buyingLimits.orEmpty().map { it.copy(id = 0) })

        val limits = dao.getAll().first()
        assertThat(limits).hasSize(2)

        val fizzy = limits.single { it.label == "Fizzy drinks" }
        assertThat(fizzy.emoji).isEqualTo("🥤")
        assertThat(fizzy.timeframe).isEqualTo(BuyingLimitTimeframe.MONTHLY)
        assertThat(fizzy.count).isEqualTo(3)
        // Keywords survive as the normalized, de-duplicated list ("Coke" -> "coke").
        assertThat(fizzy.keywordList).containsExactly("coke", "cola").inOrder()

        val cyrillic = limits.single { it.label.isEmpty() }
        assertThat(cyrillic.emoji).isEmpty()
        assertThat(cyrillic.timeframe).isEqualTo(BuyingLimitTimeframe.WEEKLY)
        assertThat(cyrillic.count).isEqualTo(1)
        assertThat(cyrillic.keywordList).containsExactly("кока")
    }

    @Test
    fun `older backup without the buying-limits field imports cleanly`() = runTest {
        // A pre-v25 backup JSON simply omits the field; Gson leaves it null, and .orEmpty() absorbs it.
        val legacyJson = """{"transactions":[],"categories":[]}"""
        val data = gson.fromJson(legacyJson, BackupData::class.java)
        dao.insertAll(data.buyingLimits.orEmpty().map { it.copy(id = 0) })
        assertThat(dao.getAll().first()).isEmpty()
    }
}
