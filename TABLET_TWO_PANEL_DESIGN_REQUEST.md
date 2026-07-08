# Claude Design request — landscape tablet, two-panel layouts for the 5 main screens

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I want to rework the **landscape-tablet** layouts for Budgetty's five main
screens — the ones in the left navigation rail: **Home, History, Insights,
Budget, Account**. Please mock up a **landscape-tablet variant of each** as a new
`*.dc.html` file in this project, matching the existing design system exactly.

## The problem I'm solving
On landscape tablets these screens currently fan the phone content out into wide
**multi-column dashboards / grids** (Home = 3 columns, History = a 3-column card
grid, Insights = donut | trend | tiles side by side, Budget = a 4-column category
grid, Account = 3 columns of settings). It reads **thin and spread out** — lots
of air, no hierarchy, and the cards balloon to fill the width.

I want to try the opposite: split each screen's content into **two panels** — a
narrower **primary/summary or list** panel on the left and a wider **detail or
feed** panel on the right — so the layout feels structured and purposeful instead
of stretched. On several screens this also lets a tap fill the **right** panel
inline (e.g. a receipt's items) instead of opening a bottom sheet.

## Shared rules for all five (read first — this is the whole point)
- **The nav rail is not a panel.** The app already shows a ~72px **navigation
  rail** at the far left on tablets (5 icons: Home, History, Insights, Budget,
  Account — the current tab highlighted). Draw it, but the **two panels live in
  the content area to the right of it**. So each mock reads: `[rail] │ [left
  panel] │ [right panel]`.
- **Two panels, a gutter, independent scroll.** A comfortable gutter (~20–24px)
  between the panels; **each panel scrolls on its own**. Give the panels a clear
  size relationship (proportions suggested per screen below) — never two equal
  halves unless noted.
- **Keep components at their normal phone density.** The fix for "spread out" is
  *structure*, not bigger cards. Reuse the existing cards, list rows, chips,
  donut, progress bars, stat tiles, and 38×38 category tiles at their **normal
  sizes** inside the panels — don't stretch them to fill a panel's width; cap
  content width and let a little margin sit inside a panel if needed.
- **Screen header spans the top.** Keep one screen title row across the top of
  the content area (as today). Controls that clearly belong to one panel (a
  search field, a period stepper) sit at the **top of that panel**, not the
  shared header.
- **List-detail screens need a selection + a placeholder.** Where the right panel
  shows the detail of a left-panel selection (History, Account, and optionally
  Insights), show the **selected left row highlighted** (use the `--secc` /
  `--onsecc` selected treatment) and design the **right panel's empty state**
  too ("Select a receipt to see its items", etc.).
- **Light and dark**, token-driven (CSS vars below) — no hard-coded grays.

## Canvas & style
- **Landscape tablet canvas, 16:10** — roughly **1180 × 760** (or 1280 × 800 if
  it renders crisper). Include the ~72px nav rail at the far left inside the mock.
- `font-family: Roboto`, Material 3 (Material You), same rounded cards (20px
  radius, 16px for rows), soft surfaces, emoji category icons.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Reuse the existing **green / amber / red** budget-progress colors for budget
  figures and bars (green <50%, amber 50–74%, red 75%+).

---

## Screens to design

### 1. `HomeScreenTabletTwoPane.dc.html` — Overview | Activity
Split the Home feed into a glance panel and an activity feed.
- **Left panel (~40%) — the glance.** Period filter (This month ▾), the big
  **"Total spent this month"** figure, the **budget progress** card (Monthly +
  Weekly bars, tappable → Budget), and a compact **category breakdown** (small
  donut + top 3–4 categories). This is the "how am I doing" summary.
- **Right panel (~60%) — the activity feed.** Section header **"Recent
  receipts"**, then the scrollable list of receipt rows (store tile, "{date} · N
  items", bold total, green "−{discount}", chevron). Keep the **"Add receipt"**
  action reachable here (extended FAB bottom-right of this panel, or a button in
  the header).
- **Interaction (nice-to-have):** a receipt row could open into the right panel
  or a centered dialog — but Home's job is the split above; the receipt *detail*
  list-detail belongs to History (screen 2).

### 2. `HistoryScreenTabletTwoPane.dc.html` — receipt list | receipt detail (the flagship)
This is the strongest two-panel case: a true **list-detail (master-detail)**.
- **Left panel (~40%) — the list.** The search field, the filter chip row (Date,
  Category, Sort, Price), the **Receipts | Items** segmented toggle, then the
  scrollable, month-grouped list of **receipt rows** (38×38 store/category tile,
  name, "{date} · N items", total). **One row is selected/highlighted** (`--secc`).
- **Right panel (~60%) — the detail.** The selected receipt shown **inline**
  (reuse the `ReceiptDetailScreen` content: store header, date, the scrollable
  item list with category tiles, subtotal, discount, **"incl. VAT"** line, grand
  total). This replaces opening a bottom sheet on tablet.
- **Right panel empty state:** when nothing is selected — a quiet centered
  placeholder, e.g. a 🧾 glyph + **"Select a receipt to see its items."**

### 3. `InsightsScreenTabletTwoPane.dc.html` — charts | breakdown detail
- **Left panel (~55%) — the visual summary.** The **period stepper** (‹ Month ›,
  Week / Month / Quarter / Half / Custom), the **donut** (category breakdown,
  total in the hollow center), the **trend chart** below it, and the key **stat
  tiles** (Total spent, Receipts, Avg / receipt).
- **Right panel (~45%) — the detailed breakdown.** The scrollable **category
  rows** (emoji tile + name + amount + % + mini bar), **Top stores**, the
  **vs-previous-period** comparison deltas (green/red), and **budget-vs-actual**
  rows. This is where the numbers live so the charts stay uncluttered.
- **Interaction (nice-to-have):** tapping a donut arc **highlights** the matching
  category row in the right panel (show one arc + its row in the selected state).

### 4. `BudgetScreenTabletTwoPane.dc.html` — money summary | category allocations
- **Left panel (~42%) — the money summary & controls.** The **Income** section,
  **Recurring payments** section, the **Breakdown** card (income − bills − spent =
  left this month, shown only when income exists), then the **Spending budget**
  header + the **Monthly / Weekly** pill + the big amount card with the
  green/amber/red progress bar + the "≈ {x} / week" hint. This is "how much money
  and my overall budget."
- **Right panel (~58%) — where it goes.** The **per-category budget** list —
  each category as a row: 38×38 tile + name + budgeted amount + a spent/budget
  progress bar (green/amber/red), editable. Replace today's wide 4-column grid
  with a **1–2 column** list that stays a comfortable width. A slim "＋ Add a
  category budget" row at the end.

### 5. `AccountScreenTabletTwoPane.dc.html` — settings nav | settings detail (iPad-Settings style)
- **Left panel (~34%) — profile + section nav.** The **profile card** (avatar
  initials e.g. "KK", name, email, plan badge Free/Premium + an **Upgrade** chip
  for free), then a grouped, **selectable list of setting sections**:
  **Budgeting** (Budget, Category memory, Custom categories) · **Preferences**
  (Theme, Language, Date format, Notifications) · **Data** (Export, Import,
  Widgets) · **Support** (Help, Contact, Rate, Privacy policy) · **Account**
  (Sign out, Delete account). One section row is **selected** (`--secc`).
- **Right panel (~66%) — the selected section's settings.** Show, say,
  **Preferences** selected on the left and its rows on the right (Theme = System,
  Language = English, Date format sample, a Notifications switch) as a single
  grouped card with inset hairline dividers — the normal `SettingRow` / switch
  style.

## Match these existing screens for style (don't recreate them)
- The current phone screens are the source of truth for content and components:
  **HomeScreen, HistoryScreen, InsightsScreen, BudgetScreen, AccountScreen**, plus
  `ReceiptDetailScreen.dc.html` (reuse verbatim as History's right panel) and
  `BudgetIncomeRecurringScreen.dc.html` (the income/recurring sections for
  Budget's left panel). Reuse their cards, rows, chips, donut, tiles, and
  progress bars exactly — I'm only changing the **landscape composition**, not
  the components or the phone layouts.

## Localization note
The app is localized into 21 languages (German and Bulgarian run long) and
amounts show in the user's currency (EUR by default). Let headers, section
labels, chips, and stat tiles **wrap/space gracefully** rather than truncate.

## Output
- Save each as a **new** `*.dc.html` file (names above) — **don't overwrite** the
  existing phone screens.
- Light + dark, token-driven (CSS vars above, no hard-coded grays).
- If a screen works better with a different split than I proposed, mock **both**
  and label them so I can compare — the two-panel idea matters more than my exact
  percentages.

Thanks!

---
---

# Follow-up request 2 — the rest of the screens + clean up the landscape set

> These 5 look great — thank you! This is a **separate follow-up message**; paste
> everything below the line into the same Claude Design project as a new turn.

---

The five two-pane landscape screens (`HomeScreenTabletTwoPane`,
`HistoryScreenTabletTwoPane`, `InsightsScreenTabletTwoPane`,
`BudgetScreenTabletTwoPane`, `AccountScreenTabletTwoPane`) look great. Two things
now: **(A)** give the remaining full-screen flows the same landscape treatment,
and **(B)** delete the old landscape designs and tidy the `Tablet & Foldable`
canvas so portrait and landscape are cleanly separated.

## A. The rest of the screens — landscape two-panel variants
Same rules, tokens, and ~1180×760 landscape canvas as the five above (nav rail is
**not** a panel; two panels + gutter + independent scroll; components at normal
density; light + dark). These flows are already conceptually two-sided, so lean
into it. Save each as a **new** `*TabletTwoPane.dc.html`.

### 6. `LoginScreenTabletTwoPane.dc.html` — brand | form
- **Left panel (~45%) — brand.** A `--primary`-tinted / soft-gradient panel: the
  **Budgetty** wordmark + a brand logo mark/illustration, and the subtitle that
  switches **"Welcome back"** (sign-in) / **"Create your account"** (sign-up).
- **Right panel (~55%) — the auth form, vertically centered.** Email field,
  masked password, full-width primary button (**Sign in** / **Create account**,
  with an inline spinner in the loading state), outlined **"Continue with
  Google"**, the **"Forgot password?"** link, the mode-toggle text button, and
  inline **error** text under the fields. Show sign-in, and note the sign-up +
  error variants.

### 7. `OnboardingScreenTabletTwoPane.dc.html` — visual | copy + CTA
- **Left panel (~50%) — the illustration** for the current page, with the **page
  dots** beneath it.
- **Right panel (~50%) — copy + action**, vertically centered: page **headline**,
  body paragraph, a full-width **"Next"** (→ **"Get started"** on the last page)
  and a quiet **"Skip"**. Mock one representative page; note it repeats per page.

### 8. `PaywallScreenTabletTwoPane.dc.html` — value | plans
- **Left panel (~50%) — brand + benefits.** The gradient premium panel: **"Budgetty
  Premium"** heading + the benefit list (unlimited scans, custom categories,
  unlimited budgets & recurring, accent themes, etc.) with check/sparkle glyphs.
- **Right panel (~50%) — plan cards.** **Monthly** and **Yearly** cards (Yearly
  with a "best value" badge), price, the primary CTA (**Start free trial** / **Go
  Premium**), plus **Restore purchases** and the terms line.
- **Premium state:** right panel shows the **"You're Premium"** status card
  instead of the plan cards (mock this variant too).

### 9. `ReviewEditScreenTabletTwoPane.dc.html` — scanned items | summary + finalize
This is the Review & Edit step of the capture flow (reached after a scan, and via
Home → tap a receipt → Edit).
- **Left panel (~58%) — the editable item list.** Store + date header, then the
  scanned line-item rows (category tile + editable name + price), an **"Add item"**
  row. This is the bulk work area and scrolls.
- **Right panel (~42%) — summary + finalize.** Subtotal, discount, **"incl. VAT"**,
  the grand total, the soft **price-mismatch notice** when totals disagree, and
  the pinned primary **"Save"** / **"Finalize"** button (56dp). If a category-rule
  "remember this?" prompt applies, it surfaces here.

### Rulings on the last two old landscape files (so nothing dangles)
- **Receipt Detail — no standalone two-pane.** On landscape it's already covered
  two ways: it's the **right panel of `HistoryScreenTabletTwoPane`**, and when
  opened from Home it's a **centered dialog** over the current screen (the app's
  `AdaptiveSheet` pattern — sheets become centered dialogs on tablet). So please
  **don't** make a full-screen landscape Receipt Detail; if useful, one optional
  frame showing it as a centered dialog over a dimmed Home is enough.
- **Trends — folded into Insights.** The app has no separate Trends route; its
  trend chart + "by category vs last period" live in `InsightsScreenTabletTwoPane`.
  So **drop landscape Trends** — no new file.

### Dialogs, pickers & sheets stay centered dialogs (not two-pane)
Everything that's a sheet/dialog/picker on phone (Add receipt, Date range,
Category picker, Category transactions, Price range, confirmations, etc.) should
**stay a centered dialog** on landscape too (same `AdaptiveSheet` treatment as
portrait tablet) — they are **not** two-panel screens. Keep the existing portrait
tablet dialog/picker frames; just make sure the landscape half of the canvas
shows them over a landscape backdrop.

