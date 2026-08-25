# Retention spec — subtle gamification for Budgetty

**Status:** approved, not started — parked for a future session · **Date:** 2026-08-24
**Applies to:** Android + iOS (full parity)

> **Picking this up later?** Both open product calls are now decided (§4.5 free-limit 1→3, and
> Firebase Analytics goes in first). Build order is at the bottom of this section. Nothing has been
> implemented — no code, no branch, no schema change.

## Principle

Budgetty is a money app, not a language app. The desired behaviour is *spending better*, not
*opening Budgetty* — so every mechanic here rewards a real financial outcome, never app usage.
Three rules follow, and they override any individual item below:

1. **No proxy rewards.** Nothing rewards opening the app, logging a receipt, or maintaining a
   session. Those are farmable and they train hollow habits.
2. **No loss framing.** No "streak broken", no flames, no countdowns, no "don't lose your
   progress". Loss framing on a money app compounds financial anxiety, which is the exact thing
   the Wellbeing Coach exists to reduce.
3. **Nothing new is gated.** None of this sits behind the paywall. Retention mechanics behind a
   paywall convert nobody and irritate everybody. (§4.5 is the one paywall touch — it *loosens* a
   gate.)

## What already exists

Three gamified systems are already shipped; the work below connects and surfaces them rather than
adding a new badge/XP layer on top.

| System | Where | Gap |
|---|---|---|
| Wellbeing score 0–100 | `ui/wellbeing/WellbeingEngine.kt` | one prior score only; tips don't say what acting is *worth* |
| Recap story | `ui/recap/` | weekly cadence is **off by default** → fires ~12×/year |
| Buying limits | `ui/buyinglimits/` | no progress before the breach, no history, no discovery |

## Build order

**0. Firebase Analytics** (baseline first — see Measurement) → `§2 StreakEngine` (pure, no UI) →
`§1 weekly recap` (consumes streaks) → `§3 wellbeing` → `§4 limits` → `§5 notifications`.
§5 is deliberately last: a notification is only worth sending once there is something worth opening.

**Schema:** v25 → **v26**, one new entity (`WellbeingScoreEntity`, §3.1). Everything else derives
from existing transactions/budgets — consistent with the app's stated "Budgetty holds no running
totals" principle.

---

# §1 — Weekly Recap as a rhythm

## Current behaviour

`RecapScheduler.due()` fires on app open once a period boundary has been crossed, guarded by
`RecapDataGuard` (≥5 lifetime receipts + the period had spend). `RecapFrequency` defaults to
`MONTHLY`. Weekly story = 3 cards (Cover → Pace → Focus). Shown-once is tracked via
`recapLastShownWeek` / `recapLastShownMonth`.

The Recap is the strongest retention asset in the app and it currently runs at 1/12th of its
possible cadence.

## 1.1 Default `recapFrequency` to `BOTH`

`AppSettings.recapFrequency: RecapFrequency = RecapFrequency.BOTH`.

**This changes behaviour for existing users** who never made an explicit choice (`SettingsStore`
reads the default when the key is absent). That is acceptable because the recap is non-destructive
and dismissible — **but only ships together with §1.4**, so the very first weekly recap a user sees
carries its own off-switch.

## 1.2 A weekly-specific data floor

`RecapDataGuard.MIN_RECEIPTS = 5` counts *lifetime* receipts. A week containing one €4 receipt
still passes it and produces a hollow story — which trains users to reflex-dismiss.

Add a period-scoped floor:

```kotlin
object RecapDataGuard {
    const val MIN_RECEIPTS = WellbeingEngine.MIN_RECEIPTS_TO_SCORE  // 5, lifetime — unchanged
    const val MIN_WEEK_RECEIPTS = 3                                  // NEW, within the week

    fun evaluate(
        kind: RecapKind,
        totalReceipts: Int,
        periodReceipts: Int,       // NEW
        periodHasSpend: Boolean,
        priorPeriodHasSpend: Boolean,
    ): RecapGuard
}
```

A weekly recap under `MIN_WEEK_RECEIPTS` is `Skip`-ped **and marked shown**, so it isn't re-checked
on every open that week. Monthly behaviour is unchanged.

## 1.3 Make the weekly story worth an interstitial

Target: **3–5 cards, under 10 seconds**. Cover → Pace → Limits *(if any)* → Streak *(if any)* → Focus.

- **Limits card** — `RecapCard.Limits` already exists but `RecapProvider.limitOutcomes()` is wired
  to the *month* window only. Give it the week window for weekly stories. Weekly limits + weekly
  recap are a natural pair.
