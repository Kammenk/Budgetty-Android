package com.budgetty.app.category

import kotlin.math.abs
import kotlin.math.min

/**
 * Spending categories: a two-level taxonomy where BOTH levels are selectable. A category is
 * either a top-level group ([Predefined.parent] == null) or a sub-category that belongs to a
 * group. Haiku assigns the most specific fit; a user may also pick just the umbrella group.
 *
 * Each category has an emoji icon and an ARGB color. Colors are plain ints (no Compose/Android
 * dependency) so this can be seeded straight into the database.
 */
object Categories {

    /** A selectable category: a group ([parent] == null) or a sub-category of [parent]. */
    data class Predefined(
        val name: String,
        val emoji: String,
        val colorArgb: Int,
        val parent: String?,
    )

    /** Fallback category used when none is chosen. */
    const val DEFAULT = "Groceries"

    /** The catch-all category a custom category's transactions fall back to when it is deleted. */
    const val OTHER = "Other"

    /** Combined delivery + service + bag/booking fees — a scanned-receipt add-on the extractor
     *  materializes as its own line item (see HaikuReceiptExtractor), not a product the model picks. */
    const val DELIVERY = "Delivery"

    /** Gratuity — a scanned-receipt add-on materialized as its own line item, kept apart from fees. */
    const val TIPS = "Tips"

    /** How many custom categories a user may create: [FREE_CUSTOM_LIMIT] on the free tier, and
     *  effectively unlimited ([MAX_CUSTOM_LIMIT]) with Premium. */
    const val FREE_CUSTOM_LIMIT = 3
    const val MAX_CUSTOM_LIMIT = Int.MAX_VALUE

    private data class Def(val name: String, val emoji: String, val parent: String?)

    private val defs: List<Def> = listOf(
        // 🧺 Groceries — basket reads clearer than the cart on the green tile
        Def("Groceries", "🧺", null),
        Def("Bakery", "🥖", "Groceries"),
        Def("Dairy", "🧀", "Groceries"),
        Def("Meat & Poultry", "🍗", "Groceries"),
        Def("Fish & Seafood", "🐟", "Groceries"),
        Def("Fruits & Vegetables", "🥬", "Groceries"),
        Def("Snacks & Sweets", "🍫", "Groceries"),
        Def("Frozen Foods", "🧊", "Groceries"),
        Def("Nuts & Snacks", "🥜", "Groceries"),
        Def("Canned & Preserved", "🥫", "Groceries"),
        Def("Grains & Pasta", "🍝", "Groceries"),
        Def("Condiments & Sauces", "🧂", "Groceries"),
        Def("Beverages", "🥤", "Groceries"),
        // 🏠 Household & Personal
        Def("Household & Personal", "🏠", null),
        Def("Household Cleaning", "🧼", "Household & Personal"),
        Def("Personal Care", "🧴", "Household & Personal"),
        Def("Beauty", "💇", "Household & Personal"),
        Def("Baby Products", "🍼", "Household & Personal"),
        Def("Pet Supplies", "🐾", "Household & Personal"),
        Def("Paper Products", "📄", "Household & Personal"),
        Def("Kitchen Supplies", "🍽️", "Household & Personal"),
        // ❤️ Health & Wellness
        Def("Health & Wellness", "❤️", null),
        Def("Health & Pharmacy", "💊", "Health & Wellness"),
        Def("Medical", "🩺", "Health & Wellness"),
        Def("Sports & Fitness", "💪", "Health & Wellness"),
        // 🍽️ Dining & Entertainment
        Def("Dining & Entertainment", "🍽️", null),
        Def("Restaurant & Dining", "🍴", "Dining & Entertainment"),
        Def("Entertainment", "🎬", "Dining & Entertainment"),
        // 🛍️ Shopping & Lifestyle
        Def("Shopping & Lifestyle", "🛍️", null),
        Def("Clothing & Accessories", "👕", "Shopping & Lifestyle"),
        Def("Electronics", "💻", "Shopping & Lifestyle"),
        Def("Garden & Plants", "🪴", "Shopping & Lifestyle"),
        Def("Home Improvement", "🛠️", "Shopping & Lifestyle"),
        Def("Tobacco & Alcohol", "🍷", "Shopping & Lifestyle"),
        // 🚗 Transportation
        Def("Transportation", "🚗", null),
        Def("Fuel", "⛽", "Transportation"),
        Def("Car Maintenance", "🔧", "Transportation"),
        // 📋 Services & Subscriptions
        Def("Services & Subscriptions", "📋", null),
        Def("Subscriptions & Services", "🔁", "Services & Subscriptions"),
        Def("Education", "🎓", "Services & Subscriptions"),
        Def("Travel & Accommodation", "🧳", "Services & Subscriptions"),
        Def("Insurance & Utilities", "🛡️", "Services & Subscriptions"),
        Def("Rent", "🔑", "Services & Subscriptions"),
        Def("Office & Work Supplies", "🗂️", "Services & Subscriptions"),
        Def("Gifts & Charitable Donations", "🎁", "Services & Subscriptions"),
        // Appended below their group headers on purpose (Video Games → Dining, Investments → Services):
        // sub-category hues are assigned by walking `defs` in order (farthest-point), so inserting these
        // mid-list would recolor every sub after them. Appending leaves all existing colors untouched;
        // `children()` filters by parent, so they still render inside their own group.
        Def("Video Games", "🎮", "Dining & Entertainment"),
        Def("Investments", "📈", "Services & Subscriptions"),
        // 🪙 Gratuity on a delivery/restaurant order — its own line, kept apart from the food.
        Def("Tips", "🪙", "Dining & Entertainment"),
        // 🛵 Combined delivery + service + bag/booking fees on a delivery-app order.
        Def("Delivery", "🛵", "Other"),
        // 📦 Catch-all
        Def("Other", "📦", null),
    )

