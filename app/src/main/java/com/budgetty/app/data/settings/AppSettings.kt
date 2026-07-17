package com.budgetty.app.data.settings

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Accent color theme. Non-DEFAULT options are premium. */
enum class AccentTheme(val label: String) {
    DEFAULT("Violet (default)"),
    SAGE("Sage"),
    OCEAN("Ocean"),
    PLUM("Plum"),
}

/**
 * Currencies for the Europe-only release. [symbol] is appended after the amount. Trimmed from a
 * global set on 2026-07-16: kept EUR/GBP/CHF/SEK/NOK and added the home currencies of the other
 * supported markets (DKK/PLN/CZK/RON) so every kept [Language]'s country can pick its own. Bulgaria
 * uses EUR (eurozone since 2026). A removed currency saved by an existing user falls back to EUR.
 */
enum class Currency(val code: String, val symbol: String) {
    EUR("EUR", "€"),
    GBP("GBP", "£"),
    CHF("CHF", "CHF"),
    SEK("SEK", "kr"),
    NOK("NOK", "kr"),
    DKK("DKK", "kr"),
    PLN("PLN", "zł"),
    CZK("CZK", "Kč"),
    RON("RON", "lei"),
}

/**
 * User-selectable date format. [pattern] is the full date (with year) used wherever a date shows
 * standalone; [dayMonthPattern] is the year-less short form for dense contexts (History day
 * headers, upload/recurring rows) so the day/month ORDER still follows the user's choice.
 */
enum class DateFormatOption(
    val sample: String,
    val pattern: String,
    val dayMonthPattern: String,
) {
    DAY_MONTH_YEAR("5 Jun 2026", "d MMM yyyy", "d MMM"),
    DMY_SLASH("05/06/2026", "dd/MM/yyyy", "dd/MM"),
    MDY_SLASH("06/05/2026", "MM/dd/yyyy", "MM/dd"),
    ISO("2026-06-05", "yyyy-MM-dd", "MM-dd"),
}

/**
 * The 16 languages offered in the Europe-only release, plus a "System default" option. [label] is
 * the language's own name (autonym) so users can find theirs regardless of the current UI language.
 * [tag] is the locale applied app-wide when selected (null = follow the system locale); it matches
 * the `res/values-<tag>/` qualifier (Norwegian Bokmål = "nb"). Languages dropped for the Europe
 * release keep their translations under `archived-locales/` at the repo root for future re-add.
 */
enum class Language(val label: String, val tag: String?) {
    SYSTEM("System default", null),
    ENGLISH("English", "en"),
    SPANISH("Español", "es"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    ITALIAN("Italiano", "it"),
    PORTUGUESE("Português", "pt"),
    RUSSIAN("Русский", "ru"),
    SWEDISH("Svenska", "sv"),
    DUTCH("Nederlands", "nl"),
    NORWEGIAN("Norsk", "nb"),
    DANISH("Dansk", "da"),
    FINNISH("Suomi", "fi"),
    POLISH("Polski", "pl"),
    CZECH("Čeština", "cs"),
    BULGARIAN("Български", "bg"),
    ROMANIAN("Română", "ro"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.DEFAULT,
    val currency: Currency = Currency.EUR,
    val dateFormat: DateFormatOption = DateFormatOption.DAY_MONTH_YEAR,
    val language: Language = Language.SYSTEM,
    /** True once the user has finished (or skipped) the first-launch onboarding carousel. */
    val onboardingSeen: Boolean = false,
    /**
     * True from a successful sign-up until the one-time Insights setup quiz is finished or skipped;
     * keeps the quiz gate up between login and the main app (surviving process death mid-quiz).
     */
    val insightsQuizPending: Boolean = false,
    /** User-set display name; blank falls back to a name derived from the email. */
    val displayName: String = "",
    /** Stable keys of Home sections the user has hidden via the Home customization menu (phone). */
    val hiddenHomeSections: Set<String> = emptySet(),
    /** Stable keys of Insights sections hidden via the customization menu (phone + tablet) or the setup quiz. */
    val hiddenInsightsSections: Set<String> = emptySet(),
    /** User-chosen display order of Home section keys; empty falls back to the default enum order. */
    val homeSectionOrder: List<String> = emptyList(),
    /** User-chosen display order of Insights section keys; empty falls back to the default enum order. */
    val insightsSectionOrder: List<String> = emptyList(),
    /** Remembered Insights period-stepper unit (a PeriodUnit name); seeds the default window. */
    val insightsPeriodUnit: String = "MONTH",
    /** Remembered History sort order (a SortOrder name); defaults to newest-first. */
    val historySort: String = "NEWEST",
    /** Recent History search terms, most-recent first (capped); powers the search quick-find. */
    val recentSearches: List<String> = emptyList(),
)
