## Why

Egyptian clinic staff work in Arabic; some vets prefer English for clinical terms. The UI must be Arabic by default, switchable per user, and correct right-to-left in tables, forms, dates, and later print views.

## What Changes

- `users.locale` column (default `ar-EG`), returned from `/me`, editable via `PATCH /api/v1/me`.
- Transloco with `ar.json` and `en.json`; every string from parts 02–03 moved out of templates.
- Document direction and NG-ZORRO direction follow the language; toggle in the header.
- Angular locale data for dates and numbers.
- Backend validation messages resolved from `Accept-Language`, sent by the interceptor from the user's locale.

## Capabilities

### New Capabilities
- `localization`: default language, per-user override, what is translated and what never is, direction.

### Modified Capabilities
- none

## Non-goals

- Translating clinic-entered text or clinical notes. Never.
- Print stylesheets (part 13 and 14 build them on this foundation).
- A third language.

## Impact

- `V4__user_locale.sql`; `PATCH /api/v1/me`.
- `web/src/assets/i18n/ar.json`, `en.json`; `messages_ar.properties`, `messages_en.properties`.
- Every later screen adds its strings to both JSON files.
