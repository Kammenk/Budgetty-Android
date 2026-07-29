# Claude Design request — Insights pie: icons + % in the legend

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create new `*.dc.html` mockups in
> that project; once they're there, Claude Code reads them back via DesignSync
> and implements the approved one.

---

Hi! A readability tweak to the **Insights spending donut**. Right now the compact
**legend** under the donut shows only a **color dot + category name + amount** —
no icon, no percentage — while the separate **"See all categories"** sheet
already shows the richer **emoji tile + name + amount + %**. Please bring that
richer treatment into the **inline legend** and the **donut center**, so
categories are identifiable at a glance and the % is right there.

## Why / the goal
- The donut already draws a per-slice **%** on the ring, and the "See all" sheet
  already shows emoji + %. The **inline legend** is the weak spot — a bare color
  dot is hard to map back to a category. Adding the **emoji** and the **%** closes
  the gap.
- No new data — every category already has an emoji + color the app looks up by
  name.

## Match these existing components for style
- **`InsightsScreen.dc.html`** — the **BreakdownCard** (the donut + the
  two-per-row inline legend + the group/expand toggle) and its **"See all
  categories"** sheet (the `emoji tile + name + amount + %` rows). We're making
  the inline legend a compact version of those sheet rows.
- Phone width **300**, `font-family: Roboto`, Material 3.

## The changes
1. **Legend rows** — replace the 10dp color dot with a **small colored emoji
   tile** (emoji on the category color, ~24–28px rounded), then the **name**, then
   a right-aligned **% + amount** (percentage prominent, amount muted). Keep the
   existing **two-per-row** grid.
2. **Donut center (on tap)** — when a slice is selected the hollow center already
   shows *name + amount + "% of spend"*; add the **category's emoji** above the
   name. The default (nothing selected) center stays **"Total" + amount**,
   unchanged.
3. **Slices themselves** — leave the existing on-ring **% leader labels as-is**;
   do **not** put emoji on the slices (thin slices get cramped).
4. **Grouped / roll-up mode** — when the breakdown toggle is in "grouped" mode,
   the legend tiles show the **group's** emoji + color (groups have their own
   emoji).

## Design tokens
- Tile fill = the category's own color (already provided per slice); text `--on`
  / muted `--onv`; `--primary` accent; lines `--outv`. Token-driven, light +
  dark.

## States to draw
1. **Default** — donut + enriched legend (emoji tile + % + amount per row), center
   = Total.
2. **Slice tapped** — one slice emphasized; center shows that category's **emoji +
   name + amount + %**.
3. **Grouped mode** — legend rolled up to the 7 groups (+ custom primaries later),
   group emoji + color.
All **light + dark**.

## Output
- New file, e.g. **`InsightsPieLegend.dc.html`** — don't overwrite
  `InsightsScreen.dc.html`.

## Implementation notes (for after the mockup)
- Color is already carried on `PieSlice`; the emoji is re-derived by label via
  `Categories.emojiOf()` (the "See all" sheet already does exactly this) — so the
  legend row (`SliceRow`) just gains the tile + %, and the center overlay gains
  the emoji. The % uses the already-computed period total (`donutPercents`). **No
  model change.**
- Trivial **iOS mirror** (its donut legend gets the same tile + %).

Thanks!
