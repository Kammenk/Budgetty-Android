package com.budgetty.app.category

/**
 * The curated pool of emoji offered when a user picks an icon for a custom category — ~220 glyphs in
 * nine themed sections, each carrying search keywords. Replaces the old "reuse the ~45 built-in
 * emoji" list ([Categories.iconChoices] now delegates here) so custom categories aren't starved for
 * icons. Every glyph is a single code point (a few with VS-16), Unicode ≤ 13.1, with no
 * ZWJ / skin-tone / gender / profession sequences so they render consistently across Android
 * versions. Kept Android-free (plain strings) so it can be unit-tested off-device.
 *
 * The list is the design's curated pool (see EMOJI_PICKER_EXPANSION_DESIGN_REQUEST); section order is
 * the grid order. Both Android and iOS should share this same vocabulary.
 */
object EmojiCatalog {

    /** One pickable icon: the [emoji] and the space-separated [keywords] it matches in search. */
    data class Entry(val emoji: String, val keywords: String)

    /** A titled group of icons, shown as a labelled section in the grid. */
    data class Section(val title: String, val entries: List<Entry>)

    /** Splits "😀 kw1 kw2 …" into an [Entry] (glyph up to the first space, keywords after). */
    private fun entries(vararg specs: String): List<Entry> = specs.map { spec ->
        val cut = spec.indexOf(' ')
        Entry(spec.substring(0, cut), spec.substring(cut + 1))
    }

