package com.budgetty.app.analytics

import android.content.Context
import android.os.Bundle
import com.budgetty.app.ui.recap.RecapKind
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.wellbeing.TipType
import com.google.firebase.analytics.FirebaseAnalytics

/** Where a buying limit was created from — the `source` param of [Analytics.logLimitCreated]. */
enum class LimitSource(val paramValue: String) { MANUAL("manual"), SUGGESTION("suggestion") }

/**
 * The one place that touches the Firebase Analytics SDK, so the rest of the app depends on this small
 * typed interface rather than Firebase directly. Every event is its own method with fixed params, so a
 * call site cannot fat-finger an event name or a param key.
 *
 * Collection is default-on with an opt-out: [com.budgetty.app.data.settings.AppSettings.analyticsEnabled]
 * defaults to true and the Account screen exposes a toggle, separate from crash reporting. The stored
 * preference is the source of truth — [setEnabled] is applied at startup ([com.budgetty.app.BudgettyApplication])
 * and again on every toggle change, so the SDK state always follows the user's choice.
 *
 * [FirebaseAnalytics.setAnalyticsCollectionEnabled] persists inside the SDK and survives process death,
 * so a user who opts out stays opted out even before startup re-applies the preference (same as
 * [com.budgetty.app.crash.CrashReporting]).
 *
 * Privacy: params carry only enums and ints (kind, type, source, length, gain, cards viewed). No
 * category name, item name, amount, email, store name, or any other free text is ever logged here, and
 * no extra collection (screen tracking, user properties) is added beyond these events — the SDK's
 * defaults, all gated by the toggle, are the only other signal.
 *
 * ⚠️ Shipping this also requires a Play Data-safety update (declare Analytics under App activity /
 * App info and performance, Shared = No) and a privacy-policy disclosure — those are Console/policy
 * tasks, not code.
 */
class Analytics(private val context: Context) {

    private val firebase: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

    /** Turns Analytics collection on/off; persists inside the SDK across process death. */
    fun setEnabled(enabled: Boolean) {
        firebase.setAnalyticsCollectionEnabled(enabled)
    }

    /** A scheduled recap story became visible. */
    fun logRecapShown(kind: RecapKind) =
        log(EVENT_RECAP_SHOWN) { putString(PARAM_KIND, kind.token()) }

    /**
     * A recap story was closed. [cardsViewed] is the highest card index reached + 1, so a partial read
     * (1) is distinguishable from a full one.
     */
    fun logRecapCompleted(kind: RecapKind, cardsViewed: Int) =
        log(EVENT_RECAP_COMPLETED) {
            putString(PARAM_KIND, kind.token())
            putLong(PARAM_CARDS_VIEWED, cardsViewed.toLong())
        }

    /** A streak was surfaced to the user (Budget row / Recap card / Wellbeing evidence). */
    fun logStreakSurfaced(kind: StreakKind, length: Int) =
        log(EVENT_STREAK_SURFACED) {
            putString(PARAM_KIND, kind.token())
            putLong(PARAM_LENGTH, length.toLong())
        }

    /** A Wellbeing tip's CTA was acted on. */
    fun logTipActed(type: TipType) =
        log(EVENT_TIP_ACTED) { putString(PARAM_TYPE, type.token()) }

    /** A Wellbeing tip's modelled "+N to your score" projection was shown. */
    fun logTipProjectedGain(type: TipType, gain: Int) =
        log(EVENT_TIP_PROJECTED_GAIN) {
            putString(PARAM_TYPE, type.token())
            putLong(PARAM_GAIN, gain.toLong())
        }

    /** A new buying limit was created, from the editor ([LimitSource.MANUAL]) or a suggestion. */
    fun logLimitCreated(source: LimitSource) =
        log(EVENT_LIMIT_CREATED) { putString(PARAM_SOURCE, source.paramValue) }

    private inline fun log(event: String, params: Bundle.() -> Unit) {
        firebase.logEvent(event, Bundle().apply(params))
    }

    /** enum name → lowercase snake token (locale-independent), e.g. BUDGET_MONTH → "budget_month". */
    private fun Enum<*>.token(): String = name.lowercase()

    private companion object {
        const val EVENT_RECAP_SHOWN = "recap_shown"
        const val EVENT_RECAP_COMPLETED = "recap_completed"
        const val EVENT_STREAK_SURFACED = "streak_surfaced"
        const val EVENT_TIP_ACTED = "tip_acted"
        const val EVENT_TIP_PROJECTED_GAIN = "tip_projected_gain"
        const val EVENT_LIMIT_CREATED = "limit_created"

        const val PARAM_KIND = "kind"
        const val PARAM_TYPE = "type"
        const val PARAM_SOURCE = "source"
        const val PARAM_LENGTH = "length"
        const val PARAM_GAIN = "gain"
        const val PARAM_CARDS_VIEWED = "cards_viewed"
    }
}
