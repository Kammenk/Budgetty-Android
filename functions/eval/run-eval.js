#!/usr/bin/env node
/**
 * Budgetty receipt-extraction eval harness.
 *
 * Runs the REAL Cloud Function prompt (../receiptPrompt.js) against a corpus of labeled receipts
 * and reports per-receipt + aggregate pass/fail. Purpose: a regression gate to run BEFORE and AFTER
 * any prompt/schema change, so a tweak aimed at one receipt format can't silently break others.
 *
 * It calls the Anthropic API directly (same model/params/tool/tiering as the deployed function) so you
 * can test a prompt edit WITHOUT deploying. Uses only Node built-ins — no extra dependencies.
 *
 * Usage:
 *   ANTHROPIC_API_KEY=sk-ant-... node eval/run-eval.js          # whole corpus, Sonnet-only (baseline)
 *   ANTHROPIC_API_KEY=sk-ant-... node eval/run-eval.js --tiered # Haiku-first (escalate to Sonnet on a guard trip)
 *   ANTHROPIC_API_KEY=sk-ant-... node eval/run-eval.js bg-stasi-produce   # one case
 * Each run prints its API cost; run once each way and compare pass rate + cost for the tiering decision.
 *
 * Each corpus case is a folder under eval/corpus/<id>/ containing:
 *   - the receipt image/PDF (jpg/jpeg/png/pdf) — your real receipt, gitignored, never committed
 *   - expected.json — the hand-verified ground truth (see eval/README.md for the schema)
 */
const fs = require("fs");
const path = require("path");
const { extractTiered } = require("../extract");

const CORPUS_DIR = path.join(__dirname, "corpus");
const MONEY_TOL = 0.02; // currency tolerance per line and on totals — absorbs cent rounding, not a misread.
const MIME = { ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".pdf": "application/pdf" };

// Fields in expected.json the harness can actually assert on. A case that specifies none of these
// (only tags/notes) is treated as an unfilled stub, not a vacuous pass. Keys starting with "_" and
// "tags" are metadata, never assertions.
const ASSERT_KEYS = ["readable", "printedItemCount", "itemCount", "outcome", "total", "discount", "tax", "lineTotals", "expectedNames"];

// Launch coverage plan: the receipt FORMATS we must parse, each a predicate over an expected.json's
// `tags` (`has` = all present, `not` = none present). A family is COVERED once a matching case has a
// real receipt image dropped in; PENDING while only a stub exists; MISSING with no case at all. This
// turns "cover all the stores" into a measurable gap list. Weighted Bulgaria + EU first (the app
// ships EUR default + Bulgarian-first UI); North America is phase 2.
const FAMILIES = [
  { phase: 1, label: "BG supermarket, VAT-inclusive, Cyrillic",           has: ["type:supermarket", "lang:bg"] },
  { phase: 1, label: "BG service invoice, tax-on-top (net + VAT)",        has: ["type:service", "tax:onTop"] },
  { phase: 1, label: "BG pharmacy / drugstore",                           has: ["type:pharmacy"] },
  { phase: 1, label: "BG fuel (liters x price/L, single line)",           has: ["type:fuel"] },
  { phase: 1, label: "BG restaurant (service/tip line)",                  has: ["type:restaurant"] },
  { phase: 1, label: "EU discounter with deposit (Lidl/Aldi Pfand)",      has: ["type:discounter", "feature:deposit"] },
  { phase: 1, label: "Western-EU supermarket, comma-decimal + multi-VAT", has: ["type:supermarket", "feature:multiVat"], not: ["lang:bg"] },
  { phase: 1, label: "Coupon / loyalty deduction",                        has: ["feature:coupon"] },
  { phase: 1, label: "Delivery-app order summary (fees/tip)",             has: ["type:delivery"], not: ["region:na"] },
  { phase: 1, label: "Weighted produce (price/kg)",                       has: ["feature:weighted"] },
  { phase: 1, label: "Multi-buy lines + printed UNIT count ('4 X 0,34')", has: ["feature:multiBuy"] },
  { phase: 1, label: "Long 40+ item supermarket haul",                    has: ["feature:longHaul"] },
  { phase: 1, label: "Unreadable photo -> readable:false",                has: ["feature:unreadable"] },
  { phase: 2, label: "US/CA supermarket, tax-on-top ($)",                 has: ["region:na", "type:supermarket", "tax:onTop"] },
  { phase: 2, label: "Warehouse / big-box (item codes, bulk)",            has: ["type:bigbox"] },
  { phase: 2, label: "US delivery w/ substitutions (Instacart)",          has: ["region:na", "type:delivery"] },
];

