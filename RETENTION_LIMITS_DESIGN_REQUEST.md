# Claude Design request — Retention #3: Buying limits as opt-in challenges (Android, iOS, tablet)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups; once they're there, Claude Code
> reads them back via DesignSync and implements what I approve. Please
> **don't overwrite** the shipped `BuyingLimitsScreen.dc.html` / `BuyingLimitEditor.dc.html` —
> create **new** comparison files.

---

# Retention #3 — Buying limits as challenges (2026-08-25)

Hi! Last of three retention requests (same rules: real outcomes, **no loss framing**, and the one paywall touch here **loosens** a gate). Buying limits are already the healthiest gamification in the app: **self-set, self-scored, no coercion**. What's missing is **progress over time** and a way to **think of a limit at all**.

**Today** the Buying limits screen (shipped `BuyingLimitsScreen.dc.html`) shows a list of cards — each with an **emoji chip + title + a traffic-light status chip**, a **keyword chips row**, a **pip row** (one pip per unit of the cap, filled = bought; overflow pips set apart when over), and a **meta line** ("Weekly · 2 of 4 bought · resets Mon") — plus an **empty state** (explainer) and a **free-cap locked state** (`FREE_LIMIT = 1` today). The editor already has a live "CURRENTLY MATCHES" preview.

> **Note vs the pip row:** the pip row from an earlier spec draft is **already shipped** — so this request is **not** about adding pips. It's about what happens **before the breach** (streak), **over time** (history), and **discovery** (suggestions), plus one colour tweak and the free-tier bump.

**What I'm asking for:** a **side-by-side comparison mockup** per platform — **left = shipped**, **right = proposed** with the changes pinned. **Numbered pins + a legend**. **Three surfaces, one file each:** Android phone (Material 3), iOS phone (Liquid Glass), Android tablet (adaptive — nav rail + the list in a comfortable centred/multi-column layout).

## State to draw (right column)
- **Limits list** with a mix: one **on-track** limit (with a **2-week streak** + a **history strip** showing 6-of-8 met), one **at cap** (warm, not red), one **over**.
- **Empty state** with **suggestions** (the discovery case).
- **Locked state** at the **new free cap of 3** (3 of 3 used → Add locks).

## Changes to show on the "proposed" (right) side
1. **"At cap" reads as reached, not failed.** Today an over-cap pip row / status goes **red**. Reaching a cap you set yourself is not a failure — the app doesn't get to be disappointed in you. Show the **at-cap** state fully-filled in a **warm tone (amber-family, not red)** with an "at limit" status that reads calm. (Keep a distinct **over** treatment if you think it's needed, but even that should lean warm, not alarm — show your recommendation.)
2. **Per-limit streak caption (shared streak language).** A quiet caption on the card: **"· 3 weeks under"** — consecutive closed windows the count stayed within cap. Same streak motif as the Recap/Wellbeing requests: **no flames**, `current ≥ 2` only, Best-run fallback if the current run is 0. It should sit near the meta line, not become a second hero.
3. **History strip — the single best addition.** Under each card, the **last 8 closed windows** as small squares: **met / not met / no data**. This turns a limit from binary pass/fail into a visible trend — a 6-of-8 user sees real progress where a streak counter would show a demoralising 0. Draw the three square states clearly (met = good tone, not-met = warm/muted, no-data = outline/empty), with a tiny caption if it helps ("last 8 weeks").
4. **Suggested limits (opt-in discovery).** Most people never *think* to set a limit, so offer up to **3** suggestions from their most-frequently-bought items (last 60 days, quantity ≥ 6):
   > You bought **Coke** 14× last month — cap it?
   Show them **on the empty state** (as the primary path in) **and** as a **dismissible row above the list** when they already have limits. One tap opens the **editor pre-filled** (keyword + a suggested cap = current rate rounded down), where the existing "CURRENTLY MATCHES" preview keeps it honest. A dismissed suggestion never returns — draw the small **✕/dismiss** affordance.
5. **Free tier 1 → 3.** We're raising the free cap from **1** to **3** limits (unlimited stays premium). Update every place that quotes the number: the **count pill** ("2/3"), the **free hint** ("**3 limits free** · unlimited with Premium"), the **used footnote** ("3 of 3 free limits used"), and the **locked-state upsell** copy. Draw the locked state at **3 used**.

## Also (tiny) — the Budget-screen streak caption
Streaks surface in a **fourth** place: a **one-line caption on a Budget-screen category row** — e.g. **"· 3 months under"** — no new card, no new section, costs nothing when there's no streak. It's small enough that a **mini before/after of a single category row** (current row vs row-with-caption) is all I need — you can add it as a small inset on the phone file rather than a full screen.

## Platforms & components to match
- **Android** — Material 3, Roboto, phone **300×620**. Build "current" from `BuyingLimitsScreen.dc.html` (the card with emoji chip / status chip / keyword chips / **existing pip row** / meta line, the empty state, the locked state). **Not** Liquid Glass.
- **iOS** — iOS 26 **Liquid Glass**, SF Pro. Build "current" from the iOS Buying-limits mockup; native Liquid-Glass cards, not a Material port. Streak caption + history strip + suggestion row in the iOS idiom.
- **Android tablet** — Material 3 **adaptive**: left **nav rail**, the list in a comfortable centred column (the screen already caps content ~520dp) or two columns if it reads better at ≥600dp. The card internals (pips, streak, history strip, suggestions) are identical to phone — only the surrounding layout adapts.
- Keep all three in **parity**.

## Design tokens (CSS vars already in the project)
- Card = `--surfaceContainer`, status/pip **bands**: on-track = `budgetGoodColor` (green), **at-limit = `budgetWarnColor` (warm amber)**, over = lean warm (see #1) — reserve red only if you decide over truly needs it.
- History strip squares: met = good tone, not-met = warn/muted, no-data = `--outlineVariant` outline only.
- Streak motif: shared with the Recap request (good tone + dotted ghost; no flames).
- Suggestion row: a quiet informational surface (`--surfaceContainerLow`) with the item name emphasised; the dismiss ✕ in `--onSurfaceVariant`.
- Token-driven only; theme **dark + light**.

## Please produce
- **`RetentionLimits.dc.html`** (Android phone), **`iOS Retention Limits.dc.html`** (iOS phone), **`TabletRetentionLimits.dc.html`** (Android tablet) — each a **current vs proposed** comparison covering the **at-cap colour, per-limit streak caption, history strip, suggested limits (empty + dismissible row), the free-tier-3 locked state**, plus the **Budget-row caption inset**, with **numbered pins + a legend**, themed **dark + light**.

## Notes (presentation + copy; almost no data-model work)
- The **history strip and streak are derived** from transactions in each historical window — **no new storage**.
- **Suggestions are frequency-only** — no attempt to guess "staples" (that would misfire across 16 locales). A rejected suggestion is remembered so it never returns (a small `dismissedLimitSuggestions` set in settings — not a schema change).
- **Free-tier bump** is a copy + constant change; existing free users silently gain capacity (a loosened gate needs no migration). Apply on **both platforms**.
- No new database fields for anything in this request.

Thanks!
