package com.budgetty.app.data.model

import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.TransactionEntity
import java.math.BigDecimal

/**
 * The per-receipt charges that sit ON TOP of the (summed, net) line prices for the receipts these
 * [txns] belong to — added to a summed net spend to reach what was actually paid, while leaving the
 * individual line prices untouched. Two whole-receipt components:
 *
 *  - **on-top tax**: a tax-exclusive receipt (US sales tax, an EU net invoice) keeps its line items at
 *    the printed net prices and carries the tax separately ([ReceiptEntity.taxOnTop] / [ReceiptEntity.tax]).
 *  - **extra charges**: money paid beyond items, discount and tax — delivery & service fees plus a
 *    courier tip on a delivery-app order, an uncaptured deposit ([ReceiptEntity.extraCharges]).
 *
 * A tax-inclusive receipt whose items already reconcile to its total contributes nothing here. Each
 * receipt is counted once regardless of how many of its line items are in [txns] — pass the
 * transactions for the period/receipt and a lookup of all receipts.
 */
fun additiveChargesOf(
    txns: List<TransactionEntity>,
    receiptsById: Map<Long, ReceiptEntity>,
): BigDecimal =
    txns.mapTo(mutableSetOf()) { it.receiptId }
        .fold(BigDecimal.ZERO) { acc, id ->
            val receipt = receiptsById[id] ?: return@fold acc
            val onTopTax = if (receipt.taxOnTop) receipt.tax else BigDecimal.ZERO
            acc + onTopTax + receipt.extraCharges
        }

/**
 * The net per-receipt adjustment that turns a summed net line-price spend into what was actually
 * PAID: [additiveChargesOf] (on-top tax + extra charges) added, minus each receipt's order
 * [ReceiptEntity.discount] subtracted. Each receipt is counted once regardless of how many of its
 * line items are in [txns] — pass the transactions for the period/receipt and a lookup of all receipts.
 *
 * Use this (not [additiveChargesOf]) for every spend total shown to the user: a discount lowers what
 * was paid just as tax/fees raise it, so both must land on the same figure for the totals to match
 * the receipt's own printed grand total (and the finalize/Save button, which nets the discount too).
 */
fun paidAdjustmentOf(
    txns: List<TransactionEntity>,
    receiptsById: Map<Long, ReceiptEntity>,
): BigDecimal {
    val discounts = txns.mapTo(mutableSetOf()) { it.receiptId }
        .fold(BigDecimal.ZERO) { acc, id -> acc + (receiptsById[id]?.discount ?: BigDecimal.ZERO) }
    return additiveChargesOf(txns, receiptsById).subtract(discounts)
}
