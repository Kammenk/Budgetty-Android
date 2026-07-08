#!/usr/bin/env node
/**
 * Generate SYNTHETIC receipt fixtures for eval families that are hard to shop for locally
 * (a German discounter with Pfand, a Western-EU multi-VAT supermarket, and a delivery-app order
 * summary). These are NOT real
 * receipts — they're hand-built specimens whose ground truth is COMPUTED from the item data
 * below and written straight into expected.json, so the truth can never drift from the image.
 *
 * They cover FORMAT/parsing logic (VAT-inclusive totals, comma decimals, multi-rate VAT summaries
 * that must NOT leak into `tax`, deposits, loyalty discounts, non-Cyrillic OCR) — not the noise of
 * a real photographed thermal receipt. Swap in a real receipt when you have one; delete nothing else.
 *
 * Run:  node eval/tools/gen-synthetic-receipts.mjs        (from functions/)
 * Needs: Google Chrome (headless HTML->PNG) + python3 with Pillow (whitespace trim). Both dev-only.
 */
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const CORPUS = path.resolve(fileURLToPath(new URL("../corpus", import.meta.url)));
const tmp = mkdtempSync(path.join(tmpdir(), "recgen-"));

const money = (n) => n.toFixed(2).replace(".", ","); // 1.09 -> "1,09"
const round2 = (n) => Math.round(n * 100) / 100;
const GEN_NOTE = "tools/gen-synthetic-receipts.mjs — regenerate: node eval/tools/gen-synthetic-receipts.mjs";

/** Sum line prices per VAT class and derive an INCLUDED-VAT breakdown (net + tax, reconciling to gross). */
function vatBreakdown(items, rates) {
  const byClass = {};
  for (const it of items) byClass[it.cls] = round2((byClass[it.cls] || 0) + it.price);
  return Object.keys(rates).map((cls) => {
    const gross = byClass[cls] || 0;
    const net = round2(gross / (1 + rates[cls]));
    return { cls, rate: rates[cls], net, tax: round2(gross - net), gross };
  });
}

