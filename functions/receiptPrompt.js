/**
 * Single source of truth for the receipt-extraction model config: the category list, model id,
 * tool schema, and prompt. Imported by both the Cloud Function (index.js) and the offline eval
 * harness (eval/run-eval.js) so the eval always exercises the *real* prompt — change it here and
 * the regression suite covers the change. No firebase imports here on purpose: this module must be
 * requireable from a plain Node script without side effects.
 */

// The model's assignable categories: every name here must also exist in Categories.predefined on
// the client (which validates the response and drops anything unknown). Kept in sync with that set,
// EXCEPT "Investments" — a recurring-payments-only category, never a scanned receipt line item, so
// it's deliberately omitted here so the model can't file a purchased item under it.
const CATEGORIES = [
  "Groceries", "Bakery", "Dairy", "Meat & Poultry", "Fish & Seafood", "Fruits & Vegetables",
  "Snacks & Sweets", "Frozen Foods", "Nuts & Snacks", "Canned & Preserved", "Grains & Pasta",
  "Condiments & Sauces", "Beverages",
  "Household & Personal", "Household Cleaning", "Personal Care", "Beauty", "Baby Products",
  "Pet Supplies", "Paper Products", "Kitchen Supplies",
  "Health & Wellness", "Health & Pharmacy", "Medical", "Sports & Fitness",
  "Dining & Entertainment", "Restaurant & Dining", "Entertainment", "Video Games",
  "Shopping & Lifestyle", "Clothing & Accessories", "Electronics", "Garden & Plants",
  "Home Improvement", "Tobacco & Alcohol",
  "Transportation", "Fuel", "Car Maintenance",
  "Services & Subscriptions", "Subscriptions & Services", "Education", "Travel & Accommodation",
  "Insurance & Utilities", "Rent", "Office & Work Supplies", "Gifts & Charitable Donations",
  "Other",
];

const MODEL = "claude-sonnet-5"; // Claude Sonnet 5 — high-resolution vision (2576px) for small Cyrillic receipt text

const RECORD_RECEIPT_TOOL = {
  name: "record_receipt",
  description: "Record the store and every line item extracted from the receipt.",
  input_schema: {
    type: "object",
    properties: {
      storeName: {
        type: "string",
        description: "The store/merchant name from the receipt header. Empty string if unknown.",
      },
      date: {
        type: "string",
        description: "Receipt date as YYYY-MM-DD. Empty string if not printed.",
      },
      discount: {
        type: "number",
        description: "Total discount/savings on the receipt (0 if none).",
      },
      total: {
        type: "number",
        description:
          "Final total actually paid — the grand total after all discounts — in the same currency " +
          "as the line items. 0 if not printed.",
      },
      subtotal: {
        type: "number",
        description:
          "The receipt's printed sum of all line items BEFORE tax and fees (commonly labelled " +
          "SUBTOTAL / Zwischensumme / Sous-total). When item prices already include tax so nothing " +
          "is added on top, this equals the total. 0 if the receipt prints no such line.",
      },
      tax: {
        type: "number",
        description:
          "Tax ADDED ON TOP of the item prices (added sales/VAT tax shown as a separate charge). " +
          "0 when prices already include tax or no tax is added. Read it; do not compute it.",
      },
      readable: {
        type: "boolean",
        description:
          "true ONLY if the image was clear enough to actually read the product lines. false if it " +
          "is too blurry, crumpled, dark, glare-washed, or low-resolution to read the products — in " +
          "that case return an empty items array and do NOT guess.",
      },
      confidence: {
        type: "number",
        description:
          "Your overall confidence from 0 to 1 that the extracted store, totals and every line item " +
          "are correct as printed. Lower it when the image is marginal, text is partly obscured or cut " +
          "off, or you had to strain to read a price or name. A self-assessment for quality monitoring " +
          "only — it does NOT change what you extract.",
      },
      lowConfidenceFields: {
        type: "array",
        items: { type: "string" },
        description:
          "The specific fields you are unsure about — use 'storeName', 'date', 'total', 'subtotal', " +
          "'tax', 'discount', or the name of an item whose name or price you doubt. Empty array when " +
          "fully confident.",
      },
      printedItemCount: {
        type: "number",
        description:
          "The article/item count printed on the receipt (e.g. 'N АРТИКУЛА', 'N items', 'Artikel: N'). " +
          "Read it verbatim from the receipt; 0 if no such count is printed.",
      },
      items: {
        type: "array",
        description: "One entry per purchased product line.",
        items: {
          type: "object",
          properties: {
            name: { type: "string", description: "Product name as printed on the receipt." },
            quantity: { type: "number", description: "Quantity bought. Default 1 if not shown." },
            price: {
              type: "number",
              description:
                "Printed line price BEFORE any discount/coupon, in the receipt's currency " +
                "(the regular price shown for the line).",
            },
            category: { type: "string", enum: CATEGORIES, description: "Best-fitting category." },
          },
          required: ["name", "price", "category"],
        },
      },
    },
    required: ["storeName", "date", "discount", "total", "subtotal", "tax", "readable", "confidence", "lowConfidenceFields", "printedItemCount", "items"],
  },
};

