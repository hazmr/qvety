# Handoff: Qvety mobile (phone) screens

Companion to `README.md` in this bundle. All tokens, type, colour rules and the mark
spec live there — this document covers only what is different on a phone. Read the
README first.

Reference design: `design/Qvety App Screens.dc.html`. The eight desktop screens are at
the top of that file; the five phone screens are in the second block, under the heading
"The phone is not a narrow desktop". They are drawn at **390 × 780** with a 2 px outline
standing in for the device edge — that outline is not part of the design.

Fidelity: **high.** Sizes, spacing and copy are final.

## Who is on a phone, and why it matters

Staff use their own phones, mostly away from the desk — a vet checking a record in the
consult room, a receptionist sending a recall from the corridor. The phone is not a
smaller version of the front-desk screen; it is a different set of tasks done standing
up, often one-handed. Four rules follow.

1. **Tables become stacked rows.** A six-column table cannot be read at 390 px.
2. **The sidebar becomes a bottom bar.** A thumb reaches the bottom of a phone, not the top.
3. **Every target is at least 44 px.** 48 px for anything in a fixed action bar.
4. **The 32 px brand strip stays 32 px.** It is chrome, not a control.

## Layout skeleton

Every phone screen is a column of four fixed parts plus one scroller:

```
32 px   brand strip     primary fill, white mark at 17 px, clinic name, user initials
48 px   title bar       surface, 2px line rule below; page name + its ONE action
(opt)   filter/tab row  surface, 2px line rule below, cells at min-height 44 px
flex    scroll region   the only scrolling element on the screen
60 px   bottom nav      OR a 48 px action bar — never both on the same screen
```

Rules:

- The brand strip and title bar do not scroll. Use `position: sticky` or a flex column
  with `overflow` on the scroller only — not `position: fixed`, which fights the iOS
  keyboard.
- **Bottom nav and action bar are mutually exclusive.** Top-level destinations (Today,
  Patients, Invoices) get the nav. Detail screens (patient record, invoice) get the
  action bar and a back chevron in the title bar instead.
- The title bar holds exactly one action. If a screen needs more, they go in the action
  bar at the bottom.
- Respect the safe-area inset at the bottom:
  `padding-bottom: env(safe-area-inset-bottom)` on the nav and the action bar.

## Size changes from desktop

Only these. Everything else in the type scale is unchanged.

| Element | Desktop | Phone | Why |
|---|---|---|---|
| List row title | 14 px | 15 px / 600 | Read at arm's length, standing |
| List row secondary | 13 px | 13 px | unchanged |
| Text input | 14 px | **16 px** | Below 16 px, iOS zooms the viewport on focus |
| Input height | ~38 px | 52 px | Thumb target |
| Button height | ~38 px | 44 px inline, 48 px in the action bar | Thumb target |
| Tag / state pill | 12 px | 11 px | Trailing-edge pills must not push the title |
| Nav label | 14 px | 10 px | Icon carries it; label confirms |
| Bottom nav height | — | 60 px | Icon 20 px over a 10 px label |

Page padding drops from 24–28 px to **14 px**. Card padding drops to 14 px. The 2 px
inter-region rules and 1 px row rules are unchanged — do not soften them on mobile.

## The five screens

### 09 — Today's appointments (Arabic, the primary phone screen)

Brand strip, then a title bar with the page name over the date at 11 px and a compact
"موعد" button (36 px, icon + label). Below that a three-cell filter row — All / Waiting /
Unpaid, with counts — each cell 44 px minimum, the active one carrying a 2 px `primary`
underline that overlaps the row's own 2 px rule.

The list is the important part. Each row:

```
[ time 46px fixed ] [ patient + owner·reason, stacked, flex:1 ] [ state tag, flex:none ]
```

- Time: 15 px / 600, IBM Plex Sans (Latin) even in the Arabic layout, fixed 46 px column
  so the times align down the list.
- Patient: 15 px / 500. Owner and reason on one 13 px `ink-muted` line joined by " · ".
- State tag: 11 px / 500, 3 × 8 px padding, `flex: none`. Same colour logic as desktop —
  teal for clinical, green for done, amber for money and reminders, grey for waiting.
- Row padding 13 × 14 px, 1 px `line` rule between rows, `min-height: 44px`.
- The in-consultation row keeps the `primary-light` fill.
- Money amounts inside a tag switch to IBM Plex Sans for the digits.