// ---------------------------------------------------------------------------------------------------
// Receipt definitions. Prices are VAT-INCLUSIVE (what's printed on the line), in EUR.
// ---------------------------------------------------------------------------------------------------
const RECEIPTS = [
  {
    id: "de-discounter-pfand",
    tags: ["region:eu", "lang:de", "type:discounter", "tax:inclusive", "feature:deposit", "feature:multiVat"],
    description:
      "SYNTHETIC German discounter receipt (fictional 'LANDMARKT DISCOUNT'), VAT-INCLUSIVE, comma " +
      "decimals, two MwSt classes (A=19%, B=7%) plus a Pfand (Einweg deposit) line. Key trap: the printed " +
      "MwSt table (0,56 + 0,45 = 1,01) is INCLUDED in the prices, so tax must read 0, not 1,01. " +
      "Generated to unblock the eu-discounter-deposit family; replace with a real Lidl/Aldi receipt later.",
    store: { name: "LANDMARKT DISCOUNT", lines: ["Hauptstraße 12, 10115 Berlin", "USt-IdNr: DE123456789", "05.07.2026  14:23  Kasse 3"] },
    lang: "de",
    rates: { A: 0.19, B: 0.07 },
    items: [
      { name: "Vollmilch 3,5%", price: 1.09, cls: "B" },
      { name: "Bananen 1kg", price: 1.49, cls: "B" },
      { name: "Weizenmehl 405", price: 0.55, cls: "B" },
      { name: "Gouda jung 400g", price: 2.49, cls: "B" },
      { name: "Landbrot 500g", price: 1.19, cls: "B" },
      { name: "Pils 0,5L", price: 0.79, cls: "A" },
      { name: "Spülmittel", price: 0.95, cls: "A" },
      { name: "Coca-Cola 1,5L", price: 1.49, cls: "A" },
      { name: "Pfand Einweg", price: 0.25, cls: "A" },
    ],
    discount: 0,
    given: 20,
    labels: { total: "SUMME EUR", paid: "Gegeben BAR", change: "Rückgeld", vatTitle: "MwSt-Übersicht", cols: ["", "Netto", "MwSt", "Brutto"], sum: "Summe", count: "Artikel", foot: "Vielen Dank für Ihren Einkauf!" },
    // Assertions the harness will check (computed truth is filled in below):
    assert: { printedItemCount: 9, expectedNames: ["Vollmilch", "Gouda", "Coca-Cola", "Pfand"] },
  },
  {
    id: "eu-supermarket-multivat",
    tags: ["region:eu", "lang:fr", "type:supermarket", "tax:inclusive", "feature:multiVat", "feature:coupon"],
    description:
      "SYNTHETIC Western-EU supermarket receipt (fictional French 'MARCHÉ DU SUD'), VAT-INCLUSIVE (TTC), " +
      "comma decimals, two TVA rates (5,5% food / 20% non-food) plus a loyalty REMISE. Traps: the printed " +
      "TVA total (2,31) is INCLUDED so tax=0; REMISE FIDÉLITÉ is a discount (1,49), not a product; the line " +
      "prices are the printed TTC (sum 21,49) while the paid TOTAL is 20,00. Replace with a real " +
      "Carrefour/Tesco/Albert Heijn receipt later.",
    store: { name: "MARCHÉ DU SUD", lines: ["24 Av. de la République, 69003 Lyon", "TVA FR 12 345678901", "05/07/2026  18:47  Caisse 07"] },
    lang: "fr",
    rates: { A: 0.055, B: 0.2 },
    items: [
      { name: "Baguette tradition", price: 1.1, cls: "A" },
      { name: "Lait 1/2 écrémé 1L", price: 0.89, cls: "A" },
      { name: "Camembert 250g", price: 2.35, cls: "A" },
      { name: "Pommes Golden 1kg", price: 2.19, cls: "A" },
      { name: "Yaourt nature x8", price: 1.95, cls: "A" },
      { name: "Eau minérale 6x1,5L", price: 2.64, cls: "A" },
      { name: "Liquide vaisselle", price: 1.89, cls: "B" },
      { name: "Papier toil. x6", price: 3.49, cls: "B" },
      { name: "Piles AA x4", price: 4.99, cls: "B" },
    ],
    discount: 1.49,
    discountLabel: "REMISE FIDÉLITÉ",
    assertLineTotals: true,
    labels: { total: "TOTAL À PAYER", paid: "CARTE BANCAIRE", vatTitle: "Détail TVA", cols: ["", "Base HT", "TVA", "TTC"], sum: "Total", count: "NB ARTICLES", foot: "Merci de votre visite !", loyalty: "Carte fidélité ****4821" },
    assert: { itemCount: 9, printedItemCount: 9, expectedNames: ["Baguette", "Camembert", "Papier", "Piles"] },
  },
  {
    id: "eu-delivery-glovo",
    kind: "delivery",
    tags: ["region:bg", "lang:bg", "type:delivery", "tax:inclusive", "feature:coupon"],
    description:
      "SYNTHETIC delivery-app order summary (fictional 'Zumo', Bulgarian, EUR) — a DIGITAL summary, not a " +
      "fiscal receipt: product lines PLUS a delivery fee, service fee, a promo discount and a courier tip, " +
      "with one substituted item. This case exists to expose an OPEN QUESTION: delivery/service fees + tip " +
      "have no slot in the schema (items/discount/total/subtotal/tax), so 7,40 of fees+tip sits between the " +
      "product sum and the paid total. Replace with a real Glovo/Wolt/Bolt export later.",
    app: "Zumo",
    header: ["Поръчка #ZM-4471829", "Пица Рома · 05.07.2026 19:12"],
    lang: "bg",
    items: [
      { qty: 1, name: "Маргарита пица", price: 12.9 },
      { qty: 1, name: "Пеперони пица", price: 14.5 },
      { qty: 2, name: "Кока-Кола 0.5л", price: 3.6, note: "↔ Замяна: Фанта → Кока-Кола" },
      { qty: 1, name: "Тирамису", price: 5.9 },
    ],
    subtotalLabel: "Междинна сума",
    fees: [
      { label: "Такса доставка", amount: 2.9 },
      { label: "Сервизна такса", amount: 1.5 },
    ],
    promo: { label: "Промокод SUMMER4", amount: 4.0 },
    tip: { label: "Бакшиш за куриера", amount: 3.0 },
    totalLabel: "Общо",
    pay: "Платено с карта · Visa ****4821",
    assert: { expectedNames: ["Маргарита", "Пеперони", "Кока-Кола", "Тирамису"] },
  },
];

