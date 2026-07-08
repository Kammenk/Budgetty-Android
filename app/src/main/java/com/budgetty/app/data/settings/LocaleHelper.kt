package com.budgetty.app.data.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the user's chosen [Language] as a per-app locale override.
 *
 * Read directly from SharedPreferences (not Koin) because it runs in [android.app.Activity.attachBaseContext],
 * before dependency injection is available. A null/blank tag (the SYSTEM option) leaves the system
 * locale untouched. We build the [Locale] with the single-argument constructor so legacy language
 * codes are normalized to the Android resource qualifier (e.g. "id" → "in" for Indonesian).
 */
object LocaleHelper {

    /** Wraps [base] with the saved language override, or returns it unchanged for SYSTEM. */
    fun applyOverride(base: Context): Context {
        val locale = savedLocale(base) ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** The persisted language tag (e.g. "es"), or null when following the system locale. */
    fun savedTag(context: Context): String? = savedLanguage(context)?.tag

    // Locale(String) is intentional: it applies the legacy-code mapping (e.g. "id" → "in") that the
    // Android resource framework uses for folder qualifiers, which Locale.forLanguageTag does not.
    @Suppress("DEPRECATION")
    private fun savedLocale(context: Context): Locale? =
        savedLanguage(context)?.tag?.let { Locale(it) }

    private fun savedLanguage(context: Context): Language? {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("language", null) ?: return null
        return runCatching { Language.valueOf(name) }.getOrNull()
    }
}
