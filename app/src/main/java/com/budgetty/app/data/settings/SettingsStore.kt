package com.budgetty.app.data.settings

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Persists the user's app-customization settings (theme, accent, currency, date format). */
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
        historySort = prefs.getString(KEY_HISTORY_SORT, "NEWEST") ?: "NEWEST",
        recentSearches = prefs.getString(KEY_RECENT_SEARCHES, null).toLines(),
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

    /** Remembers the History sort order (a SortOrder name) for the next launch. */
    fun setHistorySort(name: String) =
        saveString(KEY_HISTORY_SORT, name) { it.copy(historySort = name) }

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
        const val KEY_PERIOD_UNIT_INSIGHTS = "insights_period_unit"
        const val KEY_HISTORY_SORT = "history_sort"
        const val KEY_RECENT_SEARCHES = "recent_searches"
        const val MAX_RECENT_SEARCHES = 6
    }
}
