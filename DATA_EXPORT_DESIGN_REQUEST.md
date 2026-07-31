# Claude Design request — Android Data export: CSV & PDF (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups; Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add **CSV & PDF export** to Budgetty (Android phone). The app
already has a JSON **backup/restore** (for moving data between installs), but people
want a **human-readable spreadsheet (CSV)** and a **statement (PDF)** for taxes,
expenses and sharing. Please mock this up in the Material 3 system. **iOS gets the
identical feature** (`IOS_DESIGN_REQUEST_DATA_EXPORT.md`).

## Why / the goal
- CSV → open in Excel/Sheets; PDF → a clean, branded spending **statement**.
- Distinct from JSON backup: backup is opaque and for migration; **export is for humans.**
- **Premium feature.** A free user tapping **Export** hits the paywall.

## Where it lives
On the **Account** screen, right next to the existing **Backup / Restore** rows — a
new **Export** row that opens an **export options sheet**, then hands the finished
file to the Android **share sheet**.

## Match these existing components for style
- **`AccountScreen.dc.html`** — the settings list; match the Backup/Restore row
  style for the new Export row + its Premium lock affordance.
- **`PeriodDropdownScreen.dc.html`** — reuse the period picker (This month / Last
  month / Last 3 / Last 6 / All time) **+ a Custom range** option.
- **The paywall lock** (`AccountPaywallScreen.dc.html`) — for the free-user state.
- Phone **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Screens / states to draw
1. **Account — Export row:** the new row in the settings list (Premium users), and
   its **Premium-locked** variant (lock + "Unlock export").
2. **Export options sheet:** 
   - **Format** toggle — **CSV (spreadsheet)** / **PDF (statement)** (reuse the app's segmented toggle).
   - **Period** — the period dropdown + Custom range.
   - **Categories** — *All categories* (optional: a multi-select to narrow).
   - A primary **Export** button.
3. **The PDF statement layout** — *the key visual*. A clean, **branded** one-page
   statement: a header (Budgetty wordmark, the period, total spent / income / net),
   a **by-category summary table** (category emoji + name + total + %), and a
   **transactions table** (date · store · category · amount). Design this so it
   looks like something a user would happily attach to an email.
4. **Success / share:** the moment the file is ready → the Android share sheet (a
   representative "Export ready" confirmation).
5. **Empty period:** chose a period with no data — a gentle "Nothing to export for this period."

## Design tokens (CSS vars already defined)
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; accent `--primary`;
  category colors for the summary table; dividers `--outv`.
- The PDF is a **document**, so it can lean lighter/print-friendly — but derive its
  accent + category colors from the same tokens for brand consistency.

## Please produce
- **`DataExportScreen.dc.html`** — the Account Export row (locked + unlocked), the
  export options sheet (state 2), the success/share state, and the empty state.
- **`ExportStatementPdf.dc.html`** — the branded PDF statement layout (state 3),
  shown as a page (portrait), light/print-friendly.
- Token-driven so the in-app parts theme in **dark + light**.

## Implementation notes (for after the mockup)
- **Native, no libraries** (fits our ethos): CSV = string building; **PDF via
  Android's `PdfDocument` / print framework**. Output via a `FileProvider` +
  `Intent.ACTION_SEND` share sheet.
- Data source = the existing transactions (+ income/recurring for the statement
  totals), filtered by the chosen period/category — the same ranges
  `DateRangeFilter` already computes.
- **Gating:** the Export row + action gate on `billingManager.isPremium`.

Thanks!
