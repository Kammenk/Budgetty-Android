# Budgetty — Full App Design Brief

Design every screen, state, dialog, and bottom sheet for **Budgetty**, an Android personal-budgeting app. Phone-first, portrait, Material 3 (Material You). Generate cohesive light **and** dark mode mockups for each screen described below. Treat this as the single source of truth for the product.

---

## 1. What Budgetty is

Budgetty is a **receipt-driven personal budgeting app**. Instead of manually typing every purchase, the user photographs or uploads a receipt (image or PDF); an AI reads it and returns the store, date, total discount, and a list of itemized line items — each already sorted into a spending category. The user reviews/edits the items, then saves. Budgetty then tracks spending across time, categories, and stores, lets the user set budgets, and surfaces insights.

**Core principles to express in the design:**
- **Effortless capture** — the camera/upload-to-saved flow is the hero. The "Add receipt" action is always one tap away.
- **Trust but verify** — AI does the heavy lifting, but the user always reviews line items before they're saved.
- **Local & private** — data lives on the device; the user owns it (manual export/import backup, in-app account deletion).
- **Calm, friendly finance** — approachable, not spreadsheet-intimidating. Rounded cards, soft surfaces, emoji category icons, color-coded budgets.

**Platform & framework:** Android, Jetpack Compose, Material 3. Login is required — there is **no guest mode**. Money is precise (BigDecimal) and shown with a user-chosen currency symbol.

---

## 2. Design system / visual language

Keep every screen consistent with this system:

- **Cards & surfaces:** Rounded cards with a **20dp corner radius** (16dp for list rows), filled with the Material 3 `surfaceContainer` tone, minimal elevation, **20dp internal padding**. Grouped settings use a single rounded card with hairline dividers inset under the row text.
- **Color / accent:** Default accent is **violet/purple** (light `#6650A4`, dark `#D0BCFF`). Premium unlocks 3 alternate accents: **Sage** (muted green `#3E5E41` / `#A8C6AA`), **Ocean** (teal-blue `#1C5C6E` / `#8FC8D8`), **Plum** (`#6A2E78` / `#CFA6D6`). All accents must work in both light and dark schemes.
- **Budget status colors (traffic-light):** **green** `#2E7D32` when under 50% of budget, **amber** `#F9A825` at 50–74%, **red** `#D32F2F` at 75%+. Used for budget figures and progress bars.
- **Typography:** Material 3 type scale. Big bold `displaySmall` for headline money totals; `titleMedium`/`titleLarge` semibold for section and card headers; muted `onSurfaceVariant` for labels and secondary text.
- **Categories:** A two-level taxonomy of **46 categories** — 7 selectable groups + 38 sub-categories + "Other" — each with an **emoji icon** and a distinct color (hues spread by the golden angle so adjacent ones stay distinguishable). See the full list in §13.
- **Store "logos":** A rounded-square tile (12dp radius) showing the store's **first letter** in white, on a color derived from the store name. When a receipt has no store, show a neutral tile with a **🧾 receipt glyph**.
- **Charts:** A **donut/ring chart** for category breakdown (thin ring, rounded segment caps, small gaps between tappable arcs, total shown in the hollow center). Linear progress bars for budgets and per-category shares.
- **Money formatting:** Currency symbol per the user's choice — default **BGN (лв)**, also **EUR (€)**, **USD ($)**, **GBP (£)**.
- **Bottom navigation:** 3 tabs — **Home** (house icon), **Insights** (pie-chart icon), **Account** (account-circle icon). Hidden on full-screen flows (Upload, Budget, Paywall).
- **Bottom sheets** for contextual actions (add receipt, receipt detail, category picker, date range, category transactions). **Dialogs** for confirmations and single-choice settings.
- **Floating action:** An **Extended FAB** labeled "Add receipt" with a camera+ icon, bottom-right, on the Home screen.

---

## 3. Screen — Login / Sign up

The first screen for any unauthenticated user (login-gated). Centered, single-column, scrollable.

