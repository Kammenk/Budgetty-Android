# Claude Design request — portrait tablet (single-pane) for every screen, in one new canvas that also holds the landscape two-pane set

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new single-pane
> `*.dc.html` screens and one new canvas file; once they're there, Claude Code
> reads them back via DesignSync and implements them.

---

Hi! I'm about to implement Budgetty's tablet layouts, and I want **one clean
canvas** that holds the whole tablet story — **portrait on one side, landscape on
the other** — so I have a single source of truth to build from.

Two parts:

- **Part 1 (new work):** design a **portrait-tablet, single-pane** version of
  **every screen** — one calm, centered column, *not* the old spread-out
  multi-column tablet layouts.
- **Part 2 (reuse):** in that same new canvas, **pull in the landscape two-pane
  screens you already made** (`*TabletTwoPane.dc.html`) — don't redesign them,
  just place them as the landscape region.

Everything goes into **one brand-new canvas file** (see "The new canvas" below).
Please **don't touch** the existing phone screens, the old `Tablet*Screen`
portrait files, or `Tablet & Foldable.dc.html` — this is a fresh, self-contained
deliverable.

## Why single-pane for portrait
The current portrait-tablet screens (`TabletHomeScreen`, `TabletBudgetScreen`,
`TabletInsightsScreen`, …) fan the phone content into **2–4 column dashboards and
grids**. On a portrait tablet that reads **thin and spread out** — the same
problem we just fixed in landscape by going two-pane. Portrait is narrower, so the
fix is even simpler: **stop multi-columning it.** Give me **one centered content
column** at a comfortable reading width, phone-style density, centered in the
tablet's width with generous side margins. Structure through width limits and
whitespace, **not** through extra columns.

## Shared rules for all portrait single-pane screens (read first — this is the point)
- **One centered column.** Cap content at a comfortable measure (~**640px**, up to
  ~720px for list-heavy screens) and **center it** in the portrait canvas with
  balanced side margins. **Never** split a portrait screen into 2/3/4 columns or a
  card grid (the category *picker* grid is the one exception — that grid is the
  component itself; see screen 12).
- **The nav rail stays.** The app already shows the ~**72px navigation rail** at
  the far left on tablets (Home, History, Insights, Budget, Account — current tab
  highlighted) in **both** orientations. Draw it; the single column lives in the
  content area to its right: `[rail] │ [ centered column ]`.
- **Components at normal phone density.** Reuse the existing cards, list rows,
  chips, donut, progress bars, stat tiles, and 38×38 category tiles at their
  **normal sizes** — don't stretch them to fill the width. A little margin inside
  the column is fine and wanted.
- **Screen header spans the content area top** (as today). Per-screen controls (a
  search field, a period stepper, a segmented toggle) sit at the top of the
  column, not floating.
- **Light and dark**, token-driven (CSS vars below) — no hard-coded grays.

## The new canvas
Create **one new canvas file** — suggested name **`Tablet Portrait + Landscape.dc.html`**
— composed via `<dc-import>`, organized into **two clearly divided, contiguous
regions**:

- **Region 1 — Portrait (single-pane):** the new `*TabletPortrait.dc.html` frames
  from Part 1, grouped by the sub-sections below (Nav → Auth & Onboarding →
  Capture & Detail → Premium → Settings & More → Dialogs).
- **Region 2 — Landscape (two-pane):** the **existing** `*TabletTwoPane.dc.html`
  frames (Part 2), in the **same sub-section order** so the two halves read as
  mirrors, followed by the landscape centered-dialog frames.

No portrait frame inside the landscape region or vice-versa; put an obvious
divider/heading between the two regions.

