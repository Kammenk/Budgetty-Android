# iOS port + design request — Category & Insights v2

> Two audiences in one doc. **For Claude Design (iOS):** paste the "Design
> request" sections into the Budgetty design project to get the `iOS *.dc.html`
> mockups in **iOS 26 Liquid Glass** (not Material). **For the iOS build:** the
> "Port notes" are the behaviour/data spec — port from the Android **ViewModels +
> repositories**, not the Compose UI (per PARITY.md). Android landed on branch
> `category-insights-v2`; the behaviour below is the source of truth. Match the
> visuals **from the mockup CSS via DesignSync**, never from screenshots.

The Android side shipped three things together. The **behaviour/model is identical
across platforms** (per the cross-platform parity directive); only the rendering is
Liquid-Glass-native on iOS.

---

## 1 · Insights pie legend — icons + %

**Behaviour (shared).** The spending donut's compact legend rows gain a **colored
emoji tile + the category %** — today they show only a color dot + name + amount.
The tapped-slice **donut centre** also gains the category's **emoji** above its
name. The **on-ring percentage labels stay** (a Canvas/Path detail on Android;
keep the equivalent on iOS). Percentages are each slice's share of the period
total, so a row's legend % matches its on-ring label.

**Design request (iOS).** Match `iOS Insights.dc.html`. Restyle the legend rows to
`emoji tile (~26pt, category color) · name · % (bold) over muted amount`, two per
row. Add the emoji to the centre on tap. Liquid Glass surfaces; light + dark.
Android reference mockup: `InsightsPieLegend.dc.html`.

**Port notes.** No model change. iOS already resolves a category's emoji + color by
name (the equivalent of `Categories.emojiOf`/`colorOf`); the legend row and centre
just consume them. % = slice ÷ period total.

---

## 2 · Expanded emoji picker (custom-category icons)

**Behaviour (shared).** When creating/editing a custom category, the icon grid now
offers a **curated ~220-emoji pool in nine searchable sections** (Food & Drink,
Transport, Home, Shopping, Health, Leisure & Hobbies, Money & Work, Animals &
Nature, Symbols) instead of the old ~45 built-in emoji. A **search field is pinned**
at the top of the icon area (live keyword filter — "parking" → 🅿️, "gym" →
🏋️/🏃/🚴…); only the sectioned grid below it scrolls. No-results shows a quiet
"No icons match …" + a hint. Tiles render on the currently-selected category color.

**The pool is shared data** — port the exact list + keywords from Android's
`EmojiCatalog.kt` (single code point each, Unicode ≤ 13.1, no ZWJ / skin-tone /
gender sequences). Both platforms use one vocabulary.

**Design request (iOS).** Match `iOS Custom Category.dc.html` — same create/edit
sheet, but replace the small icon grid with the pinned search + sectioned scrolling
grid. Android reference mockup: `EmojiPickerExpanded.dc.html` (has the full pool in
§4). Liquid Glass; light + dark; the search field is a native search style.

**Port notes.** Search semantics: normalise the query, match keyword tokens —
**exact-token hits win; prefix hits only when there are no exact ones** (so "car"
surfaces the vehicles, not "carton"/"carrot"). Keep it a plain in-memory filter.

---

## 3 · Sub-categories — user-defined two-level hierarchy

The big one. Users create **primary (parent)** categories and nest others under
them, or move a category between groups. Built-ins already sit under 7 umbrella
groups; this opens grouping up to the user.

### Model (shared — port exactly)
- Each category gets an optional **parent** (nullable). NULL = default (a built-in
  uses its code-defined group; a custom is top-level); a non-null value re-homes the
  category under that parent. **Two levels only** — a parent is always top-level.
- A category's **effective parent** = `row.parent ?? codeDefaultParent(name)`. The
  group it rolls up into = effective parent, else itself (`groupOf`).