- **Streak card** — new `RecapCard.Streak`, fed by `StreakEngine` (§2). Weekly-appropriate framing:
  "3 weeks under your Groceries budget."
- Both cards drop out entirely when there's nothing to show, so a bare week is still Cover → Pace →
  Focus and never pads.

## 1.4 In-story frequency control

On the weekly **Focus** card, a low-emphasis text row: `Weekly recaps · Change`. Opens a small sheet
with Weekly / Monthly / Both / Off, writing `recapFrequency` + `recapEnabled` directly.

This is what makes §1.1 safe. A user who finds the weekly cadence too much can fix it in two taps
at the exact moment they feel it — instead of resenting it until they find Account → Recap.

## 1.5 No backfill — document it

A user who doesn't open for three weeks sees **one** weekly recap (the just-closed week), never a
queue of three. This is already the behaviour (`justClosedWeekId` returns a single id). Write it
into the KDoc so nobody later "improves" it into a backlog.

## 1.6 Out of scope (already built)

Account → Recap section and `RecapReopenScreen` already exist and are wired
(`BudgettyApp.kt:406`). No work needed.

## Files / tests

`RecapScheduler.kt`, `RecapModel.kt`, `RecapProvider.kt`, `RecapStoryScreen.kt`, `AppSettings.kt`,
`SettingsStore.kt`, strings ×15 locales.
Tests: `RecapSchedulerTest` — full cadence table incl. both-due; `RecapDataGuardTest` — weekly floor.

---

# §2 — Outcome streaks

## Current behaviour

`RecapProvider.streakMonths()` (line 421) computes consecutive closed months where **every** budgeted
scope stayed under, capped at `MAX_STREAK = 24`. It is rendered once a month inside `BudgetStreak`
and otherwise thrown away.

Two problems: a streak only visible monthly can't motivate, and all-or-nothing across every scope is
brittle — one over-budget category out of eight zeroes a six-month run.

## 2.1 Promote to a first-class pure object

New `ui/streaks/StreakEngine.kt` — Android-free, JVM-testable, ports 1:1 to iOS, same shape as
`WellbeingEngine` / `BuyingLimitCounter`.

```kotlin
enum class StreakKind { BUDGET_MONTH, BUDGET_WEEK, LIMIT }

data class Streak(
    val kind: StreakKind,
    /** Scope: a category name, the monthly-budget scope, or a limit's displayTitle. */
    val label: String,
    /** Consecutive CLOSED periods met, ending with the most recent closed one. */
    val current: Int,
    /** Personal best within the history window (see 2.5). */
    val best: Int,
    /** How many closed periods actually backed this — honesty for the label. */
    val periodsChecked: Int,
    /** Is the OPEN period currently on track to extend it. Never counted in [current]. */
    val liveOnTrack: Boolean,
)
```

The existing all-scopes `streakMonths()` is re-sourced from this engine rather than kept as a second
implementation.

## 2.2 Per-scope, not all-or-nothing

Compute one streak per budgeted category, plus one for the monthly-budget scope, plus one per buying
limit. A user with eight categories has eight streaks; surface the best one or two.

This is the key change: it turns streaks from a single fragile counter into a reliable source of
small wins. Somebody who overspends on takeaway every month can still be six months clean on
Groceries — and that's the true, useful thing to tell them.

## 2.3 Closed periods only

`current` counts completed periods. The in-flight period contributes `liveOnTrack` only, rendered as
a dotted/ghost segment. This avoids mid-month whiplash where a number visibly drops.

## 2.4 Copy rules (non-negotiable)

- Never "you lost your streak", "streak broken", "at risk", flames, or countdowns.
- When `current == 0` and `best > 0`, show **only** `Best run: 6 months`. The absence of a current
  number is the entire signal — no consolation copy, no explanation.
- Never a time-pressure framing of any kind.

## 2.5 Strict reset + always show `best` — no grace periods

**Recommendation: a miss resets `current` to 0, and `best` is always shown beside it.**

Grace periods ("one miss doesn't count") were considered and are rejected: they add hidden state,
they're gameable, and they make the number un-explainable to the user. Showing `best` solves the
same motivational problem honestly — the user is chasing their own record, not protecting a fragile
counter, so a miss costs momentum rather than everything.

`best` is computed within the `MAX_STREAK = 24` history window and labelled accordingly
("best in the last 24 months"), so no new persistence is needed.

