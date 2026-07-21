package com.budgetty.app.review

import android.content.Context
import com.budgetty.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides *when* to ask for a Play Store rating. [ReviewPrompter] does the asking.
 *
 * Play hands out only a few in-app review prompts per user per year and never tells us how many are
 * left, so the one thing that matters is spending them at a good moment. The trigger is a
 * successfully finalized scan — the user just got the thing they came for — gated behind enough
 * usage that they have an opinion worth leaving.
 *
 * Deliberately *not* done: asking "do you like the app?" first and only routing happy users to the
 * rating card. That pattern is all over the ASO blogs and it violates Play policy, which forbids any
 * question before or during the prompt, opinion or predictive. Everyone gets asked, at a good moment.
 *
 * Device-level, like [com.budgetty.app.data.quota.ScanQuota] — a rating is per Play account per
 * device, so there is nothing to isolate per signed-in user.
 */
class ReviewTracker(context: Context) {

    private val prefs = context.getSharedPreferences("review_prompt", Context.MODE_PRIVATE)

    private val _pendingPrompt = MutableStateFlow(false)

    /** True once the user has earned a prompt. MainActivity collects this and fires the Play flow. */
    val pendingPrompt: StateFlow<Boolean> = _pendingPrompt.asStateFlow()

    /**
     * Call when a scan is finalized into a saved receipt. Mirrors the ScanQuota increment — both sit
     * behind the same guard, so an edit re-save never counts twice.
     */
    fun recordSuccessfulScan() {
        val now = System.currentTimeMillis()
        val scans = prefs.getInt(KEY_SCANS, 0) + 1
        // First write also stamps first-seen, so a brand-new user is 0 days old here and can't be
        // asked on their very first scan even if SCANS_BEFORE_PROMPT were 1.
        prefs.edit()
            .putInt(KEY_SCANS, scans)
            .apply {
                if (!prefs.contains(KEY_FIRST_SEEN)) putLong(KEY_FIRST_SEEN, now)
            }
            .apply()
        if (isEligible(now, scans)) _pendingPrompt.value = true
    }

    /**
     * Call once the prompt has been handed to Play. Starts the cooldown and lowers the flag.
     *
     * Note this records that we *asked*, not that anything was shown — Play's API deliberately never
     * reveals whether the card appeared or what the user did. Treating "asked" as "shown" is the
     * conservative choice: worst case we skip a prompt we could have spent.
     */
    fun onPromptRequested() {
        prefs.edit().putLong(KEY_LAST_ASKED, System.currentTimeMillis()).apply()
        _pendingPrompt.value = false
    }

    /** Clears prompt history, e.g. when the account is deleted. */
    fun reset() {
        prefs.edit().clear().apply()
        _pendingPrompt.value = false
    }

    private fun isEligible(now: Long, scans: Int): Boolean {
        if (scans < SCANS_BEFORE_PROMPT) return false
        // Debug builds skip the age gate so the flow is verifiable without waiting three days.
        if (!BuildConfig.DEBUG) {
            val firstSeen = prefs.getLong(KEY_FIRST_SEEN, now)
            if (daysBetween(firstSeen, now) < DAYS_USING_BEFORE_PROMPT) return false
        }
        val lastAsked = prefs.getLong(KEY_LAST_ASKED, 0L)
        return lastAsked == 0L || daysBetween(lastAsked, now) >= COOLDOWN_DAYS
    }

    private fun daysBetween(from: Long, to: Long): Long = (to - from) / DAY_MILLIS

    private companion object {
        /** Successful scans before the first ask. Keep in step with the iOS gate. */
        const val SCANS_BEFORE_PROMPT = 3

        /** Days since the first recorded scan before the first ask. Keep in step with the iOS gate. */
        const val DAYS_USING_BEFORE_PROMPT = 3L

        /**
         * Days before asking again. Play throttles far harder than this on its own; the cooldown just
         * stops us burning requests in a burst if a user scans heavily in one week.
         */
        const val COOLDOWN_DAYS = 90L

        const val DAY_MILLIS = 24L * 60 * 60 * 1000

        const val KEY_SCANS = "successful_scans"
        const val KEY_FIRST_SEEN = "first_seen"
        const val KEY_LAST_ASKED = "last_asked"
    }
}