- **Brand:** "Budgetty" wordmark (headline, bold) with a subtitle that switches between **"Welcome back"** (sign-in mode) and **"Create your account"** (sign-up mode).
- **Fields:** Email (email keyboard) and Password (masked).
- **Inline error text** in the error color appears under the fields when auth fails.
- **Primary button:** full-width — "Sign in" or "Create account". Shows a small inline spinner while loading; disabled until both fields are non-empty.
- **Google sign-in:** full-width outlined button "Continue with Google" (Google one-tap / Credential Manager).
- **Mode toggle:** text button — "New here? Create an account" ⇄ "Have an account? Sign in".
- **States to design:** empty, filled, loading (spinner in button), error (message shown), sign-up vs sign-in variants.

> Design enhancement: add a subtle brand illustration or logo mark above the wordmark, and a "Forgot password?" text link.

---

## 4. Screen — Home (primary tab)

A vertically scrolling feed. Contents top to bottom:

### 4a. Summary card (spend total)
- Card showing a small muted label **"Total spent this month"** and a large bold money figure (the period total).
- A **period filter** control (filter icon) opens a dropdown menu to switch the window: **This month, Last month, Last 3 months, Last 6 months**. The label updates to match (e.g. "Total spent last 3 months").

### 4b. Budgets progress card (tappable → Budget screen)
- Small "Budgets" label.
- **"Monthly budget progress"** row: title on the left; on the right either "**{spent} / {budget}**" (colored green/amber/red by usage) or "**Tap to set a budget**" when none exists. Below it a rounded **linear progress bar** in the same status color.
- **"Weekly budget progress"** row with its own progress bar.
- If the user has any **per-category budgets**, show a "**View other budgets**" link (accent color) at the bottom.
- The whole card is tappable and navigates to the Budget screen.

### 4c. Receipts list
- Section header "**Receipts**".
- **Empty state:** a soft card reading "No receipts yet. Tap + to upload a receipt."
- **Populated:** a list of **receipt rows**, each a card with: the store logo tile, the store name (or "Receipt"), a secondary line "**{date} · {N} items**", the receipt total (bold) right-aligned, and a green "**−{discount}**" line beneath the total when there was a discount, plus a chevron. Tapping a row opens the **Receipt detail sheet** (§4e).

### 4d. Add receipt FAB & bottom sheet
- Extended FAB "**Add receipt**" (bottom-right) opens the **Add receipt bottom sheet**:
  - Title "Add receipt" + a contextual subtitle about scans remaining:
    - Premium: "Premium — unlimited scans."
    - Free with scans left: "**{n} of 5** free scans left (photo or file). Manual entry is always free."
    - Free, exhausted: "You've used all 5 free scans. Go Premium to scan more — manual entry is always free."
  - Three large tappable option rows, each with an icon tile, title, and subtitle:
    1. **Take a photo** — "Snap the receipt with your camera" (disabled/greyed when no scans left)
    2. **Upload a file** — "Pick a PDF or image from your device" (disabled when no scans left)
    3. **Add manually** — "Enter the items yourself · always free" (always enabled)
  - When scans are exhausted and the user is not premium, a full-width "**Go Premium**" button appears at the bottom.
  - **States to design:** premium, free-with-scans, free-exhausted.

### 4e. Receipt detail bottom sheet
Opened by tapping a receipt row.
- Header: store logo tile, store name (bold title), "**{date} · {N} items**" subline, and the receipt total right-aligned. If discounted, a green "**Discount −{amount}**" line.
- Divider, then a list of the receipt's **item rows** — each showing the item name, its category (emoji + color dot), quantity × unit price, and line total, with a **trailing delete (trash) icon** in the error color.
  - Deleting an item shows an "**Item deleted**" snackbar with **Undo**.
  - Deleting the **only remaining** item deletes the whole receipt.
- Bottom: full-width outlined "**Delete receipt**" button (error-colored, trash icon). Deleting shows a "**Receipt deleted**" snackbar with **Undo**.

---

## 5. Flow — Add / Upload / Review a receipt (full-screen)