## B. Delete the old landscape designs, keep portrait, consolidate the canvas

**1. Delete these 11 old landscape screens** (the `TabletLs*` set — superseded by
the new two-pane files):
`TabletLsHome`, `TabletLsHistory`, `TabletLsInsights`, `TabletLsBudget`,
`TabletLsAccount`, `TabletLsLogin`, `TabletLsOnboarding`, `TabletLsPaywall`,
`TabletLsReceiptDetail`, `TabletLsReviewEdit`, `TabletLsTrends`.

**2. Keep every portrait design** — don't touch the `Tablet*Screen` portrait files
(`TabletHomeScreen`, `TabletHistoryScreen`, `TabletInsightsScreen`,
`TabletBudgetScreen`, `TabletAccountScreen`, `TabletLoginScreen`,
`TabletOnboardingScreen`, `TabletPaywallScreen`, `TabletReviewEditScreen`,
`TabletReceiptDetailScreen`, `TabletCategoryPickerScreen`, `TabletDialogScreen`,
`TabletTrendsScreen`) or the phone screens.

**3. Update `Tablet & Foldable.dc.html`** so it imports the **new** set:
- **Remove** every `<dc-import>` of a deleted `TabletLs*` screen.
- The landscape section should import the **nine `*TabletTwoPane` screens** (the
  5 nav ones already added + the 4 new ones above: Login, Onboarding, Paywall,
  ReviewEdit).
