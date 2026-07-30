package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A savings goal on the Budget screen: an emoji + name + target amount, optionally with a target
 * date. The amount "saved" toward it is the sum of its [SavingsContributionEntity] rows — Budgetty
 * holds no balances, so a goal is a manual tracker, never a transfer of real money.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val targetAmount: BigDecimal,
    /** Optional target date (epoch millis); null = no date, so the card/detail show no pace line. */
    val targetDate: Long? = null,
    /** Creation time (epoch millis); orders the goals list by when each was added. */
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
)