function html(r) {
  const gross = round2(r.items.reduce((s, it) => s + it.price, 0));
  const total = round2(gross - (r.discount || 0));
  const vat = vatBreakdown(r.items, r.rates);
  const vatTotal = vat.reduce((s, v) => ({ net: round2(s.net + v.net), tax: round2(s.tax + v.tax), gross: round2(s.gross + v.gross) }), { net: 0, tax: 0, gross: 0 });

  const itemRows = r.items
    .map((it) => `<div class="row"><span class="n">${it.name}</span><span class="p">${money(it.price)} ${it.cls}</span></div>`)
    .join("");
  const vatRows = vat
    .map((v) => `<tr><td>${v.cls} ${money(v.rate * 100)}%</td><td>${money(v.net)}</td><td>${money(v.tax)}</td><td>${money(v.gross)}</td></tr>`)
    .join("");
  const discountRow = r.discount ? `<div class="row"><span class="n">${r.discountLabel}</span><span class="p">-${money(r.discount)}</span></div>` : "";
  const subtotalRow = r.discount ? `<div class="row"><span class="n">Sous-total</span><span class="p">${money(gross)}</span></div>` : "";
  const payRows = r.labels.change
    ? `<div class="row"><span class="n">${r.labels.paid}</span><span class="p">${money(r.given)}</span></div>` +
      `<div class="row"><span class="n">${r.labels.change}</span><span class="p">${money(round2(r.given - total))}</span></div>`
    : `<div class="row"><span class="n">${r.labels.paid}</span><span class="p">${money(total)}</span></div>`;
  const loyalty = r.labels.loyalty ? `<div class="center muted" style="margin-top:6px">${r.labels.loyalty}</div>` : "";

  return `<!doctype html><html lang="${r.lang}"><head><meta charset="utf-8"><style>
  *{box-sizing:border-box}
  body{margin:0;background:#fff}
  .r{width:380px;margin:12px auto;padding:18px 18px 22px;color:#141414;overflow:hidden;
     font-family:'Courier New',ui-monospace,monospace;font-size:15px;line-height:1.45}
  .center{text-align:center}
  .big{font-size:19px;font-weight:700;letter-spacing:1.5px}
  .muted{color:#3a3a3a;font-size:13px}
  .sep{border-top:1px dashed #444;margin:10px 0}
  .row{display:flex;justify-content:space-between;gap:12px;width:100%}
  .row .n{flex:1 1 auto;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
  .row .p{flex:0 0 auto;white-space:nowrap;text-align:right}
  .tot{font-weight:700;font-size:16px}
  table{width:100%;border-collapse:collapse;font-size:13px;margin-top:3px}
  td,th{text-align:right;padding:1px 0}
  th:first-child,td:first-child{text-align:left}
  .bar{margin-top:14px;height:34px;background:repeating-linear-gradient(90deg,#141414 0,#141414 2px,#fff 2px,#fff 4px,#141414 4px,#141414 5px,#fff 5px,#fff 8px)}
  </style></head><body><div class="r">
    <div class="center big">${r.store.name}</div>
    ${r.store.lines.map((l) => `<div class="center muted">${l}</div>`).join("")}
    <div class="sep"></div>
    ${itemRows}
    <div class="sep"></div>
    ${subtotalRow}${discountRow}
    <div class="row tot"><span class="n">${r.labels.total}</span><span class="p">${money(total)}</span></div>
    ${payRows}
    ${loyalty}
    <div class="sep"></div>
    <div class="muted">${r.labels.vatTitle}</div>
    <table><thead><tr>${r.labels.cols.map((c) => `<th>${c}</th>`).join("")}</tr></thead>
    <tbody>${vatRows}<tr><td>${r.labels.sum}</td><td>${money(vatTotal.net)}</td><td>${money(vatTotal.tax)}</td><td>${money(vatTotal.gross)}</td></tr></tbody></table>
    <div class="row muted" style="margin-top:6px"><span class="n">${r.labels.count}</span><span class="p">${r.items.length}</span></div>
    <div class="sep"></div>
    <div class="center muted">${r.labels.foot}</div>
    <div class="bar"></div>
  </div></body></html>`;
}