    val sections: List<Section> = listOf(
        Section(
            "Food & Drink",
            entries(
                "☕ coffee cafe espresso drink", "🍵 tea matcha drink", "🧃 juice carton drink",
                "🥤 soda drink cup takeaway", "🍺 beer pub bar drink", "🍷 wine bar drink",
                "🍔 burger fastfood takeout", "🍕 pizza takeout fastfood", "🌮 taco mexican takeout",
                "🍜 noodles ramen soup", "🍝 pasta spaghetti", "🍣 sushi japanese",
                "🥗 salad healthy lunch", "🍞 bread toast bakery", "🥖 baguette bread bakery",
                "🥐 croissant pastry bakery", "🧀 cheese dairy", "🥚 eggs dairy",
                "🍗 chicken meat poultry", "🥩 steak meat beef", "🐟 fish seafood",
                "🍎 apple fruit", "🍌 banana fruit", "🍇 grapes fruit", "🥑 avocado fruit",
                "🥕 carrot vegetable veg", "🥬 greens vegetable veg", "🍫 chocolate sweets candy",
                "🍪 cookie biscuit sweets", "🍰 cake dessert sweets", "🍦 icecream dessert",
                "🥫 canned tinned pantry", "🥜 nuts snacks", "🧂 salt spices pantry",
                "🧊 ice frozen", "🍼 baby formula bottle", "🍽️ dining restaurant eatout",
                "🍴 restaurant eatout cutlery",
            ),
        ),
        Section(
            "Transport",
            entries(
                "🚗 car auto vehicle drive", "🚕 taxi cab ride car", "🚌 bus transit commute",
                "🚈 metro subway train transit", "🚆 train rail commute", "🚐 van minibus car",
                "🚲 bike bicycle cycling", "🛵 scooter moped", "🏍️ motorcycle bike",
                "⛽ fuel petrol gas car", "🅿️ parking car garage", "🎫 ticket fare pass transit",
                "🛣️ toll road motorway car", "🚦 traffic commute", "🧳 luggage travel trip",
                "✈️ flight plane travel airfare", "🚢 ferry ship cruise", "⛵ boat sailing",
                "🚁 helicopter", "🚀 rocket", "🔧 repair service garage car",
            ),
        ),
        Section(
            "Home",
            entries(
                "🏠 home house rent", "🏡 house garden home", "🏘️ housing rent mortgage",
                "🔑 keys rent deposit", "🛋️ furniture sofa living", "🛏️ bed bedroom furniture",
                "🪑 chair furniture", "🚪 door repair", "🚿 shower water", "🛁 bath bathroom",
                "🚽 toilet plumbing", "🧹 cleaning broom chores", "🧺 laundry washing",
                "🧼 soap cleaning", "🧴 lotion toiletries", "🧽 sponge cleaning",
                "💡 light bulb electricity", "🔌 power plug electricity", "⚡ energy electric utility",
                "💧 water utility", "🔥 gas heating", "🔨 diy repair tools",
                "🛠️ tools maintenance diy", "🧰 toolbox repair",
            ),
        ),
        Section(
            "Shopping",
            entries(
                "🛒 groceries shopping cart", "🛍️ shopping bags retail", "🎁 gift present",
                "📦 parcel delivery package", "🏷️ sale price tag", "🧾 receipt bill",
                "💳 card payment", "👕 clothes tshirt apparel", "👗 dress clothes apparel",
                "👖 jeans clothes", "👟 shoes sneakers trainers", "👞 shoes formal",
                "👜 handbag bag", "🎒 backpack school bag", "🧢 cap hat", "🧦 socks",
                "🧥 coat jacket", "👔 shirt work formal", "🕶️ sunglasses",
                "👓 glasses eyewear optician", "💍 jewellery ring", "⌚ watch",
                "💄 makeup cosmetics beauty",
            ),
        ),
        Section(
            "Health",
            entries(
                "💊 pharmacy medicine pills", "🩺 doctor medical checkup", "🏥 hospital clinic medical",
                "🩹 plaster firstaid", "🦷 dentist teeth dental", "😷 mask illness",
                "🌡️ fever thermometer health", "🧘 yoga wellness gym studio",
                "🏋️ gym fitness workout weights", "🏃 running jogging fitness gym",
                "🚴 cycling spin fitness gym", "🏊 swimming pool gym", "⚽ football soccer sport",
                "🏀 basketball sport", "🎾 tennis sport", "🥊 boxing gym sport",
                "💇 haircut salon barber", "💅 nails manicure salon", "🧖 spa sauna wellness",
                "❤️ health wellbeing love",
            ),
        ),
        Section(
            "Leisure & Hobbies",
            entries(
                "🎬 cinema movies film", "🎧 music streaming podcast audio", "🎵 songs music",
                "🎸 guitar music lessons", "🎹 piano keyboard music", "🥁 drums music",
                "🎤 karaoke singing", "🎨 art painting hobby", "🎭 theatre show",
                "📚 books reading", "📖 reading book study", "🎮 games gaming console",
                "🕹️ arcade games", "🎲 boardgames dice", "♟️ chess games", "🎯 darts hobby",
                "🎳 bowling", "🎪 events circus", "🎟️ tickets events entertainment",
                "🏕️ camping outdoors", "🏖️ beach holiday vacation", "🎣 fishing hobby",
                "🧩 puzzle hobby", "📷 photography camera", "🎉 party celebration",
            ),
        ),
        Section(
            "Money & Work",
            entries(
                "💰 money savings cash", "💵 cash dollars money", "💶 euros cash money",
                "💷 pounds cash money", "🏦 bank banking", "📈 investments stocks growth",
                "📉 losses stocks", "📊 budget charts report", "💼 work business job",
                "📁 files admin", "📄 documents paperwork admin", "📎 office supplies stationery",
                "🖊️ pen stationery", "✏️ pencil school stationery", "💻 laptop computer tech",
                "🖥️ desktop computer tech", "📱 phone mobile bill", "🖨️ printer office",
                "📞 calls phone bill", "📧 email internet", "🗓️ subscription calendar plan",
                "⏰ alarm time", "🏢 office rent business", "🎓 education tuition school",
                "🔔 subscriptions alerts",
            ),
        ),
        Section(
            "Animals & Nature",
            entries(
                "🐕 dog pet puppy", "🐈 cat pet kitten", "🐾 pet paws animals", "🐦 bird pet",
                "🐠 fish aquarium pet", "🐹 hamster rodent pet", "🐰 rabbit bunny pet",
                "🐴 horse riding", "🌱 plants seedling garden", "🌳 tree garden outdoors",
                "🌵 cactus plant", "🌸 blossom flowers", "🌻 sunflower flowers", "🌹 rose flowers",
                "🌿 herbs plants", "🍀 luck clover", "☀️ sun summer weather", "🌧️ rain weather",
                "❄️ snow winter cold", "🌊 sea water waves", "⛰️ mountains hiking outdoors",
                "🌍 world travel global",
            ),
        ),
        Section(
            "Symbols",
            entries(
                "⭐ star favourite", "✨ sparkle special", "✅ done complete", "✔️ check tick",
                "❗ important urgent", "❓ unknown misc", "⚠️ warning", "🔒 locked secure",
                "📌 pinned", "📍 location place", "🔖 bookmark label", "♻️ recycling eco",
                "🚩 flag marker", "🆕 new", "🔄 recurring repeat subscription",
                "⏳ pending waiting", "🕐 time hourly", "➕ plus add", "🔵 blue circle dot",
                "🟣 purple circle dot", "🟢 green circle dot", "🟡 yellow circle dot",
                "🔴 red circle dot",
            ),
        ),
    )

    /** Every pickable emoji, in section/grid order — the pool [Categories.iconChoices] exposes. */
    val all: List<String> = sections.flatMap { section -> section.entries.map { it.emoji } }

    /**
     * Icons whose keywords match [query], ranked exact-token-first. A glyph is an exact hit when one
     * of its keywords equals the query, a prefix hit when a keyword merely starts with it; exact hits
     * win outright and prefix hits only show when there are no exact ones (so "car" surfaces the
     * vehicles, not "carton"/"carrot"). Blank query → empty (the caller shows the sectioned grid).
     */
    fun search(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val exact = ArrayList<String>()
        val prefix = ArrayList<String>()
        sections.forEach { section ->
            section.entries.forEach { entry ->
                val tokens = entry.keywords.split(' ')
                when {
                    tokens.contains(q) -> exact.add(entry.emoji)
                    tokens.any { it.startsWith(q) } -> prefix.add(entry.emoji)
                }
            }
        }
        return if (exact.isNotEmpty()) exact else prefix
    }
}