## 2.6 Where it surfaces — three places, all subtle

| Surface | Treatment |
|---|---|
| **Budget screen** | a small caption on a category row: `· 3 months under`. No new section, no card. Costs nothing when there's no streak. |
| **Recap** | new weekly `Streak` card (§1.3) + the existing monthly `BudgetStreak` card, both from the engine. |
| **Wellbeing screen** | streaks listed as evidence under the Budget component: `Groceries — 4 months under`. |

**Explicitly not on Home.** Home was deliberately redesigned spent-first and is already dense; a
streak widget there would undo that work.

## 2.7 Minimum bar

Only surface `current >= 2`. A "1-month streak" is just "this month" and showing it cheapens the
whole mechanic.

## 2.8 Performance

`streakMonths()` currently re-derives a full `budgetOutcome` per month inside a loop. Per-scope
streaks multiply that by category count, over up to 24 periods, on every Budget-screen composition.

Compute it in **one pass**: group transactions by `(periodIndex, category)` once, fold into
per-scope per-period totals, then walk backwards per scope. Keep the `MAX_STREAK = 24` bound.

## 2.9 Premium

Free. Streaks are retention, not a feature to sell.

## Files / tests

New `ui/streaks/StreakEngine.kt` + `StreakModel.kt`; `RecapProvider.kt` (re-source), `BudgetScreen`,
`WellbeingScreen`, `BuyingLimitsScreen`.
Tests: `StreakEngineTest` — consecutive, reset, best, `liveOnTrack`, no-budget scope, empty period,
period with no transactions vs period with under-budget spend (these must not be conflated).

---

# §3 — Wellbeing score as meta-progression

## Current behaviour

`WellbeingScore` carries `trendDeltaVsPrevious`; `WellbeingInputs.previousScore` holds **one** prior
month. Tips are ranked, dismissible per pay-cycle month, quote a real figure, and deep-link to the fix.

Two gaps: there's no trajectory (one prior point isn't a trend), and tips say *what's wrong* but not
*how much fixing it is worth*. A score you can't predictably move is a thermometer, not a game.

## 3.1 Score history — the only new persistence in this spec

```kotlin
@Entity(tableName = "wellbeing_scores")
data class WellbeingScoreEntity(
    @PrimaryKey val periodId: String,   // pay-cycle month id, "yyyy-MM"
    val score: Int,
    val band: String,                   // WellbeingBand name
    val componentsJson: String,         // per-component sub-scores, for the breakdown-over-time view
    val computedAt: Long,
)
```

DB **v25 → v26** + migration. `WellbeingProvider` upserts a row for any **closed** month it scores.

**Why store rather than recompute:** the score depends on income, budgets and goals *as they are
now*. Recomputing last March's score against today's budgets produces a revisionist history that
silently rewrites the user's past — and would make the trend line lie. The snapshot is the only
honest option.

## 3.2 Trend line on the Wellbeing screen

Six-point sparkline of the last six closed months, plus the in-flight month as a ghost point. One
sentence under it: `Up 8 since March.`

Hand-rolled with the app's existing chart idiom — no new library. Gate on `isLoaded`; with fewer
than two stored months, render nothing at all (no placeholder, no "not enough data yet" card).

## 3.3 Attributable tips — "what this is worth"

Add `projectedGain: Int?` to `WellbeingTip`. Computed by re-running `WellbeingEngine.aggregate()`
with the affected component's sub-score replaced by its post-action value:

| TipType | Modelled action |
|---|---|
| `MISSING_BUDGET` | `budgetScore` with `budgetedCount + 1`, that category assumed within plan |
| `OVER_BUDGET` | `budgetScore` with `overCount − 1` and its overspend removed |
| `SUBSCRIPTION_COST` | `subscriptionsScore` at the share after cancelling the named sub |
| `NO_GOAL` | `goalsScore` goes null → 100; the component *enters* the weighted mean |
| `GOAL_OFF_TRACK` | that goal's mark 40 → 100 |
| Win-tone tips (`SAVINGS_WIN`, `CATEGORY_IMPROVED`, `UNDER_PACE_WIN`) | none — nothing to act on |

Rendered as a small pill on the tip row: `+6 to your score`. Suppress the pill when the gain is
`< 2` (noise).

### ⚠️ The renormalisation trap

`aggregate()` is a **weight-renormalising** mean over components that have a score. Adding a
previously-null component (`NO_GOAL` is the common case) changes the denominator — so a genuinely
good action can compute to a *lower* aggregate. Showing "−3 to your score" next to correct advice
would be actively harmful.

