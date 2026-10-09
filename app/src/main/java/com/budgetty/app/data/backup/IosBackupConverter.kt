package com.budgetty.app.data.backup

import com.budgetty.app.category.Categories
import com.budgetty.app.category.CategoryBucket
import com.budgetty.app.data.local.BudgetEntity
import com.budgetty.app.data.local.BudgetEnvelopeEntity
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.data.local.CategoryRuleEntity
import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.data.local.IgnoredSubscriptionEntity
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.local.TransactionTagEntity
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.data.local.WarrantyEntity
import com.budgetty.app.data.local.WellbeingScoreEntity
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.Language
import com.budgetty.app.data.settings.RecapFrequency
import com.budgetty.app.data.settings.ThemeMode
import com.budgetty.app.ui.home.HomeSection
import com.budgetty.app.ui.insights.InsightsSection
import com.budgetty.app.ui.util.BudgetCadence
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Converts a backup exported by Budgetty iOS (`Backup.swift`'s `BackupFile`) into the Android
 * [BackupData] shape, so both formats restore through the one [BackupManager.import] path — merge and
 * replace alike.
 *
 * The two files differ in shape more than meaning: iOS writes dates as ISO-8601 strings (Android: epoch
 * millis), nests line items in their receipt (each carrying its tag names) and contributions in their
 * goal, keeps list fields as JSON arrays (Android joins them into one newline-separated string), and
 * exports only its custom categories. Settings use iOS vocabulary and are translated value by value;
 * anything with no Android equivalent becomes null, which [BackupManager] reads as "keep the device's
 * value" — a raw iOS value is never passed through.
 *
 * Collections the iOS app added over time (buying limits, trips, …) are optional, exactly as iOS's own
 * decoder treats them, so an older iOS file still converts. A missing date or amount a row can't exist
 * without fails the whole conversion with [IllegalArgumentException] — all-or-nothing, like a malformed
 * Android backup — rather than restoring a silently corrupted row.
 */
object IosBackupConverter {

    /** The `app` marker every iOS backup carries at its top level. */
    const val IOS_APP_MARKER = "Budgetty iOS"

    /**
     * True when [root] is an iOS backup: it carries the iOS `app` marker, or — failing that — has the
     * iOS shape (receipts with nested `items`) and no top-level Android `transactions` list.
     */
    fun isIosBackup(root: JsonElement): Boolean {
        val obj = root as? JsonObject ?: return false
        if (obj.string("app") == IOS_APP_MARKER) return true
        val firstReceipt = (obj.get("receipts") as? JsonArray)?.firstOrNull() as? JsonObject
        return !obj.has("transactions") && firstReceipt?.get("items") is JsonArray
    }

    /** Converts an iOS backup [root] (see [isIosBackup]) to [BackupData]. */
    fun convert(root: JsonObject): BackupData {
        val receipts = convertReceipts(root.objects("receipts"))
        val (goals, contributions) = convertSavings(root.objects("savingsGoals"))
        val trips = root.objects("trips").map(::trip)
        return BackupData(
            transactions = receipts.transactions,
            categories = categories(
                custom = root.objects("categories"),
                overrides = root.objects("categoryOverrides"),
                transactions = receipts.transactions,
            ),
            budgets = root.objects("budgets").map {
                BudgetEntity(budgetKey = it.requireString("key"), amount = it.requireDecimal("amount"))
            },
            receipts = receipts.receipts,
            rules = root.objects("rules").map(::rule),
            recurring = root.objects("recurring").map(::recurring),
            savingsGoals = goals,
            savingsContributions = contributions,
            buyingLimits = root.objects("buyingLimits").map(::buyingLimit),
            wellbeingScores = root.objects("wellbeingScores").map(::wellbeingScore),
            tags = tagCatalog(root.objects("tags"), receipts.tagFirstSeen, trips),
            transactionTags = receipts.links,
            debts = root.objects("debts").map(::debt),
            templates = root.objects("templates").map(::template),
            warranties = root.objects("warranties").map { warranty(it, receipts.idBySecond) },
            budgetEnvelopes = root.objects("budgetEnvelopes").map(::envelope),
            trips = trips,
            ignoredSubscriptions = root.objects("ignoredSubscriptions").map {
                IgnoredSubscriptionEntity(it.requireString("merchant"), it.date("ignoredAt") ?: 0L)
            },
            settings = (root.get("settings") as? JsonObject)?.let(::settings),
        )
    }

    // ── Receipts + line items ──

    /** Receipts with their line items flattened out, plus what the tag and warranty mapping needs. */
    private class ConvertedReceipts(
        val receipts: List<ReceiptEntity>,
        val transactions: List<TransactionEntity>,
        val links: List<TransactionTagEntity>,
        /** Tag name → when a line item first carried it; the catalog createdAt when iOS sent none. */
        val tagFirstSeen: Map<String, Long>,
        /** iOS receipt createdAt in whole epoch seconds → the Android receipt id it was given. */
        val idBySecond: Map<Long, Long>,
    )

    private fun convertReceipts(dtos: List<JsonObject>): ConvertedReceipts {
        val receipts = ArrayList<ReceiptEntity>(dtos.size)
        val transactions = ArrayList<TransactionEntity>()
        val links = ArrayList<TransactionTagEntity>()
        val tagFirstSeen = LinkedHashMap<String, Long>()
        val idBySecond = HashMap<Long, Long>()
        val usedIds = HashSet<Long>()
        dtos.forEach { dto ->
            val createdAt = dto.requireDate("createdAt")
            // The receipt id is its creation time in millis — the "upload id" Android keys receipts by. Two
            // receipts created in the same millisecond would collide on that primary key (the second
            // REPLACEs the first), so the later one is bumped forward until it's free.
            var id = createdAt
            while (!usedIds.add(id)) id++
            idBySecond.putIfAbsent(Math.floorDiv(createdAt, MILLIS_PER_SECOND), id)
            val receipt = receipt(dto, id)
            receipts += receipt
            dto.objects("items").forEach { item ->
                // Synthetic ids 1..n only tie the tag links to their rows; BackupManager re-mints both.
                val txnId = transactions.size + 1L
                transactions += lineItem(item, txnId, receipt)
                item.strings("tags").map(TagEntity::normalize).filter { it.isNotEmpty() }.distinct().forEach { tag ->
                    links += TransactionTagEntity(txnId, tag)
                    tagFirstSeen.putIfAbsent(tag, item.date("createdAt") ?: createdAt)
                }
            }
        }
        return ConvertedReceipts(receipts, transactions, links, tagFirstSeen, idBySecond)
    }

    private fun receipt(dto: JsonObject, id: Long) = ReceiptEntity(
        timestamp = id,
        store = dto.string("store").orEmpty(),
        date = dto.requireDate("date"),
        discount = dto.decimal("discount") ?: BigDecimal.ZERO,
        isManual = dto.bool("isManual") ?: false,
        tax = dto.decimal("tax") ?: BigDecimal.ZERO,
        taxOnTop = dto.bool("taxOnTop") ?: false,
        extraCharges = dto.decimal("extraCharges") ?: BigDecimal.ZERO,
    )

    /**
     * One iOS line item as a transaction. Its timestamp is the receipt's PRINTED date, not the item's
     * own createdAt: Android buckets spending by that date (UploadViewModel.finalizeUpload stamps every
     * row with it), so a receipt scanned a day late still lands on the day it was paid.
     */
    private fun lineItem(item: JsonObject, id: Long, receipt: ReceiptEntity) = TransactionEntity(
        id = id,
        name = item.string("name").orEmpty(),
        timestamp = receipt.date,
        price = item.requireDecimal("price"),
        quantity = item.int("quantity") ?: 1,
        category = item.string("category")?.ifBlank { null } ?: Categories.DEFAULT,
        receiptId = receipt.timestamp,
    )

    // ── Categories ──

    /**
     * The full category table. iOS exports only its custom categories (built-ins are seeded on both
     * platforms), but a "Replace all" clears Android's categories table before inserting the backup's
     * rows — so the built-ins go in too, exactly as Android's own export carries them; the on-open
     * re-seed would only bring them back on the next launch. Built-ins pick up any nesting / bucket
     * override iOS sent in `categoryOverrides`. On a merge the device's rows win (insert-or-ignore).
     */
    private fun categories(
        custom: List<JsonObject>,
        overrides: List<JsonObject>,
        transactions: List<TransactionEntity>,
    ): List<CategoryEntity> {
        val overrideByName = overrides.associateBy { it.string("name") }
        val builtIns = Categories.predefined.map { predefined ->
            val override = overrideByName[predefined.name]
            CategoryEntity(
                name = predefined.name,
                colorArgb = predefined.colorArgb,
                icon = predefined.emoji,
                parent = override?.string("parent"),
                bucket = override?.string("bucket")?.let(::bucket),
            )
        }
        val rows = (builtIns + custom.map(::customCategory)).distinctBy { it.name }
        val known = rows.mapTo(HashSet()) { it.name }
        // A line item filed under a name with no row gets a plain one, as finalizeUpload does on save.
        val missing = transactions.map { it.category }.distinct().filterNot { it in known }
            .map { CategoryEntity(it, Categories.colorOf(it)) }
        return rows + missing
    }

    private fun customCategory(dto: JsonObject): CategoryEntity {
        val name = dto.requireString("name")
        return CategoryEntity(
            name = name,
            // iOS stores the packed 0xAARRGGBB in a 64-bit Int (an opaque color is a large positive
            // number); its low 32 bits are exactly Android's signed ARGB int.
            colorArgb = dto.long("colorArgb")?.toInt() ?: Categories.colorOf(name),
            icon = dto.string("icon").orEmpty(),
            isCustom = true,
            createdAt = dto.date("createdAt") ?: 0L,
            parent = dto.string("parent"),
            bucket = dto.string("bucket")?.let(::bucket),
        )
    }

    private fun bucket(name: String): CategoryBucket? = CategoryBucket.entries.firstOrNull { it.name == name }

    // ── Everything else ──

    private fun rule(dto: JsonObject) =
        CategoryRuleEntity(CategoryRuleEntity.key(dto.requireString("name")), dto.requireString("category"))

    private fun recurring(dto: JsonObject) = RecurringEntity(
        label = dto.string("label").orEmpty(),
        amount = dto.requireDecimal("amount"),
        isIncome = dto.bool("isIncome") ?: false,
        category = dto.string("category").orEmpty(),
        // Both apps persist the same cadence names; an unknown one reads as monthly, as it does on iOS.
        cadence = dto.string("cadenceRaw")?.takeIf { it in CADENCES } ?: RecurringEntity.Cadence.MONTHLY,
        dueDay = dto.int("dueDay") ?: 1,
        createdAt = dto.date("createdAt") ?: 0L,
        // Absent in pre-autopay iOS backups. lastPosted is the mark-as-paid stamp on both platforms.
        autoPay = dto.bool("autoPay") ?: false,
        nextDue = dto.date("nextDue") ?: 0L,
        lastPosted = dto.date("lastPosted") ?: 0L,
        active = dto.bool("active") ?: true,
    )

    /** Goals get synthetic ids 1..n so their nested contributions can point at them by goalId. */
    private fun convertSavings(dtos: List<JsonObject>): Pair<List<SavingsGoalEntity>, List<SavingsContributionEntity>> {
        val contributions = ArrayList<SavingsContributionEntity>()
        val goals = dtos.mapIndexed { index, dto ->
            val goalId = index + 1L
            dto.objects("contributions").mapTo(contributions) {
                SavingsContributionEntity(
                    goalId = goalId,
                    amount = it.requireDecimal("amount"),
                    note = it.string("note").orEmpty(),
                    date = it.date("date") ?: 0L,
                )
            }
            SavingsGoalEntity(
                id = goalId,
                name = dto.string("name").orEmpty(),
                emoji = dto.string("emoji").orEmpty(),
                targetAmount = dto.requireDecimal("targetAmount"),
                targetDate = dto.date("targetDate"),
                createdAt = dto.date("createdAt") ?: 0L,
            )
        }
        return goals to contributions
    }

    private fun buyingLimit(dto: JsonObject) = BuyingLimitEntity(
        emoji = dto.string("emoji").orEmpty(),
        label = dto.string("label").orEmpty(),
        keywords = BuyingLimitEntity.joinKeywords(dto.strings("keywords")),
        timeframe = BuyingLimitTimeframe.entries.firstOrNull { it.name == dto.string("timeframeRaw") }
            ?: BuyingLimitTimeframe.MONTHLY,
        count = (dto.int("count") ?: 1).coerceAtLeast(1),
        createdAt = dto.date("createdAt") ?: 0L,
    )

    /** Band and component keys are the same stable names on both platforms, so the snapshot copies as-is. */
    private fun wellbeingScore(dto: JsonObject) = WellbeingScoreEntity(
        periodId = dto.requireString("periodId"),
        score = requireNotNull(dto.int("score")) { "iOS backup: wellbeing score without a score" },
        band = dto.requireString("band"),
        componentsJson = dto.string("componentsJson") ?: "{}",
        computedAt = dto.date("computedAt") ?: 0L,
    )

    private fun trip(dto: JsonObject) = TripEntity(
        name = dto.string("name").orEmpty(),
        tag = TagEntity.normalize(dto.requireString("tag")),
        startDate = dto.date("startDate"),
        endDate = dto.date("endDate"),
        budgetAmount = dto.decimal("budgetAmount"),
        active = dto.bool("active") ?: false,
        createdAt = dto.date("createdAt") ?: 0L,
        endedAt = dto.date("endedAt"),
    )

    /**
     * Every tag the backup uses: iOS's own catalog when it sent one (it keeps each tag's createdAt and
     * any tag nothing carries yet), then each tag a line item carries, then each trip's tag — a trip's
     * tag must exist even before its first expense so auto-tagging works once it's resumed.
     */
    private fun tagCatalog(
        catalog: List<JsonObject>,
        firstSeen: Map<String, Long>,
        trips: List<TripEntity>,
    ): List<TagEntity> {
        val createdAt = LinkedHashMap<String, Long>()
        catalog.forEach { tag ->
            createdAt.putIfAbsent(TagEntity.normalize(tag.string("name").orEmpty()), tag.date("createdAt") ?: 0L)
        }
        firstSeen.forEach { (name, at) -> createdAt.putIfAbsent(name, at) }
        trips.forEach { createdAt.putIfAbsent(it.tag, it.createdAt) }
        return createdAt.filterKeys { it.isNotEmpty() }.map { (name, at) -> TagEntity(name, at) }
    }

    private fun debt(dto: JsonObject) = DebtEntity(
        emoji = dto.string("emoji").orEmpty(),
        name = dto.string("name").orEmpty(),
        balance = dto.requireDecimal("balance"),
        aprPercent = dto.requireDecimal("aprPercent"),
        minPayment = dto.requireDecimal("minPayment"),
        createdAt = dto.date("createdAt") ?: 0L,
    )

    private fun template(dto: JsonObject) = TemplateEntity(
        emoji = dto.string("emoji").orEmpty(),
        name = dto.string("name").orEmpty(),
        amount = dto.requireDecimal("amount"),
        category = dto.string("category").orEmpty(),
        store = dto.string("store").orEmpty(),
        askAmount = dto.bool("askAmount") ?: false,
        createdAt = dto.date("createdAt") ?: 0L,
    )

    private fun warranty(dto: JsonObject, idBySecond: Map<Long, Long>) = WarrantyEntity(
        name = dto.string("name").orEmpty(),
        emoji = dto.string("emoji") ?: "🛡️",
        store = dto.string("store").orEmpty(),
        category = dto.string("category").orEmpty(),
        purchaseDate = dto.requireDate("purchaseDate"),
        durationMonths = requireNotNull(dto.int("durationMonths")) { "iOS backup: warranty without a duration" },
        coverageNote = dto.string("coverageNote").orEmpty(),
        receiptId = warrantyReceiptId(dto.double("receiptId"), idBySecond),
        createdAt = dto.date("createdAt") ?: 0L,
    )

    /**
     * iOS links a warranty to its receipt by the receipt's createdAt in epoch SECONDS (a Double, 0 =
     * none); Android by the receipt id in millis. The receipt's own createdAt crossed the file as
     * whole-second ISO-8601, so the link is matched on the second and resolves to the id that receipt
     * was actually given (it may have been bumped past a collision). A link to a receipt not in the file
     * keeps its instant, converted to millis.
     */
    private fun warrantyReceiptId(seconds: Double?, idBySecond: Map<Long, Long>): Long {
        if (seconds == null || seconds <= 0.0) return 0L
        return idBySecond[floor(seconds).toLong()]
            ?: idBySecond[seconds.roundToLong()]
            ?: (seconds * MILLIS_PER_SECOND).roundToLong()
    }

    private fun envelope(dto: JsonObject) = BudgetEnvelopeEntity(
        name = dto.string("name").orEmpty(),
        emoji = dto.string("emoji") ?: "🧾",
        limitAmount = dto.requireDecimal("limitAmount"),
        startDate = dto.requireDate("startDate"),
        endDate = dto.requireDate("endDate"),
        // Android keeps the scope newline-joined; an empty list is "all spending" on both platforms.
        categories = dto.strings("categories").filter { it.isNotBlank() }.joinToString("\n"),
        sortOrder = dto.int("sortOrder") ?: 0,
        createdAt = dto.date("createdAt") ?: 0L,
    )

    // ── Settings ──

    /**
     * Translates iOS's settings vocabulary into [BackupSettings]' Android enum names / section keys. A
     * value already in Android vocabulary is accepted too; anything else (iOS's "system" date format, a
     * blank cadence, an unset fortnight anchor, an unknown code) becomes null so the device keeps its own.
     */
    private fun settings(dto: JsonObject) = BackupSettings(
        currency = enumName(dto.string("currency"), Currency.entries.associateBy { it.code }, Currency.entries),
        dateFormat = enumName(dto.string("dateFormat"), IOS_DATE_FORMATS, DateFormatOption.entries),
        language = enumName(dto.string("language"), IOS_LANGUAGES, Language.entries),
        themeMode = enumName(dto.string("themeMode"), IOS_THEMES, ThemeMode.entries),
        accent = enumName(dto.string("accent"), IOS_ACCENTS, AccentTheme.entries),
        monthStartDay = dto.int("monthStartDay")?.takeIf { it in 1..MAX_MONTH_START_DAY },
        budgetRolloverEnabled = dto.bool("budgetRolloverEnabled"),
        budgetCadence = BudgetCadence.fromName(dto.string("budgetCadence"))?.name,
        fortnightAnchorEpochDay = dto.long("fortnightAnchor")?.takeIf { it > 0L },
        hiddenHomeSections = dto.stringsOrNull("hiddenHomeSections")?.let { homeKeys(it) },
        hiddenInsightsSections = dto.stringsOrNull("hiddenInsightsSections")?.let { insightsKeys(it) },
        homeSectionOrder = dto.stringsOrNull("homeSectionOrder")?.let { homeKeys(it) },
        insightsSectionOrder = dto.stringsOrNull("insightsSectionOrder")?.let { insightsKeys(it) },
        customInsightsSections = dto.stringsOrNull("customInsightsSections")?.let { insightsKeys(it) },
        recapEnabled = dto.bool("recapEnabled"),
        recapFrequency = enumName(dto.string("recapFrequency"), emptyMap(), RecapFrequency.entries),
        hideAmounts = dto.bool("hideAmounts"),
        hideAmountsOnBackground = dto.bool("hideAmountsOnBackground"),
    )

    /** The Android enum name for an iOS [value] (via [ios]), or [value] itself if it is already one. */
    private fun <E : Enum<E>> enumName(value: String?, ios: Map<String, E>, android: List<E>): String? =
        value?.let { v -> ios[v]?.name ?: android.firstOrNull { it.name == v }?.name }

    private fun homeKeys(ios: List<String>): List<String>? =
        sectionKeys(ios, IOS_HOME_SECTIONS, HomeSection.entries.mapTo(HashSet()) { it.key })

    private fun insightsKeys(ios: List<String>): List<String>? =
        sectionKeys(ios, IOS_INSIGHTS_SECTIONS, InsightsSection.entries.mapTo(HashSet()) { it.key })

    /**
     * Maps iOS section ids to Android keys, dropping sections Android doesn't have (e.g. week comparison).
     * An empty list stays empty ("nothing hidden" / "default order"); a non-empty one where nothing maps
     * becomes null, so the device keeps its own layout instead of having it wiped by an untranslatable
     * one — the same rule the iOS app applies when it restores an Android file.
     */
    private fun sectionKeys(values: List<String>, ios: Map<String, String>, androidKeys: Set<String>): List<String>? {
        val mapped = values.mapNotNull { ios[it] ?: it.takeIf { key -> key in androidKeys } }.distinct()
        return mapped.takeIf { values.isEmpty() || it.isNotEmpty() }
    }

    private const val MILLIS_PER_SECOND = 1000L
    private const val MAX_MONTH_START_DAY = 31

    private val CADENCES = setOf(
        RecurringEntity.Cadence.MONTHLY,
        RecurringEntity.Cadence.WEEKLY,
        RecurringEntity.Cadence.YEARLY,
        RecurringEntity.Cadence.ONCE,
    )

    /**
     * iOS `DateFormatOption` raw values. Android has no "follow the device" option, so iOS's `system`
     * maps to nothing (the device keeps its format); the others map to the Android format with the same
     * day/month order — the part that changes how a date reads.
     */
    private val IOS_DATE_FORMATS = mapOf(
        "dmy" to DateFormatOption.DAY_MONTH_YEAR,
        "mdy" to DateFormatOption.MDY_SLASH,
        "dots" to DateFormatOption.DMY_SLASH,
    )

    /** iOS stores the language as its BCP-47 code (Android's [Language.tag]), or "system". */
    private val IOS_LANGUAGES: Map<String, Language> =
        Language.entries.associateBy { it.tag ?: "system" }

    private val IOS_THEMES = mapOf("system" to ThemeMode.SYSTEM, "light" to ThemeMode.LIGHT, "dark" to ThemeMode.DARK)

    private val IOS_ACCENTS = mapOf(
        "violet" to AccentTheme.DEFAULT,
        "sage" to AccentTheme.SAGE,
        "ocean" to AccentTheme.OCEAN,
        "plum" to AccentTheme.PLUM,
    )

    /** iOS `HomeSection` raw values → Android [HomeSection] keys. iOS's week comparison has no Android twin. */
    private val IOS_HOME_SECTIONS = mapOf(
        "totalSpent" to HomeSection.TOTAL_SPENT.key,
        "budgets" to HomeSection.BUDGETS.key,
        "upcomingBills" to HomeSection.UPCOMING_BILLS.key,
        "wellbeing" to HomeSection.WELLBEING.key,
        "receipts" to HomeSection.RECEIPTS.key,
    )

    /** iOS `InsightSection` raw values → Android [InsightsSection] keys (same cards, different ids). */
    private val IOS_INSIGHTS_SECTIONS = mapOf(
        "trend" to InsightsSection.TREND.key,
        "breakdown" to InsightsSection.BREAKDOWN.key,
        "stats" to InsightsSection.SUMMARY.key,
        "needsWantsSavings" to InsightsSection.NEEDS_WANTS_SAVINGS.key,
        "highlights" to InsightsSection.HIGHLIGHTS.key,
        "comparison" to InsightsSection.PERIOD_COMPARISON.key,
        "topCategories" to InsightsSection.TOP_CATEGORIES.key,
        "topStores" to InsightsSection.TOP_STORES.key,
        "biggestPurchases" to InsightsSection.BIGGEST_PURCHASES.key,
        "byTag" to InsightsSection.BY_TAG.key,
        "income" to InsightsSection.INCOME_SPENDING.key,
        "subscriptions" to InsightsSection.SUBSCRIPTIONS.key,
    )
}

// ── Lenient JSON readers: a missing or wrongly-typed field reads as null, never throws ──

private fun JsonObject.primitive(key: String): JsonPrimitive? = get(key) as? JsonPrimitive

private fun JsonObject.string(key: String): String? = primitive(key)?.takeIf { it.isString }?.asString

private fun JsonObject.bool(key: String): Boolean? = primitive(key)?.takeIf { it.isBoolean }?.asBoolean

private fun JsonObject.double(key: String): Double? = primitive(key)?.takeIf { it.isNumber }?.asDouble

private fun JsonObject.long(key: String): Long? = primitive(key)?.takeIf { it.isNumber }?.asBigDecimal?.toLong()

private fun JsonObject.int(key: String): Int? = long(key)?.toInt()

/** Swift's `Decimal` encodes as a JSON number; Gson keeps its literal text, so the value is exact. */
private fun JsonObject.decimal(key: String): BigDecimal? = primitive(key)?.let {
    when {
        it.isNumber -> it.asBigDecimal
        it.isString -> it.asString.toBigDecimalOrNull()
        else -> null
    }
}

/**
 * An ISO-8601 instant (`JSONEncoder.dateEncodingStrategy = .iso8601`: "2026-10-01T09:30:00Z") as epoch
 * millis. Fractional seconds and explicit offsets are accepted too.
 */
private fun JsonObject.date(key: String): Long? =
    string(key)?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

private fun JsonObject.objects(key: String): List<JsonObject> =
    (get(key) as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

private fun JsonObject.stringsOrNull(key: String): List<String>? =
    (get(key) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.asString }

private fun JsonObject.strings(key: String): List<String> = stringsOrNull(key).orEmpty()

private fun JsonObject.requireString(key: String): String =
    requireNotNull(string(key)) { "iOS backup: missing text field '$key'" }

private fun JsonObject.requireDecimal(key: String): BigDecimal =
    requireNotNull(decimal(key)) { "iOS backup: missing or invalid amount '$key'" }

private fun JsonObject.requireDate(key: String): Long =
    requireNotNull(date(key)) { "iOS backup: missing or invalid date '$key'" }
