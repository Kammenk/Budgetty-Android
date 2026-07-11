# Changelog

All notable changes to Budgetty are documented here, newest first. The format loosely
follows [Keep a Changelog](https://keepachangelog.com). Each entry lists the user-facing
**versionName** and the Play **versionCode** (which must strictly increase on every upload).

**Versioning (from 10.0.1 onward):** `versionName` is semantic — `MAJOR.MINOR.PATCH`. Bump
`PATCH` for fixes; at `.9` it rolls into the next `MINOR` (10.0.9 → 10.1.0), and `MINOR` rolls
into `MAJOR` the same way. The integer `versionCode` is derived in `app/build.gradle.kts` as
`MAJOR*100 + MINOR*10 + PATCH` — the version with the dots removed (10.0.1 → 1001) — so keep
MINOR and PATCH in 0–9 for it to stay monotonic. Earlier releases used a `1.x` name with a
plain incrementing code (…, 1.8 = code 10); the new MAJOR 10 continues from that code.

When preparing a new release, add a new section at the top describing only what changed
since the previous entry. The Play Console release-notes field wants the text wrapped in
`<en-US>…</en-US>` language tags, max 500 characters per language.

## 10.3.0 (versionCode 1030) — 2026-07-11

A feature (MINOR) release: much more accurate receipt scanning, plus a safety-net check before saving.

### Changed
- **Sharper receipt scanning** — the camera step now uses a guided document scanner that finds the receipt's edges, straightens and de-glares the image, and lets you review or retake before it's used. The cleaner picture means far fewer misread lines and prices — including the tricky cases where a weighed or multi-buy line (e.g. "2.000 x 4.19") was attached to the wrong product

### Added
- **"Double-check your items" prompt** — if your scanned items add up to less than the receipt's own printed subtotal (a sign a line was missed or misread), Budgetty now asks you to review before saving, so a dropped line doesn't slip through unnoticed

> The new strings ship fully translated in every supported language (no English-only fallback this time).

## 10.2.0 (versionCode 1020) — 2026-07-08

A feature (MINOR) release: two new spending categories and clearer category icons.

### Added
- **Two new categories** — **Video Games** (under Dining & Entertainment) and **Investments** (under Services & Subscriptions). Video Games is picked up automatically when you scan a receipt; Investments is there for tagging recurring payments and setting budgets

### Changed
- **Clearer category icons** — refreshed the emoji on a dozen categories so they're quicker to tell apart at a glance (Medical, Sports & Fitness, Entertainment, Clothing, Electronics, Garden & Plants, Subscriptions, Education, Travel, Insurance & Utilities, Rent and Office supplies)

## 10.1.0 (versionCode 1010) — 2026-07-05

A feature (MINOR) release focused on the Insights screen, plus full translation of everything on it.

### Added
- **Highlights card** — plain-language callouts about your spending: a category that rose or fell the most versus the previous period, a category you spent in for the first time, or one that dominated the period. Show or hide it from Customize sections
- **Spending pace** — the trend card now projects where the current period is heading ("On pace for about …") from how fast you're spending so far
- **Biggest purchases card** — your priciest individual buys for the selected period
- **Tap a store** in Top stores to see everything you bought there in that period, each item with its category

### Changed
- **Insights is now fully translated** — the period picker, budget labels, the new cards, empty states and other labels that previously appeared only in English are now localised in every supported language
- **Friendlier empty periods** — stepping to a period with no spending shows a clear "No spending recorded for …" message instead of blank space
- **Summary tile** shows your average spend per day instead of repeating the period total (already shown in the ring)

### Fixed
- **"1 day left"** now reads correctly in the budget countdown (was "1 days left")
- **Period arrows** no longer page back into empty months before your first receipt
- **Tablet budget card** now appears even before you've recorded any spending in a period

## 10.0.2 (versionCode 1002) — 2026-07-05

A small bug-fix (PATCH) release.

### Fixed
- **Camera no longer re-opens on foldables** — after scanning a receipt, folding/unfolding the device or rotating the screen would pop the camera open again over your receipt. It now stays on the review screen, and your in-progress edits are kept

## 10.0.1 (versionCode 1001) — 2026-07-05

First release on the new semantic-versioning scheme (see the note at the top). The version
picks up from the previous `1.8` line — its Play code was 10, so the new MAJOR is 10 — and the
`versionCode` is now derived automatically (10.0.1 → 1001). A small bug-fix (PATCH) release.

### Fixed
- **Custom categories no longer vanish after a scan** — finalizing a receipt could reset your custom categories and drop them from the picker; they now stay put
- **Sub-category budget sheet scrolls smoothly** — the per-sub-category budget sheet no longer jitters or bounces at the top and bottom of the list while scrolling

## 1.8 (versionCode 10) — 2026-07-03

### Added
- **VAT on receipts** — scans now capture the tax printed on the receipt and show each receipt's total as "incl. VAT". Receipts that add VAT on top of net prices are reconciled so the scanned total matches the printed grand total
- **Budgets tab in History** — a third view alongside Receipts and Items showing a read-only snapshot of your plan: income, recurring bills and what's left after them, with a shortcut to manage it
- **Income and savings insights** — five new Insights cards: Income vs Spending, Savings rate, Fixed vs Flexible spending, Upcoming bills, and Income by source (show or hide them from Customize sections)
- **Home-screen shortcuts** — long-press the Budgetty icon for quick actions: Scan receipt (straight to the camera) and Add manually
- **In-app update prompts** — Budgetty now uses Google Play's in-app update flow to let you know when a newer version is ready

### Changed
- Recurring bills can now be set to a one-off (**Once**) cadence, counted only in the month you add them
- The Insights custom date-range picker is now a cleaner single-month paged calendar

> Note: the new VAT and insights strings ship in English first; the existing 21-language
> translations are unaffected and untranslated strings fall back to English.

## 1.7 (versionCode 9) — 2026-07-03

### Added
- **Income and recurring payments** on the Budget screen — list your income sources and your recurring bills or subscriptions, and see a planned breakdown of what's left after them. The free tier includes three of each; Premium removes the limit
- **Custom categories** — create your own categories with a name and emoji straight from the category picker, then edit or delete them whenever you like. Free users can add up to three; Premium raises this to ten
- **Category memory** — when you change an item's category, Budgetty offers to remember that choice and apply it to matching items in future scans. Review and remove the learned rules under Account → Category rules

> Note: the new income, category and memory strings ship in English first; the existing
> 21-language translations are unaffected and untranslated strings fall back to English.

## 1.6 (versionCode 8) — 2026-07-01

### Changed
- After you add a widget from **Account → Widgets**, Budgetty now steps aside to your home screen once the widget is placed, so you can see where it landed instead of staying on the picker

## 1.5 (versionCode 7) — 2026-07-01

### Added
- Three more home-screen widgets — **This Week** (this week's spend with a vs-last-week change and comparison bars), **Scan receipt** (one tap straight into the camera), and **Top categories** (your biggest categories with a share breakdown) — joining the budget and summary cards, each in a large (4×2) or compact (2×2) size
- History now has a **Receipts / Items** switch: browse whole receipts grouped by month, or every individual item. Tap a receipt to open its details
- A **Recent receipts** section on Home showing your latest five regardless of the selected period, with a shortcut to the full History
- History **search and filtering** — sort by date or price, filter by a price range, and a quick-find panel offering your recent searches and most-used stores and categories

### Changed
- The euro (€) is now the default currency, and on a fresh install Budgetty picks your currency from your device region automatically
- The separate monthly- and weekly-budget widgets are now a single **Budget** widget that shows whichever period you've set, with the other period shown as an estimate
- Redesigned the category pickers — choosing a category is now a card grid, and the "all categories" breakdown lists each category's emoji, amount, share and a bar
- Home, Insights and History show loading skeletons on a cold start instead of briefly flashing an empty state

### Fixed
- Tapping a receipt on the Home screen opens its details again — recent receipts from an earlier month no longer did nothing
- Item rows in History's Items view are no longer tappable; only whole receipts open a detail view
- The Widgets screen's "Add to home screen" button now reflects whether that widget is actually placed (including ones added by dragging) and stays correct when you reopen the screen

> Note: the new widget, search and currency strings ship in English first; the existing
> 21-language translations are unaffected and untranslated strings fall back to English.

> Removed: the Bulgarian lev (BGN) is no longer a selectable currency (euro is the default).

## 1.4 (versionCode 6) — 2026-06-30

### Added
- Home-screen widgets — add a Monthly budget, Weekly budget, or Monthly summary card to your home screen in a large (4×2) or compact (2×2) size. Set them up from Account → Widgets with a live preview and a one-tap "Add to home screen", and tap a placed widget to jump straight to the matching screen
- The budget widgets always show a meaningful figure: if you've set only a monthly (or only a weekly) budget, the other period is derived from it

### Changed
- Refreshed the Insights period selector — the week/month/quarter/half-year control is now an elevated pill that shows the unit as a label above the period, with a tidier picker for switching units or choosing a custom range

> Note: the new widget strings ship in English first; the existing 21-language
> translations are unaffected and untranslated strings fall back to English.

## 1.3 (versionCode 5) — 2026-06-29

### Added
- Completed the large-screen redesign: History, the paywall, and the receipt review/edit screens now use multi-column tablet and landscape layouts, joining the already-adapted Home, Budget, Insights and Account screens
- On tablets, pop-up sheets (category picker, custom date range, category transactions, etc.) now open as centered dialogs instead of full-width bottom sheets

### Fixed
- Receipt scanning no longer shows made-up items from an unreadable photo. If the image is too blurry or dark, or the captured lines don't match the receipt's own printed item count and grand total, the scan is declined with a prompt to retake it — and a declined scan does not count against the free-scan allowance

> Note (internal — do not include in the Play listing): a hidden 11-tap gesture on the
> Account version label unlocks Premium on release builds for handing to testers; it is
> persisted and revocable via the `TESTER_PREMIUM_ENABLED` constant.

## 1.2 (versionCode 4) — 2026-06-28

### Added
- Tablet and large-screen support: a side navigation rail and multi-column layouts on wide displays (≥600dp)
- A three-column Home dashboard in landscape (≥840dp), with a working period filter and avatar
- Tablet-oriented stats on the Home screen

### Changed
- Onboarding, Login, Budget, Insights and Account screens now adapt to tablet widths

### Fixed
- Scanned-receipt total could come out lower than the receipt when a loyalty or coupon discount was present — the discount is no longer counted twice, and the review total now matches the receipt's printed grand total

> Note: the new tablet-layout strings ship in English first; the existing 21-language
> translations are unaffected and untranslated strings fall back to English.

## 1.1 (versionCode 3) — 2026-06-27

### Added
- In-app language switching across 21 languages, including Bulgarian
- 20 selectable currencies
- History and Support screens
- Receipt image viewer — tap a receipt to see the original
- Spending trends in Insights

### Changed
- Full visual refresh: new theme, redesigned onboarding, restyled dialogs
- Reworked category system — 46 categories (7 groups + subcategories) with emoji icons
- Profile name is now edited inline (separate edit-profile screen removed)
- "Add receipt" on a manual entry now appends the scanned items
- Consistent pill-shaped buttons throughout

## 1.0 (versionCode 2) — 2026-06-25

### Added
- In-app account deletion (Account → Delete account): removes the Firebase account and
  wipes local data (transactions, receipts, budgets), keeping the category taxonomy

## 1.0 (versionCode 1) — 2026-06 — initial internal release

- First build published to Play internal testing: AI receipt extraction, Firebase auth
  (email + Google), budgets (monthly/weekly/per-category), Insights, 46-category taxonomy,
  import/export backup, and a camera/file/manual add flow with a 5-scan free cap and
  Premium paywall gating

> Note: the 1.0 entries (versionCode 1–2) were reconstructed after the fact; 1.1 onward is
> recorded here at release time.