const subtotalOf = (items) => round2(items.reduce((s, it) => s + it.price, 0));
const deliveryTotalOf = (r) =>
  round2(subtotalOf(r.items) + r.fees.reduce((s, f) => s + f.amount, 0) - (r.promo?.amount || 0) + (r.tip?.amount || 0));

/**
 * A digital delivery-app order summary (Glovo/Wolt/Bolt-style): product lines plus a fees/tip
 * breakdown, no VAT table or barcode. Rendered as a sans-serif card so it reads as an app export,
 * not a thermal print — deliberately unlike the other fixtures.
 */
function htmlDelivery(r) {
  const eur = (n) => `${money(round2(n))} €`;
  const sub = subtotalOf(r.items);
  const total = deliveryTotalOf(r);
  const itemRows = r.items
    .map((it) => {
      const note = it.note ? `<div class="note">${it.note}</div>` : "";
      return `<div class="row"><span class="n">${it.qty}× ${it.name}</span><span class="p">${eur(it.price)}</span></div>${note}`;
    })
    .join("");
  const feeRows = r.fees.map((f) => `<div class="row muted2"><span class="n">${f.label}</span><span class="p">${eur(f.amount)}</span></div>`).join("");
  const promoRow = r.promo ? `<div class="row promo"><span class="n">${r.promo.label}</span><span class="p">-${eur(r.promo.amount)}</span></div>` : "";
  const tipRow = r.tip ? `<div class="row muted2"><span class="n">${r.tip.label}</span><span class="p">${eur(r.tip.amount)}</span></div>` : "";
  return `<!doctype html><html lang="${r.lang}"><head><meta charset="utf-8"><style>
  *{box-sizing:border-box}
  body{margin:0;background:#fff}
  .a{width:400px;margin:12px auto;padding:0 0 18px;color:#1c1c1e;overflow:hidden;border:1px solid #ececec;border-radius:16px;
     font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.4}
  .head{background:#111827;color:#fff;padding:16px 20px}
  .brand{font-size:22px;font-weight:800;letter-spacing:.5px}
  .hsub{font-size:13px;opacity:.85;margin-top:2px}
  .body{padding:14px 20px}
  .lbl{font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.6px;color:#8a8a8e;margin:6px 0 8px}
  .row{display:flex;justify-content:space-between;gap:14px;padding:3px 0}
  .row .n{flex:1 1 auto;min-width:0}
  .row .p{flex:0 0 auto;white-space:nowrap;font-variant-numeric:tabular-nums}
  .note{font-size:12px;color:#8a8a8e;margin:-1px 0 5px 16px}
  .muted2{color:#3a3a3c}
  .promo{color:#1a7f37}
  .hr{border-top:1px solid #ececec;margin:12px 0}
  .total{display:flex;justify-content:space-between;font-size:18px;font-weight:800;padding-top:2px}
  .pay{font-size:13px;color:#8a8a8e;margin-top:12px}
  </style></head><body><div class="a">
    <div class="head"><div class="brand">${r.app}</div>${r.header.map((h) => `<div class="hsub">${h}</div>`).join("")}</div>
    <div class="body">
      <div class="lbl">Продукти</div>
      ${itemRows}
      <div class="hr"></div>
      <div class="row muted2"><span class="n">${r.subtotalLabel}</span><span class="p">${eur(sub)}</span></div>
      ${feeRows}${promoRow}${tipRow}
      <div class="hr"></div>
      <div class="total"><span>${r.totalLabel}</span><span>${eur(total)}</span></div>
      <div class="pay">${r.pay}</div>
    </div>
  </div></body></html>`;
}