function findImage(caseDir) {
  return fs
    .readdirSync(caseDir)
    .map((f) => path.join(caseDir, f))
    .find((f) => MIME[path.extname(f).toLowerCase()]);
}

/** Builds the Anthropic image/document source block for a corpus file (base64-encoded). */
function sourceBlockFor(imagePath) {
  const mimeType = MIME[path.extname(imagePath).toLowerCase()];
  const data = fs.readFileSync(imagePath).toString("base64");
  return mimeType === "application/pdf"
    ? { type: "document", source: { type: "base64", media_type: "application/pdf", data } }
    : { type: "image", source: { type: "base64", media_type: mimeType, data } };
}

/**
 * Effective per-line totals as the APP actually records them, sorted. Mirrors
 * HaikuReceiptExtractor.toParsedTransaction: the model returns `price` as the LINE total, and a
 * whole quantity (>=2) is split into unit×count so it is NOT multiplied a second time. So a pack
 * size the model dropped into quantity ("Yaourt x8" -> qty 8) or a "2 x 1.50" line both collapse
 * back to the printed line total. Scoring raw price×qty here (the old behaviour) double-counted
 * those and produced false failures that never happen in the app; a genuine price misread (1.22 ->
 * 0.97) or a unit-vs-total confusion still fails, since then `price` itself is wrong.
 */
function lineTotals(items) {
  return (items || [])
    .map((it) => {
      const price = Number(it.price) || 0;
      const q = it.quantity == null ? null : Number(it.quantity);
      const count = q == null ? 1 : Math.trunc(q);
      const whole = count >= 2 && q === count;
      return whole ? (Math.round((price / count) * 100) / 100) * count : price;
    })
    .sort((a, b) => a - b);
}

/** Returns a list of failure strings (empty = pass). Only checks fields the expected.json specifies. */
function check(expected, actual, diagnostics) {
  const fails = [];
  const approx = (a, b) => Math.abs((Number(a) || 0) - (Number(b) || 0)) <= MONEY_TOL;

  // The VERDICT, not just the values: a receipt can be extracted perfectly and still be thrown away by
  // a reconciliation guard (the app shows "Couldn't read that receipt"). Asserting only the numbers
  // misses that entirely — a Greek multi-buy receipt whose printed 'ΣΥΝΟΛΟ ΕΙΔΩΝ' counts UNITS once
  // read correctly in every field while `count_mismatch` rejected it on device. Assert `outcome: "ok"`
  // on any case that must actually reach the user.
  if (expected.outcome != null && diagnostics && diagnostics.outcome !== expected.outcome) {
    fails.push(`outcome: expected ${expected.outcome}, got ${diagnostics.outcome}` +
      ` (lines ${diagnostics.itemCount}, units ${diagnostics.unitCount}, printed ${diagnostics.printedItemCount})`);
  }

  if (typeof expected.readable === "boolean" && actual.readable !== expected.readable) {
    fails.push(`readable: expected ${expected.readable}, got ${actual.readable}`);
  }
  if (expected.printedItemCount != null && Math.round(actual.printedItemCount || 0) !== expected.printedItemCount) {
    fails.push(`printedItemCount: expected ${expected.printedItemCount}, got ${actual.printedItemCount}`);
  }
  if (expected.itemCount != null && (actual.items || []).length !== expected.itemCount) {
    fails.push(`item count: expected ${expected.itemCount}, got ${(actual.items || []).length}`);
  }
  if (expected.total != null && !approx(expected.total, actual.total)) {
    fails.push(`total: expected ${expected.total}, got ${actual.total}`);
  }
  if (expected.discount != null && !approx(expected.discount, actual.discount)) {
    fails.push(`discount: expected ${expected.discount}, got ${actual.discount}`);
  }
  if (expected.tax != null && !approx(expected.tax, actual.tax)) {
    fails.push(`tax: expected ${expected.tax}, got ${actual.tax}`);
  }

  // The crux: compare the multiset of line totals. Catches a misread digit (the 1.22 -> 0.97 bug)
  // regardless of product-name order or whether the model put the value in price vs price×qty.
  if (expected.lineTotals) {
    const exp = [...expected.lineTotals].sort((a, b) => a - b);
    const act = lineTotals(actual.items);
    if (act.length !== exp.length) {
      fails.push(`line totals: expected ${exp.length} lines, got ${act.length} -> [${act.map((n) => n.toFixed(2))}]`);
    } else {
      exp.forEach((v, i) => {
        if (!approx(v, act[i])) fails.push(`line total #${i + 1}: expected ${v.toFixed(2)}, got ${act[i].toFixed(2)}`);
      });
    }
    const sum = (xs) => xs.reduce((a, b) => a + b, 0);
    if (!approx(sum(exp), sum(act))) {
      fails.push(`items sum: expected ${sum(exp).toFixed(2)}, got ${sum(act).toFixed(2)}`);
    }
  }

  // Names are informational unless asserted: every listed name must appear (case-insensitive).
  if (expected.expectedNames) {
    const got = (actual.items || []).map((it) => (it.name || "").toLowerCase());
    expected.expectedNames.forEach((n) => {
      if (!got.some((g) => g.includes(String(n).toLowerCase()))) fails.push(`missing expected product name: "${n}"`);
    });
  }
  return fails;
}