    /** Shared muted tone for the generated sub-category colors: moderate saturation + medium-high
     *  value so they read as deep yet soft, matching the design pie's family. */
    private const val BASE_SAT = 0.53f
    private const val BASE_VAL = 0.75f

    /**
     * Exact top-level group colors, taken straight from the design's Insights pie (the mockup source
     * of truth): green Groceries, rose Household, teal Health, terracotta Dining, amber
     * Transportation, plus a matching violet Shopping and blue Services for the two groups the pie
     * sample doesn't show. Sub-categories are spread across distinct hues around these (see
     * [predefined]); "Other" is the pie's neutral grey ([OTHER_COLOR]).
     */
    private val groupColor: Map<String, Int> = mapOf(
        "Groceries" to 0xFF4FA85A.toInt(),
        "Household & Personal" to 0xFFC77DB0.toInt(),
        "Health & Wellness" to 0xFF5BB6A6.toInt(),
        "Dining & Entertainment" to 0xFFE0795B.toInt(),
        "Shopping & Lifestyle" to 0xFFAE72CC.toInt(),
        "Transportation" to 0xFFD08A4A.toInt(),
        "Services & Subscriptions" to 0xFF588AC7.toInt(),
    )

    /** Neutral grey for the catch-all "Other" category (design pie value). */
    private val OTHER_COLOR: Int = 0xFF9A93A6.toInt()

    /**
     * Every selectable category (groups + sub-categories + Other), in display order. Each top-level
     * group keeps its exact design [groupColor]; every sub-category is then placed at the hue sitting
     * farthest from all already-assigned hues, so no two categories collide and the whole set spreads
     * as widely as possible around the wheel. "Other" is a neutral grey.
     */
    val predefined: List<Predefined> = run {
        // Farthest-point sampling: seed with the group hues (extracted from their pinned design
        // colors), then drop each sub-category into the widest remaining gap (scanned on a fine grid).
        // This guarantees unique, well-separated hues while the groups keep their exact design colors.
        val usedHues = groupColor.values.map { hueOf(it) }.toMutableList()
        val subHue = HashMap<String, Float>()
        defs.forEach { d ->
            if (d.parent != null) {
                var bestHue = 0f
                var bestDist = -1f
                var h = 0f
                while (h < 360f) {
                    val dist = usedHues.minOf { hueDistance(h, it) }
                    if (dist > bestDist) {
                        bestDist = dist
                        bestHue = h
                    }
                    h += 0.5f
                }
                subHue[d.name] = bestHue
                usedHues.add(bestHue)
            }
        }
        defs.map { d ->
            val color = when {
                d.name == "Other" -> OTHER_COLOR
                d.parent == null -> groupColor.getValue(d.name)
                else -> hsvColor(subHue.getValue(d.name))
            }
            Predefined(d.name, d.emoji, color, d.parent)
        }
    }