**Guard: if the recompute yields a delta ≤ 0, suppress the pill entirely** — never render a negative
projection. Ship a unit test that pins this exact case (`NO_GOAL` on a user whose only other scored
component is above the new goal score).

State in the KDoc that the number is a *modelled delta under an explicit assumption*, not a promise.

## 3.4 Rank tips by projected gain

Secondary sort key, after the existing tone/severity ranking. The top tip should be both important
and impactful, not just important.

## 3.5 Band-up nudge

When the score is within 3 points of a band boundary (40 / 60 / 80), the header shows
`3 points to Healthy`. Suppressed otherwise.

This is likely the highest-value single sentence in the whole spec: a concrete, near-term, reachable
target — the thing a bare score never gives you.

## Files / tests

`WellbeingModel.kt`, `WellbeingEngine.kt`, `WellbeingProvider.kt`, `WellbeingScreen.kt`, new
`WellbeingScoreEntity` + DAO, `BudgettyDatabase` v26 + migration, `RecapProvider` (Score card reads
stored history instead of recomputing).
Tests: projection math per `TipType`; **the renormalisation-negative guard**; history upsert
idempotence; migration v25→v26.

---

# §4 — Buying limits as opt-in challenges

## Current behaviour

Keyword substring caps, counted live from receipts (`BuyingLimitCounter`), save-time nudge
(`BuyingLimitNudger`), weekly or monthly windows, `FREE_LIMIT = 1` / unlimited premium. Reset date is
already surfaced on both the card and the nudge.

This is already the healthiest gamification in the app: **self-set, self-scored, no coercion**. What's
missing is progress *before* the breach, any sense of history, and a way to think of a limit at all.

## 4.1 Live progress on the limits list

Replace the bare count/cap with a **pip row** — one pip per unit of cap, filled = bought. Pips beat a
progress bar here because caps are typically 1–4, where a bar reads as almost-empty or full with
nothing in between.

At cap: fully filled in a warm tone — **not red**. The state is "reached", not "failed". The user set
this number themselves; the app doesn't get to be disappointed in them.

## 4.2 Per-limit streak

From `StreakEngine` with `StreakKind.LIMIT`: consecutive closed windows where the count stayed ≤ cap.
Caption on the card, `>= 2` only (§2.7).

## 4.3 History strip — the single best addition here

The last 8 closed windows as small squares: met / not met / no data.

This is what turns a limit from binary pass/fail into a visible trend. A user who went 6-of-8 sees
real progress, where a streak counter would show them a demoralising 0. Derived from transactions in
each historical window — **no new storage**.

## 4.4 Suggested limits (opt-in discovery)

The hard constraint on this feature is that the user has to *think of* a limit unprompted. Most never
will.

On the empty state (and as a dismissible row when they have limits), offer up to **3** suggestions
from the most-frequently-bought item names in the last 60 days with total quantity ≥ 6:

> You bought **Coke** 14× last month — cap it?

One tap opens the editor pre-filled: keyword + a suggested cap of the current rate rounded down. The
editor's existing "CURRENTLY MATCHES" preview keeps the substring honest before save.

Suggestions are **frequency-only** — no attempt to guess which items are "staples". Trying to
classify bread and milk out will misfire across 16 locales and produce worse results than letting the
user reject. Add `dismissedLimitSuggestions: Set<String>` to `AppSettings` so a rejected suggestion
never returns.

## 4.5 Free tier — ✅ DECIDED 2026-08-24

`FREE_LIMIT = 1` is tight for a challenge mechanic: a user can't build a habit portfolio out of one
limit, which caps how much retention this can deliver.

**APPROVED: raise `FREE_LIMIT` 1 → 3, keep unlimited premium.** The retention value of limits is
worth more than the conversion value of the 2nd and 3rd, and premium already unlocks exactly four
things via the shared `premiumBenefits()`.

Touches the paywall copy: `BuyingLimitsRepository.FREE_LIMIT`, the at-cap upsell string, and the
premium-benefit line if it quotes a number. Apply on **both** platforms. Note that existing free
users silently gain capacity — that is fine (a loosened gate never needs a migration).

## 4.6 Nudge restraint

Verify and pin the existing behaviour: at most **one** nudge per receipt save (already true — it
picks the single most-over limit). Add: don't re-nudge for a limit already exceeded earlier in the
same window. They know. Telling them twice is nagging, not helping.

