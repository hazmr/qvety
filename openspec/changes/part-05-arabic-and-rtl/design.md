## Migration `V4__user_locale.sql`

```sql
ALTER TABLE users ADD COLUMN locale text NOT NULL DEFAULT 'ar-EG';
```

## API

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/v1/me` | now includes `locale` |
| PATCH | `/api/v1/me` | `{locale}` only |

## Angular

- Transloco: `ar.json`, `en.json`, `transloco` pipe, lazy loading, `TranslocoService.setActiveLang`.
- Direction: set `dir` on `<html>` and `NzConfigService` direction from the active language.
- `provideNzI18n(ar_EG)` / `en_US` switched with the language; register Angular locale data `ar-EG`, `en-EG`.
- Interceptor sends `Accept-Language` from the user's locale.

## Backend

`MessageSource` with `messages_ar.properties` and `messages_en.properties`; `LocaleResolver` from `Accept-Language`; Bean Validation messages resolved through it.

## Decisions

- Locale on the user, not on the browser, so the same person sees the same language on any device.
- Coded values (enums) get their label in the JSON files; a new enum value is one migration plus two i18n lines.
