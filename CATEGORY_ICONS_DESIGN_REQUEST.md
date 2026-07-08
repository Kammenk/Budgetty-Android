# Claude Design request — Category emoji (batch 2: 20 remaining + 2 new)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create a new `*.dc.html`
> contact sheet in that project; once it's there, Claude Code reads it back via
> DesignSync and updates the chosen emoji in `Categories.kt`.

---

Hi! In Budgetty every spending **category** is shown as an **emoji + a colored
dot + its name** (transaction rows, the pie legend, history, widgets, and the
category picker). We're doing a curation pass over the emoji to make them
clearer and more consistent. The first batch covered the 7 umbrella groups plus
the **Groceries** and **Household & Personal** sub-categories. **This request is
batch 2: the remaining 20 categories, plus 2 brand-new ones — Video Games and
Investments — for 22 in all.**

## What I need — a candidate contact sheet (NOT drawn artwork)

To be clear up front: I am **not** asking for custom/vector icons. The app keeps
using **system emoji**. Your job is **curation** — for each of the 22 categories
below, show **2–3 candidate emoji** as tiles (the current pick plus alternatives;
the two **new** categories have no current emoji, so show candidates only) so I
can judge them at real size and pick. Please **mark one recommended pick** per
category (a ring/check on the tile). Render the whole set on **light and dark**
so I can see how the glyphs hold up.

## Match these existing components for style
- **`CategoryPickerScreen.dc.html`** — categories render as **color-dot + emoji +
  name**; that's the in-context look I'm evaluating against.
- **`Custom Category Picker Variants.dc.html`** — reuse its **emoji icon-grid
  tile** (≈44–48px rounded tile, `--sch` fill) for each candidate.
- `font-family: Roboto`. Phone width **300**, scrollable (the sheet is tall).

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant — muted labels)
- Accent / selection: `--primary`, `--secc` / `--onsecc` (use for the "recommended" ring)
- Lines: `--outv` (dividers), `--outline`
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

Each group's real category color (use it for the group sub-header + the color dot):
`Health & Wellness` **#5BB6A6** · `Dining & Entertainment` **#E0795B** ·
`Shopping & Lifestyle` **#AE72CC** · `Transportation` **#D08A4A** ·
`Services & Subscriptions` **#588AC7** · `Other` **#9A93A6**
(Sub-category dot colors are generated in-app, so a neutral `--sch` tile is fine
for the candidates — I'm judging the emoji, not the color here.)

## The 22 categories (batch 2) — current emoji + my candidate ideas
Grouped under their parent. Current pick is marked **(now)**; **(NEW)** marks the
two brand-new categories (no current emoji — pick from scratch). The rest are my
starting ideas — **please beat them where you can**.

### ❤️ Health & Wellness
- **Health & Pharmacy** — 💊 (now) · ⚕️ · 🩹
- **Medical** — 🏥 (now) · 🩺 · ⚕️
- **Sports & Fitness** — 🏋️ (now) · 💪 · 🏅  ← 🏋️ is a gendered ZWJ sequence; prefer a single-codepoint pick

### 🍽️ Dining & Entertainment
- **Restaurant & Dining** — 🍴 (now) · 🍔 · 🥡
- **Entertainment** — 🎟️ (now) · 🎬 · 🍿
- **Video Games** *(NEW)* — 🎮 · 🕹️ · 👾

### 🛍️ Shopping & Lifestyle
- **Clothing & Accessories** — 👗 (now) · 👕 · 👜
- **Electronics** — 🔌 (now) · 💻 · 📱  ← 🔌 reads weakly; a device may be clearer
- **Garden & Plants** — 🌱 (now) · 🪴 · 🌿
- **Home Improvement** — 🛠️ (now) · 🧰 · 🔨
- **Tobacco & Alcohol** — 🍷 (now) · 🍺 · 🚬

### 🚗 Transportation
- **Fuel** — ⛽ (now) · 🛢️
- **Car Maintenance** — 🔧 (now) · 🛞

### 📋 Services & Subscriptions
- **Subscriptions & Services** — 🔔 (now) · 🔁 · 💳
- **Education** — 📚 (now) · 🎓 · ✏️
- **Travel & Accommodation** — ✈️ (now) · 🧳 · 🏨
- **Insurance & Utilities** — ⚡ (now) · 🛡️ · 💡  ← two concepts; ⚡ only covers "utilities"
- **Rent** — 🏘️ (now) · 🔑 · 🏢
- **Office & Work Supplies** — 📎 (now) · 🗂️ · 🖊️
- **Gifts & Charitable Donations** — 🎁 (now) · 🫶 · 🤝
- **Investments** *(NEW)* — 📈 · 🪙 · 🏦

### 📦 Catch-all
- **Other** — 📦 (now) · 🏷️

## Rules for the picks
- **Single-codepoint & widely supported** (Unicode ≤ ~13.1). **Avoid
  gender/skin-tone/profession ZWJ sequences** (e.g. 🏋️‍♀️, 🧑‍🌾) — they render
  inconsistently across Android versions and can fall back to "tofu."
- **Distinct** — no duplicate emoji within these 22, and none that collide with
  the emoji already assigned to the other 26 categories (reserved list below).
- **Neutral & legible** — gender- and skin-tone-neutral, and readable at ~24px
  on the colored dot.
- The app is localized into 21 languages, but emoji are language-independent —
  no text inside the glyphs.

## Reserved — already used by batch 1, do NOT reuse
Group tiles: 🧺 🏠 ❤️ 🍽️ 🛍️ 🚗 📋
Groceries: 🥖 🧀 🍗 🐟 🥬 🍫 🧊 🥜 🥫 🍝 🧂 🥤
Household & Personal: 🧼 🧴 💇 🍼 🐾 📄 🍽️
(Note: 🍽️ is currently used twice — the Dining group tile **and** Kitchen
Supplies. If you spot a cleaner split, flag it, but it's out of scope here.)

## Output
- **One new file**, e.g. `CategoryEmojiCandidates.dc.html`, in this project
  (don't overwrite existing screens).
- One category **per row**, grouped under its parent with a colored sub-header;
  each row = name + the current tile + the candidate tiles, with your
  **recommended** pick ringed.
- Token-driven, **light + dark**. Phone width **300**, scrollable.

Thanks!
