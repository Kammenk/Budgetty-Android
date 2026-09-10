package com.budgetty.app.analytics

import android.content.Context
import android.os.Bundle
import com.budgetty.app.ui.recap.RecapKind
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.wellbeing.TipType
import com.google.firebase.analytics.FirebaseAnalytics

/** Where a buying limit was created from — the `source` param of [Analytics.logLimitCreated]. */
enum class LimitSource(val paramValue: String) { MANUAL("manual"), SUGGESTION("suggestion") }

/** Which in-app gate opened the paywall — the `source` param of [Analytics.logPaywallShown]. */
enum class PaywallSource { HOME, INSIGHTS, ACCOUNT, BUDGET, WIDGETS, BUYING_LIMITS, CATEGORIES }

/** The subscription plan a purchase refers to — the `plan` param of the purchase events. */
enum class SubPlan { MONTHLY, YEARLY, UNKNOWN }

/** Why a purchase attempt didn't complete — the `reason` param of [Analytics.logPurchaseFailed]. */
enum class PurchaseFailReason { CANCELLED, ERROR }

/** Why a receipt scan failed — the `reason` param of [Analytics.logScanFailed]. Mirrors UploadErrorKind. */
enum class ScanFailReason { UNREADABLE, SERVICE }

/** Which format a data export produced — the `format` param of [Analytics.logExportRun]. */
enum class ExportFormat { CSV, PDF }

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
 * Privacy: params carry only enums, ints, and navigation route names (kind, type, source, plan, reason,
 * format, screen). No category name, item name, amount, email, store name, or any other free text is
 * ever logged here — screen views log the fixed nav route, never a screen's contents. All events are
 * gated by the toggle.
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

    /** The paywall was opened, from [source]. */
    fun logPaywallShown(source: PaywallSource) =
        log(EVENT_PAYWALL_SHOWN) { putString(PARAM_SOURCE, source.token()) }

    /** A billing purchase flow was launched for [plan]. */
    fun logPurchaseStarted(plan: SubPlan) =
        log(EVENT_PURCHASE_STARTED) { putString(PARAM_PLAN, plan.token()) }

    /** A purchase for [plan] completed and was acknowledged. */
    fun logPurchaseCompleted(plan: SubPlan) =
        log(EVENT_PURCHASE_COMPLETED) { putString(PARAM_PLAN, plan.token()) }

    /** The user asked to restore purchases. */
    fun logPurchaseRestored() = log(EVENT_PURCHASE_RESTORED) {}

    /** A purchase attempt ended without completing, for [reason]. */
    fun logPurchaseFailed(reason: PurchaseFailReason) =
        log(EVENT_PURCHASE_FAILED) { putString(PARAM_REASON, reason.token()) }

    /** Onboarding was completed (the last page was dismissed). */
    fun logOnboardingCompleted() = log(EVENT_ONBOARDING_COMPLETED) {}

    /** The post-signup Insights quiz finished; [skipped] is true when the user skipped it. */
    fun logQuizCompleted(skipped: Boolean) =
        log(EVENT_QUIZ_COMPLETED) { putString(PARAM_RESULT, if (skipped) "skipped" else "completed") }

    /** The main budget amount was saved. */
    fun logBudgetSaved() = log(EVENT_BUDGET_SAVED) {}

    /** Data was exported as [format]. */
    fun logExportRun(format: ExportFormat) =
        log(EVENT_EXPORT_RUN) { putString(PARAM_FORMAT, format.token()) }

    /** A backup file was created. */
    fun logBackupCreated() = log(EVENT_BACKUP_CREATED) {}

    /** A home-screen widget was placed. */
    fun logWidgetPlaced() = log(EVENT_WIDGET_PLACED) {}

    /** A receipt scan was started. */
    fun logScanAttempted() = log(EVENT_SCAN_ATTEMPTED) {}

    /** A receipt scan returned a usable receipt. */
    fun logScanSucceeded() = log(EVENT_SCAN_SUCCEEDED) {}

    /** A receipt scan failed, for [reason]. */
    fun logScanFailed(reason: ScanFailReason) =
        log(EVENT_SCAN_FAILED) { putString(PARAM_REASON, reason.token()) }

    /** A screen became visible. Uses Firebase's standard screen_view event with the nav route. */
    fun logScreenView(route: String) =
        log(FirebaseAnalytics.Event.SCREEN_VIEW) { putString(FirebaseAnalytics.Param.SCREEN_NAME, route) }

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
        const val EVENT_PAYWALL_SHOWN = "paywall_shown"
        const val EVENT_PURCHASE_STARTED = "purchase_started"
        const val EVENT_PURCHASE_COMPLETED = "purchase_completed"
        const val EVENT_PURCHASE_RESTORED = "purchase_restored"
        const val EVENT_PURCHASE_FAILED = "purchase_failed"
        const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
        const val EVENT_QUIZ_COMPLETED = "quiz_completed"
        const val EVENT_BUDGET_SAVED = "budget_saved"
        const val EVENT_EXPORT_RUN = "export_run"
        const val EVENT_BACKUP_CREATED = "backup_created"
        const val EVENT_WIDGET_PLACED = "widget_placed"
        const val EVENT_SCAN_ATTEMPTED = "scan_attempted"
        const val EVENT_SCAN_SUCCEEDED = "scan_succeeded"
        const val EVENT_SCAN_FAILED = "scan_failed"

        const val PARAM_KIND = "kind"
        const val PARAM_TYPE = "type"
        const val PARAM_SOURCE = "source"
        const val PARAM_LENGTH = "length"
        const val PARAM_GAIN = "gain"
        const val PARAM_CARDS_VIEWED = "cards_viewed"
        const val PARAM_PLAN = "plan"
        const val PARAM_REASON = "reason"
        const val PARAM_RESULT = "result"
        const val PARAM_FORMAT = "format"
    }
}
