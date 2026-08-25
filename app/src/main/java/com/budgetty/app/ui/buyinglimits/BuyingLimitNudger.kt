package com.budgetty.app.ui.buyinglimits

import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.CountableItem
import java.time.LocalDate

/**
 * Computes the save-time buying-limit nudge and posts it to the [BuyingLimitNudgeBus]. Kept out of
 * [com.budgetty.app.ui.upload.UploadViewModel] as its own collaborator so the save flow stays lean and
 * this logic (window + substring counting) is testable in isolation and mirrors 1:1 on iOS.
 *
 * A limit qualifies when the just-saved rows contributed at least one matching unit within its current
 * window AND the window's total now meets or exceeds the cap; the most-over limit wins (ties broken by
 * the higher count, then id). Counts sum quantity on the same normalized substring rule the management
 * screen uses, so card and nudge always agree. Non-blocking: the receipt is already saved regardless.
 */
class BuyingLimitNudger(
    private val transactionRepository: TransactionRepository,
    private val buyingLimitsRepository: BuyingLimitsRepository,
    private val settingsStore: SettingsStore,
    private val nudgeBus: BuyingLimitNudgeBus,
) {
    /** [savedItems] = the rows the just-finalized receipt persisted (name, quantity, made-date millis). */
    suspend fun onReceiptSaved(savedItems: List<CountableItem>) {
        val limits = buyingLimitsRepository.getAllOnce()
        if (limits.isEmpty()) return
        val allItems = transactionRepository.getAllOnce()
            .map { CountableItem(it.name, it.quantity, it.timestamp) }
        val monthStartDay = settingsStore.settings.value.monthStartDay
        val today = LocalDate.now()

        val best = limits
            .mapNotNull { limit ->
                val keywords = limit.keywordList
                if (keywords.isEmpty()) return@mapNotNull null
                val (start, end) = BuyingLimitCounter.window(limit.timeframe, today, monthStartDay)
                val contributed = BuyingLimitCounter.countInWindow(savedItems, keywords, start, end)
                if (contributed <= 0) return@mapNotNull null
                val countAfter = BuyingLimitCounter.countInWindow(allItems, keywords, start, end)
                if (countAfter < limit.count) return@mapNotNull null
                limit to countAfter
            }
            .maxWithOrNull(compareBy({ it.second - it.first.count }, { it.second }, { it.first.id }))
            ?: return

        val (limit, countAfter) = best
        nudgeBus.post(
            BuyingLimitNudge(
                limitId = limit.id,
                title = limit.displayTitle,
                emoji = limit.emoji,
                countAfter = countAfter,
                limitCount = limit.count,
                timeframe = limit.timeframe,
            ),
        )
    }
}
