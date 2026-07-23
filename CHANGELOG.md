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

## 10.7.1 (versionCode 1071) — 2026-07-23

A bug-fix (PATCH) release: three fixes to receipt scanning and the category breakdown, all reported from closed testing.

### Fixed
- **Delivery and service fees now show as their own lines** — on an order that charges delivery, a service fee or a courier tip (a food-delivery receipt, say), those amounts were being folded invisibly into the total instead of appearing as items. It looked like the delivery hadn't been counted, and adding it back by hand quietly doubled it. A scanned receipt now lists a **Delivery & fees** row and a **Tip** row where they apply, so the total is right and nothing is counted twice.
- **A card-payment slip no longer doubles its total** — a receipt with a total but no itemised lines (the slip a card terminal prints) opened with empty fields and its whole amount hidden behind the total, so typing in the one obvious item doubled it. Such a slip now opens with a single editable line already holding the amount, ready to name and categorise — no doubling.
- **The "All categories" breakdown no longer jumps** — opening **Insights → See all categories** with enough categories to fill the screen could make the sheet bounce up and down instead of settling. It now opens steady and scrolls smoothly to the end.

> None of these change the database or how anything is stored — a scanned delivery receipt simply reads the same way it always should have.

> **Play status — built for the Closed testing ("Alpha") track (versionCode 1071) on 2026-07-23; pending upload.** It supersedes 10.7.0 (vc1070). Swapping the build on a running closed track does not restart the 14-day tester clock.

## 10.7.0 (versionCode 1070) — 2026-07-22

A feature (MINOR) release: crash reporting you can switch off, a faster cold start, two
categories where there had been one confusing one, and home-screen widgets becoming a
Premium feature past the first two.

### Added
- **Crash reporting, with a switch to turn it off** — when Budgetty crashes it can now send an anonymous report so the fault can actually be found and fixed. It's on by default and there's a real off switch in **Account → Support**; turning it off stops collection there and then. The report carries the stack trace, your device model and OS, the app version and whether the app was in the foreground — never receipts, items, prices or budgets. Reports are kept 90 days. Section 1(f) of the privacy policy sets this out in full.
- **A prompt to rate Budgetty** — after a scan goes through, and only after three successful scans and three days of use, Budgetty asks once whether you'd rate it. Declining is remembered; it won't ask again for 90 days.

### Changed
- **"Subscriptions & Services" is now two categories** — the old sub-category was near-indistinguishable from the *Services & Subscriptions* group containing it, which made picking one genuinely confusing. It becomes **Subscriptions** and **Services**. Anything already filed under the old name moves to **Subscriptions**, since a recurring line is far more often a subscription; re-filing a genuine service is two taps. Your budgets, rules and recurring bills follow the rename — nothing needs redoing.
- **Home-screen widgets past the first two now need Premium** — widgets become the fifth thing Premium unlocks, alongside scans, custom categories, recurring bills and accent themes. Free accounts can keep **two** widgets on the home screen at once, counted across all five types and both sizes. The limit is live, not a high-water mark: remove one and the slot is free again immediately. Widgets already placed keep working — add a third and it's the *new* one that shows a locked card, never an older one.
- **A faster cold start** — Budgetty now ships a baseline profile, which lets Android pre-compile the code that runs on launch. Measured on a physical Pixel 6 over ten cold starts: a median of **291ms to first display, down from 321ms** — about 9% quicker.
- **Rebuilt on Google Play Billing 9.1.0** — the subscription plumbing moves off version 7.1.1, which Google retires on 31 August 2026. No change to how subscribing works or what anything costs.

### Removed
- **The Help & FAQ row in Support** — it linked to an anchor that had never existed on the site, so it did nothing at all. **Contact us** on the same screen still reaches a human.

> Under the hood this release adds the testing and quality tooling the project had been missing: static analysis (detekt), a JVM test stack with real database tests, screenshot tests, and GitHub Actions CI running all of it on every push. None of it changes shipped behaviour. The database moves to **v18** with a migration covering the category split, pinned by an instrumented test.

> **Play status — uploaded and released to the Closed testing ("Alpha") track on 2026-07-22.** It supersedes 10.6.2 (vc1062), which is what testers were running until now. Swapping the build on a running closed track does not restart the 14-day tester clock.

