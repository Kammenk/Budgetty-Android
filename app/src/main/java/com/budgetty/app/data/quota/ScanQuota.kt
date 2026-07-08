package com.budgetty.app.data.quota

import android.content.Context

/**
 * Tracks how many AI receipt scans (photo + file) the user has used on the free tier.
 * Persisted locally; enforcement is shared across the add sheet and the upload flow.
 */
class ScanQuota(context: Context) {
    private val prefs = context.getSharedPreferences("scan_quota", Context.MODE_PRIVATE)

    fun used(): Int = prefs.getInt(KEY_USED, 0)

    fun remaining(): Int = (FREE_LIMIT - used()).coerceAtLeast(0)

    fun canScan(): Boolean = used() < FREE_LIMIT

    fun increment() {
        prefs.edit().putInt(KEY_USED, used() + 1).apply()
    }

    /** Clears the used-scan count, e.g. when the account is deleted. */
    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val FREE_LIMIT = 5
        private const val KEY_USED = "used"
    }
}
