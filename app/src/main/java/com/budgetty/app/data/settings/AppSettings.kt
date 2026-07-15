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

/** Top global currencies. [symbol] is appended after the amount. */
enum class Currency(val code: String, val symbol: String) {
    EUR("EUR", "€"),
    USD("USD", "$"),
    GBP("GBP", "£"),
    JPY("JPY", "¥"),
    CNY("CNY", "¥"),
    AUD("AUD", "A$"),
    CAD("CAD", "C$"),
    CHF("CHF", "CHF"),
    HKD("HKD", "HK$"),
    SGD("SGD", "S$"),
    INR("INR", "₹"),
    KRW("KRW", "₩"),
    SEK("SEK", "kr"),
    NOK("NOK", "kr"),
    NZD("NZD", "NZ$"),
    MXN("MXN", "MX$"),
    BRL("BRL", "R$"),
    ZAR("ZAR", "R"),
    TRY("TRY", "₺"),
}

enum class DateFormatOption(val sample: String, val pattern: String) {
    DAY_MONTH_YEAR("5 Jun 2026", "d MMM yyyy"),
    DMY_SLASH("05/06/2026", "dd/MM/yyyy"),
    MDY_SLASH("06/05/2026", "MM/dd/yyyy"),
    ISO("2026-06-05", "yyyy-MM-dd"),
}

/**
 * Top 20 world languages plus a "System default" option. [label] is the language's own name
 * (autonym) so users can find theirs regardless of the current UI language. [tag] is the locale
 * applied app-wide when selected (null = follow the system locale). Folder names in `res/` use the
 * Android resource qualifier, which is "in" for Indonesian (legacy code).
 */
enum class Language(val label: String, val tag: String?) {
    SYSTEM("System default", null),
    ENGLISH("English", "en"),
    CHINESE("中文", "zh"),
    HINDI("हिन्दी", "hi"),
    SPANISH("Español", "es"),
    FRENCH("Français", "fr"),
    ARABIC("العربية", "ar"),
    BENGALI("বাংলা", "bn"),
    PORTUGUESE("Português", "pt"),
    RUSSIAN("Русский", "ru"),
    URDU("اردو", "ur"),
    INDONESIAN("Indonesia", "id"),
    GERMAN("Deutsch", "de"),
    JAPANESE("日本語", "ja"),
    TURKISH("Türkçe", "tr"),
    KOREAN("한국어", "ko"),
    ITALIAN("Italiano", "it"),
    VIETNAMESE("Tiếng Việt", "vi"),
    POLISH("Polski", "pl"),
    UKRAINIAN("Українська", "uk"),
    DUTCH("Nederlands", "nl"),
    BULGARIAN("Български", "bg"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.DEFAULT,
    val currency: Currency = Currency.EUR,
    val dateFormat: DateFormatOption = DateFormatOption.DAY_MONTH_YEAR,
    val language: Language = Language.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val analyticsEnabled: Boolean = true,
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
