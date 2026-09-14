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

## As built (deviations)

- Direction: CDK `BidiModule` `[dir]` on the app root (NG-ZORRO 22 reads `Directionality`; there is no `NzConfigService` direction key). `<html dir lang>` set too for the page itself.
- Device fallback for the language before login in `localStorage` (`qvety.locale`); after login the user row wins.
- Theme and fonts wired here per `docs/design/frontend.md` "Wiring the tokens (part 05)": `theme.less`, `styles/qvety-tokens.css`, `@fontsource/ibm-plex-*` bundled (no CDN), `favicon.svg` + `manifest.webmanifest`. Fonts come from npm packages rather than `web/public/fonts/`.
- `ApiError` + `ApiExceptionHandler` (`com.qvety.common`) created here so localized messages have a shape; part 06 extends it. `core/api-error.ts` puts backend field messages on form controls.
- `SpaForwardController` replaced by `SpaConfig` (`PathResourceResolver`): real file → served, route → `index.html`, missing file with an extension → 404.
- Angular unit tests (vitest) run inside `./mvnw verify`.
- `messages.properties` is the English fallback; `spring.messages.fallback-to-system-locale=false`.
- Every controller declares `produces = application/json`: springdoc otherwise emits `*/*`, and the generated Angular client then requests `text` and never parses the body.
- `.ant-form-vertical .ant-form-item-label { text-align: start !important }`: NG-ZORRO's vertical label rule is left-only, and its RTL rule mirrors the horizontal layout instead.
- 2.6 was checked headless (Playwright + Firefox from the scratchpad; screenshots not committed): login, home, users list, user form in `ar` and `en`, toggle persisted across reload, zero console errors.

## Decisions

- Locale on the user, not on the browser, so the same person sees the same language on any device.
- Coded values (enums) get their label in the JSON files; a new enum value is one migration plus two i18n lines.
