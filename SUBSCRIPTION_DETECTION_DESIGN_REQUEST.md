# Claude Design request — Android Subscription detection (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups; Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add **Subscription detection** to Budgetty (Android phone). The app
mines the user's own spending history to **auto-surface recurring merchants**
(subscriptions & regular card bills) and flags likely **price hikes** — Rocket
Money's signature hook, done **on-device** (fits our no-backend, local-data ethos).
Please mock this up as new `*.dc.html` files in the Material 3 system. **iOS gets
the identical feature** (`IOS_DESIGN_REQUEST_SUBSCRIPTION_DETECTION.md`).

## Why / the goal
- Plays to Budgetty's receipt/transaction strength: we already have dated,
  merchant-tagged spending. Grouping it reveals *"you pay Netflix €13.99 every
  month, and it went up €2 in March"* — high-value, zero extra input.
- **Premium feature.** Free users see a **teaser** (how many we found + a
  locked list) → paywall. Premium unlocks the full list + price-hike flags +
  the "track as bill" action.

## What detection produces (drives the UI)
An on-device heuristic groups transactions by **normalized merchant name** and
finds **repeating charges at a regular cadence** (monthly ±a few days, or yearly)
with similar amounts, ≥3 occurrences. Each detected subscription has: **merchant
name + emoji/tile**, **typical amount**, **cadence** (monthly / yearly), **last
charge date**, **next expected date**, and a **price-change flag** when the latest
charge differs from the prior ones.

## Match these existing components for style
- **`InsightsScreen.dc.html`** — the entry point is a **"Subscriptions" card** on
  Insights; the full list is its own screen reached from there. Match Insights card
  headers, list rows and the period/stat styling.
- **Category emoji chips** — merchant tiles reuse the rounded-square emoji chip.
- **The recurring-bill add sheet** (`INCOME_RECURRING` mockups / Budget tab) — the
  **"Track as bill"** action prefills that editor, so show it handing off there.
- **The paywall / premium lock** (`AccountPaywallScreen.dc.html`) — for the free teaser.
- Phone **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Screens / states to draw
1. **Insights entry card — Premium (has data):** a "Subscriptions" summary card —
   *"6 subscriptions · €68/month"* + the top 2–3 merchants preview + a chevron into the list.
2. **Insights entry card — free (teaser/locked):** *"We found 6 recurring charges"*
   with the list **blurred/locked** and an **Unlock** CTA.
3. **Subscriptions list (Premium):** rows of detected subscriptions — merchant tile,
   name, **€amount**, cadence, **next ~{date}**, and a **price-up badge** where flagged.
   Include a monthly total at the top.
4. **Subscription detail:** the merchant, its **charge history** (dated rows and/or a
   small sparkline of amounts over time), a **price-hike callout**
   (*"Up €2.00 since March"*), and actions **Track as bill** / **Ignore**.
5. **Empty / not-enough-history:** *"We'll spot recurring charges as you add more
   receipts"* — an intentional empty state, not an error.
6. **Ignored:** show how a dismissed subscription reads (a quiet "Ignored" affordance / a way to restore).

## Design tokens (CSS vars already defined)
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; accent `--primary`;
  a **warning amber/red** for the price-up badge; dividers `--outv`.

## Please produce
- **`SubscriptionsScreen.dc.html`** — the Insights entry card (states 1 & 2), the
  full list (3), detail (4), empty (5), ignored (6).
- Token-driven only so it themes in **dark + light**. Phone first; tablet to follow.

## Implementation notes (for after the mockup)
- **On-device detection** over the existing transactions table (normalize store
  name → cluster by cadence + amount). No backend, no new network.
- **"Track as bill"** creates a recurring bill (`isIncome = false`) prefilled with
  the merchant + amount + monthly cadence — which then feeds **Safe to spend** and
  the upcoming-bills list. Nice loop with the recurring tools we already have.
- Likely a light persistence for **ignored** merchants + user price overrides.
- **Gating:** the list + flags gate on `billingManager.isPremium`; free sees the teaser.

Thanks!
