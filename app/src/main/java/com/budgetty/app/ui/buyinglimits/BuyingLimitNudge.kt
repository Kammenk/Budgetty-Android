package com.budgetty.app.ui.buyinglimits

import com.budgetty.app.data.local.BuyingLimitTimeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A pending save-time nudge: the just-saved receipt brought [title]'s buying limit to/over its cap.
 * Carries only display facts; the reset day and "this week/month" phrasing are resolved in the UI
 * (they need the locale + resources). [countAfter] is the total bought in the current window after
 * this receipt (the "your Nth" figure), [limitCount] the cap.
 */
data class BuyingLimitNudge(
    val limitId: Long,
    val title: String,
    val emoji: String,
    val countAfter: Int,
    val limitCount: Int,
    val timeframe: BuyingLimitTimeframe,
)

/**
 * A tiny app-scoped hand-off for the buying-limit save-time nudge. The receipt save flow
 * ([com.budgetty.app.ui.upload.UploadViewModel]) posts a nudge as it persists rows; Home observes it
 * and shows the floating card over the live screen once the upload flow pops back. In-app only — no
 * push, no WorkManager. A single-slot [StateFlow] (latest wins); dismiss or "View limits" clears it.
 */
class BuyingLimitNudgeBus {
    private val _nudge = MutableStateFlow<BuyingLimitNudge?>(null)
    val nudge: StateFlow<BuyingLimitNudge?> = _nudge.asStateFlow()

    fun post(nudge: BuyingLimitNudge) {
        _nudge.value = nudge
    }

    fun clear() {
        _nudge.value = null
    }
}
