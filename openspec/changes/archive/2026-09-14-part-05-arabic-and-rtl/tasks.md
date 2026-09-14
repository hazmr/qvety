## 1. Backend
- [x] 1.1 `V4__user_locale.sql`
- [x] 1.2 `locale` in `/me`; `PATCH /api/v1/me`
- [x] 1.3 `messages_ar.properties`, `messages_en.properties`, `Accept-Language` resolution for validation messages

## 2. Angular
- [x] 2.1 Transloco setup, `ar.json` and `en.json`
- [x] 2.2 Move every string from parts 02–03 into the JSON files; grep templates for leftovers
- [x] 2.3 Direction toggle in the header: `dir` on `<html>`, NG-ZORRO direction, saves via `PATCH /me`
- [x] 2.4 Angular locale data; today's date in the header as a check
- [x] 2.5 Interceptor sends `Accept-Language`
- [x] 2.6 Login, header, dashboard, practice page checked in both directions; fix alignment and icon mirroring

## 3. Verify
- [x] 3.1 Toggle survives refresh
- [x] 3.2 Backend validation error shows in Arabic for an Arabic user

## 4. Close
- [x] 4.1 `erd.md` updated (`users.locale`)
- [x] 4.2 `progress.md` entry
- [x] 4.3 Commit
