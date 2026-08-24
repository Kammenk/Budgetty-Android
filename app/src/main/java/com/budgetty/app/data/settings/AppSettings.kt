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
    /**
     * When true, Insights overlays planned recurring bills as a distinct "planned" (hatched) layer on
     * top of actual receipt spend in Breakdown, Summary and Trend — so Insights and Home stop
     * disagreeing about the month. Off by default, remembered per user; a presentation-only overlay
     * (no schema change) that reads the same [com.budgetty.app.ui.util.windowAmount] projection the
     * money-flow cards already use. See [com.budgetty.app.ui.insights.PlannedOverlay].
     */
    val insightsIncludeRecurringBills: Boolean = false,
    /**
     * Whether the one-time "Insights and Home disagree — overlay planned bills?" discovery nudge above
     * Breakdown has been dismissed. The overlay switch is off-by-default and lives in Customize (behind
     * the header ⋮), so this nudge is the one thing that surfaces the feature; shown once per user until
     * dismissed or the overlay is turned on.
     */
    val insightsOverlayNudgeDismissed: Boolean = false,
    /**
     * Day of the month the user's financial "month" starts on — their pay day (1–31). 1 is the
     * ordinary calendar month; any other value shifts "this month"/"last month", the monthly budget
     * and the Insights month stepper to run from this day (clamped to a short month's last day).
     */
    val monthStartDay: Int = 1,
    /**
     * When true, unspent budget carries into the next period (opt-in, default off). Applies to the
     * overall monthly budget and each category budget; overspend is forgiven (never rolls negative).
     */
    val budgetRolloverEnabled: Boolean = false,
    /** Remembered History sort order (a SortOrder name); defaults to newest-first. */
    val historySort: String = "NEWEST",
    /** Recent History search terms, most-recent first (capped); powers the search quick-find. */
    val recentSearches: List<String> = emptyList(),
    /** Whether Crashlytics crash collection is on. Default-on with an opt-out toggle in Account. */
    val crashReportingEnabled: Boolean = true,
    // ── App lock ──
    /** Whether the PIN / biometric lock gate is on. */
    val appLockEnabled: Boolean = false,
    /** Salted PIN hash ("saltHex:hashHex"); empty = no PIN set. */
    val pinHash: String = "",
    /** Whether biometric unlock is on (only meaningful with a PIN + enrolled hardware). */
    val biometricEnabled: Boolean = false,
    /** Idle minutes before re-locking on resume; 0 = immediately. Cold start always locks. */
    val autoLockMinutes: Int = 1,
    /**
     * Wellbeing tips the user has dismissed, each stored as "periodId|tipId" so a dismissal is scoped
     * to its pay-cycle month — the tip resurfaces next month if it still qualifies. The only new
     * persistence the Wellbeing feature adds.
     */
    val dismissedWellbeingTips: Set<String> = emptySet(),
)