> ⚠️ **Shipped without a device check:** the two-widget free cap went from its branch into this build without ever running on a real home screen. Place a third widget and confirm the third card locks — and that removing one frees the slot again. Also worth re-checking the paywall in landscape: its benefit list is now six rows, and that layout has overflowed at compact heights before.

> The yearly subscription was verified end-to-end on a Pixel 6 the same day: the paywall's €59.99 annual plan now purchases successfully, and Play reports it on a genuinely yearly billing period. An earlier misconfiguration had that plan billing monthly at the annual price; it was replaced before any tester could reach it.

## 10.6.2 (versionCode 1062) — 2026-07-20

A fix (PATCH) release: receipts bought in multiples are no longer rejected as unreadable.

### Fixed
- **Receipts with multi-buy lines now scan** — a receipt that prints its item count as a number of *units* rather than lines (common across Greece and much of the EU) was rejected with "Couldn't read that receipt", however clear the photo. A shop with three multi-buy lines — "4 × 0,34" water, "6 × 1,42" cream, "2 × 0,11" bags — prints 18 items across 9 lines, and the check that guards against misreading a receipt counted those 9 against the printed 18 and refused the scan. Retaking the photo never helped, because the photo was never the problem: the receipt had been read correctly, down to the cent, before being discarded. The check now recognises both ways a receipt counts itself.

> Under the hood, the extraction eval gained the ability to assert the *verdict* of a scan and not merely its numbers — this bug read every value correctly and was invisible to the old checks — plus a set of offline tests for those guards that need no receipt, and a corpus case for the receipt that surfaced it.

> **Play status — uploaded 2026-07-20.** This supersedes 10.6.1 (vc1061), which went to Play on 2026-07-17 and is what testers were running until now. 10.6.0 (vc1060) was built but never uploaded, so its changes reached testers *inside* 1061 rather than under their own version number. Because 1061 is cumulative, the only thing new here for anyone already on it is the multi-buy fix — the store notes for this release need cover nothing else.

## 10.6.1 (versionCode 1061) — 2026-07-17

A fix (PATCH) release: the guided setup now runs for everyone who creates an account, not only those who typed an email and password.

### Fixed
- **The guided setup after sign-up now runs for Google accounts too** — creating your account with "Continue with Google" skipped the setup questionnaire entirely and dropped you straight on Home. That meant never being asked for your **currency** (so you were left on EUR), never setting a starting income or spending budget, and getting the default Insights layout instead of one shaped around you. Signing up with Google now runs the same setup as signing up with an email; signing *in* to an existing Google account is unaffected.

> **Play status — uploaded 2026-07-17** to both the Internal testing and Closed testing ("Alpha") tracks; it was the first build of the 10.6.x line to reach Play. 10.6.0 (vc1060) was built but never uploaded, so this is the build that actually delivered 10.6.0's changes to testers — anyone installing 1061 gets everything listed under 10.6.0 below as well.

## 10.6.0 (versionCode 1060) — 2026-07-17

A feature (MINOR) release: six new languages, a Europe-focused currency list, and an Account screen and paywall that now tell you the truth about what you get.

### Added
- **Six more languages** — Swedish, Norwegian, Danish, Finnish, Czech and Romanian, each fully translated, bringing Budgetty to 16 languages.

### Changed
- **A Europe-focused language and currency list** — the pickers now match the markets Budgetty is released in: nine currencies (EUR, GBP, CHF, SEK, NOK, DKK, PLN, CZK, RON) in place of the previous global set, and eleven languages outside those markets are no longer offered. If you had one of the removed options selected, the app falls back to your system language and EUR — your receipts, budgets and history are untouched.
- **The paywall now lists everything Premium unlocks** — all five, each with the free-tier limit spelled out underneath. Two real unlocks, unlimited custom categories and unlimited recurring bills, weren't mentioned at all before. Cloud backup & sync is now marked **Coming soon** instead of being listed as though it ships today, because it doesn't — exporting and importing your data is local, and it's free.
- **A shorter Account screen** — Push notifications, Biometric authentication and Analytics are gone. None of them did anything: the switches wrote a setting nothing read. That empties Privacy & Security, so the section goes with them. **Currency** moves to Preferences, beside the other format and locale choices. **Contact support** becomes **Contact us**, and now says outright that problems, feature ideas and plain feedback are all welcome.
- **Clearer onboarding copy** — the setup screens describe what Budgetty does for you rather than the technology behind it.

