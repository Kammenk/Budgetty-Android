package com.budgetty.app.data.ingest

import java.math.BigDecimal

/**
 * Best-effort conversion of raw receipt text into [ParsedTransaction]s.
 *
 * Tuned loosely for Bulgarian receipts (comma decimal separator, optional "лв"),
 * but intentionally forgiving: the review screen is the correctness backstop, so
 * we prefer to surface candidate rows the user can edit over dropping data.
 */
class ReceiptParser {

    // A monetary amount, e.g. "1,23", "12.50" or "1 234,56".
    private val priceRegex = Regex("""\d{1,3}(?:[ .]\d{3})*[.,]\d{2}""")

    // A leading "<qty> x" or "<qty> бр" prefix, e.g. "2 x", "3бр".
    private val quantityRegex = Regex("""^(\d+)\s*(?:[xX*]|бр\.?|бр)\b""")

    // A standalone "<qty> x <unit price>" line, e.g. "1.328 x 3.35" — the weight/count
    // breakdown for the product on the next line, not a product in its own right.
    private val multiplierRegex =
        Regex("""^(\d+(?:[.,]\d+)?)\s*[xX×*]\s*(\d{1,3}(?:[ .]\d{3})*[.,]\d{2})\s*$""")

    // Lines that are clearly totals/metadata/discounts rather than product lines.
    // "отстъпк" covers отстъпка/отстъпки (discount rows).
    private val skipKeywords = listOf(
        "сума", "общо", "обща", "total", "ддс", "vat", "касов", "бон", "плащане",
        "получено", "ресто", "карта", "брой", "артикул", "еик", "фактура",
        "курс", "отстъпк",
    )

    private data class Multiplier(val quantity: BigDecimal, val unitPrice: BigDecimal)

    fun parse(rawText: String): List<ParsedTransaction> {
        val result = mutableListOf<ParsedTransaction>()
        // On these receipts the breakdown line precedes its product ("1.328 x 3.35" then
        // "Лимони кг  4.45"), so hold it and fold it into the next product line.
        var pending: Multiplier? = null

        for (raw in rawText.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (skipKeywords.any { line.lowercase().contains(it) }) continue

            val multiplier = parseMultiplier(line)
            if (multiplier != null) {
                pending = multiplier
                continue
            }

            val product = parseLine(line) ?: continue
            result += pending?.let { applyMultiplier(product, it) } ?: product
            pending = null
        }
        return result
    }

    private fun parseMultiplier(line: String): Multiplier? {
        val match = multiplierRegex.find(line) ?: return null
        val quantity = match.groupValues[1].toBigDecimalOrNull() ?: return null
        val unitPrice = match.groupValues[2].toBigDecimalOrNull() ?: return null
        return Multiplier(quantity, unitPrice)
    }

    /**
     * Folds a "<qty> x <unit price>" breakdown into its product. A whole-number quantity
     * (e.g. "3 x 1.20") becomes the row's quantity at that unit price. A fractional one
     * (a weighed item like 1.328 kg) can't be an Int, so we keep the product's own line
     * total and leave quantity at 1.
     */
    private fun applyMultiplier(product: ParsedTransaction, m: Multiplier): ParsedTransaction {
        val whole = m.quantity.stripTrailingZeros()
        return if (whole.scale() <= 0) {
            product.copy(price = m.unitPrice, quantity = whole.toInt().coerceAtLeast(1))
        } else {
            product
        }
    }

    private fun parseLine(line: String): ParsedTransaction? {
        val priceMatch = priceRegex.findAll(line).lastOrNull() ?: return null

        // A "-" right after the amount marks a discount/refund (e.g. "0.55-") — skip it.
        val afterPrice = line.substring((priceMatch.range.last + 1).coerceAtMost(line.length))
        if (afterPrice.trimStart().startsWith("-")) return null

        val price = priceMatch.value.toBigDecimalOrNull() ?: return null

        // Everything before the price is treated as the product name.
        var name = line.substring(0, priceMatch.range.first).trim()

        var quantity = 1
        quantityRegex.find(name)?.let { qty ->
            quantity = qty.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            name = name.removeRange(qty.range).trim()
        }

        name = name.trim(' ', '-', ':', '*', '.', 'x', 'X')

        if (name.isBlank()) return null

        return ParsedTransaction(
            name = ProductNameNormalizer.normalize(name),
            price = price,
            quantity = quantity,
        )
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = try {
        BigDecimal(replace(" ", "").replace(',', '.'))
    } catch (e: NumberFormatException) {
        null
    }
}
