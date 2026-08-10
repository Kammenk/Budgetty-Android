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
import java.math.BigDecimal

/**
 * Guards the savings half of backup restore (the "backup silently dropped my savings" fix). Goals
 * are re-inserted with fresh ids and each contribution's [SavingsContributionEntity.goalId] is
 * remapped onto its new goal — which hinges on [SavingsDao.insertAllGoals] returning the new row ids
 * in input order. Exercises the exact DAO sequence BackupManager.import runs, against real
 * (in-memory) Room with the goal→contribution foreign key enforced. No emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SavingsBackupRestoreTest {

    private lateinit var db: BudgettyDatabase
    private lateinit var dao: SavingsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            BudgettyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.savingsDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `insertAllGoals returns the new row ids in input order`() = runTest {
        val ids = dao.insertAllGoals(
            listOf(
                SavingsGoalEntity(id = 0, name = "Car", emoji = "🚗", targetAmount = BigDecimal("5000")),
                SavingsGoalEntity(id = 0, name = "Trip", emoji = "✈️", targetAmount = BigDecimal("2000")),
            ),
        )
        assertThat(ids).hasSize(2)
        dao.getGoals().test {
            // getGoals() orders by createdAt ASC, id ASC; both createdAt default to 0, so this is
            // insertion order — Car (lower id) then Trip — matching the returned ids positionally.
            val goals = awaitItem()
            assertThat(goals.map { it.name }).containsExactly("Car", "Trip").inOrder()
            assertThat(goals.map { it.id }).isEqualTo(ids)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `restore remaps each contribution onto its goal despite fresh goal ids`() = runTest {
        // A backup captured with these (old) ids — deliberately high so they can't accidentally
        // coincide with the fresh auto-generated ids.
        val backupGoals = listOf(
            SavingsGoalEntity(id = 100, name = "Car", emoji = "🚗", targetAmount = BigDecimal("5000")),
            SavingsGoalEntity(id = 200, name = "Trip", emoji = "✈️", targetAmount = BigDecimal("2000")),
        )
        val backupContributions = listOf(
            SavingsContributionEntity(id = 1, goalId = 100, amount = BigDecimal("300"), note = "car-1"),
            SavingsContributionEntity(id = 2, goalId = 200, amount = BigDecimal("150"), note = "trip-1"),
            SavingsContributionEntity(id = 3, goalId = 100, amount = BigDecimal("50"), note = "car-2"),
            SavingsContributionEntity(id = 4, goalId = 999, amount = BigDecimal("1"), note = "orphan"),
        )

        // The exact restore sequence BackupManager.import runs for savings.
        val newGoalIds = dao.insertAllGoals(backupGoals.map { it.copy(id = 0) })
        val goalIdMap = backupGoals.zip(newGoalIds).associate { (old, newId) -> old.id to newId }
        dao.insertAllContributions(
            backupContributions.mapNotNull { c -> goalIdMap[c.goalId]?.let { c.copy(id = 0, goalId = it) } },
        )

        val carId = goalIdMap.getValue(100)
        val tripId = goalIdMap.getValue(200)
        dao.getContributions(carId).test {
            assertThat(awaitItem().map { it.note }).containsExactly("car-1", "car-2")
            cancelAndConsumeRemainingEvents()
        }
        dao.getContributions(tripId).test {
            assertThat(awaitItem().map { it.note }).containsExactly("trip-1")
            cancelAndConsumeRemainingEvents()
        }
        // The orphan (goal 999 is absent from the backup) was dropped before insert, so the FK holds
        // and exactly three contributions were restored.
        dao.getAllContributions().test {
            assertThat(awaitItem()).hasSize(3)
            cancelAndConsumeRemainingEvents()
        }
    }
}
