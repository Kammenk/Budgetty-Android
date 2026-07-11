package com.budgetty.app.data.ingest

import java.math.BigDecimal

/**
 * An editable, pre-save receipt produced by extraction: the receipt-level details (store, date,
 * discount) plus its line [items]. [date] is the receipt's own date in epoch millis (defaulting
 * to now when the receipt doesn't show one).
 */
data class ParsedReceipt(
    val storeName: String = "",
    val date: Long = System.currentTimeMillis(),
    val discount: BigDecimal = BigDecimal.ZERO,
    /**
     * What the line items *should* sum to according to the receipt's own printed figures (its
     * subtotal, or its total once tax/discount are accounted for). Used only to surface a soft
     * "double-check the prices" warning on review — never to change the saved total. Null when no
     * trustworthy figure was extracted (e.g. manual entry), so no warning is shown.
     */
    val expectedItemsTotal: BigDecimal? = null,
    /**
     * The receipt's own printed SUBTOTAL (the sum of the line items before any fees/deposits) when it
     * prints one, else null. Unlike [expectedItemsTotal] this is never reconstructed from the grand
     * total, so it stays a clean item-sum anchor: fees and deposits sit *above* the subtotal, so a
     * shortfall of the read items against it means a line was dropped or under-read — not that a fee
     * went uncaptured. Drives the blocking "a line may be missing" check on review; null = no check.
     */
    val receiptSubtotal: BigDecimal? = null,
    /**
     * The receipt's tax/VAT. When [taxOnTop] is false it is *contained in* the line prices (the common
     * tax-inclusive receipt); when true it is *added on top* of the printed, net line prices (a
     * tax-exclusive receipt). Either way it's persisted and shown as an "incl. VAT" line; 0 when the
     * receipt reports no tax.
     */
    val tax: BigDecimal = BigDecimal.ZERO,
    /**
     * True for a tax-exclusive receipt (US sales tax, EU net invoice): [tax] is added on top of the
     * (kept-as-printed) line prices to reach what was paid, rather than being contained within them.
     */
    val taxOnTop: Boolean = false,
    /**
     * Money paid beyond the line items, [discount] and on-top [tax] — delivery & service fees plus a
     * courier tip on a delivery-app order, an uncaptured deposit, etc. The gap by which the printed
     * grand total exceeds what the items reconcile to; added to summed spend so totals equal what was
     * paid, without inventing product line items. 0 for an ordinary receipt that reconciles.
     */
    val extraCharges: BigDecimal = BigDecimal.ZERO,
    val items: List<ParsedTransaction> = emptyList(),
)