/** Reads the `tags` array from a case's expected.json (empty if none / unparseable). */
function tagsOf(dir) {
  try {
    const j = JSON.parse(fs.readFileSync(path.join(dir, "expected.json"), "utf8"));
    return Array.isArray(j.tags) ? j.tags : [];
  } catch {
    return [];
  }
}

function matchesFamily(tags, fam) {
  const set = new Set(tags);
  if (fam.has && !fam.has.every((t) => set.has(t))) return false;
  if (fam.not && fam.not.some((t) => set.has(t))) return false;
  return true;
}

/** API-free static report: which format families have a receipt yet. Turns coverage into a number. */
function coverageReport(cases) {
  const rows = cases.map((id) => {
    const dir = path.join(CORPUS_DIR, id);
    return { id, tags: tagsOf(dir), collected: !!findImage(dir) };
  });
  const icon = { COVERED: "✅", PENDING: "🟡", MISSING: "⬜" };
  const linesFor = (phase) =>
    FAMILIES.filter((f) => f.phase === phase).map((fam) => {
      const hits = rows.filter((r) => matchesFamily(r.tags, fam));
      const status = hits.some((r) => r.collected) ? "COVERED" : hits.length ? "PENDING" : "MISSING";
      const who = hits.length ? `  [${hits.map((h) => h.id + (h.collected ? "" : "*")).join(", ")}]` : "";
      return { status, text: `  ${icon[status]} ${status.padEnd(7)} ${fam.label}${who}` };
    });

  console.log("\n=== Format coverage (launch readiness) ===");
  console.log("Launch-critical - Bulgaria + EU:");
  const crit = linesFor(1);
  crit.forEach((l) => console.log(l.text));
  console.log("Phase 2 - North America (after EU/BG is green):");
  linesFor(2).forEach((l) => console.log(l.text));

  const done = crit.filter((l) => l.status === "COVERED").length;
  const untagged = rows.filter((r) => r.tags.length === 0).map((r) => r.id);
  console.log(`\n${done}/${crit.length} launch-critical families have a real receipt collected.  (* = stub awaiting a receipt image)`);
  if (untagged.length) console.log(`Untagged cases (add \`tags\` to place them on the map): ${untagged.join(", ")}`);
}

