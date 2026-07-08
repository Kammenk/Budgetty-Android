# Claude Design request — Receipt detail bottom sheet (explore alternatives)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create new `*.dc.html` mockups in
> that project; once they're there, Claude Code can read them back via DesignSync
> and implement an approved direction.

---

Hi! I want to **rethink the receipt detail bottom sheet** in Budgetty. It's now a
shared component that opens from **two** places — tapping a receipt on the **Home**
screen *and* tapping a receipt row in the **History** screen — so it's worth
getting right. Below is exactly what it does today; please mock up **3–4
alternative directions** as new `*.dc.html` files, keeping the design system.

## What this sheet is (the current design — your baseline)

A **modal bottom sheet** (rounded 28px top, drag handle, scrim) that shows a
single saved receipt so the user can review it, remove things, or jump to the
full editor. It is a **review surface, not an editor** — the point is that a tap
should never immediately drop the user into edit mode.

**Anatomy, top to bottom:**
1. **Pinned header row** (does not scroll):
   - Left: **store "logo"** tile (52px rounded square, store's first letter in
     white on a name-derived color; neutral tile + 🧾 when the store is unknown).
   - Middle: **store name** (`titleLarge`, bold, 1 line, ellipsized) and a muted
     subtitle **"{date} · {N items}"** (e.g. "24 Jun 2026 · 7 items").
   - Right: the **receipt total** (`titleMedium`, semibold) and, if any, a green
     **"Discount −€3.20"** line under it.
   - A hairline divider under the header.
2. **Scrolling item list** (only this middle region scrolls; header and actions
   stay put — the sheet is compact for short receipts and caps+scrolls long ones):
   - One row per line item: category emoji tile, item name, category label,
     quantity, and line price. Each row has a trailing **trash icon** to delete
     that item. Deleting an item shows an **"Item deleted" snackbar with Undo**
     *inside the sheet*. Deleting the **last** item deletes the whole receipt.
3. **Pinned action row** (does not scroll): two equal-width outlined pill buttons —
   **Edit** (pencil) → opens the full receipt editor; **Delete** (trash, error-red
   outline/text) → deletes the whole receipt (with an Undo snackbar on the parent
   screen).

**States to cover in the mocks:**
- **Default** — a typical grocery receipt with 5–8 items and a discount line.
- **Long receipt** — 15+ items, so the middle list scrolls while header + actions
  stay pinned (show a scroll affordance / cut-off).
- **Single-item receipt** — where "delete the only item" == "delete the receipt".
- **Item-deleted** — the in-sheet "Item deleted · Undo" snackbar visible.
- **No / unknown store** — neutral 🧾 tile, title falls back to "Receipt".
- **Dark mode** — at least one mock in dark.

## Match the existing design system
- Reuse the app's **bottom-sheet pattern** (see `DateRangeScreen.dc.html` /
  `PriceRangeScreen.dc.html`): scrim, drag handle, rounded 28px top, comfortable
  20px side padding.
- **Store logo** and **category emoji tiles** exactly as elsewhere in the app.
- Money is bold; discounts and "good" figures use the **green** budget-status
  color; the whole-receipt Delete uses the **error/red** color.
- Phone preview size **300×620**, `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh),
  `--schh` (surfaceContainerHighest)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted)
- Accent: `--primary`, `--onprimary`; selected fills use `--secc` / `--onsecc`
- Status: `--good` (green), `--warn` (amber), `--bad`/`--error` (red)
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`

## Directions I'd like you to explore (pick 3–4, or propose your own)
1. **Refined baseline** — the same structure, but tightened: better visual
   hierarchy between store/date/total, nicer item rows, a clearer split between
   the scrolling list and the pinned header/actions.
2. **Receipt-y / "paper" treatment** — lean into the receipt metaphor (subtle
   perforated edge, monospace-ish totals, a "subtotal / discount / total"
   summary block at the bottom of the list) while staying on-brand and calm.
3. **Summary-forward** — add a compact **insights strip** to the header: this
   receipt's biggest category, # of categories, or how it compares to the user's
   average basket at this store. Keep it glanceable, not busy.
4. **Action-light** — demote Edit/Delete into a small overflow (⋯) menu so the
   sheet reads as a clean read-only summary, with the item list as the hero.

Show the **line-item delete** interaction and the **Edit / Delete** actions in
whichever directions include them, so I can compare how each handles destructive
actions + undo.

## Output
- Save each as a new `*.dc.html` file (e.g. `ReceiptSheetRefined.dc.html`,
  `ReceiptSheetPaper.dc.html`, …). **Don't overwrite** the existing
  `ReceiptDetailScreen.dc.html` — add alternatives alongside it.
- Token-driven (use the CSS vars above, no hard-coded grays) so they theme in
  light and dark.
- Phone first. Once I pick a direction, I'll ask for the matching tablet /
  centered-dialog variant (the sheet becomes a centered dialog on tablets).

Thanks!
