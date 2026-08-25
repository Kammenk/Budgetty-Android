package com.budgetty.app.data.settings

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Persists the user's app-customization settings (theme, accent, currency, date format). */
@Suppress("TooManyFunctions") // A flat store of one-line preference accessors; each is trivial by design.
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        themeMode = read(KEY_THEME, ThemeMode.SYSTEM),
        accent = read(KEY_ACCENT, AccentTheme.DEFAULT),
        currency = readCurrency(),
        dateFormat = read(KEY_DATE, DateFormatOption.DAY_MONTH_YEAR),
        language = read(KEY_LANGUAGE, Language.SYSTEM),
        onboardingSeen = prefs.getBoolean(KEY_ONBOARDING_SEEN, false),
        insightsQuizPending = prefs.getBoolean(KEY_QUIZ_PENDING, false),
        displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
        hiddenHomeSections = prefs.getStringSet(KEY_HIDDEN_HOME, emptySet()).orEmpty().toSet(),
        hiddenInsightsSections = prefs.getStringSet(KEY_HIDDEN_INSIGHTS, emptySet()).orEmpty().toSet(),
        homeSectionOrder = prefs.getString(KEY_ORDER_HOME, null).toKeyList(),
        insightsSectionOrder = prefs.getString(KEY_ORDER_INSIGHTS, null).toKeyList(),
        insightsPeriodUnit = prefs.getString(KEY_PERIOD_UNIT_INSIGHTS, "MONTH") ?: "MONTH",
        insightsIncludeRecurringBills = prefs.getBoolean(KEY_INCLUDE_RECURRING_BILLS, false),
        insightsOverlayNudgeDismissed = prefs.getBoolean(KEY_OVERLAY_NUDGE_DISMISSED, false),
        monthStartDay = prefs.getInt(KEY_MONTH_START_DAY, 1).coerceIn(1, 31),
        budgetRolloverEnabled = prefs.getBoolean(KEY_BUDGET_ROLLOVER, false),
        historySort = prefs.getString(KEY_HISTORY_SORT, "NEWEST") ?: "NEWEST",
        recentSearches = prefs.getString(KEY_RECENT_SEARCHES, null).toLines(),
        crashReportingEnabled = prefs.getBoolean(KEY_CRASH_REPORTING, true),
        appLockEnabled = prefs.getBoolean(KEY_APP_LOCK, false),
        pinHash = prefs.getString(KEY_PIN_HASH, "").orEmpty(),
        biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
        autoLockMinutes = prefs.getInt(KEY_AUTO_LOCK, 1),
        dismissedWellbeingTips = prefs.getStringSet(KEY_DISMISSED_TIPS, emptySet()).orEmpty().toSet(),
        recapEnabled = prefs.getBoolean(KEY_RECAP_ENABLED, true),
        recapFrequency = read(KEY_RECAP_FREQUENCY, RecapFrequency.MONTHLY),
        recapLastShownWeek = prefs.getString(KEY_RECAP_LAST_WEEK, "").orEmpty(),
        recapLastShownMonth = prefs.getString(KEY_RECAP_LAST_MONTH, "").orEmpty(),
    )

    fun setThemeMode(value: ThemeMode) = save(KEY_THEME, value) { it.copy(themeMode = value) }
    fun setAccent(value: AccentTheme) = save(KEY_ACCENT, value) { it.copy(accent = value) }
    fun setCurrency(value: Currency) = save(KEY_CURRENCY, value) { it.copy(currency = value) }
    fun setDateFormat(value: DateFormatOption) = save(KEY_DATE, value) { it.copy(dateFormat = value) }
    fun setLanguage(value: Language) = save(KEY_LANGUAGE, value) { it.copy(language = value) }

    fun setOnboardingSeen(value: Boolean = true) =
        save(KEY_ONBOARDING_SEEN, value) { it.copy(onboardingSeen = value) }

    fun setDisplayName(value: String) =
        saveString(KEY_DISPLAY_NAME, value) { it.copy(displayName = value) }

    /** Arms (or disarms) the one-time post-signup Insights setup quiz gate. */
    fun setInsightsQuizPending(value: Boolean) =
        save(KEY_QUIZ_PENDING, value) { it.copy(insightsQuizPending = value) }

    /**
     * Applies a finished setup quiz in one shot: the derived hidden sections and section order (the
     * same settings the Customize-sections menu edits, so everything stays reversible there), the
     * raw encoded answers (kept only for future re-tuning; nothing reads them back yet), and the
     * cleared pending flag that lets the app leave the quiz gate.
     */
    fun applyInsightsQuizResult(hidden: Set<String>, order: List<String>, encodedAnswers: String) {
        val editor = prefs.edit()
            .putStringSet(KEY_HIDDEN_INSIGHTS, hidden)
            .putString(KEY_QUIZ_ANSWERS, encodedAnswers)
            .putBoolean(KEY_QUIZ_PENDING, false)
        // An empty order means "default": clear the key so the customize sheet's reset row behaves
        // as if the user never reordered.
        if (order.isEmpty()) editor.remove(KEY_ORDER_INSIGHTS)
        else editor.putString(KEY_ORDER_INSIGHTS, order.joinToString(","))
        editor.apply()
        _settings.update {
            it.copy(
                hiddenInsightsSections = hidden,
                insightsSectionOrder = order,
                insightsQuizPending = false,
            )
        }
    }

    /** Hides ([hidden] = true) or shows a Home section, persisted by its stable section key. */
    fun setHomeSectionHidden(sectionKey: String, hidden: Boolean) {
        val updated = _settings.value.hiddenHomeSections.toggled(sectionKey, hidden)
        prefs.edit().putStringSet(KEY_HIDDEN_HOME, updated).apply()
        _settings.update { it.copy(hiddenHomeSections = updated) }
    }

    /** Hides ([hidden] = true) or shows an Insights section, persisted by its stable section key. */
    fun setInsightsSectionHidden(sectionKey: String, hidden: Boolean) {
        val updated = _settings.value.hiddenInsightsSections.toggled(sectionKey, hidden)
        prefs.edit().putStringSet(KEY_HIDDEN_INSIGHTS, updated).apply()
        _settings.update { it.copy(hiddenInsightsSections = updated) }
    }

    /** Persists the full Home section display order (a list of stable section keys). */
    fun setHomeSectionOrder(order: List<String>) {
        prefs.edit().putString(KEY_ORDER_HOME, order.joinToString(",")).apply()
        _settings.update { it.copy(homeSectionOrder = order) }
    }

    /** Persists the full Insights section display order (a list of stable section keys). */
    fun setInsightsSectionOrder(order: List<String>) {
        prefs.edit().putString(KEY_ORDER_INSIGHTS, order.joinToString(",")).apply()
        _settings.update { it.copy(insightsSectionOrder = order) }
    }

    /** Restores the Home sections to their default state: all shown, in their natural order. */
    fun resetHomeSections() {
        prefs.edit().remove(KEY_HIDDEN_HOME).remove(KEY_ORDER_HOME).apply()
        _settings.update { it.copy(hiddenHomeSections = emptySet(), homeSectionOrder = emptyList()) }
    }

    /** Restores the Insights sections to their default state: all shown, in their natural order. */
    fun resetInsightsSections() {
        prefs.edit().remove(KEY_HIDDEN_INSIGHTS).remove(KEY_ORDER_INSIGHTS).apply()
        _settings.update { it.copy(hiddenInsightsSections = emptySet(), insightsSectionOrder = emptyList()) }
    }

    /** Remembers the Insights period-stepper unit (a PeriodUnit name) for the next launch. */
    fun setInsightsPeriodUnit(unitName: String) =
        saveString(KEY_PERIOD_UNIT_INSIGHTS, unitName) { it.copy(insightsPeriodUnit = unitName) }

    /** Toggles the Insights "planned recurring bills" overlay (opt-in, default off). */
    fun setInsightsIncludeRecurringBills(value: Boolean) =
        save(KEY_INCLUDE_RECURRING_BILLS, value) { it.copy(insightsIncludeRecurringBills = value) }

    /** Marks the one-time Insights overlay discovery nudge as dismissed (never resurfaces). */
    fun dismissInsightsOverlayNudge() =
        save(KEY_OVERLAY_NUDGE_DISMISSED, true) { it.copy(insightsOverlayNudgeDismissed = true) }

    /** Remembers the History sort order (a SortOrder name) for the next launch. */
    fun setHistorySort(name: String) =
        saveString(KEY_HISTORY_SORT, name) { it.copy(historySort = name) }

    /** Toggles budget rollover (unspent budget carries into the next period). */
    fun setBudgetRolloverEnabled(value: Boolean) =
        save(KEY_BUDGET_ROLLOVER, value) { it.copy(budgetRolloverEnabled = value) }

    /** Sets the pay-day the financial month starts on (1–31; 1 = calendar month). */
    fun setMonthStartDay(value: Int) {
        val day = value.coerceIn(1, 31)
        prefs.edit().putInt(KEY_MONTH_START_DAY, day).apply()
        _settings.update { it.copy(monthStartDay = day) }
    }

    /** Persists the crash-reporting opt-out. Applying it to the Crashlytics SDK is the caller's job. */
    fun setCrashReportingEnabled(value: Boolean) =
        save(KEY_CRASH_REPORTING, value) { it.copy(crashReportingEnabled = value) }

    // ── App lock ──

    /** Sets a new PIN (stored hashed) and turns the lock on. */
    fun setPin(pin: String) {
        val hash = PinHash.hash(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).putBoolean(KEY_APP_LOCK, true).apply()
        _settings.update { it.copy(pinHash = hash, appLockEnabled = true) }
    }

    fun verifyPin(pin: String): Boolean = PinHash.verify(pin, _settings.value.pinHash)

    fun setBiometricEnabled(value: Boolean) =
        save(KEY_BIOMETRIC, value) { it.copy(biometricEnabled = value) }

    fun setAutoLockMinutes(value: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK, value).apply()
        _settings.update { it.copy(autoLockMinutes = value) }
    }

    /** Turns the lock off and forgets the PIN + biometric preference. */
    fun disableAppLock() {
        prefs.edit().remove(KEY_PIN_HASH).putBoolean(KEY_APP_LOCK, false).putBoolean(KEY_BIOMETRIC, false).apply()
        _settings.update { it.copy(appLockEnabled = false, pinHash = "", biometricEnabled = false) }
    }

    /** Records [query] as the most-recent History search, de-duplicated and capped. */
    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val updated = (listOf(q) + _settings.value.recentSearches.filterNot { it.equals(q, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        prefs.edit().putString(KEY_RECENT_SEARCHES, updated.joinToString("\n")).apply()
        _settings.update { it.copy(recentSearches = updated) }
    }

    /** Drops a single term from the recent-search list (the ✕ on a quick-find pill). */
    fun removeRecentSearch(query: String) {
        val updated = _settings.value.recentSearches.filterNot { it.equals(query, ignoreCase = true) }
        prefs.edit().putString(KEY_RECENT_SEARCHES, updated.joinToString("\n")).apply()
        _settings.update { it.copy(recentSearches = updated) }
    }

    /** Clears the entire recent-search history ("Clear all"). */
    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        _settings.update { it.copy(recentSearches = emptyList()) }
    }

    // ── End-of-period recap ──

    /** Turns the end-of-period recap interstitial on/off. Keeps the stored cadence either way. */
    fun setRecapEnabled(value: Boolean) =
        save(KEY_RECAP_ENABLED, value) { it.copy(recapEnabled = value) }

    /** Sets which period(s) trigger a recap (Weekly / Monthly / Both). */
    fun setRecapFrequency(value: RecapFrequency) =
        save(KEY_RECAP_FREQUENCY, value) { it.copy(recapFrequency = value) }

    /**
     * Stamps the periods a recap open has just handled: [weekId] (yyyy-MM-dd start) and/or [monthId]
     * (yyyy-MM) — either may be null when only one cadence was due. Written once per completed period
     * so the interstitial fires at most once, matching the [ReviewTracker]-style last-shown key.
     */
    fun setRecapShown(weekId: String?, monthId: String?) {
        val editor = prefs.edit()
        weekId?.let { editor.putString(KEY_RECAP_LAST_WEEK, it) }
        monthId?.let { editor.putString(KEY_RECAP_LAST_MONTH, it) }
        editor.apply()
        _settings.update {
            it.copy(
                recapLastShownWeek = weekId ?: it.recapLastShownWeek,
                recapLastShownMonth = monthId ?: it.recapLastShownMonth,
            )
        }
    }

    /** Records a Wellbeing tip as dismissed for its period ([scopedId] = "periodId|tipId"). */
    fun dismissWellbeingTip(scopedId: String) {
        val updated = _settings.value.dismissedWellbeingTips + scopedId
        prefs.edit().putStringSet(KEY_DISMISSED_TIPS, updated).apply()
        _settings.update { it.copy(dismissedWellbeingTips = updated) }
    }

    /**
     * Wipes every setting tied to the signed-in user — identity, search history, app-lock PIN +
     * biometric, the setup questionnaire, dismissed tips, and personal section layout — while
     * leaving device/app display preferences (theme, accent, currency, date format, language,
     * onboarding, month-start day) intact.
     *
     * These prefs are a single device-global store, so anything left here leaks to the next account
     * on a shared device. Every sign-out and account-deletion path MUST call this (currently
     * AuthViewModel.signOut, AppLockViewModel.forgotPin, AccountViewModel.deleteAccount); add the
     * call to any new sign-out path too.
     */
    fun clearUserState() {
        prefs.edit()
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_RECENT_SEARCHES)
            .remove(KEY_PIN_HASH)
            .remove(KEY_APP_LOCK)
            .remove(KEY_BIOMETRIC)
            .remove(KEY_QUIZ_PENDING)
            .remove(KEY_QUIZ_ANSWERS)
            .remove(KEY_DISMISSED_TIPS)
            .remove(KEY_HIDDEN_HOME)
            .remove(KEY_HIDDEN_INSIGHTS)
            .remove(KEY_ORDER_HOME)
            .remove(KEY_ORDER_INSIGHTS)
            // Per-user recap timing (not the cadence preference, which is device-global like theme):
            // reset so the next account on a shared device gets fresh recap boundaries.
            .remove(KEY_RECAP_LAST_WEEK)
            .remove(KEY_RECAP_LAST_MONTH)
            .remove(KEY_INCLUDE_RECURRING_BILLS)
            .remove(KEY_OVERLAY_NUDGE_DISMISSED)
            .apply()
        _settings.update {
            it.copy(
                displayName = "",
                recentSearches = emptyList(),
                appLockEnabled = false,
                pinHash = "",
                biometricEnabled = false,
                insightsQuizPending = false,
                dismissedWellbeingTips = emptySet(),
                hiddenHomeSections = emptySet(),
                hiddenInsightsSections = emptySet(),
                homeSectionOrder = emptyList(),
                insightsSectionOrder = emptyList(),
                recapLastShownWeek = "",
                recapLastShownMonth = "",
                insightsIncludeRecurringBills = false,
                insightsOverlayNudgeDismissed = false,
            )
        }
    }

    private fun Set<String>.toggled(key: String, present: Boolean): Set<String> =
        if (present) this + key else this - key

    private fun String?.toKeyList(): List<String> =
        this?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    // Recent searches are newline-joined (not comma) since a search term may itself contain commas.
    private fun String?.toLines(): List<String> =
        this?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun save(key: String, value: Enum<*>, update: (AppSettings) -> AppSettings) {
        prefs.edit().putString(key, value.name).apply()
        _settings.update(update)
    }

    private fun save(key: String, value: Boolean, update: (AppSettings) -> AppSettings) {
        prefs.edit().putBoolean(key, value).apply()
        _settings.update(update)
    }

    private fun saveString(key: String, value: String, update: (AppSettings) -> AppSettings) {
        prefs.edit().putString(key, value).apply()
        _settings.update(update)
    }

    private inline fun <reified T : Enum<T>> read(key: String, default: T): T {
        val name = prefs.getString(key, null) ?: return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    /**
     * Currency resolves to the user's saved choice when present, otherwise to the device region's
     * currency (if we support it), otherwise [Currency.EUR]. Locale detection only seeds a fresh
     * install: a previously saved value — even one we've since removed — falls back to EUR, never
     * to the locale, so an explicit past choice is never silently re-derived.
     */
    private fun readCurrency(): Currency {
        val saved = prefs.getString(KEY_CURRENCY, null) ?: return localeDefaultCurrency()
        return runCatching { enumValueOf<Currency>(saved) }.getOrDefault(Currency.EUR)
    }

    /**
     * Maps the device's region to a supported [Currency], falling back to [Currency.EUR]. Reads the
     * system locale (not the app's language override) so the region reflects the actual device, and
     * needs no sign-in, permission, or network.
     */
    private fun localeDefaultCurrency(): Currency = runCatching {
        val region = Resources.getSystem().configuration.locales[0]
        val code = android.icu.util.Currency.getInstance(region).currencyCode
        Currency.entries.firstOrNull { it.code == code }
    }.getOrNull() ?: Currency.EUR

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_CURRENCY = "currency"
        const val KEY_DATE = "date_format"
        const val KEY_LANGUAGE = "language"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        const val KEY_QUIZ_PENDING = "insights_quiz_pending"
        const val KEY_QUIZ_ANSWERS = "insights_quiz_answers"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_HIDDEN_HOME = "hidden_home_sections"
        const val KEY_HIDDEN_INSIGHTS = "hidden_insights_sections"
        const val KEY_ORDER_HOME = "home_section_order"
        const val KEY_ORDER_INSIGHTS = "insights_section_order"
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_PIN_HASH = "app_lock_pin_hash"
        const val KEY_BIOMETRIC = "app_lock_biometric"
        const val KEY_AUTO_LOCK = "app_lock_auto_minutes"
        const val KEY_PERIOD_UNIT_INSIGHTS = "insights_period_unit"
        const val KEY_INCLUDE_RECURRING_BILLS = "insights_include_recurring_bills"
        const val KEY_OVERLAY_NUDGE_DISMISSED = "insights_overlay_nudge_dismissed"
        const val KEY_MONTH_START_DAY = "month_start_day"
        const val KEY_BUDGET_ROLLOVER = "budget_rollover_enabled"
        const val KEY_HISTORY_SORT = "history_sort"
        const val KEY_RECENT_SEARCHES = "recent_searches"
        const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        const val KEY_DISMISSED_TIPS = "dismissed_wellbeing_tips"
        const val KEY_RECAP_ENABLED = "recap_enabled"
        const val KEY_RECAP_FREQUENCY = "recap_frequency"
        const val KEY_RECAP_LAST_WEEK = "recap_last_shown_week"
        const val KEY_RECAP_LAST_MONTH = "recap_last_shown_month"
        const val MAX_RECENT_SEARCHES = 6
    }
}
