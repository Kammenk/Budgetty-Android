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
 * Exercises [WellbeingScoreDao] against a real (in-memory) Room database on the JVM via Robolectric —
 * no emulator, mirroring [CategoryRuleDaoTest]. Pins the two behaviours §3.1's stored history depends
 * on: upsert is idempotent per periodId (a re-scored month never duplicates), and the reads the future
 * trend view uses return months in chronological order.
 */
// Bare Application, NOT BudgettyApplication — the real one starts Koin + Firebase in onCreate().
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WellbeingScoreDaoTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: WellbeingScoreDao

    private fun score(periodId: String, value: Int, band: String = "HEALTHY") =
        WellbeingScoreEntity(
            periodId = periodId,
            score = value,
            band = band,
            componentsJson = """{"SAVINGS":$value}""",
            computedAt = 1_700_000_000_000L,
        )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.wellbeingScoreDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `upsert on the same periodId REPLACEs rather than duplicating`() = runTest {
        dao.upsert(score("2026-02", 60, band = "GETTING_THERE"))
        dao.upsert(score("2026-02", 74, band = "HEALTHY"))

        // PK = periodId, so re-scoring the just-closed month keeps exactly one row with the latest
        // values — the non-revisionist, no-duplicate guarantee the trend line relies on.
        val all = dao.getAllOnceViaFlow()
        assertThat(all).hasSize(1)
        assertThat(all.single().score).isEqualTo(74)
        assertThat(all.single().band).isEqualTo("HEALTHY")
    }

    @Test
    fun `getAll emits oldest to newest and re-emits on write`() = runTest {
        // One write, one await — Room's invalidation tracker conflates rapid back-to-back writes into
        // fewer emissions, so writing all three first and awaiting three items would be flaky.
        dao.getAll().test {
            assertThat(awaitItem()).isEmpty()

            dao.upsert(score("2026-03", 70))
            assertThat(awaitItem().map { it.periodId }).containsExactly("2026-03").inOrder()

            dao.upsert(score("2026-01", 50))
            // periodId is "yyyy-MM", so lexical order == chronological order.
            assertThat(awaitItem().map { it.periodId }).containsExactly("2026-01", "2026-03").inOrder()

            dao.upsert(score("2026-02", 60))
            assertThat(awaitItem().map { it.periodId })
                .containsExactly("2026-01", "2026-02", "2026-03").inOrder()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getRecent returns the newest N, oldest first`() = runTest {
        listOf("2025-11", "2025-12", "2026-01", "2026-02", "2026-03").forEach { dao.upsert(score(it, 60)) }

        // Newest three picked, then flipped back to ascending for a left-to-right sparkline.
        assertThat(dao.getRecent(3).map { it.periodId })
            .containsExactly("2026-01", "2026-02", "2026-03").inOrder()
    }

    @Test
    fun `insertAll IGNOREs a periodId clash so a restore keeps the on-device snapshot`() = runTest {
        dao.upsert(score("2026-02", 74))
        // A backup carrying an older copy of the same month must not overwrite the finalized on-device row.
        dao.insertAll(listOf(score("2026-02", 10), score("2026-01", 55)))

        val all = dao.getAllOnceViaFlow().associate { it.periodId to it.score }
        assertThat(all).containsEntry("2026-02", 74)
        assertThat(all).containsEntry("2026-01", 55)
    }

    @Test
    fun `clearAll empties the table`() = runTest {
        dao.upsert(score("2026-02", 60))
        dao.clearAll()
        assertThat(dao.getAllOnceViaFlow()).isEmpty()
    }

    private suspend fun WellbeingScoreDao.getAllOnceViaFlow(): List<WellbeingScoreEntity> {
        var out: List<WellbeingScoreEntity> = emptyList()
        getAll().test { out = awaitItem(); cancelAndConsumeRemainingEvents() }
        return out
    }
}
