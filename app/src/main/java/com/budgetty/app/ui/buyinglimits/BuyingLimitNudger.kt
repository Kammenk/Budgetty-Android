package com.budgetty.app.ui.buyinglimits

import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.CountableItem
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Computes the save-time buying-limit nudge and posts it to the [BuyingLimitNudgeBus]. Kept out of
 * [com.budgetty.app.ui.upload.UploadViewModel] as its own collaborator so the save flow stays lean and
 * this logic (window + substring counting) is testable in isolation and mirrors 1:1 on iOS.
 *
 * Restraint (§4.6): at most ONE nudge per receipt save (the single most-over limit wins), and never a
 * re-nudge for a limit that was ALREADY at/over its cap BEFORE this receipt — only the save that
 * actually crosses the line nudges. Telling someone twice in the same window is nagging, not helping.
 * The decision is factored into the pure [selectNudge] so it unit-tests without Android or repositories.
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
        val nudge = selectNudge(limits, allItems, savedItems, LocalDate.now(), monthStartDay)
        if (nudge != null) nudgeBus.post(nudge)
    }

    companion object {
        /**
         * The one nudge (if any) a just-saved receipt should raise. A limit qualifies when this receipt
         * contributed ≥ 1 matching unit in the limit's current window AND the window's total now meets or
         * exceeds the cap AND the window was NOT already at/over the cap before this receipt (§4.6). The
         * most-over limit wins (ties: higher count, then id). Pure — no Android, no repositories.
         */
        fun selectNudge(
            limits: List<BuyingLimitEntity>,
            allItems: List<CountableItem>,
            savedItems: List<CountableItem>,
            today: LocalDate,
            monthStartDay: Int,
            firstDayOfWeek: DayOfWeek = BuyingLimitCounter.localeFirstDayOfWeek(),
        ): BuyingLimitNudge? {
            val best = limits
                .mapNotNull { limit ->
                    val keywords = limit.keywordList
                    if (keywords.isEmpty()) return@mapNotNull null
                    val (start, end) = BuyingLimitCounter.window(limit.timeframe, today, monthStartDay, firstDayOfWeek)
                    val contributed = BuyingLimitCounter.countInWindow(savedItems, keywords, start, end)
                    if (contributed <= 0) return@mapNotNull null
                    val countAfter = BuyingLimitCounter.countInWindow(allItems, keywords, start, end)
                    if (countAfter < limit.count) return@mapNotNull null
                    // Already at/over BEFORE this receipt → they know; don't re-nudge in the same window.
                    if (countAfter - contributed >= limit.count) return@mapNotNull null
                    limit to countAfter
                }
                .maxWithOrNull(compareBy({ it.second - it.first.count }, { it.second }, { it.first.id }))
                ?: return null

            val (limit, countAfter) = best
            return BuyingLimitNudge(
                limitId = limit.id,
                title = limit.displayTitle,
                emoji = limit.emoji,
                countAfter = countAfter,
                limitCount = limit.count,
                timeframe = limit.timeframe,
            )
        }
    }
}
