# Claude Design request — History screen compact single-row transactions

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I want to make the transaction rows on the **History screen** more compact.
Today each transaction (on both the **Receipts** tab and the **Items** tab) is a
**two-line** row: a title on top and a muted subtitle underneath. I'd like to
collapse each one into a **single line** so the list is denser and more
scannable. Please mock this up as new `*.dc.html` files, matching the existing
design system exactly.

## Why / the goal
- **One line per transaction**, not two. Same information, tighter vertical rhythm.
- Move the secondary detail **inline, right after the title** (dimmed), instead
  of stacking it on a second line.
- The title can get long (store names, product names), so when it doesn't fit it
  should **marquee** (scroll horizontally) rather than just ellipsize.
- **Shrink the leading icon/logo tile** so the row is shorter and the title has
  more room.

## Match these existing components for style
- `HistoryScreen.dc.html` — the base layout to build on (header, search field,
  filter chip row, month headers, day headers, the current two-line item rows
  with the rounded leading tile). Build the compact rows into this.
- Phone preview size **300×620**, `font-family: Roboto`.
- Note: the **date headers** already exist on both tabs (a collapsible
  "Today / Yesterday / Wed 25 Jun" row with the day's total). So the date is
  **not** part of the transaction row — don't add it there.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`; positive/savings green used for discounts
- Lines: `--outv` (row dividers, `border-top:1px solid var(--outv)`)
- Existing row styling to stay consistent with: title 13.5px/600 `--on`,
  secondary text 11px `--onv`, amount 13.5px/700 `--on`.

## The two rows to redesign

### Receipts tab row
**Current (two lines):**
```
[logo 38] Kaufland Some Very Long Mall Name          €42.60
          12 items                                    −€3.10  ›
```
**Target (one line):**
```
[logo 28] Kaufland Some Very Long M…  · 12 items      €42.60 ›
```
- **Title** = store name. Marquees when too long to fit.
- **Inline meta** = the item count, e.g. `· 12 items`, in 11px `--onv`, sitting
  right after the title (does **not** scroll — it's a fixed suffix).
- **Amount** = receipt total, pinned right (13.5px/700).
- Keep the **right chevron** `›` — a receipt row is tappable and opens
  `ReceiptDetailScreen`.
- **Discount** (the `−€3.10` that used to be on the second line) — please
  propose how to fit it on one line. Options to try: a small green `−€3.10`
  immediately **left of** the total, or a small green chip after the total, or
  drop it from the row (it's still shown in the receipt detail). Show me a
  variant so I can pick.

### Items tab row
**Current (two lines):**
```
[tile 38] Organic whole milk 2L                       €2.49
          Groceries · Lidl
```
**Target (one line):**
```
[tile 28] Organic whole milk 2L  · Groceries · Lidl   €2.49
```
- **Title** = item name. Marquees when too long.
- **Inline meta** = `Category · Store` (the app's existing `·` separator), 11px
  `--onv`, fixed suffix right after the title.
- **Amount** = line total, pinned right (13.5px/700).
- Item rows are **not** tappable, so **no chevron**.

## Marquee behavior (the key thing to nail)
The title scrolls horizontally when it overflows, while the dimmed inline meta
(`· 12 items` / `· Category · Store`) and the amount stay put. In the mockup,
please illustrate this: include at least **one row per tab with a title long
enough to overflow**, shown mid-scroll (e.g. clipped at the right edge with a
soft fade), and a couple of short-title rows that simply read
`Title · meta` with the meta right after the title. A short caption noting
"marquees when long" is fine.

## Smaller leading icon
- Current leading tile is **38×38** (radius 12) in the mockup. Shrink it to about
  **28×28** (radius ~9), keeping the same rounded-square style, store-color fill
  + first letter for receipts, and category-color fill + emoji for items.
- Tighten the row so it lands around **44–48px tall** instead of the current ~60.

## Please produce
- `HistoryReceiptsRowScreen.dc.html` — the Receipts tab with the compact rows
  (a couple of days of grouped receipts, including one long store name and at
  least one row that has a discount, so I can see the discount treatment).
- `HistoryItemsRowScreen.dc.html` — the Items tab with the compact rows
  (including one long product name).
- If the discount has more than one reasonable treatment, add a small side-by-side
  or a second variant row so I can choose.

## Output
- Save each as a **new** `*.dc.html` file — don't overwrite `HistoryScreen.dc.html`.
- Token-driven only (use the CSS vars above, no hard-coded grays) so they theme
  correctly in dark + light.
- Phone first. Once I approve, I'll ask for the tablet / landscape variants.

Thanks!
