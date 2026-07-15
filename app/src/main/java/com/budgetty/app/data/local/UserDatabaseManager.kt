package com.budgetty.app.data.local

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Owns the per-account Room databases. Every signed-in Firebase user gets their own database file,
 * so accounts sharing a device never see each other's receipts; signed-out access (e.g. a widget
 * refreshing after sign-out) reads an empty scratch database instead.
 *
 * Instances are cached per uid and stay open across account switches: sign-out tears the UI down
 * asynchronously, so a straggling collector may still touch the previous instance, and closing it
 * under them buys nothing (at most a couple of accounts ever open on a real device). The one close
 * path is [deleteDataFor] (account deletion).
 */
class UserDatabaseManager(
    private val context: Context,
    private val auth: FirebaseAuth,
) {
    private val _activeUid = MutableStateFlow(auth.currentUser?.uid)

    /** The uid whose data is currently readable; null when signed out. */
    val activeUid: StateFlow<String?> = _activeUid.asStateFlow()

    private val instances = mutableMapOf<String?, BudgettyDatabase>()

    init {
        auth.addAuthStateListener { _activeUid.value = it.currentUser?.uid }
    }

    /** The signed-in user's database; an empty scratch database when signed out. */
    val database: BudgettyDatabase
        get() = databaseFor(auth.currentUser?.uid)

    /**
     * A flow that runs [query] against the active user's database and re-runs it whenever the
     * signed-in account changes, so long-lived collectors (widgets, app-scoped observers) never
     * keep serving a previous account's data. Repositories route every flow-returning API through
     * this.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> flow(query: (BudgettyDatabase) -> Flow<T>): Flow<T> =
        activeUid.flatMapLatest { uid -> query(databaseFor(uid)) }

    /** Closes and irreversibly deletes [uid]'s database file — the account-deletion wipe. */
    fun deleteDataFor(uid: String) {
        synchronized(this) { instances.remove(uid) }?.close()
        context.deleteDatabase(fileNameFor(uid))
    }

    @Synchronized
    private fun databaseFor(uid: String?): BudgettyDatabase = instances.getOrPut(uid) {
        val name = fileNameFor(uid)
        if (uid != null) adoptLegacyDatabase(name)
        Room.databaseBuilder(context, BudgettyDatabase::class.java, name)
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
            )
            .addCallback(categorySeedCallback)
            .build()
    }

    /** "budgetty-u-<uid>.db" per account; a scratch file for signed-out access. */
    private fun fileNameFor(uid: String?): String =
        if (uid == null) "budgetty-anon.db" else "budgetty-u-$uid.db"

    /**
     * Adopts the pre-account-separation database ("budgetty.db"): the first signed-in account to
     * open its database after this update takes the legacy file over — in practice the user who
     * was signed in when the update installed, i.e. the data's owner. The rename keeps the WAL
     * sidecars so un-checkpointed writes survive.
     */
    private fun adoptLegacyDatabase(targetName: String) {
        val legacy = context.getDatabasePath(LEGACY_DB_NAME)
        if (!legacy.exists()) return
        val target = context.getDatabasePath(targetName)
        if (target.exists()) return
        legacy.renameTo(target)
        for (suffix in listOf("-wal", "-shm")) {
            val sidecar = File(legacy.path + suffix)
            if (sidecar.exists()) sidecar.renameTo(File(target.path + suffix))
        }
    }

    private companion object {
        const val LEGACY_DB_NAME = "budgetty.db"
    }
}
