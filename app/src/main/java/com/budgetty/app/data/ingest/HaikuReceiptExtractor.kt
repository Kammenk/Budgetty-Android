package com.budgetty.app.data.ingest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.remote.ExtractRequest
import com.budgetty.app.data.remote.ExtractResponse
import com.budgetty.app.data.remote.ExtractedItem
import com.budgetty.app.data.remote.ReceiptApi
import com.budgetty.app.store.StoreNormalizer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

/**
 * Extracts transactions by sending the receipt image/PDF to the Haiku-backed Cloud
 * Function proxy ([ReceiptApi]). The proxy holds the Anthropic key server-side and
 * returns structured line items that are already categorized.
 *
 * Requires a signed-in user: the Firebase ID token is attached so the proxy can reject
 * unauthenticated callers (no open endpoint).
 *
 * Images are downscaled and re-encoded before upload (see [prepareUpload]) so large phone
 * photos don't bloat the upload or cost — Claude downsamples big images anyway.
 */
class HaikuReceiptExtractor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val api: ReceiptApi,
) : ReceiptExtractor {

    override suspend fun extract(uri: Uri): ParsedReceipt = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: error("Not signed in")
        val idToken = user.getIdToken(false).await().token ?: error("Could not obtain auth token")

        val sourceMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val (bytes, mimeType) = prepareUpload(uri, sourceMime)
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val response = api.extract(
            authorization = "Bearer $idToken",
            request = ExtractRequest(fileBase64 = base64, mimeType = mimeType),
        )

        val items = response.items.map { it.toParsedTransaction() }
        validateExtraction(response, items)
        // A tax-EXCLUSIVE receipt (a separate VAT/sales tax added ON TOP of the net line prices — a
        // US sales-tax receipt, a Bulgarian net invoice) prints its item prices net of tax. Keep those
        // printed prices exactly as read and carry the tax as an add-on ([ParsedReceipt.taxOnTop]) so
        // downstream totals still reach what was paid (item sum + tax), without silently inflating the
        // per-item prices. The common tax-inclusive receipt already has its tax inside the prices, so
        // it keeps taxOnTop = false and nothing is added on top.
        val gross = grossOf(items)
        val taxOnTop = isTaxOnTop(response, gross)
        val discount = reconcileDiscount(items, response.total, response.discount)

        // Delivery/service/bag fees and a tip, each materialized as its own visible line item (in the
        // Delivery / Tips categories) instead of vanishing into the invisible total-gap. Driven by the
        // model's explicit amounts, so they're captured even when the printed grand total is misread.
        val chargeItems = chargeItemsOf(response)
        val chargesTotal = grossOf(chargeItems)

        // The printed item-sum anchors describe the PRODUCT rows; the review screen sums all rows
        // (products + these charges), so lift both anchors by the charges to keep the soft "prices too
        // high" and blocking "line missing" checks aligned. Tax-on-top items anchor on the net SUBTOTAL.
        val productsSubtotal = (response.subtotal ?: 0.0).takeIf { it > 0 }?.let { BigDecimal.valueOf(it) }
        val productsExpected = if (taxOnTop) productsSubtotal else expectedItemsTotal(response)

        ParsedReceipt(
            // Collapse the raw header ("Кауфланд (Kaufland) Пловдив-Христо Боте") to its canonical
            // brand ("Kaufland") here, at the ingest boundary, so the review screen and everything
            // saved downstream use the simple name. normalize() also trims/space-collapses.
            storeName = StoreNormalizer.normalize(response.storeName.orEmpty()),
            date = parseReceiptDate(response.date),
            discount = discount,
            expectedItemsTotal = productsExpected?.let { it + chargesTotal },
            receiptSubtotal = productsSubtotal?.let { it + chargesTotal },
            // Tax-on-top: the reported tax, added on top. Tax-inclusive: only when genuinely contained.
            tax = if (taxOnTop) BigDecimal.valueOf(response.tax ?: 0.0) else containedTax(response, gross),
            taxOnTop = taxOnTop,
            // The itemized charges come OUT of the total-gap; only a still-unexplained residual (an
            // uncaptured deposit/fee) stays as the invisible add-on so the total still equals what was
            // paid, without double-counting the delivery/tip rows we just added.
            extraCharges = (extraChargesOf(response, gross, discount, taxOnTop) - chargesTotal)
                .coerceAtLeast(BigDecimal.ZERO),
            items = items + chargeItems,
        )
    }

    /**
     * Delivery/service/bag fees ([ExtractResponse.deliveryAndFees]) and a tip ([ExtractResponse.tip]),
     * each turned into its own line item — a combined "Delivery & fees" row (Delivery category) and a
     * "Tip" row (Tips category) — so they're visible and tracked rather than folded invisibly into the
     * total. An amount below [EXTRA_CHARGES_MIN] is treated as noise and skipped.
     */
    private fun chargeItemsOf(response: ExtractResponse): List<ParsedTransaction> = buildList {
        chargeItem(response.deliveryAndFees, R.string.upload_charge_delivery, Categories.DELIVERY)?.let(::add)
        chargeItem(response.tip, R.string.upload_charge_tip, Categories.TIPS)?.let(::add)
    }

    private fun chargeItem(amount: Double?, nameRes: Int, category: String): ParsedTransaction? {
        val value = BigDecimal.valueOf(amount ?: 0.0).setScale(2, RoundingMode.HALF_UP)
        if (value < EXTRA_CHARGES_MIN) return null
        return ParsedTransaction(
            name = context.getString(nameRes),
            price = value,
            quantity = 1,
            category = category,
            categoryColor = colorFor(category),
        )
    }

    /**
     * The figure the line items *should* sum to, for the review screen's soft "double-check the
     * prices" warning (it never changes the saved total). Used for the tax-inclusive path (the
     * tax-exclusive path anchors on the printed subtotal directly): we anchor on the printed SUBTOTAL
     * when present (the number that equals the item sum) and otherwise reconstruct it from the grand
     * total, adding back any order discount since line prices are pre-discount. Returns null when
     * there's no trustworthy figure, so no warning shows rather than a misleading one. Compared
     * against gross (the same pre-discount item sum the review screen shows) with a tolerance, on the client.
     */
    private fun expectedItemsTotal(response: ExtractResponse): BigDecimal? {
        val subtotal = response.subtotal ?: 0.0
        if (subtotal > 0) return BigDecimal.valueOf(subtotal)
        val total = response.total ?: 0.0
        if (total <= 0) return null
        val discount = response.discount ?: 0.0
        return BigDecimal.valueOf(total + discount)
    }

    /**
     * The tax/VAT contained in a tax-INCLUSIVE receipt's prices, to store as an informational "incl.
     * VAT" figure — only when it's genuinely inside the total the items add up to (the receipt is
     * tax-inclusive and its printed total is covered by the item sum). Returns 0 otherwise, so a net
     * total is never mislabelled as tax-inclusive. The tax-exclusive (tax-on-top) path stores the
     * reported tax directly instead. [itemsGross] is the item sum.
     */
    private fun containedTax(response: ExtractResponse, itemsGross: BigDecimal): BigDecimal {
        val tax = response.tax ?: 0.0
        val total = response.total ?: 0.0
        if (tax <= 0.0 || total <= 0.0) return BigDecimal.ZERO
        val covered = itemsGross.add(TAX_ON_TOP_TOLERANCE_MIN) >= BigDecimal.valueOf(total)
        return if (covered) BigDecimal.valueOf(tax) else BigDecimal.ZERO
    }

    /**
     * Rejects an extraction we shouldn't trust, throwing a user-facing message that the upload
     * screen surfaces directly (see [com.budgetty.app.ui.upload.UploadViewModel]). Because this
     * runs before the scan is counted, a rejected receipt does not consume the user's scan quota.
     *
     * It guards against the model confabulating a plausible-but-wrong receipt when the photo is too
     * poor to actually read: we honor the model's own "unreadable" flag, then cross-check the line
     * items against the receipt's own printed anchors — its article count and its grand total.
     */
    private fun validateExtraction(response: ExtractResponse, items: List<ParsedTransaction>) {
        // 1) The model told us it couldn't read the lines (and so returned none rather than guesses).
        if (response.readable == false) throw ReceiptUnreadableException(UNREADABLE_MESSAGE)

        // 2) Article-count cross-check: capturing far fewer/more items than the receipt itself prints
        // ("N АРТИКУЛА") means we misread it. Only checked when a count is actually printed.
        //
        // That printed count is EITHER the number of product lines or the number of units, depending on
        // the receipt format — Greek "ΣΥΝΟΛΟ ΕΙΔΩΝ" counts units, so a basket with multi-buy lines
        // ("6 X 1,42") prints many more articles than it has lines. Accept whichever reading lands in
        // band; only a count matching NEITHER means we genuinely misread the receipt.
        val printedCount = response.printedItemCount ?: 0
        val inCountBand = { n: Int ->
            n >= printedCount * MIN_COUNT_RATIO && n <= printedCount * MAX_COUNT_RATIO
        }
        if (printedCount >= MIN_PRINTED_COUNT_TO_CHECK &&
            !inCountBand(items.size) && !inCountBand(countUnits(items))
        ) {
            throw ReceiptUnreadableException(UNREADABLE_MESSAGE)
        }

        // 3) Money sanity: the line items, net of the discount the model itself reported, shouldn't
        // overshoot the printed grand total by a wide margin (deposits/fees only raise the total, so
        // they don't trip this). A large overshoot means invented or duplicated lines.
        val printedTotal = response.total ?: 0.0
        if (printedTotal > 0) {
            val overshoot = grossOf(items).toDouble() - (response.discount ?: 0.0) - printedTotal
            if (overshoot > maxOf(printedTotal * MAX_OVERSHOOT_RATIO, MAX_OVERSHOOT_ABS)) {
                throw ReceiptUnreadableException(UNREADABLE_MESSAGE)
            }
        }
    }

    /** Sum of line totals (`price × quantity`), exactly as the review screen and discount math do it. */
    private fun grossOf(items: List<ParsedTransaction>): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }

    /**
     * Number of ARTICLES the basket represents, as a receipt's own "N items" line counts them.
     * [ParsedTransaction.quantity] is already the per-line article count — [toParsedTransaction] sets it
     * to N only for a whole "N x price" multiplier and to 1 for a single or weighed/fractional line — so
     * summing it needs no further normalization. Mirrored server-side by extract.js `countUnits`.
     */
    private fun countUnits(items: List<ParsedTransaction>): Int = items.sumOf { it.quantity }

    /**
     * Resolves the order-level discount that the review total subtracts (`gross − discount`).
     *
     * When the receipt prints a grand total we anchor on it: `discount = gross − printedTotal`, so the
     * finalize total equals the amount actually paid. This neutralizes per-line coupons the model may
     * have already netted into item prices (which would otherwise be subtracted a second time via the
     * separate discount field) and absorbs small OCR drift in the line prices. We only trust the
     * printed total when it's positive and not greater than the extracted gross — a larger total would
     * mean uncaptured charges (deposits/fees), not a discount — otherwise we fall back to the model's
     * reported discount. [gross] is summed exactly as the review screen does, so the two agree to the cent.
     */
    private fun reconcileDiscount(
        items: List<ParsedTransaction>,
        printedTotal: Double?,
        reportedDiscount: Double?,
    ): BigDecimal {
        val gross = grossOf(items)
        val total = printedTotal?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
        return if (total.signum() > 0 && total <= gross) {
            gross.subtract(total)
        } else {
            reportedDiscount?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
        }
    }

    /**
     * True when the receipt is tax-EXCLUSIVE: a separate tax was added on top of the line prices, so
     * the printed pre-discount total (total + discount) exceeds the net item sum by (about) the
     * reported tax. This is what tells a Bulgarian net invoice / US sales-tax receipt apart from the
     * common tax-inclusive receipt, where the total already equals the item sum — for that one it
     * returns false and the prices are left untouched. Kept strict (the arithmetic must line up within
     * a tolerance) so a tax-inclusive receipt that merely reports its contained VAT isn't grossed up.
     */
    private fun isTaxOnTop(response: ExtractResponse, netItemsSum: BigDecimal): Boolean {
        val tax = response.tax ?: 0.0
        val total = response.total ?: 0.0
        if (tax <= 0.0 || total <= 0.0 || netItemsSum.signum() <= 0) return false
        val target = BigDecimal.valueOf(total + (response.discount ?: 0.0))
        if (target <= netItemsSum) return false
        val tolerance = target.multiply(TAX_ON_TOP_TOLERANCE_RATIO).max(TAX_ON_TOP_TOLERANCE_MIN)
        return netItemsSum.add(BigDecimal.valueOf(tax)).subtract(target).abs() <= tolerance
    }

    /**
     * Money paid beyond the line items, order [discount] and on-top tax — delivery & service fees plus
     * a courier tip on a delivery-app order, an uncaptured deposit, etc. It's the gap by which the
     * printed grand total exceeds what the items reconcile to (item sum − discount + any on-top tax),
     * carried on the receipt and added to summed spend ([com.budgetty.app.data.model.additiveChargesOf])
     * so totals equal what was actually paid — without inventing product line items for the fees. Returns
     * 0 when no grand total is printed, or when the items already meet/exceed it (nothing extra was paid);
     * a sub-[EXTRA_CHARGES_MIN] gap is treated as cent-rounding, not a charge. [itemsGross] is the item
     * sum; [discount] the reconciled order discount; [taxOnTop] whether the reported tax is added on top.
     */
    private fun extraChargesOf(
        response: ExtractResponse,
        itemsGross: BigDecimal,
        discount: BigDecimal,
        taxOnTop: Boolean,
    ): BigDecimal {
        val total = response.total ?: 0.0
        if (total <= 0.0) return BigDecimal.ZERO
        val onTopTax = if (taxOnTop) BigDecimal.valueOf(response.tax ?: 0.0) else BigDecimal.ZERO
        val gap = BigDecimal.valueOf(total).subtract(itemsGross.subtract(discount).add(onTopTax))
        return if (gap >= EXTRA_CHARGES_MIN) gap else BigDecimal.ZERO
    }

    /**
     * Returns the bytes to upload and their MIME type. Images are decoded, downscaled so the
     * long edge is at most [MAX_IMAGE_EDGE] px, and re-encoded as JPEG (stepping quality down
     * until under [MAX_IMAGE_BYTES]). PDFs pass through but are rejected over [MAX_PDF_BYTES].
     */
    private fun prepareUpload(uri: Uri, mimeType: String): Pair<ByteArray, String> {
        if (mimeType.startsWith("image/")) {
            val decoded = try {
                decodeDownsampled(uri)
            } catch (e: Exception) {
                error("Could not read this image. Try another photo or a PDF.")
            }
            val scaled = decoded.scaleToMaxEdge(MAX_IMAGE_EDGE)
            if (scaled != decoded) decoded.recycle()
            val jpeg = scaled.toJpeg(JPEG_QUALITY, MAX_IMAGE_BYTES)
            scaled.recycle()
            return jpeg to "image/jpeg"
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read receipt file")
        if (mimeType == "application/pdf" && bytes.size > MAX_PDF_BYTES) {
            error("This PDF is too large (${bytes.size / 1_000_000} MB). Please use a smaller file.")
        }
        return bytes to mimeType
    }

    /** Decodes [uri] with sampling so an enormous source image never loads at full resolution. */
    private fun decodeDownsampled(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Software allocation so the bitmap is readable by createScaledBitmap / compress.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longEdge = maxOf(info.size.width, info.size.height)
            var sample = 1
            while (longEdge / sample > MAX_IMAGE_EDGE * 2) sample *= 2
            if (sample > 1) decoder.setTargetSampleSize(sample)
        }
    }

    /** Scales so the long edge is at most [maxEdge]; returns the same bitmap if already small. */
    private fun Bitmap.scaleToMaxEdge(maxEdge: Int): Bitmap {
        val longEdge = maxOf(width, height)
        if (longEdge <= maxEdge) return this
        val ratio = maxEdge.toFloat() / longEdge
        val w = (width * ratio).toInt().coerceAtLeast(1)
        val h = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, w, h, true)
    }

    /** Compresses to JPEG, stepping quality down until under [maxBytes] (or [MIN_JPEG_QUALITY]). */
    private fun Bitmap.toJpeg(quality: Int, maxBytes: Int): ByteArray {
        var q = quality
        while (true) {
            val out = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, q, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes || q <= MIN_JPEG_QUALITY) return bytes
            q -= 10
        }
    }

    private fun ExtractedItem.toParsedTransaction(): ParsedTransaction {
        val resolved = category?.takeIf { name ->
            Categories.predefined.any { it.name.equals(name, ignoreCase = true) }
        } ?: ""
        // The proxy returns `price` as the whole LINE total alongside a separate `quantity`, but
        // the app stores a per-UNIT price and multiplies by quantity everywhere (the review total,
        // Home, Insights, Budget, History). Split the line total back into a unit price so it isn't
        // counted twice: a whole count >= 2 (e.g. "2 x 2.04 = 4.08") becomes 2.04 at quantity 2; a
        // single item or a weighed/fractional line keeps the line total at quantity 1.
        val lineTotal = price?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
        val count = quantity?.toInt() ?: 1
        val wholeCount = count >= 2 && quantity == count.toDouble()
        return ParsedTransaction(
            name = ProductNameNormalizer.normalize(name),
            price = if (wholeCount) lineTotal.divide(BigDecimal(count), 2, RoundingMode.HALF_UP) else lineTotal,
            quantity = if (wholeCount) count else 1,
            category = resolved,
            categoryColor = colorFor(resolved),
        )
    }

    private fun colorFor(category: String): Int =
        Categories.predefined.firstOrNull { it.name.equals(category, ignoreCase = true) }?.colorArgb
            ?: Categories.defaultColor

    /** Parses "YYYY-MM-DD" to epoch millis (local midnight); falls back to now. */
    private fun parseReceiptDate(raw: String?): Long = try {
        if (raw.isNullOrBlank()) {
            System.currentTimeMillis()
        } else {
            LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    private companion object {
        /** Match Sonnet 5's high-resolution long-edge target (2576px) so small Cyrillic receipt text stays
         *  legible and the image isn't resampled twice. (Was 1568px — Sonnet 4.6's ceiling.) */
        const val MAX_IMAGE_EDGE = 2576
        /** High quality: thermal-receipt glyphs are tiny, so JPEG artifacts at lower quality cause misreads. */
        const val JPEG_QUALITY = 95
        const val MIN_JPEG_QUALITY = 50
        const val MAX_IMAGE_BYTES = 4 * 1024 * 1024   // 4 MB cap on the uploaded image
        const val MAX_PDF_BYTES = 20 * 1024 * 1024    // 20 MB cap on PDFs

        /** Shown when we refuse an extraction we don't trust (unreadable photo / confabulated lines). */
        const val UNREADABLE_MESSAGE =
            "Couldn't read this receipt clearly. Retake the photo in good light with the receipt flat and the text in focus."

        /** Only cross-check the printed article count once a receipt claims at least this many items. */
        const val MIN_PRINTED_COUNT_TO_CHECK = 3
        /** Accept item counts within this band of the printed count (tolerates a dropped bag/line). */
        const val MIN_COUNT_RATIO = 0.6
        const val MAX_COUNT_RATIO = 1.5
        /** Allow line items to exceed the printed total by this fraction (or [MAX_OVERSHOOT_ABS]). */
        const val MAX_OVERSHOOT_RATIO = 0.35
        const val MAX_OVERSHOOT_ABS = 1.5

        /**
         * Treat a receipt as tax-exclusive only when its pre-discount total exceeds the net item sum
         * by the reported tax within this tolerance (2% of the target, min 0.05) — absorbs per-line
         * VAT rounding without misclassifying a tax-inclusive receipt (whose total already equals the
         * item sum). See [isTaxOnTop].
         */
        val TAX_ON_TOP_TOLERANCE_RATIO: BigDecimal = BigDecimal("0.02")
        val TAX_ON_TOP_TOLERANCE_MIN: BigDecimal = BigDecimal("0.05")

        /** Ignore a sub-5-cent gap between the printed total and the reconciled items as rounding, not a charge. */
        val EXTRA_CHARGES_MIN: BigDecimal = BigDecimal("0.05")
    }
}
