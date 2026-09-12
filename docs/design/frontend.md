# Qvety frontend design reference

How the brand in `brand.md` is applied to screens, components, and behavior. Read this before building any Angular screen. The pixel reference for everything below is `canvas/Qvety App Screens.dc.html`, eight screens stacked top to bottom at 1280 px wide.

The canvases are prototypes that show intended look, density, and behavior. They are not production code. Each screen is recreated with NG-ZORRO components themed from `tokens/qvety-theme.less`, Transloco strings, and `dir` switching on the document root. Parts 02 onward each build the screens they own; nothing here is built ahead of its part.

## Fidelity

High. Colors, type sizes, weights, spacing, and copy are final and should be matched. The two exceptions are in the open items in `brand.md`.

## Spacing, radius, elevation

- **Border radius is 0 everywhere.** Override NG-ZORRO's `@border-radius-base`.
- **No shadows.** Separation is done with 1 px `line` rules inside a surface and 2 px rules between major regions. Nothing floats.
- Grid gutters between cells are 2 px of `line` color showing through a grid gap: cells are white, the gap is the rule.
- Page padding 24 to 28 px. Card padding 22 to 26 px. Table cells 11 px × 16 px.

## Application chrome

- **Header**: 32 px `primary` strip. Mark at 18 px white, clinic name, then language toggle and user name pushed to the trailing edge. No wordmark.
- **Sidebar**: 196 px white with a 2 px `line` edge. Active item has a `primary-light` fill and a 3 px `primary` bar on its leading edge. Collapsed: mark only at 24 px.
- **Page title row**: 20 px / 600 title, date or context beside it, primary action button at the trailing edge.
- **Stat cells**: a row of white cells on `line` with 2 px gutters. Money and reminder counts in `accent-text`; clinical counts in `ink`.

## Tables

- Header row on `table-head`, 12 px / 500 `ink-muted` labels, 2 px bottom rule.
- 1 px `line` rules between rows.
- Hover fill `canvas`. Selected or in-progress row fill `primary-light`.
- Status is a 12 px tag: teal for clinical, green for done, amber for money and reminders, grey for waiting.
- Archived rows are entirely `ink-muted` with a `danger` tag.
- Phone numbers, IDs, and invoice numbers in IBM Plex Mono 13 px. Every table has `tabular-nums`.

## Buttons and controls

| Kind | Rest | Hover and pressed |
|---|---|---|
| Primary | `primary` fill, white text | `primary-hover` fill |
| Secondary | white fill, 1 px `line` border | `canvas` fill |
| Outlined | white fill, 1 px `primary` border, `primary` text | `primary-light` fill |
| Disabled | `line` fill, `ink-muted` text, `cursor: not-allowed` | no change |

- Focus: every interactive element gets `outline: 2px solid #2F6E6A; outline-offset: 2px`. Never the browser default.
- Search input: fixed 260 px, 1 px `line` border, Ant search icon.
- Filter chips: selected chip is solid `primary`; the rest are white with a `line` border.
- Segmented control (payment method): a flex row with 2 px gaps; the selected segment is solid `primary`.
- Tabs: active tab is `primary` with a 2 px `primary` underline that overlaps the strip's own 2 px `line` rule.

## Direction and language

- `dir` on the document root follows the user's language preference. Layout, tables, forms, sidebar edge, and print sheets all mirror.
- The mark and object icons never mirror. Directional icons (back, next, send) do.
- Email, password, phone, and every numeric or Latin value stay `dir="ltr"` and left-aligned inside an Arabic layout, set in IBM Plex Sans. Labels around them are RTL.
- Western digits only, both languages.
- No animation beyond NG-ZORRO defaults.

## The eight screens

### 1. Login (Arabic)

Full-bleed `teal-soft` ground. A 400 px white card, centered, 44 px padding. Stacked lockup at the top (52 px mark over 30 px wordmark), a 2 px `line` divider, two fields, a full-width primary button, and a footer row with "forgot password" and the language switch. Inputs stay LTR.