A full-screen flow (no bottom nav) with a top app bar titled "**Upload receipt**" and a back arrow. The entry source is camera, file, or manual.

### 5a. Capture
- **Camera:** launches the system camera to photograph the receipt.
- **File:** opens the system file picker for a PDF or image.
- **Manual:** skips capture and starts with one empty item row.

### 5b. Processing states (centered spinner + label)
- "**Preparing…**" (initial)
- "**Reading receipt…**" (AI extraction in progress — the receipt image is sent to the cloud and returned as structured line items)
- "**Saving…**"
- **Error state:** an error message in the error color plus a "**Go back**" button (e.g. unreadable receipt / network problem).

> Design enhancement: replace the bare spinner with a friendly "scanning receipt" animation (a scan line sweeping a receipt illustration) to make the wait feel intentional.

### 5c. Review & edit list (the core review screen)
Header "**Review & edit before saving**", then a scrolling list of **editable item cards**. Each card has:
- **Product** text field (name).
- A **trash icon** to remove the row.
- **Category field:** a read-only field showing "**{emoji} {category}**" with a dropdown chevron; tapping it opens the **Category picker sheet** (§5d). To its left, a **color swatch dot** — tapping it opens a small popup grid of palette colors (§5e) to recolor that category.
- **Price** field (decimal keyboard) and **Qty** field (number keyboard) side by side.

Below the list: a full-width outlined "**+ Add transaction**" button to insert a blank row. At the bottom, an optional inline error line, and a full-width primary "**Finalize upload**" button that saves the receipt and returns Home.

### 5d. Category picker bottom sheet
- A **search field** ("Search") at the top.
- When the search is empty: a grouped list — each of the 7 **groups** appears as a **bold, selectable** row (emoji + name), with its **sub-categories** indented beneath it. "Other" appears as a group with no children. The currently selected category row is highlighted (secondary container).
- When searching: a flat list of matching categories.
- Tapping any row selects it (adopting its color) and dismisses the sheet.

### 5e. Category color popup
A dropdown anchored to the color swatch. Title "Category color", then a grid (4 per row) of the **12 curated palette colors**. The current color is outlined. Tapping a color applies it and closes the popup.

---

## 6. Screen — Insights (primary tab)

A vertically scrolling set of cards.

### 6a. Breakdown card (donut)
- Header "**Breakdown**" with the active period label beneath it, and a **period filter** control (filter icon) on the right offering the presets (This month / Last month / Last 3 months / Last 6 months) **plus a "Custom range…"** entry that opens the date-range sheet (§6e). The active option is checked.
- A **donut chart**: one ring segment per category (colored by category), thin ring with rounded caps and small gaps. The hollow center shows "**Total**" + the period total. **Tapping a segment** selects it — the center then shows that category's name, its spend, and its **percentage**, while other segments dim.
- Below the donut, a **two-column legend** of the top categories (color dot + name + spend). Tapping a legend row opens that category's transactions sheet (§6f). If there are more than 6 categories, a "**See all categories**" text button opens a full list sheet.
- **Empty state:** "No spending in this period" inside the ring area.

### 6b. Summary card
Header "**Summary**", then a 2×2 grid of stat tiles:
- **Total spent** (period total)
- **Receipts** (count of receipts in period)
- **Avg / receipt** (total ÷ receipts)
- **Saved** (sum of receipt discounts in period)

### 6c. Top categories card
Header "**Top categories**". Up to 5 rows, each: a color dot, category name, spend (right-aligned), and a thin **linear bar** showing that category's share of the total in its color. Tapping a row opens that category's transactions sheet.

### 6d. Top stores card
Header "**Top stores**". Up to 5 rows, each: a small store logo tile (first letter), store name, and total spend at that store (right-aligned). Chains are normalized so two branches of the same store merge into one entry.

### 6e. Custom date range bottom sheet
A near-full-height sheet wrapping a Material 3 **date-range picker** (calendar). Footer with "**Cancel**" and a primary "**Apply**" button (enabled only once both a start and end day are chosen). Confirming sets an inclusive custom range; the period label then reads e.g. "**1 Jun – 25 Jun 2026**".

