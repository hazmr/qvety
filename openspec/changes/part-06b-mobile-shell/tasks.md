## 1. Shell
- [ ] 1.1 `ViewportService` (`isPhone` signal from `matchMedia`, `actionBarPresent` signal)
- [ ] 1.2 Shell: phone column (strip, scroller, bottom nav) below 768 px; desktop header above; `100dvh`, only the scroller scrolls
- [ ] 1.3 `title-bar` component (title, back chevron that mirrors in RTL, one action)
- [ ] 1.4 `action-bar` component (48 px buttons, sticky bottom, safe-area inset; hides the bottom nav)
- [ ] 1.5 "Me" sheet from the initials: name, language toggle, logout (`nz-drawer` bottom, radius 0, no shadow)

## 2. Shared components
- [ ] 2.1 `list-page`: stacked rows on phone with `title | secondary | tag` column roles, load-more paging; table on desktop
- [ ] 2.2 `form-page`: phone sizes (16 px / 52 px inputs, 44 px buttons, 14 px padding); Save via action bar on phone

## 3. Screens
- [ ] 3.1 Login: bottom-sheet layout on phone (screen 13), card on desktop
- [ ] 3.2 Users list and form on the new shell
- [ ] 3.3 Clients list, detail (action bar: Edit, Archive), form on the new shell
- [ ] 3.4 Home on the new shell (bottom nav destination)

## 4. Verify
- [ ] 4.1 Playwright walkthrough at 1280, 390, 360 in `ar` and `en`: no horizontal overflow, sizes per spec, nav/action-bar exclusivity, zero console errors
- [ ] 4.2 `./mvnw verify` green (Angular unit tests included)

## 5. Close
- [ ] 5.1 `docs/design/frontend.md` "Application chrome" notes the phone variant (pointer to MOBILE.md)
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
