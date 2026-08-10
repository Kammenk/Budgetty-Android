# Claude Design request — Manage Categories (a permanent home for category CRUD)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! Today the only place a Budgetty user can **create / edit / delete / re-home**
a category is *inside the category picker*, which they only reach mid-task — while
reviewing a scanned receipt or choosing a category for a recurring payment. I want
to give category management a **permanent home**, reachable any time from the
**Account** screen. Please mock up the screens below as new `*.dc.html` files,
matching the existing design system exactly.

## What the feature does (context)
- A new row in the **Account → Account** settings group opens a full-screen
  **Manage categories** screen.
- On it the user can see every category, and:
  - **Create** a custom category — name, emoji icon, color, and parent group.
  - **Edit** a custom category — same four fields; renaming cascades everywhere.
  - **Delete** a custom category — its past transactions fall back to **Other**.
  - **Re-home** a category into a different group ("Move to group").
- The taxonomy is **two levels**: top-level **groups** (Groceries, Dining &
  Entertainment, …) each contain **sub-categories**; both levels are real,
  spendable categories. There is a catch-all **Other**.
- Free tier = **3 custom categories**; Premium = unlimited. The screen must show
  this cap and route to the paywall when a free user is at the limit.

This is the **same data and the same create/edit form** the picker already uses —
we're re-presenting it as a management surface (no "pick this one" tap target,
no receipt/recurring context around it).

## Match these existing components for style
- **Category picker** (brief's category picker screen) — the grouped grid of
  category tiles (color tile + emoji + name), the **Create** tile, the
  **"Your categories — 2 / 3"** cap header, and the **create/edit form** (preview
  tile, name field, color swatches, emoji search grid, parent picker). Reuse all
  of this treatment; this screen is its sibling.
- **Account settings rows** (brief's Account screen) — the new entry row must
  match the existing `SettingRow`s (leading icon, title, chevron), sitting in the
  Account group near "Category rules".
- Categories render everywhere as **color tile + emoji + name** in the category's
  own muted color — keep that exactly.
- Buttons are **fully-rounded pills**; confirmations are **centered dialogs**, not
  bottom sheets. Phone preview **300 × 620**, `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh),
  `--scl` (surfaceContainerLow)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Screens to design

### 1. `ManageCategoriesScreen.dc.html` — the main screen (primary state)
A full-screen list with a back arrow and title **"Manage categories"**. Contents,
top to bottom:
- A **cap header** — "Your categories — 2 / 3" with a subtle hint that Premium
  unlocks unlimited (only for free users); and a prominent **"+ Create category"**
  action (a button or a create tile — your call which reads best here).
- **Your categories** — the user's custom categories first, each as a row or tile
  the user can **tap to edit** directly (today editing is hidden behind a
  long-press — on a dedicated management screen it should be an obvious tap, with
  an explicit edit affordance and a way to delete).
- **Built-in categories** — every predefined group with its sub-categories under
  it, grouped and collapsible if that helps the length. Built-ins can't be
  deleted or renamed, but they **can be moved to another group** ("Move to
  group") — show how that reads on a built-in row.
- Consider how a **group** (a top-level, spendable category) is distinguished from
  its **sub-categories** visually.

Show it once **phone** (300 × 620) and once **tablet/landscape** as a two-pane
list-detail (categories on the left, the create/edit form on the right) — this
matches how the Account screen already behaves on a wide tablet.

### 2. `ManageCategoriesEmptyScreen.dc.html` — no custom categories yet
The same screen for a user who has created **zero** custom categories: the
"Your categories" section shows a friendly empty state that invites them to create
their first, with the built-in list still below. This is the common first-run
state, so it matters.

### 3. `ManageCategoryEditScreen.dc.html` — create / edit form (reached from §1)
The create/edit form as it appears **when opened from this screen** (not the
picker). It's the existing form — preview tile, name field, **color swatch row**,
**emoji search + sectioned grid**, **parent-group picker** — plus, for an existing
custom category, a **Delete** action. Please show the **edit** variant (fields
pre-filled, Delete present) so we can see the delete affordance and confirm dialog
copy. A brand-new-create variant can reuse the picker's existing form mock — only
mock what differs here.

## Open design questions (please explore + note your recommendation)
These are genuinely undecided — mock what you think is best and say why:
- **Reorder** — is dragging custom categories into a preferred order worth it, or
  is grouping enough? (We don't support ordering today.)
- **"Move to group" affordance** — a dedicated control on the row, a menu, or a
  field inside the edit form? Today it's a long-press context menu.
- **Built-in list density** — 20+ built-ins across 7 groups is long; collapsible
  group headers vs. a flat grouped list vs. only showing custom by default with a
  "Show built-in" expander.
- **Delete confirmation copy** — the honest, non-alarming way to say "past
  transactions in this category move to *Other*".

## Platforms / output
- Save each as a new `*.dc.html` file in this project (don't overwrite existing
  screens).
- **Phone first** (300 × 620), **plus a tablet two-pane** variant of §1. Keep them
  dark/light-token driven (CSS vars above, no hard-coded grays).
- The app is native on **both Android and iOS**, and this screen ships on **both**
  — please keep the layout to shared primitives (list, tiles, form, dialog) that
  read the same on Material (Android) and on the iOS build, rather than anything
  platform-specific. I'll implement Android + iOS from the same mock.

## Already covered — please DON'T redo
- The **category picker grid** and its **create/edit form** already exist — build
  the management screen and its Account entry on top of them; only mock what's new
  or different (the management framing, the tap-to-edit rows, the empty state, the
  tablet two-pane).

## Copy (draft — tighten if a shorter phrasing reads better)
- Account row: **Manage categories**
- Screen title: **Manage categories**
- Cap header: **Your categories — %1$d / %2$d**  ·  premium hint: **Unlimited with Premium**
- Create action: **Create category**
- Section headers: **Your categories** · **Built-in**
- Move action: **Move to group**
- Delete confirm: title **Delete category?**, body **Past transactions in
  "{name}" will move to Other.**, actions **Cancel** / **Delete**

Note: the app is localized into **21 languages** (German and Bulgarian run long) —
please let titles, headers and the delete body wrap gracefully rather than
truncate.

Thanks!
