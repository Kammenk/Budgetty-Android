# Insights "Hybrid" redesign — implementation & open-questions plan

**Status:** proposed, **not committed**. No code written yet.
**Design source:** `InsightsHybrid.dc.html`, `iOS InsightsHybrid.dc.html`, `TabletInsightsHybrid.dc.html` (Claude Design project `5b8c8470-…`).
**This doc's job:** turn the chosen mockups into a decision list + a phased build plan so we can size the work and de-risk it *before* touching code. Read the "Open decisions" section first — several of them change the scope.

---

## 1. What we're building (one paragraph)

Replace the single long scroll on Insights with a **grouped, tabbed** screen: a summary **Overview** tab plus **Spending / Money / Trends** groups that hold today's existing cards, and a user-composed **Custom** tab. Wellbeing and Recap move out of the scroll into the **toolbar** (a score ring + a recap button). Period and planned-overlay context stay global across tabs. On tablet the same content model is driven by **nav-rail sub-groups** with Overview as a **permanent left column**.

**What stays exactly as-is:** every existing section's *content* and data (Breakdown donut, Top categories/stores, Biggest, Subscriptions, Income-vs-spending, Savings rate, Needs/Wants + bucket trend, Income by source, Trend, Period comparison, Highlights, By-category change), the period stepper, the planned-bills overlay, and all the ViewModel derivations behind them.

**What's genuinely new (not just re-bucketing):** the **Overview** composite, the **"N things to set up"** consolidated nudge, the **Custom** tab + its picker + persistence, and the **toolbar** wellbeing/recap controls.

---

## 2. Open decisions (these gate the build)

Each has my recommendation. Answering these first keeps us from building throwaway UI.