## Canvas & style
- **Portrait-tablet canvas ~**`800 × 1280`** (10" tablet portrait). Long scrolling
  screens (Insights, Budget) may use a taller frame — that's fine. Include the
  ~72px nav rail at the far left inside each portrait mock.
- Landscape frames keep the two-pane `~1180 × 760` canvas they already have.
- `font-family: Roboto`, Material 3 (Material You), same rounded cards (20px
  radius, 16px rows), soft surfaces, emoji category icons.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Reuse the existing **green / amber / red** budget-progress colors (green <50%,
  amber 50–74%, red 75%+).

---

# Part 1 — Portrait single-pane screens (new)

Save each as a **new** `<Screen>TabletPortrait.dc.html`. The matching **phone
screen is the source of truth** for content and components — reuse them verbatim,
just stacked in one centered column at tablet width.

## 1a. The five nav screens

### 1. `HomeScreenTabletPortrait.dc.html`
Source: `HomeScreen`. One column, top→bottom: period filter (This month ▾) → big
**"Total spent this month"** figure → **budget progress** card (Monthly + Weekly
bars, tappable → Budget) → **category breakdown** (donut + top categories) →
**"Recent receipts"** list (store tile · "{date} · N items" · total · green
−discount · chevron) → **"Add receipt"** extended FAB bottom-right. No 3-column
dashboard. Include the empty state (no receipts yet).

### 2. `HistoryScreenTabletPortrait.dc.html`
Source: `HistoryScreen`. One column: search field → filter chip row (Date,
Category, Sort, Price) → the **Receipts | Items | Budgets** segmented toggle →
month-grouped list (38×38 tile, name, "{date} · N items", total). Tapping a
receipt opens **Receipt Detail as a centered dialog** (not a side pane — that's the
landscape job). No 3-column card grid. Note the search empty state.

### 3. `InsightsScreenTabletPortrait.dc.html`
Source: `InsightsScreen`. One column, stacked (no donut|trend|tiles side-by-side):
**period stepper** (‹ Month ›, Week/Month/Quarter/Half/Custom) → **donut** (total in
the hollow center) → **trend chart** → **stat tiles** (Total spent, Receipts, Avg /
receipt) → **category breakdown** rows (emoji tile + name + amount + % + mini bar)
→ **Top stores** → **vs-previous-period** comparisons → **budget-vs-actual** rows →
the **income/recurring cards** (Income-vs-Spending, Savings-rate, Fixed-vs-Flexible,
Upcoming-bills, Income-by-source). Respect the "Customize sections" show/hide order.

### 4. `BudgetScreenTabletPortrait.dc.html`
Source: `BudgetScreen` (+ `BudgetIncomeRecurringScreen`). One column, in order:
**Income** section → **Recurring payments** section → **Breakdown** card
(income − bills − spent = left this month; only when income exists) → **Spending
budget** header → **Monthly / Weekly** pill + big amount card with green/amber/red
progress + "≈ {x} / week" hint → **per-category budget** list (each: 38×38 tile +
name + budgeted amount + spent/budget bar, editable) → slim "＋ Add a category
budget" row. Replace today's 4-column grid with the single list. Include the
money-empty state.

### 5. `AccountScreenTabletPortrait.dc.html`
Source: `AccountScreen`. One column: **profile card** (avatar initials "KK", name,
email, Free/Premium badge + **Upgrade** chip when free) → grouped settings cards
with inset hairline dividers — **Budgeting** (Budget, Category memory, Custom
categories) · **Preferences** (Theme, Language, Date format, Notifications) ·
**Data** (Export, Import, Widgets) · **Support** (Help, Contact, Rate, Privacy) ·
**Account** (Sign out, Delete account). No 3-column settings.

## 1b. Auth & onboarding

### 6. `LoginScreenTabletPortrait.dc.html`
Source: `LoginScreen`. Centered, single column: brand mark + **Budgetty** wordmark
+ "Welcome back" / "Create your account" subtitle → email → masked password →
full-width **Sign in / Create account** (inline spinner in loading) → outlined
**Continue with Google** → **Forgot password?** link → mode-toggle text button →
inline error text. Note sign-up + error variants. (No nav rail here — pre-auth.)

### 7. `OnboardingScreenTabletPortrait.dc.html`
Source: `OnboardingScreen`. Centered column: illustration for the current page →
page dots → headline → body paragraph → full-width **Next** (→ **Get started** on
the last page) + quiet **Skip**. Mock one representative page. (No nav rail —
pre-app.)

## 1c. Capture & detail

### 8. `UploadReadingTabletPortrait.dc.html`
Source: `UploadReadingScreen`. The "reading your receipt" progress state, centered:
the receipt thumbnail/placeholder + animated progress + status copy. Single column.

### 9. `ReviewEditScreenTabletPortrait.dc.html`
Source: `ReviewEditScreen`. One column: store + date header → scanned line-item rows
(category tile + editable name + price) → **Add item** row → summary (subtotal,
discount, **"incl. VAT"**, grand total) → the soft **price-mismatch notice** when
totals disagree → pinned **Save / Finalize** button (56dp). If the category-rule
"remember this?" prompt applies, show it here as a centered dialog.

### 10. `ReceiptDetailTabletPortrait.dc.html`
Source: `ReceiptDetailScreen`. On tablet this opens as a **centered dialog**
(AdaptiveSheet) over the current screen — store header, date, scrollable item list
with category tiles, subtotal, discount, **"incl. VAT"** line, grand total; header
+ buttons pinned, only the item list scrolls. Show it as a centered-dialog frame
over a dimmed portrait Home backdrop, **not** a full-bleed multi-column page.

## 1d. Premium

### 11. `PaywallScreenTabletPortrait.dc.html`
Source: `PaywallScreen`. Centered column: **Budgetty Premium** heading + benefit
list (unlimited scans, custom categories, unlimited budgets & recurring, accent
themes) → **Monthly** and **Yearly** plan cards (Yearly "best value" badge, price)
→ primary CTA (**Start free trial / Go Premium**) → **Restore purchases** + terms
line. Also mock the **"You're Premium"** status variant.

## 1e. Settings & more (single-pane, centered)

### 12. `CategoryPickerTabletPortrait.dc.html`
Source: `CategoryPickerScreen`. Full-screen picker surface, centered. **Keep the
3-column emoji-card grid** — that grid *is* the component, not a tablet
multi-column layout; just center the surface at a comfortable width with the
search field and "＋ Create category" affordance. (This is the one screen that
stays a grid.)

### 13. `CategoryMemoryTabletPortrait.dc.html`
Source: `Category Memory Variants` / `CategoryRulesScreen`. The learned name→
category rules list, single centered column (rule rows + the "remember / propagate"
prompt shown as a centered dialog).

### 14. `WidgetPickerTabletPortrait.dc.html`
Source: `WidgetPickerScreen`. The in-app "Add a widget" picker — widget-type cards
stacked/centered in one column, phone density.

### 15. `SettingsDetailTabletPortrait.dc.html`
Sources: `NotificationPrefsScreen` + `SupportAboutScreen`. A representative
settings sub-page (e.g. Notifications prefs, and Support/About) as a single
grouped card in one centered column — the normal `SettingRow` / switch style.

## 1f. Dialogs, sheets & pickers — stay centered dialogs

### 16. `DialogsTabletPortrait.dc.html`
Everything that's a sheet/dialog/picker on phone **stays a centered dialog** on
portrait tablet (the app's `AdaptiveSheet` pattern) — **not** a pane, **not**
full-screen. One frame is enough, showing a representative centered dialog over a
dimmed portrait backdrop, and note it covers: **Add receipt** sheet, **Date
range** picker, **Category transactions** sheet, **Price range** sheet, **Sort**
menu, **Income** sheet, **Recurring payment** sheet, **Custom category** picker,
**Period dropdown**, **Customize sections** sheet, and confirmation dialogs.

---

# Part 2 — Landscape two-pane (reuse the existing set)

**Do not redesign these — reuse them by `<dc-import>` in the new canvas's landscape
region.** They already exist and are approved:

- `HomeScreenTabletTwoPane.dc.html`
- `HistoryScreenTabletTwoPane.dc.html`
- `InsightsScreenTabletTwoPane.dc.html`
- `BudgetScreenTabletTwoPane.dc.html`
- `AccountScreenTabletTwoPane.dc.html`
- `LoginScreenTabletTwoPane.dc.html`
- `OnboardingScreenTabletTwoPane.dc.html`
- `PaywallScreenTabletTwoPane.dc.html`
- `ReviewEditScreenTabletTwoPane.dc.html`

Place them in the **same sub-section order** as Part 1 (Nav → Auth & Onboarding →
Capture & Detail → Premium). Then add the **landscape centered-dialog** frames
(same AdaptiveSheet dialogs as 1f, over a landscape backdrop). Note that in
landscape, **Receipt Detail** is already covered — it's the right panel of
`HistoryScreenTabletTwoPane` and a centered dialog when opened from Home — so no
separate landscape Receipt Detail is needed.

---

## Localization note
The app is localized into 21 languages (German and Bulgarian run long) and amounts
show in the user's currency (EUR by default). Let headers, section labels, chips,
and stat tiles **wrap/space gracefully** rather than truncate.

## Output
- **New files:** the `*TabletPortrait.dc.html` screens from Part 1, all light +
  dark, token-driven (no hard-coded grays).
- **New canvas:** `Tablet Portrait + Landscape.dc.html` — Region 1 (portrait
  single-pane) then Region 2 (landscape two-pane), clearly divided, mirrored order.
- **Reuse (don't recreate):** the nine `*TabletTwoPane.dc.html` files, imported
  into Region 2.
- **Don't touch:** the phone screens, the old `Tablet*Screen` portrait files, or
  `Tablet & Foldable.dc.html`.
- When done, tell me briefly what you created and how you split the new canvas so I
  can eyeball it before I implement.

Thanks!
