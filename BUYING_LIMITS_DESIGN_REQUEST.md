# Claude Design request — Buying limits (keyword purchase limits · Account → new screen · Android phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups in that project; once they're there,
> Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add **Buying limits** to Budgetty — a way for the user to cap how
many of something they buy in a week or month ("no more than 1 Coke a week"). It's
a **habit nudge**, a sibling to **Category rules**: the user sets a limit from a new
screen in **Account**, Budgetty counts matching items off their scanned receipts,
and warns them **in-app** when they go over. Please mock this up as new `*.dc.html`
files, matching the existing Material 3 design system exactly. **iOS gets the
identical feature** — once these phone layouts are approved I'll spin a matching iOS
(Liquid Glass) request; behaviour must be the same on both platforms.

## What the feature does (context)
- A limit = **one or more keywords** + a **timeframe (Weekly / Monthly)** + a
  **count** (e.g. 1, 2, 3). Example: keywords *coke, cola* · Monthly · limit **3**.
- Budgetty matches those keywords against the **names of items on saved receipts**
  (case-insensitive, Cyrillic-safe, **substring** — "coke" catches "Coca-Cola 500ml"
  and "Coke Zero") and **sums the quantities** bought in the current window. Multiple
  keywords **OR** into one counter, because the same product prints under many names.
- The window resets on the user's real cycle: **Monthly follows the pay-cycle month**
  (`monthStartDay`); **Weekly** on the first day of the week.
- When a **just-saved receipt** brings a limit **to or over** its count, the app shows
  a **gentle in-app nudge** — **no push notifications**. Otherwise the user checks
  progress on the Buying-limits screen.
- Tone: a **nudge, not a scold** — same wellbeing-coach register.

## Where it lives
- A new **"Buying limits"** row in the **Account** grouped list, right beside
  **Category rules** and **Manage categories** (same `SettingRow` treatment) → opens a
  **dedicated full-screen "Buying limits" destination** (top app bar + back arrow,
  **bottom nav hidden** — same chrome as Category rules / Budget).
- The screen is a **list of limit cards** + an **"Add limit"** CTA; tapping a card
  edits it. Add/Edit is a **bottom sheet**.

