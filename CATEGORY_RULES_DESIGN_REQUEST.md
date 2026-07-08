# Claude Design request — Category memory (learned name → category rules)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I've added a **"category memory"** feature to Budgetty and need mockups for
its one new visible surface — a confirmation dialog on the **Review & edit**
screen. Please mock up the screens below as new `*.dc.html` files in this
project, matching the existing design system exactly.

## What the feature does (context)
Budgetty learns how the user categorizes items. When the user changes a scanned
item's category on the Review & edit screen, and other saved transactions share
that item's name, the app offers to (a) recategorize all of them at once and
(b) remember the choice so future receipts auto-file that item under it. This
request covers only the **moment right after a category is picked** in the
Category picker (§5d): a confirmation dialog appears over the Review & edit list
(§5c). It only appears when other same-name transactions exist — brand-new item
names never trigger it.

Per the brief, confirmations are **dialogs, not bottom sheets** — so this is a
centered Material 3 dialog with a scrim, matching the app's other confirmation
dialogs.

## Match these existing components for style
- **Review & edit screen** (brief §5c — the "Review & edit before saving" screen
  with the editable item cards and the emoji category field). This is what the
  dialog floats over; show it dimmed behind the scrim for context.
- **Category picker sheet** (brief §5d) — the dialog is the step *immediately
  after* this.
- Categories render everywhere as **color dot + emoji + name** in the category's
  muted color — reuse that treatment for the target category inside the dialog.
- Buttons are **fully-rounded pills**. Phone preview size **300×620**,
  `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Screens to design

### 1. `CategoryRulePromptScreen.dc.html` — the prompt (primary state)
A centered confirmation dialog over the dimmed Review & edit screen. Contents:
- **Title:** "Apply to matching items?"
- **Body:** "12 other transactions named "Bananas" will be set to 🛒 Groceries."
  Render the target category as color-dot + emoji + name in its own color.
- **A checkbox row** (checked): "Always use 🛒 Groceries for "Bananas"" — the
  "remember for future receipts" control. Make it a clearly tappable full-width row.
- **Two pill buttons** in the M3 dialog action area:
  - text/secondary **"Just this one"**
  - filled primary **"Update all 12"**
- Float the dialog over a recognizable slice of the Review & edit list (an item
  card mid-edit) behind the scrim, so the context reads.

### 2. `CategoryRulePromptSingleScreen.dc.html` — single match + remember off
The same dialog, in its low-count / opted-out variant, so we can see:
- Singular copy: "1 other transaction named "Oat milk" will be set to 🥛 Dairy."
- The checkbox **unchecked**.
- Primary button label for the N=1 case — "Update all 1" reads awkwardly; if you
  think a cleaner singular ("Update both" / "Update the other one") works better,
  mock it and note your recommendation.

## Optional / stretch (nice to explore, not required)

### 3. `ScannedItemRuleHintScreen.dc.html` — the "auto-applied" indicator
The Review & edit screen right after a scan, where one item's category was set
automatically from a saved rule. Show a **subtle, non-nagging hint** on that
item card (e.g. a small label/badge near the category field like "from your
preferences"). This is currently silent in the app — we'd like to see a
light-touch option that builds trust without nagging.

### 4. `SavedRulesScreen.dc.html` — manage saved rules (future)
A Settings/Account list of remembered rules. Each row: color-dot + emoji +
category, the item name it applies to, and a way to remove it. We shipped
**inline-only** management (no such screen), so this is purely exploratory for a
possible future iteration.

## Copy (current build — tweak if a shorter phrasing reads better)
- Title: **Apply to matching items?**
- Body: **%d other transactions named "{name}" will be set to {emoji category}.**
- Checkbox: **Always use {emoji category} for "{name}"**
- Primary: **Update all %d**
- Secondary: **Just this one**

Note: the app is localized into 21 languages (German and Bulgarian run long, and
item names may be long or Cyrillic) — please let the title, body, and checkbox
wrap gracefully rather than truncate.

## Already covered — please DON'T redo
- The **Review & edit screen** (§5c) and **Category picker sheet** (§5d) already
  exist — build the dialog on top of them; don't recreate them.

## Output
- Save each as a new `*.dc.html` file in this project (don't overwrite existing
  screens).
- Keep them dark/light-token driven (use the CSS vars above, no hard-coded grays).
- Phone first (300×620). Once I approve these, I'll ask for matching tablet
  variants (the app already centers dialogs on tablet).

Thanks!
