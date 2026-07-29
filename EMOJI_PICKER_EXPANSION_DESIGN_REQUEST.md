# Claude Design request — Expanded emoji picker (custom category create/edit)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create new `*.dc.html` mockups in
> that project; once they're there, Claude Code reads them back via DesignSync
> and implements the approved one.

---

Hi! Small but useful one. In Budgetty, when a user **creates or edits a custom
category** they pick an **emoji** for it (shown on a colored tile). Today the
icon grid only offers the **~45 distinct emoji already used by the built-in
categories** (see the create/edit view in `Custom Category Picker Variants.dc.html`).
That's too limited — a user making "Coffee," "Gym," or "Dog" often can't find a
fitting glyph. Please mock up an **expanded, searchable emoji picker** for that
same create/edit view.

We are **not** introducing custom/vector icons — the app stays on **system
emoji** (consistent with `CUSTOM_CATEGORIES_DESIGN_BRIEF` and the
`CategoryEmojiCandidates` curation). This is purely about offering a **bigger,
organized, searchable** pool.

## Why / the goal
- Custom categories currently reuse only the built-in emoji vocabulary (~45).
- Expand the selectable pool to a **curated ~150–250 emoji**, grouped into
  **sections** with a **search box**, so any custom category gets a good icon in
  a couple of taps.
- Keep it emoji: native, free, identical on Android + iOS (parity), no asset
  pipeline, no libraries.

## Match these existing components for style
- **`Custom Category Picker Variants.dc.html`** — the create/edit view (View B)
  with the **live preview tile**, name field, 12-swatch color row, and the
  **5-per-row emoji icon grid**. We're replacing that short grid with the
  expanded, sectioned, searchable one — everything else in View B stays.
- **`CategoryPickerScreen.dc.html`** — reuse its **search field** styling for the
  emoji search input.
- Phone width **300**, scrollable; `font-family: Roboto`; Material 3 (Material
  You), not iOS.

## The expanded grid
- **Search field** pinned at the top of the icon area (same look as the picker's
  search). Typing filters the grid live by keyword (e.g. "car," "food," "gym").
  Show a **no-results** state.
- **Sectioned grid** when not searching: light sub-headers grouping the emoji —
  e.g. **Food & Drink · Transport · Home · Shopping · Health · Leisure & Hobbies
  · Money & Work · Animals & Nature · Symbols**. 5 tiles per row, ~44–48px
  rounded tiles.
- **Each tile shows the emoji on the currently-selected category color** (as
  today) so the grid previews the final look; the selected emoji keeps the
  ring/tile highlight.
- The grid **scrolls within the sheet** — the header / live preview / name field
  / color row stay put; only the emoji area scrolls.

## Design tokens (CSS vars already in the project)
- Surfaces `--bg`, `--sc`, `--sch` (tiles) · Text `--on`, `--onv` (section
  sub-headers, muted) · Accent/selection `--primary`, `--secc`/`--onsecc`
  (selected ring) · Lines `--outv`. Token-driven only, light **and** dark.

## States to draw
1. **Browse** — sectioned grid, nothing typed, one emoji selected (ring), preview
   tile reflecting it.
2. **Searching** — a query in the field, filtered results, the match highlighted.
3. **No results** — empty query state.
4. **Tablet** — the same expanded picker inside the **centered-dialog** create
   view.
All in **light + dark**.

## Output
- New file, e.g. **`EmojiPickerExpanded.dc.html`** — don't overwrite the existing
  picker / variants files.
- Token-driven, light + dark, phone 300 + tablet dialog.

## Implementation notes (for after the mockup)
- `Categories.iconChoices` stops being `predefined.map { emoji }.distinct()` and
  becomes a **curated constant list (~150–250)** with a small in-code **keyword
  index** for search (native `contains`, no library). Sections are just an
  ordered grouping of that list.
- The pool should **include the final built-in category emoji** (post-
  `CategoryEmojiCandidates` curation) so built-ins and customs share vocabulary.
- Same **emoji rules as the curation pass**: single-codepoint, widely supported
  (Unicode ≤ ~13.1), **no ZWJ / skin-tone / gender / profession sequences** (they
  fall back to "tofu" on older Android). Language-independent (no text in glyphs).
- This **supersedes** the "icon grid = the ~45 built-in emoji" line in
  `CUSTOM_CATEGORIES_DESIGN_BRIEF` — same view, bigger searchable pool. **iOS
  mirrors the same list.**

Thanks!
