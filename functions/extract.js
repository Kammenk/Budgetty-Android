/**
 * Shared receipt-extraction core: the model calls, the on-server reconciliation guards, and the
 * Haiku-first tiering that escalates to Sonnet only when a guard trips.
 *
 * Deliberately free of firebase imports (uses only global `fetch`, Node 22+) so the offline eval
 * harness (eval/run-eval.js) can `require` it and exercise the EXACT tiering the Cloud Function runs —
 * change the escalation rule here and the eval covers the change without a deploy.
 */
const { MODEL, HAIKU_MODEL, RECORD_RECEIPT_TOOL, PROMPT } = require("./receiptPrompt");

// Per-1M-token list prices (USD), standard rates — the honest long-run number for the cost decision
// (Sonnet 5's intro $2/$10 lapses 2026-08-31; Haiku has no intro discount). Used by the eval to price a
// run and by callers that want a per-scan estimate. Keep in sync with platform.claude.com/pricing.
const PRICING = {
  "claude-sonnet-5": { in: 3.0, out: 15.0 },
  "claude-haiku-4-5": { in: 1.0, out: 5.0 },
};

// Cross-check thresholds — the on-device validateExtraction (HaikuReceiptExtractor.kt) recomputed here so
// the SERVER can decide the same "would the app reject this?" outcome the phone will. That outcome is both
// what we log and what the Haiku-first tier escalates on. KEEP IN SYNC with the client constants.
const MIN_PRINTED_COUNT_TO_CHECK = 3;
const MIN_COUNT_RATIO = 0.6;
const MAX_COUNT_RATIO = 1.5;
const MAX_OVERSHOOT_RATIO = 0.35;
const MAX_OVERSHOOT_ABS = 1.5;

/**
 * Number of ARTICLES the basket represents, as a receipt's own "N items" line counts them: a whole
 * multiplier ('4 X 0,34') contributes its N, while a weighed line ('0.775 kg') and any missing or
 * fractional quantity count as the single article they are. KEEP IN SYNC with the client's countUnits.
 */
function countUnits(items) {
  return items.reduce((sum, it) => {
    const qty = Math.round(Number(it.quantity) || 0);
    return sum + (qty >= 2 ? qty : 1);
  }, 0);
}

/**
 * Distills a scan into a compact, queryable diagnostic. `outcome` names the single on-device gate the
 * app would reject on (in the client's check order) or "ok" when it passes — so the logs classify
 * failures exactly as the user experiences them, and the tier escalates exactly when the app would balk.
 * Deliberately carries NO product names and no image: only counts, the receipt's own totals, the store,
 * and the model's self-assessment.
 */
function scanDiagnostics(input) {
  const items = Array.isArray(input.items) ? input.items : [];
  const itemCount = items.length;
  const unitCount = countUnits(items);
  const printedItemCount = Math.round(Number(input.printedItemCount) || 0);
  // Sum of the printed line totals, exactly as the app sums them (price = the extended line total),
  // so this `grossItems` and the overshoot below match what the client computes to the cent.
  const grossItems = items.reduce((sum, it) => sum + (Number(it.price) || 0), 0);
  const total = Number(input.total) || 0;
  const discount = Number(input.discount) || 0;

  // A receipt's printed count is EITHER its number of product lines or its number of units — Greek
  // 'ΣΥΝΟΛΟ ΕΙΔΩΝ' and many EU formats count units, so a basket with '6 X 1,42' multi-buy lines prints
  // far more than it has lines. Accept whichever reading lands in band; only a count that matches
  // NEITHER means we actually misread the receipt.
  const countChecked = printedItemCount >= MIN_PRINTED_COUNT_TO_CHECK;
  const inCountBand = (n) =>
    n >= printedItemCount * MIN_COUNT_RATIO && n <= printedItemCount * MAX_COUNT_RATIO;
  const countMismatch = countChecked && !inCountBand(itemCount) && !inCountBand(unitCount);

  const overshoot = total > 0 ? grossItems - discount - total : 0;
  const overshootTrips =
    total > 0 && overshoot > Math.max(total * MAX_OVERSHOOT_RATIO, MAX_OVERSHOOT_ABS);

  // Same order the client checks: the model's abstention first, then the two money/count cross-checks.
  let outcome = "ok";
  if (input.readable === false) outcome = "not_readable";
  else if (countMismatch) outcome = "count_mismatch";
  else if (overshootTrips) outcome = "overshoot";

  const round2 = (n) => Math.round(n * 100) / 100;
  return {
    outcome,
    readable: input.readable !== false,
    confidence: typeof input.confidence === "number" ? input.confidence : null,
    lowConfidenceFields: Array.isArray(input.lowConfidenceFields) ? input.lowConfidenceFields : [],
    storeName: input.storeName || "",
    itemCount,
    unitCount,
    printedItemCount,
    countMismatch,
    total,
    subtotal: Number(input.subtotal) || 0,
    tax: Number(input.tax) || 0,
    discount,
    grossItems: round2(grossItems),
    overshoot: round2(overshoot),
    overshootTrips,
  };
}

