package com.budgetty.app.data.repository

import androidx.room.withTransaction
import com.budgetty.app.data.local.TagCount
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.local.TransactionTagEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/**
 * Single point of access to free-form tags: the catalog, the transaction↔tag links, and the
 * Manage-screen rename / merge / delete operations. Names are normalized through [TagEntity.normalize]
 * before they reach here, so every lookup and link agrees on the same key.
 */
class TagRepository(
    private val db: UserDatabaseManager,
) {
    private val dao get() = db.database.tagDao()

    /** Every catalog tag with its transaction count, most-used first — Manage list + autocomplete. */
    val tagCounts: Flow<List<TagCount>> = db.flow { it.tagDao().tagCounts() }

    /** Tag names by most recent use — the tag input's "Recent" row. */
    val recentTags: Flow<List<String>> = db.flow { it.tagDao().recentTags() }

    /** Every transaction↔tag link, live — History/Insights join it against their period transactions. */
    val allLinks: Flow<List<TransactionTagEntity>> = db.flow { it.tagDao().allLinks() }

    /** The tag names on one transaction (re-populates the review rows when editing a receipt). */
    suspend fun tagsForTransaction(transactionId: Long): List<String> =
        dao.tagsForTransaction(transactionId)

    /** Makes sure each [names] exists in the catalog (createdAt set the first time it's seen). */
    suspend fun ensureTags(names: Collection<String>) {
        if (names.isEmpty()) return
        val now = System.currentTimeMillis()
        dao.insertTags(names.map { TagEntity(it, now) })
    }

    /** Links transactions to tags (ignoring links that already exist). */
    suspend fun link(links: List<TransactionTagEntity>) {
        if (links.isNotEmpty()) dao.link(links)
    }

    /**
     * Saves the exact tag set for one freshly-inserted transaction: ensures the names are in the
     * catalog, then links them. Used by the upload save path once a row has its database id.
     */
    suspend fun setTagsFor(transactionId: Long, names: Collection<String>) {
        if (names.isEmpty()) return
        ensureTags(names)
        link(names.map { TransactionTagEntity(transactionId, it) })
    }

    /**
     * Renames [from] to [to], or merges into it when [to] already exists — one operation either way:
     * make sure [to] is in the catalog, move [from]'s links onto it (keeping transactions that already
     * had [to]), then drop [from]. Transactions are never deleted. Atomic.
     */
    suspend fun renameOrMerge(from: String, to: String) {
        if (from == to || to.isBlank()) return
        db.database.withTransaction {
            val tagDao = db.database.tagDao()
            tagDao.insertTags(listOf(TagEntity(to, System.currentTimeMillis())))
            tagDao.repointLinks(from, to)
            tagDao.deleteTag(from)
        }
    }

    /** Removes a tag from the catalog; its links cascade away, transactions untouched. */
    suspend fun deleteTag(name: String) = dao.deleteTag(name)
}
