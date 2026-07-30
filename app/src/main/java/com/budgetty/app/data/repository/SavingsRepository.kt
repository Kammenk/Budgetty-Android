package com.budgetty.app.data.repository

import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Reads/writes savings goals and their contributions (a goal's saved total = Σ its contributions). */
class SavingsRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.savingsDao()

    val goals: Flow<List<SavingsGoalEntity>> = db.flow { it.savingsDao().getGoals() }
    val allContributions: Flow<List<SavingsContributionEntity>> =
        db.flow { it.savingsDao().getAllContributions() }

    fun goal(id: Long): Flow<SavingsGoalEntity?> = db.flow { it.savingsDao().getGoal(id) }

    fun contributions(goalId: Long): Flow<List<SavingsContributionEntity>> =
        db.flow { it.savingsDao().getContributions(goalId) }

    suspend fun upsertGoal(goal: SavingsGoalEntity): Long = dao.upsertGoal(goal)

    suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)

    suspend fun addContribution(contribution: SavingsContributionEntity): Long =
        dao.insertContribution(contribution)

    suspend fun deleteContribution(id: Long) = dao.deleteContribution(id)

    companion object {
        /** Savings goals allowed on the free tier; Premium is unlimited. */
        const val FREE_GOAL_LIMIT = 1
    }
}