/** Dollar cost of a single call's `usage` block for `model`, at the PRICING list rates. */
function costOf(model, usage) {
  const p = PRICING[model];
  if (!p || !usage) return 0;
  return ((Number(usage.input_tokens) || 0) * p.in + (Number(usage.output_tokens) || 0) * p.out) / 1e6;
}

/**
 * One Anthropic Messages call with the shared prompt/tool, identical params to the deployed function.
 * Returns `{ input, usage }` (the tool_use input = the structured receipt, plus token usage for costing).
 * Throws an Error tagged `upstreamStatus` on a non-2xx (so the caller can map it to a 502) or `noToolUse`
 * when the model returned no tool_use block.
 */
async function callModel({ model, sourceBlock, apiKey }) {
  const body = {
    model,
    // A long receipt (49 items ≈ 2100 output tokens) needs headroom; 8192 clears a 40+ line haul.
    max_tokens: 8192,
    tools: [RECORD_RECEIPT_TOOL],
    tool_choice: { type: "tool", name: "record_receipt" },
    messages: [{ role: "user", content: [sourceBlock, { type: "text", text: PROMPT }] }],
  };
  // Sonnet 5 runs adaptive thinking when `thinking` is omitted, so it must be explicitly disabled to keep
  // extraction tight and deterministic. Haiku 4.5 has no thinking unless asked, and rejects a `disabled`
  // block — so omit the field there entirely.
  if (!model.startsWith("claude-haiku")) body.thinking = { type: "disabled" };

  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: { "content-type": "application/json", "x-api-key": apiKey, "anthropic-version": "2023-06-01" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const detail = await res.text();
    throw Object.assign(new Error(`Anthropic ${res.status}`), { upstreamStatus: res.status, detail });
  }
  const json = await res.json();
  const toolUse = (json.content || []).find((b) => b.type === "tool_use");
  if (!toolUse) {
    throw Object.assign(new Error("No tool_use block returned"), {
      noToolUse: true,
      detail: JSON.stringify(json).slice(0, 500),
    });
  }
  return { input: toolUse.input, usage: json.usage || { input_tokens: 0, output_tokens: 0 } };
}

/**
 * Extracts a receipt, optionally via the Haiku-first tier. With `haikuFirst`, reads on cheap Haiku and
 * ESCALATES to Sonnet only when the reconciliation guards trip (scanDiagnostics().outcome !== "ok") or
 * Haiku errors outright — i.e. exactly the reads the app would have rejected. Without it, one Sonnet call.
 *
 * Returns `{ input, diagnostics, modelUsed, escalated, usages, cost }`: `input` is the final structured
 * receipt to serve, `usages` is the per-call [{model, usage}] for cost auditing, `cost` sums them at
 * PRICING rates. Propagates the (final) call's error if extraction fails entirely.
 */
async function extractTiered({ sourceBlock, apiKey, haikuFirst }) {
  const usages = [];
  const finalize = (input, modelUsed, escalated) => {
    const cost = usages.reduce((sum, u) => sum + costOf(u.model, u.usage), 0);
    return { input, diagnostics: scanDiagnostics(input), modelUsed, escalated, usages, cost };
  };

  if (haikuFirst) {
    let haiku = null;
    try {
      haiku = await callModel({ model: HAIKU_MODEL, sourceBlock, apiKey });
      usages.push({ model: HAIKU_MODEL, usage: haiku.usage });
    } catch (e) {
      // Haiku failed to return a usable read — fall through and let Sonnet handle it.
      haiku = null;
    }
    // Accept Haiku's read only when it clears every guard the app checks; otherwise escalate.
    if (haiku && scanDiagnostics(haiku.input).outcome === "ok") {
      return finalize(haiku.input, HAIKU_MODEL, false);
    }
    const sonnet = await callModel({ model: MODEL, sourceBlock, apiKey });
    usages.push({ model: MODEL, usage: sonnet.usage });
    return finalize(sonnet.input, MODEL, true);
  }

  const sonnet = await callModel({ model: MODEL, sourceBlock, apiKey });
  usages.push({ model: MODEL, usage: sonnet.usage });
  return finalize(sonnet.input, MODEL, false);
}

module.exports = { PRICING, scanDiagnostics, costOf, callModel, extractTiered };