async function main() {
  const args = process.argv.slice(2);
  const wantCoverage = args.includes("--coverage") || args.includes("-c");
  const wantJson = args.includes("--json"); // dump the raw model output per case (debugging a case)
  const wantTiered = args.includes("--tiered"); // Haiku-first (escalate to Sonnet on a guard trip) vs Sonnet-only
  const only = args.find((a) => !a.startsWith("-"));

  const cases = fs.existsSync(CORPUS_DIR)
    ? fs.readdirSync(CORPUS_DIR).filter((d) => fs.statSync(path.join(CORPUS_DIR, d)).isDirectory())
    : [];

  // Coverage is a static file scan — no API key or network needed, so handle it before the key check.
  if (wantCoverage) {
    if (cases.length === 0) {
      console.error(`No corpus cases found in ${CORPUS_DIR}`);
      process.exit(2);
    }
    coverageReport(cases);
    process.exit(0);
  }

  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    console.error("Set ANTHROPIC_API_KEY (the same key the Cloud Function uses) to run the eval:");
    console.error("  ANTHROPIC_API_KEY=sk-ant-... node eval/run-eval.js");
    console.error("Tip: `node eval/run-eval.js --coverage` needs no key — it just shows format gaps.");
    process.exit(2);
  }
  const selected = only ? cases.filter((c) => c === only) : cases;
  if (selected.length === 0) {
    console.error(only ? `No corpus case "${only}" found.` : `No corpus cases found in ${CORPUS_DIR}`);
    process.exit(2);
  }

  let pass = 0;
  let fail = 0;
  let skip = 0;
  let reads = 0; // cases that actually hit the model (for the escalation rate + cost)
  let escalations = 0; // tiered reads that fell through to Sonnet
  let runCost = 0; // summed API cost of this run, at extract.js PRICING rates
  for (const id of selected) {
    const dir = path.join(CORPUS_DIR, id);
    if (!fs.existsSync(path.join(dir, "expected.json"))) {
      console.log(`SKIP  ${id} (no expected.json)`);
      skip++;
      continue;
    }
    const image = findImage(dir);
    if (!image) {
      console.log(`SKIP  ${id} (drop the receipt image into eval/corpus/${id}/)`);
      skip++;
      continue;
    }
    const expected = JSON.parse(fs.readFileSync(path.join(dir, "expected.json"), "utf8"));
    if (!ASSERT_KEYS.some((k) => expected[k] != null)) {
      console.log(`FAIL  ${id} (stub: image present but no truth fields yet — fill in expected.json)`);
      fail++;
      continue;
    }
    try {
      const r = await extractTiered({ sourceBlock: sourceBlockFor(image), apiKey, haikuFirst: wantTiered });
      const actual = r.input;
      reads++;
      runCost += r.cost;
      if (r.escalated) escalations++;
      // In tiered mode, show which model served: [haiku] accepted, or [haiku→sonnet] on a guard trip.
      const tag = wantTiered ? (r.escalated ? "  [haiku→sonnet]" : "  [haiku]") : "";
      if (wantJson) console.log(`      ${JSON.stringify(actual)}`);
      const fails = check(expected, actual, r.diagnostics);
      if (fails.length === 0) {
        console.log(`PASS  ${id}${tag}`);
        pass++;
      } else {
        console.log(`FAIL  ${id}${tag}`);
        fails.forEach((f) => console.log(`        - ${f}`));
        fail++;
      }
    } catch (e) {
      console.log(`ERROR ${id}: ${e.message}`);
      fail++;
    }
  }
  console.log(`\n${pass} passed, ${fail} failed, ${skip} skipped (of ${selected.length}).`);
  if (reads > 0) {
    const rate = Math.round((escalations / reads) * 100);
    const mode = wantTiered
      ? `Haiku-first — escalated to Sonnet on ${escalations}/${reads} reads (${rate}%)`
      : "Sonnet-only (baseline)";
    console.log(`Mode: ${mode}.`);
    console.log(`Est. API cost this run: $${runCost.toFixed(4)}  (list rates: Sonnet $3/$15, Haiku $1/$5 per 1M tok).`);
  }
  if (!only) coverageReport(cases);
  process.exit(fail > 0 ? 1 : 0);
}

main();