- Keep landscape **dialogs/pickers** as the centered-dialog frames described in A.

**4. Separate portrait and landscape in the HTML.** Right now the canvas
**interleaves** them (PORTRAIT → LANDSCAPE → AUTH & ONBOARDING → LANDSCAPE AUTH &
ONBOARDING → CAPTURE & DETAIL → LANDSCAPE CAPTURE & DETAIL → …). Please reorganize
into **two contiguous top-level regions with a clear divider**:
- **Region 1 — Portrait (& Foldable):** every portrait/foldable frame, grouped by
  the existing sub-sections (Tablet Portrait, Auth & Onboarding, Capture & Detail,
  Settings & More, Dialogs, System & Secondary, Budget Income & Sheets, Foldable…).
- **Region 2 — Landscape (two-pane):** every landscape frame — the nine
  `*TabletTwoPane` screens first, then the landscape dialogs/pickers — with the
  **same sub-section order** as portrait so the two halves read as mirrors.
- No landscape frames left sitting inside the portrait region, and vice-versa.

**5. Optional:** if `TabletLandscape.dc.html` is a separate, now-stale landscape
canvas that the landscape region of `Tablet & Foldable` supersedes, delete it too
— otherwise leave it. Your call; flag what you did.

## Output
- New files: the 4 `*TabletTwoPane.dc.html` above, light + dark, token-driven.
- Deletions + the `Tablet & Foldable.dc.html` reorg per section B.
- Tell me briefly what you deleted, what you added, and how you split the canvas,
  so I can eyeball it before I implement.

Thanks!
