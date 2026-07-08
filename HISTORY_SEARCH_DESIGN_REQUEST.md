# Claude Design request — History screen search improvements

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I want to improve the **History screen** in the Budgetty app so users can
find past purchases more easily. Please mock up the screens below as new
`*.dc.html` files in this project, matching the existing design system exactly.

## Match these existing components for style
- `HistoryScreen.dc.html` — base layout to build on (header, search field, filter
  chip row, month headers, item rows with the 38×38 rounded category tile).
- `DateRangeScreen.dc.html` — the bottom-sheet pattern (scrim, drag handle,
  rounded 28px top, Cancel/Apply pill buttons). Reuse this pattern for any new sheet.
- Phone preview size **300×620**, `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected chips use `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Reuse existing styling: chips = radius 99px, pad 6×13, 12px/600, selected =
  `--secc`/`--onsecc`; search field = `--sc`, radius 99px, pad 11×16; item rows =
  name 13.5px/600 `--on`, subtitle 11px `--onv`, price 13.5px/700, divider
  `border-top:1px solid var(--outv)`.

## Screens to design

### 1. `HistorySearchScreen.dc.html` — active search / results state
The History screen with a query typed in (use **"coffee"**). Show:
- The search field containing "coffee" with a small ✕ clear button on the right.
- **A results-summary row** directly under the filters, e.g.
  **"8 items · €34.60"** — count on the left (13px/700 `--on`), total on the
  right (`--onv`). This is the payoff of searching, so make it clear but compact.
- **A "Sort" chip** added to the filter chip row (after Date), label "Sort"
  with a small up/down arrows glyph.
- Item rows where the matched text **"Coffee"** is highlighted inside the name
  (e.g. tinted `--primary` or a subtle `--secc` highlight behind the matched
  span). Matches should span item name, store, and category — show at least one
  row that matched on the **store** and one on the **category** to make that clear.
- Add a subtle right-chevron (or keep rows clean but tappable) to hint that a
  row opens its full receipt (`ReceiptDetailScreen`).

### 2. `HistorySortScreen.dc.html` — sort options
The Sort control opened. Either a small dropdown/popover anchored to the Sort
chip, or a compact bottom sheet (your call — match the app). Options, single-select:
- Newest first (default, checked)
- Oldest first
- Price: high → low
- Price: low → high

### 3. `HistorySearchEmptyScreen.dc.html` — focused, empty query ("quick find")
The search field focused but empty — help the user start a search. Show:
- A **"Recent searches"** section: 3–4 removable pills (e.g. "milk", "Lidl",
  "fuel").
- A **"Top stores"** row of chips (e.g. Kaufland, Lidl, dm, Shell) and a
  **"Top categories"** row of emoji+label chips (e.g. 🥬 Fruits & Veg, ⛽ Fuel,
  🍴 Restaurant). Tapping one would apply it as a filter.

### 4. `PriceRangeScreen.dc.html` — price filter sheet
A bottom sheet matching `DateRangeScreen.dc.html`'s pattern, for filtering by
amount:
- Title "Price range", drag handle, big current value readout (e.g.
  **"€0 – €100"**).
- A min–max **range slider** with two handles, plus the two endpoint values
  shown as `--secc` value chips.
- Cancel / Apply pill buttons (outline + filled `--primary`), same as DateRange.

## Already covered — please DON'T redo
- **Custom date range**: `DateRangeScreen.dc.html` already exists. (Optional: if
  easy, show a small preset row — This month / Last month / Last 3 / Last 6 /
  Custom — at the top of it, since the app uses presets today. Otherwise skip.)
- **Receipt detail on tap**: reuse the existing `ReceiptDetailScreen.dc.html`.

## Output
- Save each as a new `*.dc.html` file in this project (don't overwrite
  `HistoryScreen.dc.html`).
- Keep them dark/light-token driven (use the CSS vars above, no hard-coded
  grays) so they theme correctly.
- Phone first. Once I approve these, I'll ask for matching `TabletHistory…` /
  `TabletLsHistory…` variants.

Thanks!
