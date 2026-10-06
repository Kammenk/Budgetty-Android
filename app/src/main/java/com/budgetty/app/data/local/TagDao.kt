package com.budgetty.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One tag with how many transactions carry it — drives autocomplete counts and the Manage list. */
data class TagCount(
    val name: String,
    val count: Int,
)

@Dao
@Suppress("TooManyFunctions") // A DAO: catalog + join reads, links, and the manage mutations.
interface TagDao {

    // ── Catalog ──────────────────────────────────────────────────────────────────────────────────

    /** Adds tags to the catalog, keeping any that already exist (counts/createdAt untouched). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<TagEntity>)

    /** Every catalog tag row, live — used by backup export (preserves createdAt). */
    @Query("SELECT * FROM tags")
    fun allTags(): Flow<List<TagEntity>>

    /**
     * Every catalog tag with its transaction count (0 for a tag nothing carries yet, e.g. a brand-new
     * trip tag), most-used first. Backs both the Manage list and the tag-input autocomplete.
     */
    @Query(
        "SELECT t.name AS name, COUNT(jt.transactionId) AS count FROM tags t " +
            "LEFT JOIN transaction_tags jt ON jt.tagName = t.name " +
            "GROUP BY t.name ORDER BY count DESC, t.name ASC",
    )
    fun tagCounts(): Flow<List<TagCount>>

    /** Tag names ordered by most recent use (by the newest transaction carrying each) — the tag
     *  input's "Recent" row. Tags nothing carries yet are omitted (they have no use to be recent). */
    @Query(
        "SELECT jt.tagName FROM transaction_tags jt " +
            "JOIN transactions t ON t.id = jt.transactionId " +
            "GROUP BY jt.tagName ORDER BY MAX(t.timestamp) DESC",
    )
    fun recentTags(): Flow<List<String>>

    // ── Links ────────────────────────────────────────────────────────────────────────────────────

    /** Links transactions to tags, ignoring any link that already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(links: List<TransactionTagEntity>)

    /** The tag names on one transaction — used to re-populate the review rows when editing a receipt. */
    @Query("SELECT tagName FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun tagsForTransaction(transactionId: Long): List<String>

    /** Every (transactionId, tagName) link, live — History and Insights join this against the
     *  transactions they already hold for the period to render per-row pills and the by-tag totals. */
    @Query("SELECT * FROM transaction_tags")
    fun allLinks(): Flow<List<TransactionTagEntity>>

    // ── Manage: rename / merge / delete ─────────────────────────────────────────────────────────

    /** Re-points every link on [from] onto [to], keeping any transaction that already had [to]
     *  (dedup via IGNORE). The first half of a rename/merge; [deleteTag] drops the old row after. */
    @Query(
        "INSERT OR IGNORE INTO transaction_tags (transactionId, tagName) " +
            "SELECT transactionId, :to FROM transaction_tags WHERE tagName = :from",
    )
    suspend fun repointLinks(from: String, to: String)

    /** Drops a tag from the catalog; its links cascade away (FK ON DELETE CASCADE). Transactions are
     *  never deleted. */
    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteTag(name: String)

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM transaction_tags")
    suspend fun clearLinks()
}
