/**
 * Budgetty Cloud Function — receipt extraction proxy.
 *
 * Receives a receipt (base64 image or PDF) from the signed-in app, calls the Anthropic Messages API to
 * extract structured, already-categorized line items, and returns them. The Anthropic API key lives ONLY
 * here as a secret — never on the device. Unauthenticated callers are rejected.
 *
 * Model selection: by default Sonnet 5 (one call). Set env HAIKU_FIRST=on to enable the Haiku-first tier
 * (extract.js) — a cheap Haiku read that escalates to Sonnet only when the reconciliation guards trip.
 * The toggle defaults OFF so shipping this code changes nothing until it's flipped for the live test.
 */
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");

// Prompt, tool schema, model ids, the reconciliation guards, and the Haiku-first tiering live in shared
// modules so the offline eval harness (eval/run-eval.js) exercises the exact prompt AND tiering the
// deployed function uses — change either and the regression suite covers the change.
const { extractTiered } = require("./extract");

// Live A/B switch for the Haiku-first tier — read once at cold start. Set HAIKU_FIRST=on on the Cloud Run
// service to route reads through Haiku (escalating to Sonnet on a guard trip); unset/anything-else keeps
// the Sonnet-only path. Lets the tier be turned on and back off without a code change.
const HAIKU_FIRST = process.env.HAIKU_FIRST === "on";

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
      const result = await extractTiered({
        sourceBlock,
        apiKey: ANTHROPIC_API_KEY.value(),
        haikuFirst: HAIKU_FIRST,
      });
      const input = result.input;
      const diag = result.diagnostics;

      // One structured line per scan. In Cloud Logging, filter by jsonPayload.event="scan_result".
      // `outcome != "ok"` is exactly the set the app rejects on-device. `model`/`escalated` measure the
      // Haiku-first tier live: jsonPayload.escalated=true is the Haiku-miss rate, jsonPayload.model the
      // serving mix. `cost` is the per-scan USD at list rates. No product names or image are logged.
      const scanLog = {
        event: "scan_result",
        mimeType,
        model: result.modelUsed,
        escalated: result.escalated,
        cost: Math.round(result.cost * 1e6) / 1e6,
        ...diag,
      };
      if (diag.outcome === "ok") logger.info("scan_result", scanLog);
      else logger.warn("scan_result", scanLog);

      res.json({
        storeName: input.storeName || "",
        date: input.date || "",
        discount: input.discount || 0,
        total: input.total || 0,
        subtotal: input.subtotal || 0,
        tax: input.tax || 0,
        // Default to readable when the field is absent, so a missing flag never blocks a scan;
        // only an explicit `false` from the model signals "too poor to read".
        readable: input.readable !== false,
        printedItemCount: Math.round(input.printedItemCount || 0),
        items: input.items || [],
      });
    } catch (e) {
      // A billing outage (HTTP 400 "credit balance too low") surfaces here as an upstream error on the
      // final call; keeping a truncated detail makes that diagnosable from the logs instead of a guess.
      if (e && e.upstreamStatus) {
        logger.error("scan_result", {
          event: "scan_result",
          outcome: "service_error",
          httpStatus: e.upstreamStatus,
          detail: String(e.detail || "").slice(0, 500),
        });
        res.status(502).json({ error: "Extraction service error" });
        return;
      }
      if (e && e.noToolUse) {
        logger.error("scan_result", { event: "scan_result", outcome: "no_tool_use", detail: e.detail });
        res.status(502).json({ error: "No structured output returned" });
        return;
      }
      logger.error("scan_result", {
        event: "scan_result",
        outcome: "internal_error",
        detail: String((e && e.message) || e),
      });
      res.status(500).json({ error: "Internal error" });
    }
  }
);
