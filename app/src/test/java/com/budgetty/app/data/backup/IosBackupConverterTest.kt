package com.budgetty.app.data.backup

import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.local.RecurringEntity
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/**
 * Edge cases of the pure iOS → Android mapping: format detection, date/number parsing, and the
 * rule that an iOS value with no Android equivalent becomes null (or the documented default) rather
 * than being passed through raw. The end-to-end restore is covered by [IosBackupImportTest].
 */
class IosBackupConverterTest {

    @Test
    fun `detects iOS files by marker or by shape, never an Android export`() {
        assertThat(isIos("""{"app":"Budgetty iOS","receipts":[]}""")).isTrue()
        // No marker, but receipts nest items and there is no Android transactions list.
        assertThat(isIos("""{"receipts":[{"items":[]}]}""")).isTrue()
        assertThat(isIos(Gson().toJson(BackupData()))).isFalse()
        assertThat(isIos("""{"transactions":[],"receipts":[{"items":[]}]}""")).isFalse()
        assertThat(isIos("""{"receipts":[]}""")).isFalse()
        assertThat(isIos("""{"hello":"world"}""")).isFalse()
        assertThat(isIos("[]")).isFalse()
    }

    @Test
    fun `accepts fractional seconds and explicit offsets in dates`() {
        val data = convert(
            """{"app":"Budgetty iOS","receipts":[
                {"createdAt":"2026-10-03T18:45:12.250Z","date":"2026-10-03T02:00:00+02:00","items":[]}
            ]}""",
        )
        val receipt = data.receipts.single()
        assertThat(receipt.timestamp).isEqualTo(Instant.parse("2026-10-03T18:45:12.250Z").toEpochMilli())
        assertThat(receipt.date).isEqualTo(Instant.parse("2026-10-03T00:00:00Z").toEpochMilli())
        // Money fields iOS always writes but a hand-trimmed file might not: zero, as on Android.
        assertThat(receipt.discount.signum()).isEqualTo(0)
        assertThat(receipt.tax.signum()).isEqualTo(0)
    }

