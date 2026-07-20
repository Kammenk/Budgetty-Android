package com.budgetty.app.review

import androidx.activity.ComponentActivity
import com.budgetty.app.BuildConfig
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager

/**
 * Launches Google Play's in-app review card — the native rating dialog shown without leaving the app.
 * [ReviewTracker] decides when; this only performs the ask.
 *
 * Three things about this API that are easy to get wrong:
 *
 * 1. **It tells you nothing.** Success and "Play silently declined to show anything" complete
 *    identically. There is no callback for "the user rated". Never branch on the outcome.
 * 2. **Play may show nothing at all** — quota exhausted, already reviewed, ineligible account. That
 *    is normal, not an error.
 * 3. **It only works for builds installed by Play.** Sideloaded and debug builds get nothing, which
 *    is why DEBUG uses [FakeReviewManager]: it exercises the same code path and completes
 *    successfully without rendering a card, so the wiring is verifiable off-track.
 */
class ReviewPrompter(private val activity: ComponentActivity) {

    private val manager: ReviewManager =
        if (BuildConfig.DEBUG) FakeReviewManager(activity) else ReviewManagerFactory.create(activity)

    /**
     * Ask Play for the review card. Fire-and-forget by design — see the caveats above.
     *
     * The two-step request/launch dance is Play's: [ReviewManager.requestReviewFlow] prepares a
     * short-lived token that [ReviewManager.launchReviewFlow] then spends.
     */
    fun request() {
        manager.requestReviewFlow().addOnCompleteListener { request ->
            // A failed request means Play has nothing for this user right now. Nothing to recover
            // from and nothing worth surfacing — the user never asked for this dialog.
            if (!request.isSuccessful) return@addOnCompleteListener
            manager.launchReviewFlow(activity, request.result)
        }
    }
}