| # | Decision | Options | Recommendation |
|---|----------|---------|----------------|
| **D1** | Do per-tab sections stay individually hide/reorderable, or are the Spending/Money/Trends groups **fixed**? | (a) Fixed groups; personalization only via Custom. (b) Keep per-tab hide+reorder *and* Custom. | **(a) Fixed.** One curation surface (Custom) is simpler and matches the mockup. Avoids two competing customization models. |
| **D2** | What happens to existing users' saved `hiddenInsightsSections` / `insightsSectionOrder`, and to the **setup quiz** that writes them (`applySetupQuiz`)? | (a) Reset on first launch of new model. (b) Translate: seed **Custom** from their still-visible sections, in their saved order, and land them on Custom. (c) Keep prefs, apply only inside Custom. | **(b) if we value continuity, (a) if we value shipping.** Either way, **re-map the quiz** so its output seeds Custom membership instead of a hidden-set. This is the fiddliest migration piece — treat it as its own task. |
| **D3** | Is **Overview** fixed & non-customizable, and is it the default landing tab? Persist last-viewed tab? | Fixed vs customizable; land-on-Overview vs remember-last. | **Fixed, non-customizable, default landing = Overview.** Land on Overview each cold start (don't persist across launches; optional: remember within a session). |
| **D4** | Define **"N things to set up."** What counts, how is it dismissed? | Closed checklist (allocation-not-chosen, no income/budget, overlay-never-tried, categories-not-bucket-tagged), each dismissible. | Start with a **small closed set** (savings-allocation ask + add-income/budget), each linking to its fix, card hidden when empty, dismissals persisted in `SettingsStore`. **Deferrable** — Overview can ship without it. |
| **D5** | **Custom** default contents + discoverability. | Empty with a strong empty-state, or seeded (e.g. Breakdown + Top categories). | **Seed a small default** so the tab isn't empty on first open (unless D2(b) seeds it from migration), plus a one-time hint so users find it. |
| **D6** | Role of the leftover **toolbar hamburger** (still present in the phone/iOS mockups, unwired). | (a) Drop it. (b) Repurpose as an "Insights settings" overflow (overlay toggle, savings-allocation, reset). | **(a) Drop it** on phone/iOS — Overview's toggle chips + the Custom picker cover it. Keep a labeled "Customize" entry on the **tablet** rail (as drawn). |
| **D7** | Per-tab **empty / loading / no-data** states. | Define each. | Each tab renders a compact per-tab empty state (never a blank pane); Overview honors the `isLoaded` gate so there's no cold-start flash. Real work — see Phase 8. |
| **D8** | **Analytics** semantics with tabs. | Keep section-impression events? Add tab-view events? | Add a `insights_tab_selected` event; accept that off-tab sections aren't "viewed." Coordinate with the telemetry program before wiring. |
| **D9** | iOS **5-segment** control fit on small iPhones + Dynamic Type. | (a) 5 short labels (mockup). (b) 4 segments, Overview as default screen. (c) scrollable/menu picker. | **(a) with a verified fallback to (c)** on iPhone SE/mini and large text. |

> **Fastest way to close these:** I can run you through D1–D9 as a quick decision pass and record the answers, or you can annotate this table.

**Decisions locked (2026-09-11):** D1 = **fixed groups** · D2 = **reset** old prefs · D3 = Overview **fixed & default landing** · D4 = **build the small checklist** · D5 = **seed a small Custom default** · D6 = **drop the toolbar hamburger** · D7 = **design empty/no-data states before building** (→ follow-up Claude Design brief) · D8 = **add `insights_tab_selected`** · D9 = **mock the iOS tab-overflow options, then pick** (→ same brief; see D10).

### D10 — Toolbar density (phone), added 2026-09-11
The heads-up flagged the toolbar as overcrowded but the first plan omitted it. Even after D6 drops the hamburger, the header still packs **title + wellbeing ring + recap + period chip** in one row above the 5-tab row — which crowds on narrow phones, at large Dynamic Type, and with long period labels ("Q2 2026", localized months, custom ranges).

**Decision — a three-row stacked header:**
1. **Title row** — "Insights" (leading) + wellbeing ring & recap button (trailing icons; recap only when a recap is ready).
2. **Period row** — the existing full-width period stepper, unchanged from today.
3. **Tab row** — the `SegmentedToggle` tabs, full width.

Keeps wellbeing **always visible across tabs** (the point of promoting it out of the scroll), matches today's proven header height (title + period) plus one tab row, and removes the title-vs-period contention. *Optional later polish:* collapse the title row on scroll so period + tabs stick. *Lighter alternative if 3 rows feel tall:* put the ring + recap on the **Overview tab only** (toolbar = title + period), at the cost of always-visible wellbeing on the other tabs.

---

## 3. Target architecture (Android)

Current: `InsightsScreen.kt` (~2,960 lines) → `InsightsScreenContent` → `InsightsPhoneBody` / `InsightsTabletBody`, one `verticalScroll(Column)`, sections chosen via `resolveSectionOrder(...)` + `shows(section)`.

Proposed decomposition:

- **`InsightsTab` enum** — `OVERVIEW, SPENDING, MONEY, TRENDS, CUSTOM` (stable keys; label + which existing `InsightsSection`s belong to each).
- **Tab chrome** — reuse the existing **`SegmentedToggle`** component (per our segmented-toggle convention — *not* M3 `SegmentedButton`, and not a new control) for the phone/iOS in-screen tabs. Tablet uses rail sub-items instead (below).
- **Per-tab bodies** — thin composables (`OverviewTabContent`, `SpendingTabContent`, `MoneyTabContent`, `TrendsTabContent`, `CustomTabContent`). The Spending/Money/Trends ones are mostly **cut-and-paste of the existing `InsightCard { … }` blocks** out of `InsightsPhoneBody`'s `when(section)` — low-risk moves, same gating (`isLoaded`, `hasData`, `hasIncome/hasBills`).
- **`OverviewTabContent`** — the one net-new composite: hero "spent" (reuses total/projection), 50/30/20 mini-bar (reuses `needsWantsSplit`), stat tiles (reuses Summary values), "Top of the month" (reuses donut + top-3 slices) with a "Spending ›" deep-link, "Worth knowing" (reuses top 2 highlights) with a "Trends ›" deep-link, optional "N things to set up" card, and the two global toggle chips.
- **`CustomTabContent` + section picker** — a new bottom-sheet picker (toggles + drag-reorder). Honor the bottom-sheet scroll convention (`weight(1f, fill=false)`, not `heightIn(max)` → detekt `SheetScrollNotCapped`). **Do not** refactor the shared `SectionsMenu` (Home still needs its current behavior) — build the Custom picker as its own component, or generalize `SectionsMenu` behind a flag without changing Home's call sites (`HomeScreen.kt:395`).
- **Toolbar** — title + wellbeing **score ring** (reuse the Home wellbeing ring; arc = score, green ≥70 / amber 40–69 / red <40, with number + contentDescription) + **recap** play button (shown only when `showRecapEntry`) + period chip. `WellbeingInsightsRow` / `RecapReopenRow` leave the scroll. TopAppBar insets zeroed per the edge-to-edge convention (detekt `TopAppBarInsetNotZeroed`).
- **State (`InsightsViewModel` / `InsightsUiState`)** — add `selectedTab`, `customSections: List<String>` (ordered membership), and (if D4) the setup-checklist items. New derivations for Overview summary + checklist counting. Reminder: run `:app:testDebugUnitTest` after any constructor/DI change.
- **Persistence (`SettingsStore.kt`, SharedPreferences — no Room migration)** — add `KEY_CUSTOM_INSIGHTS` (ordered membership) and, per D4, dismissed-checklist keys. Add setter/reset + include in **backup/restore** (`setHiddenInsightsSections` has a sibling today; add the Custom equivalent) and extend the backup round-trip **guard test**. Re-map `applySetupQuiz` per D2.
- **Tablet (`InsightsTabletBody`)** — Overview becomes a **permanent left column**; the four groups become **nav-rail sub-items** (Spending/Money flow/Trends/Custom); right pane is the 2-col grid; picker is a **centered dialog**. Note the tablet already ignores saved section *order* (positional two-pane) — the new model formalizes that.

---

## 4. Phased plan (each phase is independently reviewable; several are optional/deferrable)

| Phase | Deliverable | Size | Depends on | Notes |
|-------|-------------|------|-----------|-------|
| **P0** | Lock decisions D1–D9 (short decision record) | XS | — | This doc seeds it. |
| **P1** | **Tab shell + re-bucket existing cards** (Android phone). `InsightsTab` enum + `SegmentedToggle` header; move existing `InsightCard` blocks into Spending/Money/Trends. No Overview/Custom yet (temporarily default to Spending). | **M** | P0(D1) | **This alone delivers most of the "declutter"** at low risk — no new data logic. Good candidate to ship first. |
| **P2** | **Overview tab** (Android phone) — the new composite + deep-links + `isLoaded` gate | **L** | P1 | Biggest new-build chunk. Make Overview the default landing tab. |
| **P3** | **Consolidated setup nudge** + move global toggles (overlay, savings-allocation) onto Overview; retire the scattered nudges | **M** | P2, D4, D6 | Deferrable / can fold into P2. |
| **P4** | **Custom tab** + picker + persistence + **migration** (old prefs → Custom) + quiz remap + backup + guard test | **L** | P1, D2, D5 | The fiddly one (migration + backup + quiz). |
| **P5** | **Toolbar** wellbeing ring + recap button; remove pinned rows | **S–M** | P1 | Touches the Wellbeing/Recap entry points. |
| **P6** | **Tablet** adaptation (rail sub-groups + permanent Overview column + dialog picker) | **L** | P1–P5 | Distinct chrome; reuses the tab/content model. |
| **P7** | **iOS parity** — SwiftUI port matching `iOS InsightsHybrid` / iPad `TabletInsightsHybrid`; append PARITY.md port brief | **XL** | P1–P6 | Separate repo (`/Users/kamenkostov/Budgetty iOS/Budgetty`); port from ViewModels/repos, match mockup CSS. iOS `SettingsStore` equivalent for Custom membership. |
| **P8** | **Cross-cutting polish** — per-tab empty/loading/no-data states, a11y (score-ring label, tab semantics), analytics (`insights_tab_selected`), Roborazzi screenshot tests per tab, device + sim verification | **M–L** | all | Don't skip — this is where the mockup's happy-path gaps get filled. |

**Total: Large→XL across both platforms.** The single most valuable low-risk slice is **P1** (grouping) — it's the bulk of the readability win without the new Overview/Custom logic, and it's shippable before you commit to the rest.

---

## 5. Conventions & guardrails to honor (so this matches the rest of the app)

- **Reuse `SegmentedToggle`** for tabs; don't add a tab library or use M3 `SegmentedButton` (prefer-native + segmented-toggle conventions).
- **Screen-preview convention:** thin `*Screen` wrapper + stateless `*Content` + `@Preview`; edit `*Content`. Add previews per tab.
- **Dimens tokens** (`MaterialTheme.dimens`), **pill button shapes** / 56dp actions, **edge-to-edge insets** (zeroed TopAppBar), **bottom-sheet scroll cap**, **loading-state gate** (no cold-start flash) — all have detekt rules; CI gates `lintDebug` + `:app:detekt` (baselines match on content — editing a flagged line resurfaces it).
- **Tablet/phone parity + full iOS parity** are hard requirements (budgeting/insights = shared surface).
- **Branch-per-task**, merge to `main` only after done + tested + verified; **don't open PRs**.

---

## 6. Testing & verification

- **JVM unit tests** (`:app:testDebugUnitTest`): new ViewModel derivations (Overview summary, Custom membership resolution, setup-checklist counting), and the **backup round-trip guard** extended for the Custom-membership pref. Run after any DI/constructor change.
- **Roborazzi screenshot tests** per tab (note: `verifyRoborazziDebug` ≠ `testDebugUnitTest`).
- **Device (Pixel 9 Pro, `ANDROID_SERIAL`):** verify tab switching, Custom picker, toolbar controls, dark mode, large font, small-width. **Do not** use `connectedAndroidTest` on a seeded device — it uninstalls the app and wipes data; use the DB seed workflow (pull/checkpoint/edit/push) if seeding is needed.
- **iOS:** simulator verification per the simctl recipe; then device.

---

## 7. Risks & mitigations

- **Scope creep past "declutter."** Overview + checklist + Custom are new product logic. *Mitigation:* phase them; P1 alone declutters.
- **Migration regressions** (existing hidden/order prefs, quiz, backup). *Mitigation:* dedicated P4 task + guard test + a manual before/after on a real seeded profile.
- **Density on small phones / large text** (5 tabs + 4-control toolbar) and **iOS 5-segment fit.** *Mitigation:* D9 fallback; verify at 375pt + max Dynamic Type early.
- **Parity drift** (phone vs tablet vs iOS are three layouts). *Mitigation:* build the shared tab/content model once; PARITY.md port brief; match mockup CSS not screenshots.
- **Analytics meaning shift.** *Mitigation:* D8 decided before wiring.

---

## 8. Recommendation

If you want to move: do **P0 → P1** first (grouping), look at it on device, and decide whether Overview/Custom/toolbar (P2–P5) and the tablet/iOS ports (P6–P7) are worth it. That gets you ~80% of the decluttering feel for a fraction of the risk, and every later phase is additive. If you'd rather not build yet, this plan + the mockups are captured in memory and we can pick it up anytime.
