# iOS Design Fixes — parity with the Android design

Comparison of the 24 `iOS *.dc.html` mockups against their Android counterparts (`*Screen.dc.html` +
`handoff/SCREENS.md`) in the Budgetty design project (`claude.ai/design`, project
`5b8c8470-38ec-49d0-b332-b27a9000b4b0`).

**Scope note for Claude Design:** the iOS mockups are an intentional iOS-native re-imagining (bottom tab bar,
SF Symbols, native sheets, large titles, iOS system good/warn/bad colors) — *do not* undo those. Every item
below is a **feature / content / data** gap where iOS is missing something the Android design has, or shows
content that contradicts a current product decision. Keep the existing iOS token block and layout language;
just add/adjust the pieces called out.

---

## Fix 1 — `iOS Insights`: add a spending-over-time Trend card + MoM comparison
**Gap:** Neither `iOS Insights` nor `iOS Insights Extra Cards` has any trend / spending-over-time chart. The
Android design has a dedicated Trends view (`TrendsScreen`) with a monthly bar trend (padded to 7 bars) and a
month-over-month callout ("12% less than last month").
**Do:** Add a **Trend** card to the Insights scroll — a compact bar (or line) chart of spend per period,
padded to 7 bars, in the tint color, with a "▲/▼ X% vs last month" delta line above or below it. Match the
existing Insights card styling (rounded `--bg2` card, 17px semibold title "Trend" / "Spending over time").
Include it in the iPhone scroll and the iPad grid, and add it to the "Customize sections" sheet list.

## Fix 2 — `iOS Budget`: add sub-category budgets
**Gap:** Android `BudgetScreen` supports **per-subcategory budgets**: the "Categories" grid says *"Tap to set
sub-budgets"*, tapping a category opens a sheet with a subcategory budget-input list (e.g. Groceries →
Supermarket / Organic / Bakery), and there's an **"Active sub-budgets [N]"** summary block above the grid with
inline sub-budget editing and traffic-light progress bars. `iOS Budget` only has flat category-level budgets
("Set budget"), with no sub-budget sheet and no active-sub-budgets summary.
**Do:** (a) Make each Category Budget card tappable → present a native sheet titled with the category
(emoji + name) containing a "Category budget" input and a "Subcategories" list, each row = sub name +
"€X spent" + an inline budget input + a thin traffic-light bar; Done button. (b) Add an **"Active sub-budgets"**
section (with a count badge) above the category grid listing every sub that has a budget, grouped by parent,
with inline editing. Add the "Tap to set sub-budgets" helper under the "Category Budgets" header.

## Fix 3 — `iOS Login`: remove "Continue as Guest"
**Gap:** `iOS Login` shows a "Continue as Guest" CTA (phone + iPad). Anonymous auth was **removed for Android
parity** — the Android login design has no guest path.
**Do:** Delete the "or" divider + "Continue as Guest" row on both iPhone and iPad. Keep email/password, the
Sign In/Sign Up segmented control, Forgot Password, "Sign in with Apple" (HIG-correct, keep it), and the
sign-in/up toggle.

## Fix 4 — `iOS Account`: add Language selector (and Import)
**Gap:** Android Account → Preferences has **Theme / Accent / Date / Language** dialogs; the app ships 21
languages. `iOS Account` Preferences has Appearance / Accent color / Date format but **no Language row**
(confirmed also absent from `iOS Support & About`). Also, the Account card shows only **"Export data"** — the
Android design has Export **and Import** (ImportBackupDialog).
**Do:** (a) Add a **Language** row to the Preferences group (globe icon, value = current language, chevron →
selection list). (b) Add an **Import data** row next to Export in the Account group.
**Minor (same screen):** for free users the **Accent color** row should indicate it's Premium-gated (Android
shows "Premium" → Paywall); add a small "Premium" badge/lock when tier = free.

## Fix 5 — `iOS Paywall`: remove the "free trial" language + add the missing states
**Gap:** `iOS Paywall` advertises **"Subscribe — Start Free Trial"** and **"3-day free trial · Cancel anytime
in App Store."** The Android/product paywall explicitly says **"no free trial · cancel anytime."** This is a
direct contradiction (and risks an App Store review issue if no trial is actually offered).
**Do:** Remove all free-trial wording. CTA → **"Subscribe"** (or "Go Premium"); footer subtitle →
**"No free trial · cancel anytime."** Also add the two missing paywall states the Android design has:
**Already-Premium** ("You're Premium ✓") and **Unavailable** (billing unavailable) — the iPhone mockup only
shows the Plans state.
**Minor:** the iPhone feature list omits **"10 custom categories"** that the iPad version lists — make the
feature set consistent across devices.

## Fix 6 — `iOS Scan`: add the processing + read-error states
**Gap:** `iOS Scan` covers only the camera viewfinder and the Review sheet. The Android design
(`UploadReadingScreen`) also has a **processing** state (scan-line animation over a receipt with
"Preparing… / Reading receipt… / Saving…") and an **error** state ("Couldn't read that receipt" +
"Try again" / "Go back"). Scan failures are common, so the error state matters.
**Do:** Add a `step` value between "scan" and "review": a **Reading** state (centered receipt illustration +
animated scan line + status label cycling Preparing/Reading/Saving) and an **Error** state (error glyph +
generic "Couldn't read that receipt" message + "Try again" primary and "Go back" secondary).

---

## Minor / polish (nice-to-have, not blocking)
- **`iOS History`:** the header filter chip labeled **"Date"** opens a **"Sort By"** sheet (Newest/Oldest/
  Highest/Lowest/Name) — relabel the chip **"Sort"** to match what it opens.
- **`iOS Category Transactions`:** the item list looks chronological; the Android `CategoryTxnSheet` sorts
  **largest-first** and **merges** duplicate rows (same product + store + unit price). Match that ordering.
- **Sample-data consistency across mockups:** income shows **€3,500** on `iOS Budget` but **€2,400** on
  `iOS Home` / `iOS Insights Extra Cards`; the version string is **"1.4.2 (42)"** on Support & About but
  **"Budgetty 1.0"** on Account. Unify the sample values so the set reads as one coherent app.

## Screens verified at parity (no change needed)
Home, History, Insights (base cards), Insights Extra Cards, Receipt Detail (in fact richer than the Android
mockup — has the incl-VAT totals block), Category Picker, Custom Category, Manual Entry, History Filters,
Date Range, Notifications, Onboarding, Category Memory, Empty States, Widgets, Biometric Lock, Support & About.