### 6f. Category transactions bottom sheet
Opened by tapping a category (donut segment, legend row, or top-category row).
- Header: a color dot (matching the category's slice), "**{emoji} {category}**", an item-count subline, and the category's total for the period (bold, right-aligned).
- Divider, then a list of merged line rows — identical product + same store + same unit price are collapsed into one row showing the product name, **"{qty} × {unit price}"**, the store it came from, and the line total. Sorted largest-spend first.
- **Empty state:** "No transactions in this category".

---

## 7. Screen — Budget (full-screen)

Full-screen with a top app bar "**Budgets**" and a back arrow. A scrolling list:

- **Monthly budget** card — label + a single amount field (decimal keyboard) with the **currency symbol** as a suffix, placeholder "0".
- **Weekly budget** card — same pattern, slightly smaller emphasis.
- Section header "**Per-category budgets**".
- A row **per category** (all 46, groups bold): the category **emoji**, its **name**, an amount field (140dp wide, currency suffix) on the right. Once a budget is set for a category, a thin **progress bar** appears under the name showing spent-vs-limit in the green/amber/red status color.

Editing any field saves immediately. **States to design:** all-empty (fresh), some budgets set with progress bars in each status color.

---

## 8. Screen — Account (primary tab)

A vertically scrolling settings screen. Title "**Account**" (large, bold).

### 8a. Profile header card
Avatar circle with the user's **initials** (derived from email) in the accent container color, a **display name** (best-effort from the email's local part, e.g. "alex.rivera" → "Alex Rivera"), and the **signed-in email** beneath.

### 8b. Account card (grouped rows)
1. **Subscription** — star icon, trailing pill **"Free"** or **"Premium"**; opens the Paywall.
2. **Budget** — wallet icon; opens the Budget screen.
3. **Push notifications** — bell icon, trailing **switch**.
4. **Currency** — money icon, trailing value "**BGN (лв)**"; opens the Currency selection dialog.
5. **Export data** — upload icon; opens the system save-file dialog to write a JSON backup ("budgetty-backup.json"). Toast on success/failure.
6. **Import data** — download icon; opens the system file picker, then the **Import backup dialog** (§8f).

### 8c. Preferences card
1. **Theme** — dark-mode icon, value = "System default" / "Light" / "Dark"; opens the Theme dialog.
2. **Accent color** — palette icon, value = the accent name, or "**Premium**" when the user is free (tapping a locked accent opens the Paywall); opens the Accent dialog when premium.
3. **Date format** — calendar icon, value = a sample date (e.g. "5 Jun 2026"); opens the Date format dialog.
4. **Language** — language icon, value = "System default" / "English"; opens the Language dialog.

### 8d. Privacy & Security card
1. **Biometric authentication** — fingerprint icon, trailing **switch**.
2. **Analytics** — analytics icon, trailing **switch**.

### 8e. Footer actions
- A full-width "**Sign out**" card (error-colored label).
- A subtle "**Delete account**" text button (error-colored) → opens the Delete account dialog (§8g).
- A centered version line: "**Budgetty · v1.0**".

### 8f. Import backup dialog
Title "Import backup". Body: "This merges the backup on top of your current data — nothing is deleted. Or choose 'Replace all' to wipe your current data first." Buttons: **Cancel**, **Replace all**, **Merge**. Toast confirms the result.

### 8g. Delete account dialog
Title "Delete account?". Body: "This permanently deletes your Budgetty account and erases all data on this device — receipts, transactions, and budgets. This can't be undone." Buttons: **Cancel** and a red **Delete**. During deletion the buttons disable. A follow-up "**Couldn't delete account**" dialog handles two outcomes: re-auth required ("For your security, please sign out and sign in again, then delete your account.") or a generic error, each with an **OK** button.

