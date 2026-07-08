# Budgetty — Custom Categories Design Brief

> This is a **new feature within an existing app** — match the current Budgetty design system (see `BUDGETTY_DESIGN_BRIEF.md`, the master brief). Deliver light **and** dark, phone portrait, plus the tablet centered‑dialog variant. **Do not replicate the reference app that inspired this** — in particular, use **emoji** category icons (not monochrome vector glyphs) and Budgetty's own section styling.

## Feature: Custom categories in the category picker

**Context.** Budgetty has 46 built‑in spending categories, each an **emoji on a rounded‑square colored tile (12dp radius)**. Users pick a category for each receipt line item from a **bottom‑sheet picker** (on tablet this same picker renders as a centered dialog). We're adding the ability for users to **create, use, edit, and delete their own categories** — without leaving the picker.

> **Reuse the app's existing colors and emoji — do not introduce new ones.** The color choices offered when creating a category are Budgetty's existing **12‑swatch category palette** (the muted tones the built‑in categories already use). The icon choices are the **~45 distinct emoji already used across the 46 built‑in categories** — the app's existing emoji vocabulary. A custom category must be indistinguishable from a built‑in one; it simply recombines the same palette + emoji set under a user‑chosen name.

**Golden rule:** everything happens **inside the existing category‑picker sheet**. Creating a category swaps the sheet's content to a create view (morphing header + back arrow) and swaps back — never a separate full screen. Keep the create view short enough to also fit the tablet centered‑dialog form factor.

---

### View A — Pick a category (the picker, extended)

- Reuse the current picker: search field on top, then the 7 predefined groups as section headers each with a 3‑up grid of emoji‑tile cards; selected item shows the violet outline + check badge.
- **Add a new top section, "Your categories," pinned above the predefined groups**, containing:
  - A **"＋ Create" tile** as the first item (dashed or tinted‑surface tile with a ＋ glyph and "Create" label).
  - The user's **custom category tiles** (emoji‑on‑color, same card style as built‑ins), each selectable like any category.
  - A quiet counter near the section header: **"3 of 10"** (custom categories used / max).
- **Free‑tier state:** when a free user is at their cap (3 custom), the "＋ Create" tile switches to a **locked state** — a small lock glyph and "Unlock more" — that opens the **paywall** instead of the create view. Existing custom tiles stay fully usable.
- **Edit affordance:** long‑press (or a small edit icon on) a custom tile opens View B in edit mode for that category. Built‑in categories are not editable.
- Design states: empty "Your categories" (only the Create tile), a few custom categories, near cap, **free user at 3/3 (locked Create)**, premium at 10/10 (Create tile disabled with "Maximum reached").

### View B — Create / Edit a custom category (same sheet, swapped content)

- **Header:** back arrow (returns to View A) + title ("New category" / "Edit category").
- **Live preview:** a prominent emoji‑on‑colored‑tile showing the current icon + color + typed name, so the user sees the end result.
- **Name field:** single line, rounded/pill filled field, placeholder "Name."
- **Color row:** a horizontal row (scroll or wrap) of the **existing 12‑swatch Budgetty category palette** — the same muted tones the built‑in categories use, no new colors. The selected swatch gets a ring/halo.
- **Icon grid:** a scrollable grid (5 per row) whose options are **Budgetty's existing category emoji** (the ~45 distinct emoji already used by the 46 built‑in categories) — no new/arbitrary emoji. Each emoji is shown on the currently‑selected color so the grid previews the final look; the selected emoji is highlighted with a ring/tile.
- **Primary action:** full‑width **"Save"** button (Budgetty's fully‑rounded pill), **disabled until** a name is entered and an icon chosen; enabled state uses the accent.
- **Edit mode extras:** a **"Delete category"** destructive text action; tapping it shows a confirm dialog — *"Move N transactions to Other and delete this category?"* — with Cancel / Delete.
- Design states: empty (Save disabled), valid (Save enabled), name‑too‑long/duplicate inline hint, edit mode (fields pre‑filled), delete confirm dialog.

### Premium messaging

Free users get **3** custom categories; premium unlocks up to **10** total. Surface this at the moment of friction (locked Create tile → paywall), plus the "N of 10" counter. Don't hide the Create tile from free users — show it locked.

### Visual / system specifics to honor

- Category tile: rounded‑square **12dp** radius, category color fill, centered emoji glyph — identical to existing categories.
- Cards/sheet: existing rounded surfaces, `surfaceContainer` tones, violet accent (`#6650A4` light / `#D0BCFF` dark), fully‑rounded buttons.
- Selected‑category treatment (outline + check badge) unchanged from today's picker.
- Provide **light + dark** for every state, and the **tablet centered‑dialog** rendering of both views.
- Include content‑description intent for a11y (icons/tiles are labeled).

### Explicitly different from the reference that inspired this

- Emoji icons, **not** monochrome vector glyphs — drawn from the app's existing category emoji set.
- Color options are the app's existing 12‑swatch palette, **not** new colors.
- Creation lives **inside the picker sheet** (mode swap), **not** on a standalone "Custom category" screen.
- Use Budgetty's own "Your categories / All categories" section styling and card shapes, not the reference's layout.

---

## Implementation notes (for the build, not the design)

These are engineering decisions captured alongside the brief so the design and the eventual build stay aligned. They don't need to be reflected in mockups.

- **Icons are emoji, reusing the existing set.** Category icons render as an emoji glyph on a colored tile everywhere in the app (picker, pie chart, transaction rows, widgets). The custom‑category icon grid draws from the ~45 distinct emoji already used by the 46 built‑ins — no new asset pipeline, no icon library (respects the no‑external‑libs rule).
- **Colors reuse `Categories.palette`.** The 12‑swatch muted palette already exists in code (`Categories.palette`, with `defaultColor = palette.first()`); the color row offers exactly these.
- **Storage.** Color is already persisted per category (`categories` table); emoji is currently *derived* from the static list, so a schema change adds an icon column and lets the picker merge stored custom categories (today it reads only the static 46).
- **Caps.** 3 custom for free users, 10 total with premium (`billingManager.isPremium`). Existing custom categories remain usable if premium lapses; only creation of new ones past the free cap is blocked.
- **Delete.** Reassigns affected transactions to "Other" and drops any category rule / budget pointing at the deleted category (hence the confirm dialog copy).
- **Rename.** Cascades the new name across transactions / rules / budgets (category is stored as a name string) — or, as a cheaper alternative, lock the name after creation and allow editing color/icon only. *(Decision pending.)*
