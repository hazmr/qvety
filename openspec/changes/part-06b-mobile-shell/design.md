## Breakpoint

One CSS custom media point: `@media (max-width: 767.98px)` = phone. A `ViewportService` exposes `isPhone` as a signal (from `matchMedia('(max-width: 767.98px)')`) so templates can switch structure, not only style.

## Shell

`layout/shell`:
- Phone: `<header class="strip">` (32 px, mark, clinic name, initials) → `<router-outlet>` inside `<main class="scroller">` (flex: 1, `overflow: auto`) → `<nav class="bottom-nav">` (60 px, `Home`, `Clients`, `Users` for admins; active cell `primary-light` fill, 3 px primary bar on the top edge). Language toggle and logout move into a small "Me" sheet opened from the initials (out of scope to over-design: a simple `nz-drawer` bottom with name, language toggle, logout).
- Desktop: current header unchanged.
- Column layout with `height: 100dvh` (visual viewport, so the keyboard does not hide the action bar) and only `.scroller` scrolling.

`layout/title-bar` component: inputs `titleKey` / `title`, optional `back` (route), optional one `action` (`{labelKey, link | click}`), optional `subtitle`. Renders the 48 px bar with a 2 px line rule; back chevron uses a directional icon that mirrors under `[dir=rtl]` (`transform: scaleX(-1)`).

`layout/action-bar` component: content projection for one or two buttons at 48 px, `position: sticky; bottom: 0`, safe-area padding; the shell hides the bottom nav while an action bar is present (a signal in `ViewportService`, set by the action bar on init/destroy).

## List page

`shared/list-page`: on phone render `<ul class="rows">` with `<li class="row">` per item: `.row__title` from the first column, `.row__secondary` from the `secondary` columns joined by " · ", `.row__tag` from an optional `tag` column; `min-height: 44px`, padding 13 × 14 px, 1 px rules. Paging via a "load more" button (44 px) at the end of the list. Desktop keeps `nz-table`. Column config gains `role?: 'title' | 'secondary' | 'tag' | 'hidden'`.

## Forms

`shared/form-page` and login: phone CSS only — `input, textarea, .ant-select-selector { font-size: 16px; min-height: 52px }`, buttons 44 px, card padding 14 px. Submit button moves into an action bar on detail/form screens.

## Login

Phone template branch: `teal-soft` full-bleed ground, `lockup-stacked-white.svg` centred in the upper region, `<section class="sheet">` anchored bottom (2 px ink top rule, 24 × 18 px padding), two 52 px inputs, 52 px primary button, 44 px footer row with the language switch (no "forgot password" until part 03's reset flow gets a self-service path — it does not; the footer shows the language switch only).

## Screens touched

- Users list: stacked rows on phone (name / phone · role, status tag); "Add user" as the title-bar action; row actions move to the user form (deactivate, reset password) reached by tapping the row.
- Clients list: title-bar action "Add"; rows name / phone · email.
- Client detail: back chevron, summary, tabs, action bar with Edit and Archive.
- Client form and user form: back chevron, Save in the action bar.

## Checks

Playwright walkthrough at 1280 × 800, 390 × 780, 360 × 780 in `ar` and `en`: no horizontal overflow (`document.documentElement.scrollWidth <= innerWidth`), inputs ≥ 52 px on phone, buttons ≥ 44 px, bottom nav present on top-level, action bar present on detail, never both.