## Files / tests

`BuyingLimitsScreen.kt`, `BuyingLimitsViewModel.kt`, `BuyingLimitEditorSheet.kt`,
`BuyingLimitNudger.kt`, new suggestion helper in `ui/util/`, `AppSettings.kt`, `SettingsStore.kt`.
Tests: history-strip window derivation; suggestion ranking + dismissal; nudge de-duplication within a
window.

---

# §5 — Notifications (last, and deliberately minimal)

## Framing

There is **no** `POST_NOTIFICATIONS` permission, no WorkManager, no AlarmManager in the project today.
That is a clean slate, and the cost of getting this wrong is asymmetric: by the time a user reaches
the off-switch, you've already annoyed them. An uninstall is unrecoverable; a missed notification is
not.

So the design principle is **earned, not scheduled**.

## 5.1 A hard cap, enforced in code

**Maximum 4 notifications per rolling 30 days, across all types.** Not a guideline — a
`NotificationBudget` gate that counts sends in the window and silently drops anything over. Every
send path goes through it. This is the single most important item in §5.

## 5.2 Ask at the right moment, never at launch

No permission prompt on first run. Ask only once the user has both:

- seen at least one recap, **and**
- created at least one budget or one buying limit

— i.e. demonstrated the app matters to them. Precede the system prompt with an in-app explainer sheet
that states exactly what will be sent **and quotes the 4-per-month cap**. If they decline, never ask
again (Android 13+ allows only two system prompts anyway — burning them is permanent).

## 5.3 What may send — and nothing else

| Trigger | Cadence | Why it's earned |
|---|---|---|
| Recap ready | ≤1/week, and only when `RecapDataGuard` would actually return `Show` | it's a payoff, not a chore |
| Buying limit reached | ≤1 per limit per window | the user explicitly asked for exactly this alert |
| Bill due tomorrow | ≤1 per bill | actionable, time-critical, real cost to missing it |

**Never, under any circumstances:** "you haven't opened Budgetty in a while" · "your streak is at
risk" · "don't lose your progress" · generic re-engagement · marketing · paywall pushes ·
tip-of-the-day · anything triggered by *absence* rather than by an event in the user's finances.

This list is the thing that erodes first under growth pressure. It's written here so that erosion has
to be a deliberate, visible decision.

## 5.4 Controls

Per-type toggles plus a global off, in Account. All three default on — but that default only ever
applies to a user who explicitly opted in at §5.2.

## 5.5 Quiet by construction

Delivery window 09:00–20:00 local. Anything computed outside it is deferred to the next window's
start, or dropped if it would be stale by then. A budgeting app has nothing worth saying at 23:40.

## 5.6 Implementation

`POST_NOTIFICATIONS` in the manifest; **WorkManager** (AndroidX — consistent with the no-external-libs
rule); one daily periodic worker evaluating all three triggers against the budget gate.

No FCM, no server, no push. Everything is local and derived from data already on the device, so
nothing leaves it — which also means **no change to the Play Data-safety declaration**.

## 5.7 Sequencing

Ships only after §1–§4 are live and observed.

---

# Cross-cutting

## Measurement — ✅ DECIDED 2026-08-24: add Firebase Analytics FIRST

There is no Firebase Analytics dependency in the project at all. D7/D30 retention is therefore
unmeasurable, which means every item in this spec — including my reasoning for it — is an untested
hypothesis.

**APPROVED: add Firebase Analytics, and land it BEFORE §1–§4** so there is a pre-change baseline to
compare against. Shipping the mechanics first and instrumenting after leaves you unable to tell
whether any of this worked. Firebase is already wired (Crashlytics is live), so this is a dependency
plus an event layer, not new infrastructure.

Minimal honest event set:

```
recap_shown{kind}          recap_completed{kind, cards_viewed}
streak_surfaced{kind, length}
tip_acted{type}            tip_projected_gain{type, gain}
limit_created{source=manual|suggestion}
```

⚠️ Adding Analytics changes the Play Data-safety declaration — it is not a free addition.

## iOS parity

The directive is *entire* parity for budgeting/saving features. All new logic is deliberately placed
in pure, Android-free objects — `StreakEngine`, the extended `WellbeingEngine`, `RecapScheduler`,
`RecapDataGuard` — which port 1:1. Port from those plus the ViewModels, never from Compose. Append a
port brief to `PARITY.md` on each Android merge.

## Paywall impact

None, except §4.5, which loosens a gate. No item here is premium-gated.
