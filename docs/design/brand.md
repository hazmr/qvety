# Qvety brand

The visual identity: mark, wordmark, color, type, icon rules, and the finished asset files. For how the identity is applied to screens and components, read `frontend.md` in this folder.

Qvety is practice management software for Egyptian veterinary clinics. Users are receptionists, vets, and clinic owners on shared desktop PCs and personal phones. Arabic is the default language; English is a per-user option. The interface is Angular with NG-ZORRO (Ant Design), so the visual language must sit comfortably next to Ant Design components.

## Personality

Three words, in order of importance: **calm, clear, trustworthy.**

- Calm: a clinic front desk is loud and busy. The screen should be the quiet part of the room.
- Clear: staff scan for the next patient, the unpaid invoice, the overdue vaccine. Nothing decorative competes with data.
- Trustworthy: owners bring sick animals. The brand should feel medical without feeling cold, and warm without feeling childish.

Avoid: cartoon animals, paw-print wallpaper, gradients, glossy 3D, stock "vet with puppy" imagery, anything that reads as a pet shop rather than a clinic.

## The mark: "Cut Q"

A solid disc with the counter and the tail both cut out of it as negative space. One shape, one color, no inner detail. Chosen from four candidates; the others are in `canvas/Qvety Logo Explorations.dc.html`.

```text
viewBox 0 0 64 64
  disc     circle cx=31 cy=30 r=23
  counter  circle cx=31 cy=30 r=8.5            (cut out)
  tail     line (31,30) -> (50,49), width 9,   (cut out)
           round cap, exits the bowl at 45 degrees
content bbox: x 8, y 7, w 46.5, h 46.5
```

Rules:

- **Never mirrored.** Only the order of mark and wordmark changes between LTR and RTL. A Q with its tail flipped is a different letter.
- Clear space on all sides equals the height of the bowl.
- Minimum 16 px on screen, 6 mm in print. There is no smaller variant.
- Minimum wordmark height 12 px on screen, 4 mm in print; below that, mark only.
- No gradient, no two-tone, no outline, no shadow, nothing inside the bowl.
- Never combined with a cross, stethoscope, heart, or heartbeat line.

## Wordmark

- Text: `Qvety`, always Latin, capital Q, lowercase rest. No Arabic transliteration. The Arabic interface shows the Latin wordmark; this is normal for software brands in Egypt.
- Typeface: IBM Plex Sans Medium, tracking `0.8` at 40 px. See open item 1: the stock Plex Q does not match the mark and needs redrawing.
- Optional Arabic descriptor under the wordmark for print and marketing only: `إدارة العيادات البيطرية`. Never in the app header.

## Lockups

| Lockup | Layout | Used in |
|---|---|---|
| Horizontal LTR | mark left, wordmark right | English UI, English print |
| Horizontal RTL | mark right, wordmark left | Arabic UI, Arabic print |
| Stacked | mark above wordmark, centered | login page, splash, social avatars |
| Mark only | just the mark | favicon, app icon, collapsed sidebar, print footer |

Color variants of every lockup: `primary` teal on white (default); white on `primary` (dark headers) and white on `teal-soft` (app icon, login); pure black on white (invoices, certificates, receipts, photocopies); pure white on transparent (overlays, future dark mode).

## Color

Primary is a muted teal, taken from the tone Apple used on the iPhone 16 "Teal" body: dark teal from the camera ring for anything clickable, soft teal from the body for large surfaces. It sits between blue (trust) and green (health), the two colors clinics already use in their waiting rooms, and it is greyed enough to stay calm on a screen that is open all day. The accent is a warm amber used sparingly for money and reminder states so financial states stand out from clinical states.

All values pass WCAG AA (4.5:1) for normal text on white unless noted.

| Token | Hex | Use |
|---|---|---|
| `primary` | `#2F6E6A` | buttons, links, active nav, the mark. 5.3:1 on white |
| `primary-hover` | `#245855` | hover and pressed |
| `primary-light` | `#E1EEEC` | selected rows, tag fills; text on it in `primary` |
| `teal-soft` | `#8FB5B0` | login ground, app icon ground, empty-state tiles. Never behind text under 20 px |
| `accent` | `#D9822B` | money badges, unpaid, recall due. Fills and icons only; 2.9:1, not small text on white |
| `accent-light` | `#FBEFE2` | accent grounds |
| `accent-text` | `#8A4F14` | text on `accent-light` |
| `ink` | `#1F2933` | body text |
| `ink-muted` | `#616E7C` | secondary text, placeholders, disabled labels |
| `line` | `#D9DEE3` | borders, dividers, **and disabled fills** |
| `surface` | `#FFFFFF` | cards, tables |
| `canvas` | `#EEF3F2` | page ground behind cards |
| `table-head` | `#F6F9F8` | table header row fill |
| `success` | `#1E8449` | paid, vaccinated, completed. Tint `#E6F2EB` |
| `warning` | `#B7791F` | overdue, expiring, pending. Tint `#FBEFE2` |
| `danger` | `#C0392B` | errors, archived, rejected. Tint `#FBE9E7` |
| `info` | `#2F6E6A` | same as primary; do not introduce a second blue |

Two rules that are easy to get wrong:

1. **Disabled controls are grey, never pale teal.** `line` fill with `ink-muted` text. On a cheap front-desk monitor a lighter teal is indistinguishable from live teal.
2. **Teal and green carry clinical state; amber carries money and reminders.** A receptionist should tell which kind of problem a row has without reading it.

Dark mode is out of scope for the pilot. The white-on-transparent logo variant exists so it stays possible later.