Bottom nav: the same five destinations as the desktop sidebar. Active cell is
`primary-light` fill, `primary` icon and label, and a 3 px `primary` bar on the **top**
edge of the cell (the desktop's leading-edge bar, rotated).

### 10 — Patient record (Arabic)

Title bar: back chevron, patient name at 16 px / 600, recall tag. **The back chevron
mirrors in RTL** — it is directional. The mark and the object icons do not.

Then a fixed identity block (species/sex/age/weight on one 14 px `ink-muted` line, mono
patient ID, then a 1 px rule and the owner row with a 44 px "Open WhatsApp" outlined
button). The owner phone is mono, `dir="ltr"`, trailing-aligned inside the RTL layout.

Tab strip below — Visits / Vaccinations / Weight / Invoices — horizontally scrollable,
cells 44 px, active tab underlined 2 px `primary`. Do not wrap the tabs onto two lines.

The scroller opens with the "next due" block in `accent-light` (label left, date right),
then the visit timeline: title + date on one row, vet on a 12 px line, note at 14 px /
1.6, separated by 1 px rules.

Action bar: "Print certificate" (secondary) and "New visit" (primary), equal width, 48 px.

### 11 — Recalls and the WhatsApp handoff (Arabic)

A list of due recalls — patient · owner, then the reason and date, then a 44 px "واتساب"
primary button per row. Overdue dates read as "متأخر 3 أيام" rather than a date.

Tapping it opens a **bottom sheet**, not a new route: scrim `rgba(31,41,51,0.45)` over the
list, sheet anchored to the bottom, `surface` fill, 2 px `ink` top rule, 20 × 18 px
padding. It contains the recipient with mono LTR phone, the prefilled message in an
`canvas` box with a 1 px border at 14 px / 1.8, one line of 12 px explanation, and
Cancel (auto width) + "فتح واتساب" (fills the rest) at 48 px.

**The message is composed on the client and opened in WhatsApp on the staff member's own
device.** The server never sends anything. That sentence is in the sheet on purpose —
keep it. No logo goes into the message.

Dismiss on scrim tap and on back gesture. The sheet does not scroll; the message is short
by design.

### 12 — Collect payment (English)

Title bar: back chevron, "Invoice" over the mono invoice number at 11 px, unpaid tag
pushed to the trailing edge.

Scroller in three blocks separated by 2 px rules: patient and owner summary; the line
items as label/amount rows with a 2 px `ink` rule above the total at 18 px / 600; then
the payment panel — a three-way segmented control (48 px cells, 2 px gaps showing `line`
through, selected cell a solid `primary` fill), the amount field at 20 px / 600 in a
52 px box, and change due in a `primary-light` block at 15 px / 500.

Change due is derived live from amount received minus total. Action bar: "Print" (auto
width, secondary) and "Confirm payment" (fills, primary), 48 px.

All amounts `font-variant-numeric: tabular-nums`, Western digits, in both languages.

### 13 — Login (Arabic)

Full-bleed `teal-soft` ground with the stacked lockup in white centred in the upper
region — 64 px mark over the 34 px wordmark. The form is a `surface` sheet anchored to
the bottom with a 2 px `ink` top rule, 24 × 18 px padding: two 52 px inputs at 16 px, a
52 px primary button, and a 44 px footer row with "forgot password" and the language
switch.

Email and password inputs stay `dir="ltr"` and left-aligned inside the RTL layout,
because their content is Latin. This is the general rule for any Latin-content field.

## RTL specifics on phone

- Direction comes from `dir` on the document root, driven by the user's language setting.
  Everything mirrors: nav order, row internals, tab strip scroll direction, sheets.
- **Mirror:** back chevron, next/previous, send.
- **Do not mirror:** the Qvety mark, object icons (syringe, scale, animal heads), and any
  field whose content is Latin (email, phone, invoice number, batch number, microchip).
- Mixed-content lines: wrap the Latin or numeric run in a span set to
  `font-family: 'IBM Plex Sans'` with `direction: ltr`. Arabic dates written as
  "26 سبتمبر 2026" use Latin digits with the Arabic month name.

## Implementation notes

- Breakpoint: single phone layout below **768 px**; the desktop layout above it. There is
  no tablet design yet — tablets get the desktop layout, which works down to ~900 px.
- NG-ZORRO: `nz-table` is not used on phone. Render the list with `nz-list` or plain
  elements. The bottom sheet can be `nz-drawer` with `nzPlacement="bottom"`, themed to
  0 radius and no shadow. The segmented control is `nz-segmented` with `@border-radius-base: 0`.
- Keyboard: when a field is focused, the action bar should sit above the keyboard, not
  behind it. Use the visual-viewport height rather than `100vh`.
- No animation beyond NG-ZORRO defaults, and no pull-to-refresh gesture — the list has an
  explicit refresh in the title bar where one is needed.
- Test at 390 px (iPhone 13/14/15) and at 360 px (common Android). Nothing in this layout
  breaks at 360; the fixed 46 px time column is the tightest element.

## Open items

Same list as the README — the wordmark Q, `favicon.ico`, the five undrawn custom icons.
Nothing in the phone layout depends on them; the empty-state graphic uses the mark.
