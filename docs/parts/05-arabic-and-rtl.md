# Part 05 — Arabic and RTL

## Business

Egyptian clinic staff work in Arabic. Some vets prefer English for clinical terms. The UI must be Arabic by default, switchable per user, and correct in right-to-left layout including tables, forms, dates, and later PDFs.

Write in `docs/domain/localization.md`: default language per practice, per-user override, what is translated (UI strings, coded value labels) and what is never translated (clinic-entered text, clinical notes).

## Stack you learn

- Transloco: JSON per language, `transloco` pipe, lazy loading, language switch.
- NG-ZORRO: `provideNzI18n(ar_EG)`, `NzConfigService` direction, `dir` attribute on `<html>`.
- Angular locale data for dates and numbers (`ar-EG`, `en-EG`).
- Backend: `Accept-Language` for error messages; user preference stored in `users.locale`.

## Steps

1. Add `locale` to `users` (`V4__user_locale.sql`, default `ar-EG`). Return it from `/me`.
2. Transloco setup with `ar.json` and `en.json`. Move every string from parts 02–03 into them. No hard-coded text remains.
3. Direction: when language is `ar`, set `dir="rtl"` on `<html>` and NG-ZORRO direction; `ltr` for `en`. A toggle in the header saves the preference via `PATCH /api/v1/me`.
4. Login page, header, dashboard, and the practice page checked in both directions. Fix alignment and icon mirroring.
5. Date and number pipes use the user's locale. Show today's date in the header as a check.
6. Backend validation messages: `messages_ar.properties` and `messages_en.properties`, resolved by `Accept-Language`, which the Angular interceptor sends from the user's locale.

## Ask Claude

- "Set up Transloco in an Angular 22 standalone app with a language switch that also toggles document direction."
- "Which NG-ZORRO components need special handling in RTL?"
- "Wire Spring's `MessageSource` to `Accept-Language` for Bean Validation messages."

## Done when

- Zero hard-coded UI strings (grep the templates).
- Toggle switches language and direction without reload glitches; preference survives refresh.
- Validation error from the backend shows in Arabic when the user is in Arabic.

## Self-check

- Which text will never be translated, and why is that a business rule and not a technical limit?
- How does a new screen get its strings? Write the two-line recipe in `docs/domain/localization.md`.
