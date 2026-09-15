## Why

Staff use their own phones more than the desk PC: a vet checking a record in the consult room, a receptionist sending a recall from the corridor. The screens built in parts 02–06 assume a desktop (header nav, `nz-table` lists, 38 px inputs). `docs/design/MOBILE.md` arrived after part 06; this change retrofits the shell and the existing screens so every later part starts from a phone-first base.

## What Changes

- Responsive shell: below 768 px a 32 px brand strip, a 48 px title bar with one action, a single scroller, and a 60 px bottom nav (top-level) or a 48 px action bar (detail screens); desktop chrome above 768 px.
- `shared/list-page` renders stacked rows on phone (no `nz-table`), the table on desktop; search stays.
- `shared/form-page` and login: 16 px inputs, 52 px input height, 44/48 px buttons, 14 px page padding on phone.
- Login as the bottom sheet of screen 13 on phone; the centred card on desktop.
- Users list and clients screens follow (title bar action, detail action bar, back chevron that mirrors in RTL).
- Headless checks run at 390 × 780 and 360 × 780 as well as 1280 × 800.

## Capabilities

### New Capabilities
- `app-shell`: the frame every screen lives in — brand strip, navigation, title bar, action bar, breakpoints, target sizes, direction.

### Modified Capabilities
- none (client and user behaviour unchanged; layout only)

## Non-goals

- Tablet layout: tablets get the desktop layout (works to ~900 px), per MOBILE.md.
- Offline mode, push notifications, native wrappers.
- Screens 09–12 themselves (today, patient record, recalls sheet, payment): built by parts 10, 08, 15, 14 on this shell.
- Pull-to-refresh, animations beyond NG-ZORRO defaults.

## Impact

- `web/src/app/layout/shell.*`, `shared/list-page`, `shared/form-page`, `features/login`, `features/clients`, `features/settings/users`, `styles.scss`, i18n keys for nav labels.
- No backend change, no migration.