The same values live as NG-ZORRO Less variables in `tokens/qvety-theme.less` and as CSS custom properties in `tokens/qvety-tokens.css`. They are wired into the Angular build in part 05.

## Typography

Arabic is primary, so the Arabic face was picked first and Latin matched to it.

- Arabic: **IBM Plex Sans Arabic**, weights 400, 500, 600.
- Latin: **IBM Plex Sans**, weights 400, 500, 600. Drawn to the same proportions as Plex Arabic, so mixed lines do not jump in x-height.
- Mono (invoice numbers, patient IDs, phone numbers only): **IBM Plex Mono**, weights 400, 500.

Nothing lighter than 400, nothing heavier than 600.

| Size | Role |
|---|---|
| 20 px | page title, weight 600 |
| 18 px | emphasis numbers in panels, weight 600 |
| 16 px | section title, weight 600 |
| 14 px | body, Arabic and Latin alike. Do not bump Arabic up |
| 13 px | table meta, buttons |
| 12 px | secondary text, tags, table headers |

**Numbers are always Western Arabic digits (0-9), in both languages**, never Eastern Arabic (٠-٩). Invoices, phone numbers, and weights must match the receipt printer, the bank, and the client's phone. `font-variant-numeric: tabular-nums` on every table and every money column.

## Iconography

Generic icons come from the **Ant Design outlined set** and are not redrawn: search, edit, print, calendar, user, and so on.

Custom icons are drawn on a 24 px grid, 1.5 px stroke, rounded caps and joins, outlined, single color, so they sit next to Ant icons without looking foreign.

| Icon | Meaning | Status |
|---|---|---|
| `weight` | weight entry; flat scale platform | drawn, `<symbol>` in `canvas/Qvety Brand Guidelines.dc.html` |
| `patient-archived` | archived patient; folder with a diagonal strike | drawn, same file |
| `platform-admin` | Qvety staff area; shield with the Q tail | drawn, same file |
| `species-dog` | head silhouette, side profile, no breed detail | not drawn |
| `species-cat` | head silhouette, ears up | not drawn |
| `species-bird` | side profile, perched | not drawn |
| `species-other` | generic paw pad, four toes | not drawn |
| `vaccine` | syringe, single barrel, no drop | not drawn |
| `recall` | bell with a small clock, not a phone | not drawn |
| `whatsapp-open` | official WhatsApp glyph, one color, never restyled | use as-is |

Directional icons (back, next, send) mirror in RTL. Object icons (syringe, scale, animal heads) do not.

## Asset files

Finished files, already in the repo under `web/public/`. SVG is the source of truth; PNGs are exports. All SVGs have `viewBox` set and no fixed `width`/`height`, so they scale from a CSS box.

| File | Content |
|---|---|
| `brand/mark.svg`, `mark-white.svg`, `mark-black.svg` | mark only, three colorways |
| `brand/wordmark.svg` | wordmark, ink |
| `brand/lockup-ltr.svg`, `lockup-rtl.svg` | horizontal, primary teal |
| `brand/lockup-ltr-black.svg`, `lockup-rtl-black.svg` | horizontal, print |
| `brand/lockup-stacked.svg`, `lockup-stacked-white.svg` | stacked; login, splash, avatars |
| `brand/print/logo-black.png` | 1200 px, transparent, invoice and certificate footer |
| `favicon.svg` | teal on transparent |
| `favicon.ico` | 16, 32, 48 packed from `favicon.svg` |
| `apple-touch-icon.png` | 180×180, white mark on `primary`, opaque, square (iOS rounds it) |
| `icons/icon-192.png`, `icon-512.png` | white mark on `teal-soft` |
| `icons/icon-maskable-512.png` | same, mark inside the central 80% safe zone |
| `og-image.png` | 1200×630, stacked lockup on `canvas` |

## Where the mark appears

| Place | Treatment |
|---|---|
| App header | 32 px strip, mark only at 18 px, white on `primary`. No wordmark in either language |
| Login | stacked lockup, about 120 px wide, on a `teal-soft` ground |
| Collapsed sidebar | mark only at 24 px, `primary` |
| Invoice and certificate | clinic's own uploaded logo in the leading corner (right in Arabic, left in English). Qvety black mark in the footer at 6 mm as "powered by" |
| Platform admin | mark plus the `platform-admin` shield beside it, so staff screens are never mistaken for practice screens |
| WhatsApp message | no logo, text only. The server never sends anything |

## Design canvases

`canvas/*.dc.html` are the Claude Design artboards this brand was produced in. Open any of them in a browser; `support.js` must sit beside them. They are references, not production code.

- `Qvety Brand Guidelines.dc.html`: mark, color, type, icons, placement rules.
- `Qvety App Screens.dc.html`: the eight application screens described in `frontend.md`.
- `Qvety Platform Site.dc.html`: the marketing site. Pricing is placeholder.
- `Qvety Logo Explorations.dc.html`: the four candidates; "Cut Q" was chosen.

## Open items

1. **The wordmark is live IBM Plex Sans text.** Plex's Q has a short straight tail and does not match the mark's 45° void. The Q needs redrawing by hand, and all wordmark text in the lockup SVGs converting to outlines, before those files ship. Until then the lockups render in whatever fallback font the viewer has.
2. **Six custom icons are specified but not drawn**: the four `species-*`, `vaccine`, and `recall`.
3. **Pricing on the marketing site is placeholder.** Replace before launch.
4. **Login and empty-state illustrations** are optional and not drawn. The 72 px `teal-soft` tile with the white mark is the fallback and is fine on its own.