const PROMPT =
  "You are a receipt parser. Read this receipt (it may be in Bulgarian) and extract every " +
  "purchased line item. For each item give the product name exactly as printed, the quantity " +
  "(default 1), and the printed line price BEFORE any discount or coupon (the regular price shown " +
  "for that line) as a number. For a line bought in multiples (e.g. '2 x 1.50', '3 @ 0.99', '12@55'), " +
  "the price is the line's EXTENDED total for all units (e.g. 3.00 for '2 x 1.50'), with quantity 2 — " +
  "do not report the per-unit price. For a weighed item (e.g. '0.775 kg x 5.90/kg'), the price is the " +
  "line total charged (e.g. 4.57). A pack size or count that is part of a product NAME (e.g. 'Yaourt " +
  "nature x8', 'Piles AA x4', 'Eau 6x1,5L', '6 eggs') is NOT a quantity — set quantity to 1 and price to " +
  "that line's printed total; only a separate 'N x price' / 'N @ price' multiplier line makes quantity N. " +
  "Each product's price is the amount printed on the SAME row as that product's name. A standalone " +
  "'N x price' or 'weight x price/kg' line belongs to exactly ONE product — the one whose printed line " +
  "total equals that multiplication — so never copy a price or a multiplier from one row onto a different " +
  "product, even when weight, multiplier or discount lines are interleaved between the product rows. " +
  "Assign each item to the single best-fitting " +
  "category from the allowed list — prefer the most specific sub-category (e.g. bread -> Bakery, " +
  "milk/cheese -> Dairy, chicken -> Meat & Poultry, coffee/tea/soda -> Beverages, shampoo -> Personal Care); " +
  "if only the broad area is clear, use the umbrella group (e.g. Groceries, Transportation); use Other if " +
  "nothing fits. For a fuel/petrol receipt, output ONE line item for the fuel whose price is the TOTAL amount " +
  "paid for it — the fuel total / amount due (e.g. MONTANT, MONTANT REEL, TOTAL), which is the litres times the " +
  "price per litre. Never use the number of litres or the price-per-litre as the price, and set its quantity to 1. " +
  "Also capture: the store/merchant name from the header; the receipt date as YYYY-MM-DD " +
  "(empty string if not printed); and the total discount/savings as a number (0 if none) — include every " +
  "reduction you can see: per-line markdowns (e.g. 'Positionsrabatt', 'Reduced to Clear'), multibuy savings " +
  "and coupons. A reduction line (e.g. ОТСТЪПКА / ОТСТЪПКИ, отстъпка, Rabatt, XTRA) can appear BETWEEN " +
  "product rows; it is a discount, never a product, and its amount must NOT be attached to a neighbouring " +
  "product — sum it into the discount instead. " +
  "ALWAYS read the receipt's printed GRAND TOTAL — the final amount due / actually paid (e.g. TOTAL, " +
  "ОБЩА СУМА, ZU ZAHLEN, SUMME, MONTANT, סה\"כ) — into the `total` field whenever any grand total is " +
  "printed. Put it in `total`, never only in `subtotal`, and never leave `total` 0 when a grand total is " +
  "shown. Deposits (Pfand), bag and service fees and tips are part of what was paid, so they belong in the " +
  "total. Do NOT compute the total by subtracting change/return (Zurück, CHANGE, RESTO, עודף, ресто) from " +
  "the cash tendered — read the printed total line itself. " +
  "The total must be in the same currency as the line items (0 only if genuinely not printed). " +
  "Also capture the items subtotal — the receipt's printed sum of all line items " +
  "before tax and fees (commonly labelled SUBTOTAL); when item prices already include tax so nothing is added on " +
  "top, this equals the total; use 0 if the receipt prints no such line. And capture the tax added on top of the " +
  "item prices (a separate sales/VAT charge), using 0 when prices already include tax or none is added; read these " +
  "values, do not compute them. Do not create line items for subtotals, taxes, loyalty, coupon, or payment " +
  "lines — only real products. " +
  "Also report printedItemCount: the article/item count the receipt prints (e.g. 'N АРТИКУЛА', " +
  "'N items', 'TOTAL ITEMS: N'), or 0 if none is printed. " +
  "IMPORTANT: never invent or guess. Only report a store, item, price, or total you can actually " +
  "read on the receipt. If the photo is too blurry, crumpled, dark, or low-resolution to read the " +
  "product lines reliably, set readable to false and return an empty items array instead of " +
  "filling in plausible-looking products. Set readable to true only when you genuinely read the lines. " +
  "Finally, report your overall confidence from 0 to 1 that everything you extracted matches what is " +
  "printed (lower it for a marginal image or any value you had to strain to read), and list in " +
  "lowConfidenceFields the specific fields you are unsure about (an empty array when fully confident). " +
  "These are self-assessments for quality monitoring and must NOT change what you extract. " +
  "Call the record_receipt tool with the result.";

module.exports = { CATEGORIES, MODEL, RECORD_RECEIPT_TOOL, PROMPT };
