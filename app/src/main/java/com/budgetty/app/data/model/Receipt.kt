package com.budgetty.app.data.model

import com.budgetty.app.data.local.TransactionEntity
import java.math.BigDecimal

/**
 * A receipt: the transactions saved together from one upload (sharing [timestamp]), the [store]
 * they came from, their combined [price], and any [discount]. Derived from [TransactionEntity]s
 * grouped by timestamp and joined with the receipts table — not itself a database table.
 *
 * [price] is the amount paid before [discount]: the summed line totals, plus any tax added on top for
 * a tax-exclusive receipt ([com.budgetty.app.data.local.ReceiptEntity.taxOnTop]) and any extra charges
 * paid beyond the items ([com.budgetty.app.data.local.ReceiptEntity.extraCharges] — delivery/service
 * fees, a courier tip) — so it reflects what was paid even though the individual line prices are kept net.
 */
data class Receipt(
    val id: Long,
    val store: String,
    val transactions: List<TransactionEntity>,
    val timestamp: Long,
    val price: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    /** Tax/VAT contained in [price] (see [com.budgetty.app.data.local.ReceiptEntity.tax]); shown as "incl. VAT". */
    val tax: BigDecimal = BigDecimal.ZERO,
) {
    /**
     * What was actually paid: the pre-discount [price] less the order [discount], never below zero.
     * This is the figure to show as the receipt's headline total (it matches the finalize/Save button);
     * [price] stays the gross total and [discount] is shown separately as the savings line.
     */
    val paid: BigDecimal get() = (price - discount).coerceAtLeast(BigDecimal.ZERO)
}
