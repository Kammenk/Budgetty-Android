package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * One dated entry toward a [SavingsGoalEntity]: a deposit ([amount] > 0) or a withdrawal
 * ([amount] < 0). A goal's saved total is the sum of its contributions. Deleting a goal cascades to
 * its contributions.
 */
@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    /** Signed: positive = added to savings, negative = withdrawn. */
    val amount: BigDecimal,
    @ColumnInfo(defaultValue = "")
    val note: String = "",
    /** When the contribution happened (epoch millis); defaults to when it was logged. */
    @ColumnInfo(defaultValue = "0")
    val date: Long = 0L,
)
