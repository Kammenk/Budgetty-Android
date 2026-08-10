package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Query("SELECT * FROM savings_goals ORDER BY createdAt ASC, id ASC")
    fun getGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoal(id: Long): Flow<SavingsGoalEntity?>

    /** Insert or update; returns the goal's row id (the freshly-minted id when inserting). */
    @Upsert
    suspend fun upsertGoal(goal: SavingsGoalEntity): Long

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    /** Every contribution across all goals — the goals list sums these per goal for each card total. */
    @Query("SELECT * FROM savings_contributions")
    fun getAllContributions(): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY date DESC, id DESC")
    fun getContributions(goalId: Long): Flow<List<SavingsContributionEntity>>

    @Insert
    suspend fun insertContribution(contribution: SavingsContributionEntity): Long

    @Query("DELETE FROM savings_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    // ── Backup restore (fresh ids; goals inserted before their contributions) ──
    /**
     * Inserts goals with fresh ids and returns the new row ids in the same order as [goals], so the
     * restore can remap each contribution's [SavingsContributionEntity.goalId] onto its new goal.
     */
    @Insert
    suspend fun insertAllGoals(goals: List<SavingsGoalEntity>): List<Long>

    @Insert
    suspend fun insertAllContributions(contributions: List<SavingsContributionEntity>)

    @Query("DELETE FROM savings_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM savings_contributions")
    suspend fun clearContributions()
}
