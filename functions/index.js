/**
 * Budgetty Cloud Function — receipt extraction proxy.
 *
 * Receives a receipt (base64 image or PDF) from the signed-in app, calls the
 * Anthropic Messages API (Claude Sonnet 5) to extract structured, already-categorized
 * line items, and returns them. The Anthropic API key lives ONLY here as a secret —
 * never on the device. Unauthenticated callers are rejected.
 */
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");

// Prompt, tool schema and model id live in a shared module so the offline eval harness
// (eval/run-eval.js) exercises the exact same prompt the deployed function uses.
const { MODEL, RECORD_RECEIPT_TOOL, PROMPT } = require("./receiptPrompt");

// Cross-check thresholds mirror the client's on-device validateExtraction (HaikuReceiptExtractor.kt).
// We recompute them here only to LOG whether the app will reject an extraction: that rejection happens
// on the phone AFTER we return 200, so the server otherwise never sees its own most useful failures.
// Keep these in sync with the client constants.
const MIN_PRINTED_COUNT_TO_CHECK = 3;
const MIN_COUNT_RATIO = 0.6;
const MAX_COUNT_RATIO = 1.5;
const MAX_OVERSHOOT_RATIO = 0.35;
const MAX_OVERSHOOT_ABS = 1.5;

/**
 * Distills a scan into a compact, queryable diagnostic. `outcome` names the single on-device gate the
 * app would reject on (in the client's check order) or "ok" when it passes — so the logs classify
 * failures exactly as the user experiences them. Deliberately carries NO product names and no image:
 * only counts, the receipt's own totals, the store, and the model's self-assessment.
 */
function scanDiagnostics(input) {
  const items = Array.isArray(input.items) ? input.items : [];
  const itemCount = items.length;
  const printedItemCount = Math.round(Number(input.printedItemCount) || 0);
  // Sum of the printed line totals, exactly as the app sums them (price = the extended line total),
  // so this `grossItems` and the overshoot below match what the client computes to the cent.
  const grossItems = items.reduce((sum, it) => sum + (Number(it.price) || 0), 0);
  const total = Number(input.total) || 0;
  const discount = Number(input.discount) || 0;

  const countChecked = printedItemCount >= MIN_PRINTED_COUNT_TO_CHECK;
  const countMismatch =
    countChecked &&
    (itemCount < printedItemCount * MIN_COUNT_RATIO || itemCount > printedItemCount * MAX_COUNT_RATIO);

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

exports.extractReceipt = onRequest(
  {
    region: "europe-west1",
    secrets: [ANTHROPIC_API_KEY],
    maxInstances: 5,
    timeoutSeconds: 120,
    memory: "512MiB",
    // Let Cloud Run pass requests through to our code; we enforce the Firebase
    // ID-token check inside, so the endpoint is NOT actually open.
    invoker: "public",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    // Require a valid Firebase ID token — no open endpoint.
    const match = /^Bearer (.+)$/.exec(req.headers.authorization || "");
    if (!match) {
      res.status(401).json({ error: "Missing Authorization bearer token" });
      return;
    }
    try {
      await admin.auth().verifyIdToken(match[1]);
    } catch (e) {
      res.status(401).json({ error: "Invalid auth token" });
      return;
    }

    const { fileBase64, mimeType } = req.body || {};
    if (!fileBase64 || !mimeType) {
      res.status(400).json({ error: "Missing fileBase64 or mimeType" });
      return;
    }

    const sourceBlock =
      mimeType === "application/pdf"
        ? { type: "document", source: { type: "base64", media_type: "application/pdf", data: fileBase64 } }
        : { type: "image", source: { type: "base64", media_type: mimeType, data: fileBase64 } };

    try {
      const anthropicRes = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-api-key": ANTHROPIC_API_KEY.value(),
          "anthropic-version": "2023-06-01",
        },
        body: JSON.stringify({
          model: MODEL,
          // Long hauls (a 49-item receipt needs ~2100 output tokens, and Sonnet 5's tokenizer runs
          // ~30% higher) were silently truncated at 2048 — the tool_use JSON got cut off, losing the
          // grand total or the tail of the items. 8192 clears a full 40+ line receipt with headroom.
          max_tokens: 8192,
          // Extraction, not creative writing: keep decoding tight so the model reads what's actually on
          // the receipt instead of confabulating on a hard (blurry/curled) photo. Sonnet 5 rejects a
          // non-default `temperature` (400), so determinism now comes from disabling thinking + the
          // forced tool_choice + the tool schema, rather than the temperature:0 the old model used.
          thinking: { type: "disabled" },
          tools: [RECORD_RECEIPT_TOOL],
          tool_choice: { type: "tool", name: "record_receipt" },
          messages: [{ role: "user", content: [sourceBlock, { type: "text", text: PROMPT }] }],
        }),
      });

      if (!anthropicRes.ok) {
        const detail = await anthropicRes.text();
        // A billing outage (HTTP 400 "credit balance too low") shows up here as an every-scan failure;
        // keeping a truncated detail makes that diagnosable from the logs instead of a guess.
        logger.error("scan_result", {
          event: "scan_result",
          outcome: "service_error",
          httpStatus: anthropicRes.status,
          detail: detail.slice(0, 500),
        });
        res.status(502).json({ error: "Extraction service error" });
        return;
      }

      const data = await anthropicRes.json();
      const toolUse = (data.content || []).find((b) => b.type === "tool_use");
      if (!toolUse) {
        logger.error("scan_result", {
          event: "scan_result",
          outcome: "no_tool_use",
          detail: JSON.stringify(data).slice(0, 500),
        });
        res.status(502).json({ error: "No structured output returned" });
        return;
      }

      const diag = scanDiagnostics(toolUse.input);
      // One structured line per scan. In Cloud Logging, filter by jsonPayload.event="scan_result".
      // `outcome != "ok"` is exactly the set the app rejects on-device — the failures the server never
      // saw before. A low-confidence read still returns 200 (the app shows it); query
      // jsonPayload.confidence to surface those. No product names or image are logged.
      const scanLog = { event: "scan_result", mimeType, ...diag };
      if (diag.outcome === "ok") logger.info("scan_result", scanLog);
      else logger.warn("scan_result", scanLog);

      res.json({
        storeName: toolUse.input.storeName || "",
        date: toolUse.input.date || "",
        discount: toolUse.input.discount || 0,
        total: toolUse.input.total || 0,
        subtotal: toolUse.input.subtotal || 0,
        tax: toolUse.input.tax || 0,
        // Default to readable when the field is absent, so a missing flag never blocks a scan;
        // only an explicit `false` from the model signals "too poor to read".
        readable: toolUse.input.readable !== false,
        printedItemCount: Math.round(toolUse.input.printedItemCount || 0),
        items: toolUse.input.items || [],
      });
    } catch (e) {
      logger.error("scan_result", {
        event: "scan_result",
        outcome: "internal_error",
        detail: String((e && e.message) || e),
      });
      res.status(500).json({ error: "Internal error" });
    }
  }
);