### 8h. Single-choice selection dialog (Theme / Accent / Currency / Date format / Language)
A shared dialog: a title and a vertical list of **radio-button rows** (one per option, with a readable label such as "USD ($)" or "06/05/2026"), the current value selected. A "**Done**" button closes it. Selecting a row applies immediately.

> **What the not-yet-wired Account items would do when developed** (design them as fully functional):
> - **Push notifications:** When on, Budgetty sends budget-threshold alerts (e.g. you've hit **50% / 80% / 100%** of a monthly, weekly, or per-category budget), a **weekly spending summary**, and a "**receipt saved**" confirmation after a scan. Design a notification-preferences sub-screen (toggles per alert type, quiet hours) and example notification cards.
> - **Biometric authentication:** When on, Budgetty requires **fingerprint / face unlock** when the app is opened or resumed from background, and before sensitive actions (export, delete account). Design the lock screen (app logo + "Unlock Budgetty" + biometric prompt + a passcode fallback).
> - **Analytics:** An opt-out for **anonymous usage analytics** (which screens are used, feature adoption — never receipt contents). Design an explanatory line and a link to the privacy policy.
> - **Language:** Expands to **full localization** (e.g. English + Bulgarian, given the app handles Bulgarian receipts). Design the picker with more languages and right-aligned native names.

---

## 9. Screen — Paywall (Budgetty Premium, full-screen)

Full-screen with a top app bar "**Budgetty Premium**" and a back arrow. Scrolling column:

- Headline "**Get more from Budgetty**".
- A **benefits list** (check icon + text): "Unlimited receipt scans", "Accent color themes", "Cloud backup & sync".
- **Plan cards** — one per available subscription (monthly and yearly), each an elevated card with the plan name, its **formatted price** from the store, and a "**Subscribe**" button. Design a "best value" badge on the yearly plan.
- **States to design:**
  - Already premium: "You're Premium ✓ — thanks for the support!"
  - Plans not yet available: "Subscriptions aren't available yet — they'll appear here once the store listing is live."
  - Plans loaded (two plan cards).
- A "**Restore purchases**" text button at the bottom.

> Design enhancement: a richer hero (gradient header, premium crown/sparkle motif), a free-vs-premium comparison table, and a small "no free trial · cancel anytime" reassurance line.

---

## 10. Global states to cover for every screen

For each primary screen, produce these variants where relevant:
- **Empty / first-run** (no receipts, no budgets, no insights data).
- **Populated** (realistic sample data — grocery, pharmacy, fuel, dining receipts).
- **Loading** (AI extraction, auth, data fetch).
- **Error** (unreadable receipt, network failure, auth failure).
- **Light and dark mode** for all of the above.
- **Free vs Premium** where the two differ (Add sheet, Accent color, Paywall).

---

## 11. Recommended additions (design these too — they round out the product)

1. **Onboarding carousel** (first launch, before/after login): 3–4 slides — "Snap any receipt", "We read & categorize it", "See where your money goes", "Set budgets that keep you on track" — each an illustration + headline + dot indicators, with "Skip" and "Get started".
2. **Transaction history screen** (already partly built in code, currently not surfaced): a single chronological list of **all** line items grouped by **month** headers, each row showing the item, category, store, date, and amount — reachable from Account or as a 4th tab. Add **search** and category/store/date **filters**.
3. **Full-screen receipt image viewer:** store the captured receipt image and let the user re-open it from the Receipt detail sheet (pinch-to-zoom), so they can cross-check the AI's reading against the original.
4. **Insights — trends over time:** a monthly **spend trend** line/bar chart and a month-over-month comparison ("you spent 12% less than last month"), plus per-category trend.
5. **Budget threshold notification designs** (system notifications + an in-app alerts inbox) tied to the Push notifications toggle.
6. **Empty-state & error illustration set:** a small reusable illustration family (empty receipts, empty insights, no search results, offline, scan failed).
7. **Edit profile:** let the user set a display name and avatar rather than deriving them from the email.
8. **Account "Support & About" group:** Help / FAQ, Contact support, Rate Budgetty, Privacy Policy, Terms — as additional settings rows.
9. **Home quick stats:** an optional compact strip on Home (this week vs last week, top category this month).
10. **Home-screen widget** (Android): "spent this month vs budget" + a quick "Add receipt" shortcut.

---

## 12. Sample data to use in mockups

Make the screens feel real with believable receipts:
- **Kaufland** — 2026-06-24 — 12 items — 47.86 лв (−3.20 лв discount): Bakery bread, Dairy milk & cheese, Fruits & Vegetables, Beverages.
- **Lidl** — 2026-06-22 — 8 items — 31.40 лв: Meat & Poultry, Frozen Foods, Snacks & Sweets.
- **DM / pharmacy** — 2026-06-20 — 4 items — 22.10 лв: Personal Care, Health & Pharmacy.
- **Shell (fuel)** — 2026-06-18 — 1 item — 90.00 лв: Fuel.
- **Restaurant** — 2026-06-17 — 78.50 лв: Restaurant & Dining.

Budgets example: Monthly 1200 лв (spent 712 лв → amber), Weekly 300 лв (spent 168 лв → green), per-category Groceries 400 лв, Dining & Entertainment 150 лв.

---

## 13. Category taxonomy (46 categories, emoji icons)

Each category has an emoji and a distinct color. Both groups (bold) and sub-categories are selectable.

- **🛒 Groceries** — 🥖 Bakery · 🧀 Dairy · 🍗 Meat & Poultry · 🐟 Fish & Seafood · 🥬 Fruits & Vegetables · 🍫 Snacks & Sweets · 🧊 Frozen Foods · 🥜 Nuts & Snacks · 🥫 Canned & Preserved · 🍝 Grains & Pasta · 🧂 Condiments & Sauces · 🥤 Beverages
- **🏠 Household & Personal** — 🧼 Household Cleaning · 🧴 Personal Care · 💇 Beauty · 🍼 Baby Products · 🐾 Pet Supplies · 📄 Paper Products · 🍽️ Kitchen Supplies
- **❤️ Health & Wellness** — 💊 Health & Pharmacy · 🏥 Medical · 🏋️ Sports & Fitness
- **🍽️ Dining & Entertainment** — 🍴 Restaurant & Dining · 🎟️ Entertainment
- **🛍️ Shopping & Lifestyle** — 👗 Clothing & Accessories · 🔌 Electronics · 🌱 Garden & Plants · 🛠️ Home Improvement · 🍷 Tobacco & Alcohol
- **🚗 Transportation** — ⛽ Fuel · 🔧 Car Maintenance
- **📋 Services & Subscriptions** — 🔔 Subscriptions & Services · 📚 Education · ✈️ Travel & Accommodation · ⚡ Insurance & Utilities · 🏘️ Rent · 📎 Office & Work Supplies · 🎁 Gifts & Charitable Donations
- **📦 Other**

---

## 14. Screen checklist (generate all of these)

1. Login / Sign up (sign-in, sign-up, loading, error)
2. Home — empty, populated, with/without budgets
3. Add receipt bottom sheet (premium / free-with-scans / free-exhausted)
4. Receipt detail bottom sheet
5. Upload flow — capturing, "Reading receipt…", error
6. Review & edit list (with editable item cards)
7. Category picker bottom sheet (grouped + search)
8. Category color popup
9. Insights — donut (default + segment selected), summary, top categories, top stores, empty
10. Period filter dropdown
11. Custom date range bottom sheet
12. Category transactions bottom sheet
13. Budget screen (empty + with budgets/progress)
14. Account screen (free + premium)
15. Selection dialogs (Theme / Accent / Currency / Date format / Language)
16. Import backup dialog
17. Delete account dialog + "couldn't delete" dialog
18. Paywall (plans loaded / already premium / unavailable)
19. *(Additions)* Onboarding, Transaction history, Receipt image viewer, Insights trends, Biometric lock, Notification preferences & alerts, Empty/error illustrations

Generate every screen in both **light and dark** mode, using the design system in §2.
