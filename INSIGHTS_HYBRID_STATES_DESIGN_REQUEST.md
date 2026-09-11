# Claude Design request — Insights "Hybrid" missing states — phone + iOS + tablet

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It should **extend the existing Hybrid
> mockups** (`InsightsHybrid.dc.html`, `iOS InsightsHybrid.dc.html`,
> `TabletInsightsHybrid.dc.html`) — same tokens, same 5-group model — not start
> fresh. Once the new files are there, Claude Code reads them back via DesignSync.

---

Hi! We picked the **Hybrid** Insights direction you resolved (`InsightsHybrid.dc.html` + iOS + Tablet) — thank you, it's great. Those mockups show the **populated happy path**, but the real screen has several states they didn't draw, and I'd like those designed **before** we build. This is a **follow-up** to the Hybrid files: same five-group model (**Overview · Spending · Money · Trends · Custom**), same card style and tokens — we're just filling the gaps. Please do all three surfaces (Android phone M3, iOS 26 Liquid Glass, tablet adaptive).

## Decisions already locked (so everything stays consistent)
- Groups are **fixed**; the only user-composed view is **Custom**. **Overview** is fixed and the **default landing tab**.
- The old toolbar **hamburger is gone**. Global toggles (planned-bills overlay, "what counts as savings") live as **quick chips/rows on Overview** (as you drew).
- Wellbeing + recap stay in the **toolbar** (score ring + recap button); recap only appears when a recap is ready.

## What to draw (four things)

### 1. Per-tab empty / loading / no-data states
Today's screen carefully hides sections when there's nothing to show; in the tab model each tab must instead show a **friendly, intentional** state — never a blank pane or a wall of empty cards. Please draw:

- **A · First run (no receipts yet).** A brand-new user with zero spend. Draw **Overview, Spending, Money, Trends** each as a single **invitation** (one graphic + one line + a verb-first CTA into scanning a receipt), not a stack of empty cards. *(Custom's first-run "Your own view / Add sections" already exists — keep it.)* Example tone: *"Scan a receipt to see your month at a glance."*
- **B · Empty period.** The user **has** data, but has stepped to a period with **no spend** (e.g. a future/last month). A period-aware message — *"No spending in June"* — clearly different from A (this isn't a new user). One per tab, compact.
- **C · Money tab, no income or budget.** The money-flow cards need a plan. Draw a single invitation — *"Add your income or a budget to see your money flow"* — with a CTA into **Budget**. (Also draw the **partial** case: income but no spend, or vice-versa, where some cards populate and one shows its own inline nudge.)
- **D · Trends, not enough history.** Before there's enough history for a trend/comparison — *"A month or two of history unlocks your trends."*
- **E · Custom with data-less sections.** Sections were chosen but have no data this period — each chosen card shows a small inline empty, not a blank.
- **F · Loading (cold start).** What shows before data resolves — a calm skeleton/placeholder, **no flashing zeros** (we gate on an `isLoaded` flag).

### 2. The "N things to set up" card (Overview)
You drew it only as a collapsed chip. Please draw the whole thing:
- **Collapsed chip** — as-is (*"2 things to set up · Review ✕"*).
- **Expanded list** — up to ~4 setup items, each a row with a label + a verb-first CTA and its own dismiss: *"Choose what counts as savings"* → savings-allocation choice; *"Add income or a budget"* → Budget; *"Try the planned-bills overlay"* → toggles it on; *"Tag categories as needs / wants"* → Manage Categories. Keep it light and non-nagging.
- **All-done / empty** — show what Overview looks like when there's **nothing** to set up (the card is simply absent, not an empty box).

### 3. iOS — scrollable tab bar (this is the chosen fix)
Five segments don't fit iOS's segmented control at 390pt (you already shortened "Money flow" → "Money"). We've chosen a **horizontally scrollable** pill row: **all five pills present, swipeable**, active pill styled, with a subtle edge affordance that more exist off-screen. Draw it in **Liquid Glass**, and show it holding up at **large Dynamic Type** and on a **narrow (375pt)** device. (Android keeps its own `SegmentedToggle`, which already scrolls — no change there.)

### 4. Phone — three-row stacked header (fixes toolbar crowding)
The Hybrid header packs title + ring + recap + period into one row; on narrow phones / large text / long period labels it's too tight. Draw the **stacked** version:
1. **Title row** — "Insights" + wellbeing **ring** & **recap** button as trailing icons.
2. **Period row** — the full-width period stepper.
3. **Tab row** — the segmented tabs.
Draw both the **recap-ready** (button present) and **no-recap** (button absent) variants, and the wellbeing ring in its **three states** (green ≥70 / amber 40–69 / red <40).

## Per-surface notes
- **Android phone — M3.** The stacked header (#4) + all empty states (#1) + the checklist (#2). Phone **300×620**, Roboto.
- **iOS phone — iOS 26 Liquid Glass.** The scrollable tab bar (#3) + empty states + checklist, native chrome; match `iOS InsightsHybrid.dc.html`.
- **Tablet — M3 adaptive.** No segmented control (groups live in the **nav rail**) so #3 doesn't apply; #4's controls sit in the top bar (draw the no-recap variant). Draw the **permanent Overview left column** in its first-run/empty state, the **right-pane** empty states per group, and the checklist inside the Overview column. Build from `TabletInsightsHybrid.dc.html`.

## Design tokens
- Reuse the Hybrid files' vars exactly: surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; dividers `--outv` (iOS: the glass/label/tint set already in `iOS InsightsHybrid.dc.html`). Empty-state art stays flat and on-brand (violet `#6650A4` light / `#D0BCFF` dark), muted `onSurfaceVariant` for secondary copy. Both themes.

## Please produce
- **`InsightsHybridStates.dc.html`** — Android phone: stacked header (recap on/off, ring states) + the empty/first-run/loading states for each tab + the checklist card (collapsed / expanded / all-done).
- **`iOS InsightsHybridStates.dc.html`** — iOS Liquid Glass: scrollable tab bar (default + large text) + the same empty states + checklist.
- **`TabletInsightsHybridStates.dc.html`** — adaptive tablet: Overview-column + right-pane empty states + checklist, per the notes above.

## Notes (for after the mockups)
- These complete the Hybrid set; implementation follows the locked plan (`INSIGHTS_HYBRID_IMPLEMENTATION_PLAN.md`), matched to the mockup CSS, with full Android + iOS + tablet parity.

Thanks!
