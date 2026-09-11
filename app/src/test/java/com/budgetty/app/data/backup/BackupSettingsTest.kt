package com.budgetty.app.data.backup

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

/**
 * Guards the preferences block added to the JSON backup: it must round-trip losslessly, a backup
 * written before the block existed must still decode (settings == null, so the current on-device
 * prefs are kept), and — most importantly — the block must never grow to carry security, consent or
 * transient-gate state into a shareable file.
 */
class BackupSettingsTest {

    private val gson = Gson()

    @Test
    fun `settings block round-trips through json`() {
        val settings = BackupSettings(
            currency = "GBP",
            dateFormat = "ISO",
            language = "FRENCH",
            themeMode = "DARK",
            accent = "OCEAN",
            monthStartDay = 25,
            budgetRolloverEnabled = true,
            hiddenHomeSections = listOf("wellbeing", "streak"),
            hiddenInsightsSections = listOf("trend"),
            homeSectionOrder = listOf("safeToSpend", "budget"),
            insightsSectionOrder = listOf("breakdown", "trend"),
            customInsightsSections = listOf("breakdown", "top_categories"),
            recapEnabled = false,
            recapFrequency = "MONTHLY",
        )
        val restored = gson.fromJson(gson.toJson(BackupData(settings = settings)), BackupData::class.java)
        assertThat(restored.settings).isEqualTo(settings)
    }

    @Test
    fun `a backup written before the settings block decodes with null settings`() {
        // Valid pre-settings backup: real data, no "settings" key. Gson leaves the absent field null,
        // which BackupManager treats as "keep the current device preferences".
        val legacyJson = """{"transactions":[],"categories":[],"budgets":[]}"""
        val restored = gson.fromJson(legacyJson, BackupData::class.java)
        assertThat(restored.settings).isNull()
    }

    @Test
    fun `settings block carries no security, consent or transient fields`() {
        // The whole point of a curated block: a PIN hash, a consent flag, or a one-time gate must
        // never ride along in a backup the user can share. This fails loudly if BackupSettings is
        // ever widened to include something it should not.
        val allowed = setOf(
            "currency", "dateFormat", "language", "themeMode", "accent",
            "monthStartDay", "budgetRolloverEnabled",
            "hiddenHomeSections", "hiddenInsightsSections", "homeSectionOrder", "insightsSectionOrder",
            "customInsightsSections",
            "recapEnabled", "recapFrequency",
        )
        val actual = BackupSettings::class.java.declaredFields
            .map { it.name }
            .filterNot { it.startsWith("$") } // ignore any synthetic fields the compiler adds
            .toSet()
        assertThat(actual).isEqualTo(allowed)
    }
}