- **Parents are spendable** — a group's total = its own direct spend + children's.
- Works for **custom AND built-in** categories (re-homing a built-in = storing a
  parent override on its row).
- **Delete** a primary → children **promoted to top-level** (kept, not deleted).
  **Rename** → children follow. **Nesting** a category that has children **releases
  those children** to top-level (keeps two levels).
- **Organising is free** — the premium lever stays the custom-category count. No new
  paywall.

### Data (port notes)
Android added a nullable `parent` column to the categories table (**DB migration
v19→20**, additive: existing rows stay NULL, grouping keeps resolving from code —
nothing moves on upgrade). On iOS, add the equivalent nullable `parent` to the
SwiftData/Core Data category model + a lightweight migration, stored **by name
string** (consistent with how transactions reference categories). `groupOf` /
`parentOf` consult the stored parent first, then the code default. The re-seed of
built-ins must **not** overwrite a user's parent override (same as color/emoji).

### Design request (iOS) — a few forks to show both ways
Match `iOS Category Picker.dc.html` + `iOS Custom Category.dc.html`. Android
reference mockups: `CategoryHierarchy.dc.html` (picker forks + parent selector +
re-home) and `CategoryHierarchyBudget.dc.html` (budget roll-up + insights).

- **View A — picker hierarchy.** Custom primaries render as **group headers with
  their children** beneath (Android chose Fork 1 **Option A**: the header row itself
  is tappable to pick the parent; a "General" tile was the alternative). Built-in
  children can be **re-homed** — Android uses **long-press → "Move to group…"** (Fork
  2 Option A). Show the iOS-native equivalents (a context menu / swipe action fit
  Liquid Glass better than a long-press).
- **View B — create/edit Parent selector.** A "Parent category" row → a picker of
  **top-level categories (groups + custom primaries) + "None (top-level)"**, minus
  the category itself (a sub-category can't be a parent). On iOS this is a native
  picker/sheet, not a custom dialog.
- **View C — Budget roll-up.** A primary with children shows a **rolled-up total**
  (own + children) and expands to its children, each with its own budget field, plus
  the primary's own-budget line. On Android this reused the existing group→children
  budget sheet; iOS should reuse its equivalent budget grouping.
- **View D — Insights.** The existing grouped-donut toggle now folds **custom
  children into their custom primary** — no new UI, just what `groupOf` returns.

### Port notes (behaviour)
- Custom-category save carries the chosen `parent`. Rename cascades to children;
  delete promotes children to top-level; setting a non-null parent releases the
  category's own children.
- A separate "set parent" action backs both the built-in re-home and the selector.
- Budget group total budget = own + children budgets (display roll-up only — nothing
  caps a child). Group spend already = own + children.

---

## iOS-specific rendering reminders
- **Liquid Glass** materials + the **custom dock** are final — match the mockup CSS,
  not Android's Material surfaces.
- Any new scrolling screen/sheet must respect `underFloatingDock()` (the shell's
  `safeAreaInset` doesn't reach tab roots).
- Prefer **native sheets/pickers/menus** over porting Android's custom dialogs
  (parent picker, "Move to group").
- New user-facing strings (icon search placeholder + no-results, "Parent category" /
  "None (top-level)") need `Localizable.xcstrings` entries; the emoji section titles
  are English today on Android too — localise if desired.

## Reference
- Android impl (branch `category-insights-v2`): `EmojiCatalog.kt`, `Categories.kt`
  (`parentOf`/`groupOf`/`defaultParentOf`), `CategoryPickerScreen.kt`, `PieChart.kt`,
  `BudgetScreen.kt` (`budgetGroups`/`effectiveChildren`), the `parent` column +
  `MIGRATION_19_20`.
- Android design requests: `PIE_LEGEND_ICONS_DESIGN_REQUEST.md`,
  `EMOJI_PICKER_EXPANSION_DESIGN_REQUEST.md`, `SUBCATEGORIES_DESIGN_REQUEST.md`.