/** expected.json for the thermal (supermarket/discounter) fixtures. */
function thermalExpected(r) {
  const gross = subtotalOf(r.items);
  return {
    _synthetic: true,
    _generator: GEN_NOTE,
    _description: r.description,
    tags: r.tags,
    readable: true,
    total: round2(gross - (r.discount || 0)),
    discount: round2(r.discount || 0),
    // tax is intentionally NOT asserted on these VAT-INCLUSIVE receipts: the model tends to report the
    // contained VAT (e.g. 1,01 / 2,31) rather than 0, and that's harmless — the app's isTaxOnTop() treats
    // `total + discount <= items-sum` as inclusive and adds NOTHING on top (containedTax() just stores it
    // as the informational "incl. VAT" figure), so paid totals stay correct. "Nothing added on top" is
    // already enforced here by the total + discount + lineTotals checks. See HaikuReceiptExtractor.isTaxOnTop.
    _taxNote: "VAT-inclusive: tax not asserted (model may report the contained VAT; app adds nothing on top). See HaikuReceiptExtractor.isTaxOnTop.",
    ...(r.assert.itemCount != null ? { itemCount: r.assert.itemCount } : {}),
    ...(r.assert.printedItemCount != null ? { printedItemCount: r.assert.printedItemCount } : {}),
    ...(r.assertLineTotals ? { lineTotals: r.items.map((it) => round2(it.price)) } : {}),
    expectedNames: r.assert.expectedNames,
  };
}

/**
 * expected.json for the delivery-app fixture. Deliberately asserts ONLY `total` + product names:
 * fees + tip have no schema slot yet (see _openQuestion), so itemCount/lineTotals/discount are left
 * open until we decide how they should be modelled.
 */
function deliveryExpected(r) {
  const sub = subtotalOf(r.items);
  const fees = round2(r.fees.reduce((s, f) => s + f.amount, 0));
  const feesTip = round2(fees + (r.tip?.amount || 0));
  return {
    _synthetic: true,
    _generator: GEN_NOTE,
    _description: r.description,
    tags: r.tags,
    readable: true,
    total: deliveryTotalOf(r),
    expectedNames: r.assert.expectedNames,
    _openQuestion:
      `Delivery fees + tip have NO first-class slot in the schema (items/discount/total/subtotal/tax). ` +
      `Products ${money(sub)}; promo -${money(r.promo.amount)}; fees ${money(fees)} + tip ${money(r.tip.amount)} = +${money(feesTip)}; ` +
      `paid total ${money(deliveryTotalOf(r))}. Only \`total\` and the ${r.assert.expectedNames.length} product names are asserted — ` +
      `decide how fees/tip should be modelled before asserting itemCount / lineTotals / discount.`,
    _notItems: "Fees, tip and promo must NOT be invented as product line items; the products are the only real items.",
  };
}

for (const r of RECEIPTS) {
  const caseDir = path.join(CORPUS, r.id);
  mkdirSync(caseDir, { recursive: true });

  const htmlPath = path.join(tmp, `${r.id}.html`);
  const rawPng = path.join(tmp, `${r.id}.raw.png`);
  const outPng = path.join(caseDir, "receipt.png");
  writeFileSync(htmlPath, r.kind === "delivery" ? htmlDelivery(r) : html(r));

  execFileSync(CHROME, [
    "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
    "--default-background-color=FFFFFFFF", "--virtual-time-budget=1500",
    `--window-size=900,2000`, `--screenshot=${rawPng}`, `file://${htmlPath}`,
  ], { stdio: "ignore" });

  // Trim the white margins to a tight receipt (Pillow).
  execFileSync("python3", ["-c", `
import sys
from PIL import Image, ImageChops
im = Image.open("${rawPng}").convert("RGB")
bg = Image.new("RGB", im.size, (255,255,255))
bbox = ImageChops.difference(im, bg).getbbox()
if bbox:
    m = 16
    l,t,rr,b = bbox
    im = im.crop((max(0,l-m), max(0,t-m), min(im.width,rr+m), min(im.height,b+m)))
im.save("${outPng}")
print("wrote ${outPng}", im.size)
`], { stdio: "inherit" });

  const expected = r.kind === "delivery" ? deliveryExpected(r) : thermalExpected(r);
  writeFileSync(path.join(caseDir, "expected.json"), JSON.stringify(expected, null, 2) + "\n");
  console.log(`${r.id}: total ${money(expected.total)}`);
}
console.log("Done. Run `node eval/run-eval.js --coverage` to see the two families flip to COVERED.");
