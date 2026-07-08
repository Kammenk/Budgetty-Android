# Receipt-extraction eval harness

A small regression gate for the receipt parser. It runs the **real** extraction prompt
(`functions/receiptPrompt.js`, shared with the deployed Cloud Function) against a corpus of labeled
receipts and reports per-receipt pass/fail.

## Why it exists

The prompt is **global** — one instruction is applied to every receipt in the world. There's no way
to tune for one format (e.g. a Bulgarian greengrocer receipt) without affecting all others, and a
regression on, say, US tax-exclusive receipts is silent — users just get wrong numbers. This harness
is the safety net: **run it before and after any prompt or schema change** and diff the results.

It calls the Anthropic API directly with the same model, `temperature: 0`, and tool as production,
so you can validate a prompt edit **without deploying**.

## Running

```bash
cd functions
ANTHROPIC_API_KEY=sk-ant-...  node eval/run-eval.js                 # whole corpus (+ coverage summary)
ANTHROPIC_API_KEY=sk-ant-...  node eval/run-eval.js bg-stasi-produce # one case
node eval/run-eval.js --coverage                                    # format-gap report, no key/network
```

Use the same Anthropic key the Cloud Function uses. The key is read from the environment and never
written anywhere. Each run costs a few API tokens per receipt; `temperature: 0` keeps it ~repeatable.

## Coverage — what "launch-ready" means here

The parser is **store-agnostic**: it's one Claude prompt, so it has no per-merchant logic. What breaks
extraction isn't the store's *identity* (Kroger vs Ralphs) but the receipt's *format* — tax model
(EU VAT-inclusive vs US tax-on-top), language/script, number format, deposits, coupons, weighted
items, delivery-app fees, unreadable photos. So coverage is measured in **format families**, not brands.

Each case carries a `tags` array; `run-eval.js --coverage` maps those tags onto the launch plan
(defined in `FAMILIES` at the top of `run-eval.js`) and prints, per family:

- ✅ **COVERED** — a matching case has a real receipt image dropped in.
- 🟡 **PENDING** — a stub exists (tagged, planned) but no receipt image yet (`*` next to its id).
- ⬜ **MISSING** — no case for this family at all.

The launch bar (Bulgaria + EU first; North America is phase 2) is: **every phase-1 family COVERED and
a green run.** Tags use namespaced values — `region:bg|eu|na`, `lang:bg|de|fr|…`,
`type:supermarket|discounter|service|pharmacy|fuel|restaurant|delivery|bigbox`,
`tax:inclusive|onTop`, `feature:weighted|coupon|deposit|voided|multiVat|dualCurrency|longHaul|unreadable`.
One receipt can (and should) satisfy several families — a long Lidl haul with a Lidl Plus coupon covers
supermarket + longHaul + coupon at once.

### Case states

| state | when | shown as |
|---|---|---|
| SKIP | no `expected.json`, or no receipt image yet | pending — collect the receipt |
| FAIL (stub) | image present but `expected.json` has no assertable field | you added a photo but never labelled it |
| PASS / FAIL | image + at least one assertion | the actual regression check |

## Adding a receipt (the important part)

A corpus of one proves nothing. Grow it toward **10–20 deliberately diverse** receipts so a change
that helps one format and hurts another shows up:

- EU **VAT-inclusive** (items sum to the total) — incl. the Bulgarian ones.
- US/Canada **tax-exclusive** (items sum to the *subtotal*; `total = subtotal + tax`).
- A **restaurant** bill, a **fuel** receipt, a long **supermarket** receipt.
- One with a **coupon/discount**, one with a **deposit/bag fee**.
- A deliberately **blurry/crumpled** one (should come back `readable: false`).

To add a case:

1. `mkdir eval/corpus/<id>/` and drop the receipt in as `receipt.jpg` (or `.png`/`.pdf`).
   Images are **gitignored** — they're personal data and are not committed.
2. Create `eval/corpus/<id>/expected.json` with the hand-verified truth. Assertion fields are optional
   and the harness only checks the ones you provide — but **at least one is required** (a case with an
   image and no assertions FAILs as an unlabelled stub, so a half-finished case can't show green). Keys
   starting with `_` are ignored (use them for notes); `tags` drives the coverage report, not the check.

   | field | meaning |
   |---|---|
   | `tags` | format-family labels for the coverage report (see Coverage above); not asserted |
   | `readable` | expected readable flag (`false` for an intentionally unreadable photo) |
   | `printedItemCount` | the "N items / N АРТИКУЛА" count printed on the receipt |
   | `itemCount` | how many line items extraction should return |
   | `total` | printed grand total (± 0.02) |
   | `discount` | expected discount (± 0.02) |
   | `tax` | expected tax added on top (± 0.02; `0` for VAT-inclusive) |
   | `lineTotals` | array of each line's `price × qty`; compared as a sorted multiset (± 0.02) — this is what catches a misread digit |
   | `expectedNames` | product-name substrings that must each appear (case-insensitive) |

   The corpus already ships **stub** cases (tagged, no image) for every launch-critical family — run
   `node eval/run-eval.js --coverage` to see them, then just drop a receipt into the 🟡 PENDING folders.

See `corpus/bg-stasi-produce/expected.json` for a worked example.

## Synthetic fixtures

Two launch-critical families that are hard to shop for locally (a German discounter with Pfand, a
Western-EU multi-VAT supermarket) ship as **synthetic** specimens under `de-discounter-pfand/` and
`eu-supermarket-multivat/`. They're built by `tools/gen-synthetic-receipts.mjs` (headless Chrome renders
an HTML receipt to PNG; the totals + `expected.json` truth are **computed from the item data**, so they
can't drift from the image). These are the only receipt images tracked in git — they carry no personal
data. Regenerate after editing the generator:

```bash
node eval/tools/gen-synthetic-receipts.mjs
```

They exercise FORMAT logic (VAT-inclusive totals, comma decimals, multi-rate VAT summaries that must
NOT leak into `tax`, deposits, loyalty discounts, non-Cyrillic reading) — not the noise of a real
photographed thermal receipt. **Swap in a real Lidl/Aldi and Carrefour/Tesco/AH receipt when you have
one** (drop it in as `receipt.jpg`; the harness picks the first image, so remove the synthetic `.png`).

## Exit codes

`0` all passed · `1` at least one failed · `2` setup problem (no key / no cases).
