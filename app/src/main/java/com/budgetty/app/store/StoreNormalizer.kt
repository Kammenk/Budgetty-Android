package com.budgetty.app.store

/**
 * Collapses raw receipt store names to a single canonical brand.
 *
 * The same chain prints under different branch/legal names — "Kaufland Lyulin" and
 * "КАУФЛАНД БЪЛГАРИЯ ЕООД" are both Kaufland. Insights groups spend by store, so without this
 * the one chain shows up as several rows. We look for a curated set of brand keywords (Latin and
 * Cyrillic) in the name; anything unrecognised falls through unchanged (just trimmed and
 * whitespace-collapsed), so unknown stores still display and group by their own name.
 *
 * Pure Kotlin (no Android deps) so it can be unit-tested on the host. Add chains as needed.
 */
object StoreNormalizer {

    private class Brand(val canonical: String, vararg val aliases: String)

    // Aliases must be lowercase. A single-word alias matches a whole word only (so a short one
    // like "dm" can't hit inside another word); an alias containing a space or hyphen is matched
    // as a substring (for multi-word brands like "t market").
    private val brands = listOf(
        Brand("Kaufland", "kaufland", "кауфланд" ,"каулланд"),
        Brand("Lidl", "lidl", "лидл"),
        Brand("Billa", "billa", "била"),
        Brand("Fantastico", "fantastico", "фантастико"),
        Brand("T-Market", "t-market", "t market", "тмаркет", "т маркет"),
        Brand("Metro", "metro", "метро"),
        Brand("Praktiker", "praktiker", "практикер"),
        Brand("Technopolis", "technopolis", "технополис"),
        Brand("Technomarket", "technomarket", "техномаркет"),
        Brand("Lilly", "lilly", "лили"),
        Brand("dm", "dm", "дм"),
        Brand("OMV", "omv"),
        Brand("Shell", "shell", "шел"),
        Brand("Lukoil", "lukoil", "лукойл"),
    )

    private val tokenSplit = Regex("[^\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")

    /** The canonical brand for [raw], or [raw] trimmed/space-collapsed when no brand matches. */
    fun normalize(raw: String): String {
        val cleaned = raw.trim().replace(whitespace, " ")
        if (cleaned.isEmpty()) return cleaned
        val lower = cleaned.lowercase()
        val tokens = lower.split(tokenSplit).filterTo(HashSet()) { it.isNotEmpty() }
        for (brand in brands) {
            val matched = brand.aliases.any { alias ->
                if (alias.any { it == ' ' || it == '-' }) lower.contains(alias) else alias in tokens
            }
            if (matched) return brand.canonical
        }
        return cleaned
    }
}
