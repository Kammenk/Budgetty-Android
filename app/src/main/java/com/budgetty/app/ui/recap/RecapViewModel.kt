package com.budgetty.app.ui.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.analytics.Analytics
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.streaks.StreakKind
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.PayCycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Current recap on/off + cadence, for the in-story frequency control's marked selection (§1.4). */
data class RecapPrefs(val enabled: Boolean, val frequency: RecapFrequency)

/** State the recap gate observes (see [RecapViewModel.interstitial]). */
data class RecapInterstitialState(
    /** True once the DB has loaded and the data guard has run for the due period. */
    val isLoaded: Boolean = false,
    /** The due decision, for stamping the period(s) as shown on close/skip. */
    val due: RecapDue? = null,
    /** The story to show; null once [isLoaded] means the guard skipped it (stamp shown, show nothing). */
    val story: RecapStory? = null,
)

/**
 * Backs the end-of-period recap: the on-open interstitial (the gate collects [interstitial]) and the
 * Insights re-open ([reopen]). All the "when does it fire" logic lives in [RecapScheduler]; this just
 * wires it to the settings + the [RecapProvider], and stamps the last-shown keys on close.
 *
 * The gate first does the cheap [RecapScheduler.due] check itself (settings + clock, no DB) so the
 * common "nothing due" open never touches this at all; when something is due it collects [interstitial],
 * which loads the story off the main thread and reports [RecapInterstitialState.isLoaded] so the gate
 * can hold a neutral backdrop instead of flashing Home.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecapViewModel(
    private val provider: RecapProvider,
    private val settings: SettingsStore,
    private val analytics: Analytics,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val firstDayOfWeek: () -> DayOfWeek = { BuyingLimitCounter.localeFirstDayOfWeek() },
) : ViewModel() {

    val interstitial: StateFlow<RecapInterstitialState> =
        settings.settings.flatMapLatest { s ->
            val due = RecapScheduler.due(
                enabled = s.recapEnabled,
                frequency = s.recapFrequency,
                lastShownWeek = s.recapLastShownWeek,
                lastShownMonth = s.recapLastShownMonth,
                today = today(),
                monthStartDay = s.monthStartDay,
                firstDayOfWeek = firstDayOfWeek(),
            )
            if (due == null) {
                flowOf(RecapInterstitialState(isLoaded = true))
            } else {
                flow {
                    emit(RecapInterstitialState(isLoaded = false, due = due))
                    emitAll(
                        provider.story(due.show, offset = -1).map { story ->
                            RecapInterstitialState(isLoaded = true, due = due, story = story)
                        },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RecapInterstitialState())

    /**
     * Current recap on/off + cadence, so the re-opened story's frequency control (§1.4) can mark the
     * live selection. Backed by the same [SettingsStore.settings] flow as Account → Recap, so the two
     * always agree.
     */
    val recapPrefs: StateFlow<RecapPrefs> =
        settings.settings
            .map { RecapPrefs(it.recapEnabled, it.recapFrequency) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                RecapPrefs(true, RecapFrequency.BOTH),
            )

    /** Stamps the due period(s) as shown. Both the close (Done / ✕) and the guard-skip call this. */
    fun markShown(due: RecapDue) = settings.setRecapShown(due.markWeek, due.markMonth)

    /**
     * Applies the in-story frequency control (§1.4): [enabled] false = Off (recap off, cadence
     * remembered); the three cadences set enabled + that frequency. Writes straight to [SettingsStore],
     * whose single [SettingsStore.settings] flow also backs Account → Recap, so the two stay in sync
     * with no extra wiring. Applied for the NEXT open — the current story is latched by the gate so this
     * never tears it down mid-read.
     */
    fun setRecapFrequencyChoice(enabled: Boolean, frequency: RecapFrequency) {
        settings.setRecapEnabled(enabled)
        if (enabled) settings.setRecapFrequency(frequency)
    }

    /** A streak card was surfaced in the story (fired once per surfaced streak when the story appears). */
    fun onStreakSurfaced(kind: StreakKind, length: Int) = analytics.logStreakSurfaced(kind, length)

    /** A scheduled recap story became visible (fired once when the interstitial appears). */
    fun onRecapShown(kind: RecapKind) = analytics.logRecapShown(kind)

    /**
     * The recap story was closed. [cardsViewed] is the highest card index reached + 1, so a partial
     * read is distinguishable from a full one.
     */
    fun onRecapCompleted(kind: RecapKind, cardsViewed: Int) = analytics.logRecapCompleted(kind, cardsViewed)

    /**
     * The last recap the user saw, recomputed on demand for the Insights re-open — null when none was
     * ever shown (the entry stays hidden then). Prefers whichever period ended more recently.
     */
    val reopen: StateFlow<RecapStory?> =
        settings.settings.flatMapLatest { s ->
            val target = lastShownTarget(s.recapLastShownWeek, s.recapLastShownMonth)
            if (target == null) flowOf(null) else provider.story(target.first, target.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private fun lastShownTarget(weekId: String, monthId: String): Pair<RecapKind, Int>? {
        val month = monthId.takeIf { it.isNotEmpty() }?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        val week = weekId.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val monthEnd = month?.atEndOfMonth()
        val weekEnd = week?.plusDays(DAYS_IN_WEEK - 1L)
        return when {
            month != null && (weekEnd == null || !monthEnd!!.isBefore(weekEnd)) ->
                RecapKind.MONTHLY to monthOffset(month)
            week != null -> RecapKind.WEEKLY to weekOffset(week)
            else -> null
        }
    }

    private fun monthOffset(shown: YearMonth): Int {
        val currentCycle = YearMonth.from(PayCycle.month(today(), settings.settings.value.monthStartDay, 0).first)
        return ChronoUnit.MONTHS.between(currentCycle, shown).toInt()
    }

    private fun weekOffset(shownWeekStart: LocalDate): Int {
        val currentWeekStart = today().with(TemporalAdjusters.previousOrSame(firstDayOfWeek()))
        return ChronoUnit.WEEKS.between(currentWeekStart, shownWeekStart).toInt()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DAYS_IN_WEEK = 7
    }
}