### Fixed
- **The sign-in button stays on screen on short landscape windows** — on a phone held sideways, the sign-in form ran past the bottom of the pane and left the button roughly 60dp below the fold.
- **Paywall benefits are visible on arrival in landscape** — a phone on its side showed only one of the five before you scrolled.

> The six new locales ship fully translated (all 562 strings + 7 plurals each), but the translations are machine-generated and have **not had a native review** — worth a pass before a wide release. Under the hood this release also adds Firebase Test Lab Robo coverage across a device matrix (`scripts/TESTLAB.md`) and turns on Room schema export with an instrumented migration suite, so a bad database migration now fails a test instead of a user's phone.

## 10.5.0 (versionCode 1050) — 2026-07-15

A feature (MINOR) release: a quick guided setup after sign-up tailors the app to you, and each account now keeps its data to itself.

### Added
- **Guided setup after you create an account** — new accounts get a short, skippable questionnaire that shapes the app around how you'll use it. It asks about your main goal, whether you track income, recurring bills, and a spending budget, and how much detail you like — then tailors your Insights screen to match (promoting the sections you care about, tucking away the ones you don't). Along the way you pick your **currency**, and can optionally set a starting **monthly income** and **spending budget** right in the flow, which pre-fill your Budget tab. Everything it sets is reversible afterwards — from Insights → ⋮ → Customize sections, the Budget tab, and Account.

### Changed
- **The Budget screen now opens on your spending budget** — the budget (period toggle and amount) leads the screen, with income, recurring bills and the income-vs-bills breakdown below it, so the figure you came to set is right there without scrolling
- **Your date format now applies to short dates too** — History day headers, the Insights trend labels, and the add-receipt and recurring-bill rows now follow the date format you chose in Settings, instead of a fixed "Wed, 25 Jun" style

### Fixed
- **Each account's data stays its own** — every signed-in account now keeps its receipts, budgets and history in a separate store on the device. Previously, signing into a second account on the same device could surface the first account's data; now you only ever see your own.
- **Same-day receipts on Home list newest first** — when several receipts share the same date, the most recently added one now sorts to the top of Recent receipts instead of an arbitrary order

> The setup-quiz text ships fully translated in every supported language. This release also localizes the "Delivery & fees" and "Tip" line-item labels and the newest category names (Video Games, Investments, Tips, Delivery), which were English-only in 10.4.0.

## 10.4.0 (versionCode 1040) — 2026-07-14

A feature (MINOR) release: the Home total now reflects your recurring bills, scanned delivery fees and tips become visible line items, and the free plan gets twice the scans.

### Added
- **Recurring bills on the Home summary** — the "Total spent" card now pairs your receipt-backed spending with your planned recurring bills: a slim spent-vs-planned strip, a "Spent" and a "Bills · planned" line, and a combined "With bills" total. The headline no longer looks low by leaving out rent, subscriptions, and other fixed costs. Bills are clearly marked as planned (not yet spent) and shown for the current month only; the card collapses to the plain total when you have no recurring bills. Large amounts scroll instead of being cut off.
- **Delivery, fees and tips as line items** — when a scanned receipt includes delivery/service/bag fees or a tip, they now appear as their own line items — a combined "Delivery & fees" item and a separate "Tip" item, filed under the new Delivery and Tips categories — instead of silently disappearing into the gap between your items and the printed total. They come from amounts the extractor reads explicitly, so they hold up even when the printed grand total is misread; the behind-the-scenes extra-charges adjustment now carries only the leftover, so nothing is counted twice

### Changed
- **10 free scans, only counted when saved** — the free plan's scan allowance is raised from 5 to 10, and a scan is now consumed only when you save the finished receipt: failed reads and abandoned reviews no longer use one up
- **Unlimited custom categories for Premium** — Premium no longer caps custom categories at ten; create as many as you like (the free plan keeps three)

### Fixed
- Review-screen prices keep their trailing zeros (3.20, no longer shown as 3.2)
- Sheets that can outgrow the screen (Customize sections, add income or recurring bill, add receipt, price range, custom date range) now scroll instead of clipping
- Text-field placeholders are muted so empty inputs no longer look already filled

> The recurring-bills strings ship translated in every supported language; the two new line-item labels ("Delivery & fees", "Tip") are English-only in this build — translations to follow.

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