    @Test
    fun `receipts created in the same millisecond get distinct ids`() {
        val receipt = """{"createdAt":"2026-10-01T09:30:00Z","date":"2026-10-01T00:00:00Z","store":"S",
            "items":[{"name":"x","price":1,"quantity":1,"category":"Fuel"}]}"""
        val data = convert("""{"app":"Budgetty iOS","receipts":[$receipt,$receipt,$receipt]}""")

        val base = Instant.parse("2026-10-01T09:30:00Z").toEpochMilli()
        assertThat(data.receipts.map { it.timestamp }).containsExactly(base, base + 1, base + 2).inOrder()
        assertThat(data.transactions.map { it.receiptId }).containsExactly(base, base + 1, base + 2).inOrder()
        assertThat(data.transactions.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `a missing required date or amount fails the conversion`() {
        assertThrows(IllegalArgumentException::class.java) {
            convert("""{"app":"Budgetty iOS","receipts":[{"createdAt":"2026-10-01T09:30:00Z","items":[]}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            convert(
                """{"app":"Budgetty iOS","receipts":[{"createdAt":"2026-10-01T09:30:00Z",
                    "date":"2026-10-01T00:00:00Z","items":[{"name":"x","quantity":1,"category":"Fuel"}]}]}""",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            convert("""{"app":"Budgetty iOS","receipts":[{"createdAt":"yesterday","date":"today","items":[]}]}""")
        }
    }

    @Test
    fun `unknown enum values fall back to the documented defaults`() {
        val data = convert(
            """{"app":"Budgetty iOS",
                "recurring":[{"label":"x","amount":5,"isIncome":false,"category":"","cadenceRaw":"BIWEEKLY",
                    "dueDay":3,"createdAt":"2026-08-01T08:00:00Z","active":true}],
                "buyingLimits":[{"emoji":"","label":"","keywords":[" Coke ","coke",""],"timeframeRaw":"DAILY",
                    "count":0,"createdAt":"2026-08-01T08:00:00Z"}],
                "categories":[{"name":"Mine","colorArgb":4278190335,"icon":"x","createdAt":"2026-08-01T08:00:00Z",
                    "bucket":"MAYBE"}]}""",
        )
        assertThat(data.recurring.single().cadence).isEqualTo(RecurringEntity.Cadence.MONTHLY)
        val limit = data.buyingLimits.single()
        assertThat(limit.timeframe).isEqualTo(BuyingLimitTimeframe.MONTHLY)
        assertThat(limit.keywords).isEqualTo("coke") // normalized + de-duplicated, as Android stores them
        assertThat(limit.count).isEqualTo(1)
        val mine = data.categories.single { it.name == "Mine" }
        assertThat(mine.bucket).isNull()
        assertThat(mine.colorArgb).isEqualTo(0xFF0000FF.toInt()) // iOS's 64-bit packed ARGB, narrowed
    }

    @Test
    fun `settings with no Android equivalent become null, never raw iOS values`() {
        val settings = convert(
            """{"app":"Budgetty iOS","settings":{"currency":"USD","dateFormat":"system","language":"xx",
                "themeMode":"sepia","accent":"neon","monthStartDay":0,"budgetCadence":"","fortnightAnchor":0,
                "recapFrequency":"DAILY","hiddenHomeSections":["weekComparison"],"hiddenInsightsSections":[]}}""",
        ).settings!!

        assertThat(settings.currency).isNull()
        assertThat(settings.dateFormat).isNull()
        assertThat(settings.language).isNull()
        assertThat(settings.themeMode).isNull()
        assertThat(settings.accent).isNull()
        assertThat(settings.monthStartDay).isNull()
        assertThat(settings.budgetCadence).isNull()
        assertThat(settings.fortnightAnchorEpochDay).isNull()
        assertThat(settings.recapFrequency).isNull()
        // Present but nothing maps: keep the device's layout rather than wipe it.
        assertThat(settings.hiddenHomeSections).isNull()
        assertThat(settings.homeSectionOrder).isNull() // absent: keep the device's
        assertThat(settings.hiddenInsightsSections).isEmpty() // an empty list is a real "nothing hidden"
    }

    @Test
    fun `settings already in Android vocabulary are accepted`() {
        val settings = convert(
            """{"app":"Budgetty iOS","settings":{"dateFormat":"ISO","language":"FRENCH","themeMode":"LIGHT",
                "accent":"SAGE","insightsSectionOrder":["top_stores","topCategories"]}}""",
        ).settings!!

        assertThat(settings.dateFormat).isEqualTo("ISO")
        assertThat(settings.language).isEqualTo("FRENCH")
        assertThat(settings.themeMode).isEqualTo("LIGHT")
        assertThat(settings.accent).isEqualTo("SAGE")
        assertThat(settings.insightsSectionOrder).containsExactly("top_stores", "top_categories").inOrder()
    }

    @Test
    fun `iOS system language and default accent map to their Android counterparts`() {
        val settings = convert("""{"app":"Budgetty iOS","settings":{"language":"system","accent":"violet"}}""")
            .settings!!

        assertThat(settings.language).isEqualTo("SYSTEM")
        assertThat(settings.accent).isEqualTo("DEFAULT")
    }

    @Test
    fun `a warranty linked to a receipt outside the file keeps its instant in millis`() {
        val data = convert(
            """{"app":"Budgetty iOS","warranties":[{"name":"TV","purchaseDate":"2026-01-01T00:00:00Z",
                "durationMonths":24,"receiptId":1767225600.5,"createdAt":"2026-01-01T00:00:00Z"}]}""",
        )
        assertThat(data.warranties.single().receiptId).isEqualTo(1_767_225_600_500L)
    }

    private fun isIos(json: String) = IosBackupConverter.isIosBackup(JsonParser.parseString(json))

    private fun convert(json: String): BackupData =
        IosBackupConverter.convert(JsonParser.parseString(json) as JsonObject)
}