Owned by part 03.

### 2. Today's appointments (Arabic, the primary screen)

Header and sidebar as above. Page title row with the date and a "new appointment" primary button; four stat cells; then the appointments table. The in-consultation row is highlighted `primary-light`.

Owned by part 10.

### 3. Patient list (English)

Same chrome mirrored to LTR. Search input and a filter chip row above the table. Phone numbers in mono. One archived row shown for the muted treatment.

Owned by part 08.

### 4. Patient record (English)

Breadcrumb, then a white summary card in three columns: identity (name, recall tag, species / sex / age / weight, mono patient ID), owner (name, mono phone, an outlined "Open WhatsApp" button), and actions. Below it a tab strip. Body is a 2:1 grid: left, the visit timeline, each entry a title row with date and vet at the trailing edge, then the note at 14 px / 1.6, separated by 1 px rules; right, a rail with "next due" in an `accent-light` block, weight history, and the balance at 24 px / 600.

Owned by parts 08 and 13.

### 5. Invoice and payment (Arabic)

2:1 grid. Left, the invoice: header with mono invoice number and an "unpaid" amber tag, a four-cell metadata grid, the line-item table (item / qty / amount), and a 260 px totals block at the trailing edge with a 2 px `ink` rule above the total. Right, the payment panel: three-way segmented control for method, amount field at 18 px / 600, change due in a `primary-light` block, two stacked buttons. Arabic labels RTL; every number and Latin value LTR.

Owned by part 14.

### 6. Vaccination certificate (print, black only)

Shown as an LTR and an RTL sheet. Pure black on white: no teal, no grey fills, nothing that dies on a fax-quality printer or a photocopy. Clinic name block and mono certificate number, a 2 px black rule, 26 px title, a definition list on a 150 px label column, signature line, and the "powered by Qvety" footer with the black mark at 14 px. When a clinic has uploaded its own logo it takes the leading corner in place of the name block.

Owned by part 13.

### 7. Empty states

Three cells at 2 px gutters. Each is a 72 px `teal-soft` square with the white mark at 40 px, a 16 px / 600 title, one sentence of body at 14 px capped to about 30 characters, and one or two buttons. If flat illustrations are drawn later they drop into the same 72 px slot with no layout change.

Used from part 06 onward.

### 8. Platform admin (Qvety staff)

Identical chrome plus the `platform-admin` shield beside the mark in the header and a "Staff area" tag beside the page title. Clinic table with plan, seats, last active, and status. Nothing else changes; the shield alone is the signal.

Owned by part 11.

## Client state

Nothing beyond what the screens imply: current language and direction (per user, persisted via `PATCH /api/v1/me`), the selected filter chip on the patient list, the active tab on the patient record, the payment method segment on the invoice, and the live "change due" derived from amount received minus invoice total.

## Wiring the tokens (part 05)

1. Import `docs/design/tokens/qvety-theme.less` after `ng-zorro-antd/ng-zorro-antd.less` in the Angular styles entry. The file is copied into `web/src/styles/`, not referenced from `docs/`.
2. Load `qvety-tokens.css` globally so components can use `var(--qv-*)` for anything NG-ZORRO does not cover.
3. Self-host IBM Plex Sans, Plex Sans Arabic, and Plex Mono under `web/public/fonts/`. Clinics do not always have reliable internet; no font CDN.
4. Replace the Angular default `favicon.ico` reference in `index.html` with `favicon.svg` first and `favicon.ico` as fallback, and add the apple-touch and manifest icons.

## Review checklist for any new screen

- Zero hard-coded strings; everything from `web/src/assets/i18n/*.json`.
- Checked in `ar` (RTL) and `en` (LTR). Mark and object icons not mirrored.
- Radius 0, no shadows, rules not borders-with-radius.
- Money and reminder states amber; clinical states teal or green; disabled grey.
- Numbers Western, tabular, LTR inside Arabic.
- Keyboard focus ring visible on every control.
- Print views black only.
