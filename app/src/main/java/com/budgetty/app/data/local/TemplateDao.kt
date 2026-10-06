package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    /** Every template, oldest-added first — the Add-sheet strip and the Manage list read this. */
    @Query("SELECT * FROM templates ORDER BY createdAt ASC, id ASC")
    fun getAll(): Flow<List<TemplateEntity>>

    /** One template by id, for pre-filling manual entry; null if it was deleted meanwhile. */
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Upsert
    suspend fun upsert(template: TemplateEntity): Long

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)

    /** Bulk insert with fresh ids — backup restore. */
    @Insert
    suspend fun insertAll(templates: List<TemplateEntity>)

    @Query("DELETE FROM templates")
    suspend fun clearAll()
}
