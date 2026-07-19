#!/usr/bin/env node
/**
 * Offline regression tests for the reconciliation guards in ../extract.js (`scanDiagnostics`).
 *
 * The corpus eval (run-eval.js) tests the MODEL — does it read the receipt right? These test the
 * GUARDS — given a correct read, do we actually serve it? That's a genuinely separate failure mode:
 * a receipt can be extracted perfectly to the cent and still be rejected with "Couldn't read that
 * receipt", which is exactly how the Greek multi-buy bug reached a tester. Guard logic is pure, so
 * unlike the corpus eval these need no API key, no network, and no receipt image.
 *
 *   node eval/guard-tests.js
 *
 * Exit 0 all passed · 1 a failure.
 */
const assert = require("node:assert");
const { scanDiagnostics } = require("../extract");

let pass = 0;
const failures = [];
function test(name, fn) {
  try {
    fn();
    pass++;
    console.log(`PASS  ${name}`);
  } catch (e) {
    failures.push({ name, message: e.message });
    console.log(`FAIL  ${name}\n        - ${e.message}`);
  }
}

/** A receipt fixture with the fields scanDiagnostics reads; `price` is the extended LINE total. */
const receipt = (over = {}) => ({
  readable: true,
  storeName: "TEST",
  total: 0,
  subtotal: 0,
  tax: 0,
  discount: 0,
  printedItemCount: 0,
  items: [],
  ...over,
});
const item = (price, quantity = 1) => ({ name: "x", price, quantity, category: "Groceries" });

// The regression itself: ΕΜΠΟΡΙΚΟ ΚΕΝΤΡΟ ΚΟΚΚΥΛΙΑ (Kefalonia, GR), scanned 2026-07-18/19. Prints
// "ΣΥΝΟΛΟ ΕΙΔΩΝ *18*" — eighteen UNITS across nine product lines, because three lines are multi-buys
// ("4 X 0,34", "6 X 1,42", "2 X 0,11"). Extraction was flawless (18.27 gross − 0.78 in two ΕΚΠΤΩΣΗ
// lines = the printed 17.49) yet the old guard compared 9 lines against 18 and rejected it five times.
const KOKKYLIA = receipt({
  storeName: "ΕΜΠΟΡΙΚΟ ΚΕΝΤΡΟ ΚΟΚΚΥΛΙΑ Ε.Π.Ε.",
  printedItemCount: 18,
  total: 17.49,
  discount: 0.78,
  items: [
    item(1.87), // MYTHOS Φ/Λ 500ML
    item(0.14), // ΚΕΝΗ ΦΙΑΛΗ ΜΥΘΟΣ
    item(2.11), // NUTELLA&GO 52ΓΡ
    item(1.36, 4), // ΚΟΡΠΗ ΝΕΡΟ 1.5LT — 4 X 0,34
    item(0.65), // ΚΟΡΠΗ SPORTMAX
    item(1.54), // DORITOS NACHO CHEE
    item(8.52, 6), // ΚΡΕΜΑ ΣΟΚ/ΤΑ ΤΖΩΡ — 6 X 1,42
    item(0.22, 2), // ΤΣΑΝΤΕΣ ΠΕΛΑΤΩΝ ΒΙ — 2 X 0,11
    item(1.86), // EL SABOR NACHO CHI
  ],
});

test("unit-counting receipt: 9 lines / 18 units vs printed 18 is served", () => {
  const d = scanDiagnostics(KOKKYLIA);
  assert.strictEqual(d.itemCount, 9);
  assert.strictEqual(d.unitCount, 18);
  assert.strictEqual(d.countMismatch, false);
  assert.strictEqual(d.outcome, "ok");
  // The money must still reconcile exactly — this read was never in doubt.
  assert.strictEqual(d.grossItems, 18.27);
  assert.strictEqual(d.overshoot, 0);
});

test("line-counting receipt: 9 lines vs printed 9 still passes", () => {
  const d = scanDiagnostics(receipt({ ...KOKKYLIA, printedItemCount: 9 }));
  assert.strictEqual(d.outcome, "ok");
});

test("a real under-read still trips the guard", () => {
  // Only two of eighteen articles captured — matches neither 2 lines nor 2 units against printed 18.
  const d = scanDiagnostics(receipt({ printedItemCount: 18, total: 17.49, items: [item(1.87), item(0.14)] }));
  assert.strictEqual(d.countMismatch, true);
  assert.strictEqual(d.outcome, "count_mismatch");
});

test("a real over-read still trips the guard", () => {
  // Twelve invented lines against a printed count of 4 — over MAX_COUNT_RATIO on both readings.
  const items = Array.from({ length: 12 }, () => item(1.0));
  const d = scanDiagnostics(receipt({ printedItemCount: 4, total: 12, items }));
  assert.strictEqual(d.outcome, "count_mismatch");
});

test("a weighed line counts as one article, not its fractional kg", () => {
  // 0.775 kg produce + 2 singles = 3 articles, not 2.775. Printed 3 must reconcile.
  const d = scanDiagnostics(
    receipt({ printedItemCount: 3, total: 9.0, items: [item(4.57, 0.775), item(2.2), item(2.23)] }),
  );
  assert.strictEqual(d.unitCount, 3);
  assert.strictEqual(d.outcome, "ok");
});

test("quantity is never counted as less than one article per line", () => {
  // Missing/zero quantity is still one article — a 3-line receipt can't count as 0.
  const d = scanDiagnostics(
    receipt({ printedItemCount: 3, total: 6, items: [{ name: "a", price: 2 }, item(2, 0), item(2)] }),
  );
  assert.strictEqual(d.unitCount, 3);
  assert.strictEqual(d.outcome, "ok");
});

test("guard stays off below MIN_PRINTED_COUNT_TO_CHECK", () => {
  const d = scanDiagnostics(receipt({ printedItemCount: 2, total: 5, items: [item(5)] }));
  assert.strictEqual(d.countMismatch, false);
});

test("guard stays off when no count is printed", () => {
  const d = scanDiagnostics(receipt({ printedItemCount: 0, total: 5, items: [item(5)] }));
  assert.strictEqual(d.countMismatch, false);
  assert.strictEqual(d.outcome, "ok");
});

test("the model's own abstention still outranks the count check", () => {
  const d = scanDiagnostics(receipt({ ...KOKKYLIA, readable: false }));
  assert.strictEqual(d.outcome, "not_readable");
});

test("the overshoot guard is unaffected by unit counting", () => {
  // Nine lines / 18 units clears the count check, but the items overshoot the total by 10.00.
  const d = scanDiagnostics(receipt({ ...KOKKYLIA, total: 8.27, discount: 0 }));
  assert.strictEqual(d.countMismatch, false);
  assert.strictEqual(d.outcome, "overshoot");
});

console.log(`\n${pass} passed, ${failures.length} failed.`);
process.exit(failures.length > 0 ? 1 : 0);