    /** Top-level categories (the groups + Other), in display order. */
    val groups: List<Predefined> = predefined.filter { it.parent == null }

    /** Sub-categories belonging to [group], in display order. */
    fun children(group: String): List<Predefined> = predefined.filter { it.parent == group }

    /**
     * The top-level group [name] rolls up into (case-insensitive): a sub-category returns its parent
     * group, while a group, "Other", a custom, or an unknown category returns [name] unchanged. Used
     * to collapse the Insights breakdown from every category down to the top-level groups.
     */
    fun groupOf(name: String): String =
        predefined.firstOrNull { it.name.equals(name, ignoreCase = true) }?.parent ?: name

    /**
     * The colors offered when a user creates a custom category — the app's own category-color
     * family (muted, deep tones, several shared with the predefined groups), so custom categories
     * blend in with the built-ins rather than introducing new hues.
     */
    val palette: List<Int> = listOf(
        0xFFC65B5B, 0xFF4FA85A, 0xFF8B6CC4, 0xFFC8A44A, 0xFF4AA3C7, 0xFFC05E8A,
        0xFF73B647, 0xFF6B7BC4, 0xFFC8793A, 0xFF5BB6A6, 0xFFA060C0, 0xFFC77DB0,
    ).map { it.toInt() }

    /** Default color for a brand-new custom category before the user picks one. */
    val defaultColor: Int = palette.first()

    /**
     * The emoji offered in the custom-category icon grid: the distinct icons already used by the
     * predefined categories, so custom categories reuse the app's existing emoji vocabulary.
     */
    val iconChoices: List<String> = predefined.map { it.emoji }.distinct()

    /**
     * User-created categories, cached at process scope so [colorOf] and [emojiOf] resolve them
     * everywhere a category is rendered (transaction rows, charts, history, widgets) — not just in
     * the picker, which reads them straight from the database. Kept keyed by lower-cased name so
     * lookups fold the same way names are matched elsewhere; refreshed from the categories table
     * (see BudgettyApplication). Volatile because it is written from a background collector and read
     * from the UI thread.
     */
    @Volatile
    private var customByName: Map<String, Predefined> = emptyMap()

    /** Replaces the custom-category cache. [items] = (name, emoji, colorArgb) for user categories. */
    fun setCustomCategories(items: List<Triple<String, String, Int>>) {
        customByName = items.associate { (name, emoji, color) ->
            name.lowercase() to Predefined(name, emoji, color, parent = null)
        }
    }

    /** The canonical color for [name] (case-insensitive) — predefined, then custom, else [defaultColor]. */
    fun colorOf(name: String): Int =
        predefined.firstOrNull { it.name.equals(name, ignoreCase = true) }?.colorArgb
            ?: customByName[name.lowercase()]?.colorArgb
            ?: defaultColor

    /** The emoji icon for [name] (case-insensitive) — predefined, then custom, else empty. */
    fun emojiOf(name: String): String =
        predefined.firstOrNull { it.name.equals(name, ignoreCase = true) }?.emoji
            ?: customByName[name.lowercase()]?.emoji
            ?: ""

    /**
     * True if [name] is a built-in category — a group, sub-category, or "Other" (case-insensitive).
     * Lets the custom-category duplicate check keep predefined names reserved while treating an
     * orphaned/downgraded ("ghost") non-predefined row as a name a new custom may reclaim.
     */
    fun isPredefined(name: String): Boolean =
        predefined.any { it.name.equals(name, ignoreCase = true) }

    /** Shortest distance in degrees (0..180) between two hues around the color wheel. */
    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return min(d, 360f - d)
    }

    /** Hue in degrees (0..360) of an ARGB color, used to seed the sub-category hue spread from the
     *  pinned group colors. Grey/neutral colors (zero chroma) return 0. */
    private fun hueOf(argb: Int): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val chroma = max - minOf(r, g, b)
        if (chroma == 0f) return 0f
        val h = when (max) {
            r -> ((g - b) / chroma) % 6f
            g -> (b - r) / chroma + 2f
            else -> (r - g) / chroma + 4f
        }
        return (h * 60f % 360f + 360f) % 360f
    }

    /**
     * HSV → ARGB, kept in pure Kotlin so this stays Android-free. Fully opaque, with a muted
     * saturation and a medium value so categories read as deep/bland rather than bright pastels.
     */
    private fun hsvColor(hue: Float, s: Float = BASE_SAT, v: Float = BASE_VAL): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
