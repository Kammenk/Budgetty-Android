# Claude Design request — Sub-categories (user-defined 2-level grouping)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create new `*.dc.html` mockups in
> that project; once they're there, Claude Code reads them back via DesignSync
> and implements the approved ones.

---

Hi! The big one for this round: let users **organize categories into a 2-level
hierarchy** — create their own **primary (parent)** categories and nest others
under them, or move a category into a different group. Budgetty already groups
its built-ins under **7 fixed umbrella groups** (Groceries, Household & Personal,
Health & Wellness, Dining & Entertainment, Shopping & Lifestyle, Transportation,
Services & Subscriptions) — this opens that grouping up to the user.

Please mock this up as new `*.dc.html` files, matching the Material 3 system
exactly. There are a few UX forks below where I'd like you to **show 2 options so
I can pick** (same as we did for the Home period filter).

## The model (so the states make sense)
- Every category has an optional **parent**. **No parent = a primary**
  (top-level); **has a parent = a sub-category**. A category simply *becomes* a
  parent when something points to it — there's no separate "type" to toggle.
- **Two levels only** — a sub-category can't itself have children.
- **Parents are also spendable** — you can log spend directly on a parent *and* it
  has children. A group's total = **its own direct spend + its children's**.
- **Works for custom *and* built-in** categories — a user can nest or re-home
  either (the 7 built-in groups keep their current arrangement until the user
  changes it).
- **Delete a primary →** its children are **promoted to top-level** (never
  deleted, never dumped into "Other"). **Rename a primary →** its children stay
  attached.

## Match these existing components for style
- **`CategoryPickerScreen.dc.html`** + **`Custom Category Picker Variants.dc.html`**
  — the picker already renders the **7 groups as section headers each with a 3-up
  grid of emoji tiles**, plus the **"Your categories"** section and the
  create/edit (View B) form. Extend these.
- **`InsightsScreen.dc.html`** — the existing **group roll-up toggle** on the
  donut (collapse categories into their umbrella group). Custom primaries should
  roll up the same way.
- The **Budget screen mockup** (the per-category budget list) — for the roll-up
  total. If a `.dc.html` for it exists, extend it; otherwise mock the relevant
  card.
- Phone width **300**, scrollable, `font-family: Roboto`, Material 3.

## The views & the forks

### View A — Picker with user hierarchy
The picker already shows groups as section headers with child tiles. Now the
user's own **primaries become section headers** too, with their children nested
beneath, and **built-ins can be re-homed** into them.

- **Fork 1 — selecting a *spendable parent* vs. its children.** Because a parent
  is itself spendable, the user must be able to pick **the parent itself** *or*
  one of its children. Please show **two options**:
  - (a) the section header row is itself a **tappable tile** (tap the header =
    pick the parent; tap a tile below = pick a child);
  - (b) a **"General" parent tile** as the first tile in the section (represents
    the parent itself), children after it.
- Show a **custom primary** (e.g. "Kids") with a couple of children, and a
  **built-in re-homed** into a custom primary (e.g. "Fuel" moved under a custom
  "Car").

### View B — Create / Edit with a Parent selector
Extend the existing create/edit form with a **"Parent category"** row:
- Options: **"None (top-level)"** or pick an existing **primary** from a list.
  Setting a parent nests this category; choosing "None" promotes it. (This same
  selector is how you **re-parent**.)
- Show the parent-picker itself — a compact list/sheet of existing primaries with
  **"None"** pinned at the top.

### Fork 2 — re-homing a *built-in* category
Built-ins aren't editable today (name/emoji/color are fixed). We only need to let
the user change a built-in's **group**, not rename it. Please show **two
options**:
- (a) a lightweight **"Move to group…"** action on a long-press menu over the
  tile;
- (b) built-ins open a **trimmed edit view** exposing only the **Parent**
  selector (name / emoji / color locked).

### View C — Budget roll-up
On the Budget screen, a **primary with children** shows an **expandable** row: the
primary's own budget/spend plus a **rolled-up total** (own + children) so the
group reads at a glance. Children listed beneath, each with their own budget.
*(v1 is display roll-up only — no parent cap that forces onto children; that's a
later addition.)* Show a primary expanded with 2–3 children and the rolled-up
figure.

### View D — Insights (minor)
The donut's existing group roll-up now also collapses **custom** children into
their **custom primary** (today customs stand alone). Likely no visual change
beyond that — just note it renders like the built-in roll-up.

## States to draw
- Picker: a custom primary + children; a built-in re-homed into a custom primary;
  the **"Your categories"** section with a mix of primaries and loose categories
  — for **both** Fork-1 options.
- Create/Edit: Parent selector = "None" vs. nested under a primary; the
  parent-picker list.
- Re-homing a built-in — **both** Fork-2 options.
- Budget: a primary expanded with children + rolled-up total.
- **Delete-a-primary** confirm (copy e.g. *"'Car' will be removed and its 3
  sub-categories moved to top level."*).
- Empty: no custom primaries yet (today's flat picker).
- **Light + dark** throughout; **phone + tablet centered-dialog** for the picker /
  create views.

## Design tokens
- Surfaces `--bg`/`--sc`/`--sch`; text `--on`/`--onv`; accent `--primary`,
  selection `--secc`/`--onsecc`; lines `--outv`. Category tiles keep the **12dp
  rounded-square colored tile + emoji**. Token-driven, light + dark.

## Premium
- **Decided: organizing is free — no paywall here.** The premium lever stays the
  **custom-category count** (3 free / unlimited premium); creating primaries,
  nesting, and re-homing categories are all free. **No locked / paywall states in
  these mockups.**

## Output
- New `*.dc.html` file(s) — don't overwrite the existing picker / insights /
  budget screens.
- Token-driven, light + dark, phone + tablet.

## Implementation notes (for after the mockup)
- **Data:** add a nullable **`parent`** column to `CategoryEntity` (Room migration
  + version bump; **iOS mirror**). Stored **by name string**, consistent with the
  name-keyed schema; the rename-cascade must also update children's `parent`.
- `Categories.groupOf()` extended to read the **DB parent** (custom *and* built-in
  overrides) before falling back to the static taxonomy; custom categories are no
  longer forced to `parent = null`.
- **Re-homing a built-in** = upsert a `categories` row for that built-in name
  carrying the new `parent` (built-ins may have no row today). Enforce **2 levels**
  (a category that already has a parent can't be selected as another's parent).
- **Budget roll-up** sums children by parent for the group total; **Insights**
  `rollUpToGroups` uses the new `groupOf`.
- **Parents spendable:** no transaction migration needed — a category that becomes
  a parent keeps its existing transactions as its own direct spend.

Thanks!
