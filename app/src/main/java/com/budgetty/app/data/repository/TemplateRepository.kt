package com.budgetty.app.data.repository

import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** Access to the user's transaction templates (saved regulars for one-tap manual logging). Per-user. */
class TemplateRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.templateDao()

    /** Every template, oldest-added first — live. */
    val templates: Flow<List<TemplateEntity>> = db.flow { it.templateDao().getAll() }

    suspend fun getById(id: Long): TemplateEntity? = dao.getById(id)

    suspend fun upsert(template: TemplateEntity): Long = dao.upsert(template)

    suspend fun delete(id: Long) = dao.delete(id)
}
