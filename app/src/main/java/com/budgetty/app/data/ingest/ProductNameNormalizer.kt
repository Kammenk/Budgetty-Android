package com.budgetty.app.data.ingest

/**
 * Tidies a receipt line-item name for display.
 *
 * Some stores print every product in ALL CAPS ("ПЪЛНОЗ. ХЛЯБ", "FRESH MILK"). Shouting reads
 * badly in the app, so when a name has letters and not one of them is lower case we fold it to
 * sentence case — first letter upper, the rest lower ("Пълноз. хляб", "Fresh milk"). A name that
 * is already mixed case (the store cased it itself) is left exactly as-is.
 *
 * Pure Kotlin (no Android deps) so it can be unit-tested on the host, and applied at the ingest
 * boundary by the [ReceiptExtractor]s so the review screen and everything saved use the tidy name.
 */
object ProductNameNormalizer {

    /** [name] in sentence case when every letter is upper case; otherwise [name] unchanged. */
    fun normalize(name: String): String {
        val letters = name.filter(Char::isLetter)
        if (letters.isEmpty() || letters.any(Char::isLowerCase)) return name
        return name.lowercase().replaceFirstChar(Char::uppercase)
    }
}