## Gating (please show the locked state)
**Capped-free: 1 limit free, unlimited with Premium** — the same freemium pattern as
savings goals (1), widgets (2), custom categories (3). A free user who already has one
limit and taps **"Add limit"** hits a **paywall nudge**. Please design the **locked
"Add limit"** state (a Premium lock affordance + a short "Unlock unlimited buying
limits" prompt).

## The data (drives what the cards show)
Each limit card shows: an optional **emoji** (reuse the category emoji chip; default a
generic tag icon) + a **title** (the user's label, or the first keyword) + the
**keyword chips** + the **timeframe** + **progress** — *count bought* vs *limit* with a
**traffic-light** state and a **reset date**:
- **On track** (under) — green. e.g. *"Energy drinks · red bull, monster · Weekly ·
  Bought 1 of 2 · resets Mon"*.
- **At limit** — amber. e.g. *"Takeaway coffee · coffee · Weekly · Bought 5 of 5 ·
  resets Mon"*.
- **Over** — red. e.g. *"Fizzy drinks · coke, cola, fanta · Monthly · Bought 4, limit 3
  · resets Aug 1"*.

Reuse the Budgets card's **green / amber / red** status language. ("Bought 4, limit 3"
reads better than "4 of 3" when over — pick the clearest phrasing.)

## Match these existing components for style
- **Category rules screen** (the Account rules list) — same screen chrome, section
  rhythm and row style; Buying limits is its sibling.
- **`SegmentedToggle`** for the **Weekly | Monthly** switch in the editor (**not** M3
  SegmentedButton).
- **Category emoji chip** for the optional per-limit emoji; **fully-rounded pill**
  buttons; action buttons 56dp.
- **The paywall / premium lock** (`AccountPaywallScreen.dc.html` / the savings-goal cap
  lock) — reuse for the 1-limit cap.
- **Chips / tag input** for the keywords (removable chips), matching the app's chip style.
- Phone preview **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Screens / states to draw

### `BuyingLimitsScreen.dc.html` — the management screen
1. **Empty** — no limits yet: an inviting *"Set a buying limit"* empty card + a one-line
   explainer (*"Cap how many of something you buy — we'll count it off your receipts."*)
   + a primary **"Add limit"** CTA.
2. **With limits** — 3 limit cards stacked showing the three states above (one on-track
   green, one at-limit amber, one over red), each with a small progress bar and a reset
   date.
3. **Locked "Add limit" (free user at cap)** — the add affordance in its Premium-locked
   state + an "Unlock unlimited buying limits" nudge.

### `BuyingLimitEditor.dc.html` — add/edit + match preview + the nudge
4. **Add / edit sheet** — a bottom sheet with: an optional **emoji** chip + a **label**
   field; a **keyword input that adds removable chips** (draw *coke*, *cola*, *кока-кола*
   to show multiple + Cyrillic); a **Weekly | Monthly** `SegmentedToggle`; a **count
   stepper** (− 3 +); and **Save / Cancel** pills. Follow the bottom-sheet scroll
   convention (list scrolls with `weight(1f, fill=false)`, not a capped height).
5. **Live match preview** (inside the sheet) — as keywords are added, a small block:
   *"Currently matches: Coca-Cola 500ml, Coke Zero, Fanta — **4 bought this month**"*
   so the user sees exactly what the limit will catch. (This is how we keep substring
   matching honest — e.g. it warns that "ice" would also catch "juice".) Draw a
   **no-match** variant too (*"No matching items yet"*).
6. **The in-app nudge** — a **non-blocking bottom sheet / card shown right after a
   receipt is saved** that pushes a limit to/over: *"Heads up — that's your **2nd Coke
   this week** (limit 1)."* with **"View limits"** / **"Got it"**. Draw it over the Home
   / just-saved context. It must read as a friendly nudge, **not** an error — and the
   save still succeeds.

## Design tokens (CSS vars already defined in the project)
- Surfaces `--bg` / `--sc` (surfaceContainer) / `--sch` (surfaceContainerHigh); text
  `--on` / `--onv` (muted labels); accent `--primary`; **status greens / ambers / reds
  exactly as the Budgets card uses them**; dividers `--outv`; selected/emphasis
  `--secc` / `--onsecc`.
- Token-driven only (no hard-coded greys) so every frame themes in **dark + light**.

## Copy (tweak if a shorter phrasing reads better)
- Screen title: **Buying limits**
- Empty: **Set a buying limit** / *Cap how many of something you buy — we'll count it
  off your receipts.*
- Card progress (under): **Bought {n} of {limit}** · **resets {date}**
- Card progress (over): **Bought {n}, limit {limit}**
- Nudge: **Heads up — that's your {ordinal} {keyword} this {week/month} (limit {limit}).**
- Paywall: **Unlock unlimited buying limits**

Note: localized into all our supported languages (German and Bulgarian run long; keywords
may be long or Cyrillic) — please let labels, keyword chips and the nudge wrap gracefully
rather than truncate.

## Please produce
- **`BuyingLimitsScreen.dc.html`** — states 1–3 (empty / with-limits / locked).
- **`BuyingLimitEditor.dc.html`** — states 4–6 (add-edit sheet + live match preview +
  the save-time nudge).
- Token-driven, dark + light. Phone first; tablet to follow.

## Implementation notes (for after the mockup)
- **New data model:** a `BuyingLimit` entity (id, optional emoji + label, timeframe enum
  Weekly/Monthly, count) + its keywords (a joined keyword table or a normalized list), a
  repository + `BuyingLimitsViewModel`, Room migration.
- **Counting:** Σ `quantity` of `TransactionEntity` whose **normalized name**
  (`trim().lowercase()`, Unicode/Cyrillic-safe) **contains any** of the limit's keywords,
  within the current window — **Monthly** via `PayCycle.month(monthStartDay)`, **Weekly**
  via the locale first-day-of-week. This is a **new substring matcher** — Category rules
  match the whole name exactly, so nothing existing is reused for the match itself, and
  `productStats` counts rows (ignores quantity) so it isn't reused for the count.
- **The nudge is in-app only** (no notification/WorkManager infra exists, and we're not
  adding it): computed at receipt-save in the upload/save flow; fires when a saved receipt
  brings a keyword to/over its limit.
- **Gating:** free = 1 limit; the 2nd **Add limit** routes to the paywall
  (`billingManager.isPremium`), and adds a new **`premiumBenefits()`** entry.
- No push, no background work — everything on-device.

Thanks!
