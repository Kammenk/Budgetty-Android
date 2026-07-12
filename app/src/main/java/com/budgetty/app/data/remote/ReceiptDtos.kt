package com.budgetty.app.data.remote

/** Request body sent to the `extractReceipt` proxy. */
data class ExtractRequest(
    val fileBase64: String,
    val mimeType: String,
)

/** Structured receipt returned by the proxy. */
data class ExtractResponse(
    val storeName: String? = null,
    val date: String? = null,
    val discount: Double? = null,
    /** Receipt's printed grand total (amount actually paid, after discounts); null/0 if not printed. */
    val total: Double? = null,
    /** Printed sum of the line items before tax/fees (equals [total] when prices include tax); null/0 if absent. */
    val subtotal: Double? = null,
    /** Tax added on top of the item prices (0 when prices already include tax); null/0 if none. */
    val tax: Double? = null,
    /** Combined non-tip, non-product add-on charges — delivery + service + bag/booking/small-order
     *  fees, summed. Already part of [total]; materialized client-side as a "Delivery & fees" line
     *  item. null/0 when none is printed. */
    val deliveryAndFees: Double? = null,
    /** Gratuity/tip, kept separate from [deliveryAndFees]. Already part of [total]; materialized
     *  client-side as its own "Tip" line item. null/0 when none is printed. */
    val tip: Double? = null,
    /** Model's self-assessment: false when the image was too poor to read the line items reliably. */
    val readable: Boolean? = null,
    /** Article/item count printed on the receipt (e.g. "N АРТИКУЛА"); null/0 if not printed. */
    val printedItemCount: Int? = null,
    val items: List<ExtractedItem> = emptyList(),
)

data class ExtractedItem(
    val name: String = "",
    val quantity: Double? = null,
    val price: Double? = null,
    val category: String? = null,
)
