## Why

A veterinarian or receptionist can work at two clinics. Each practice creates its own user row for that person with the same phone (the `users-and-roles` spec allows it: phone and email are unique per practice, not globally). Login runs before any practice is known, so `login_lookup()` returns both rows, and `AuthService.login` checks the password against the first row only. The second clinic's password always fails with 401. `LoginSamePhoneProbeIT` (uncommitted) reproduces it: practice A password 200, practice C password 401.

The pilot has one clinic, so nobody has hit it yet. It must be fixed before a second clinic onboards anyone who already works at the first.

## What Changes

- Login checks the password against every active user row that matches the identifier, across practices.
- Exactly one row matches: a token for that practice, as today.
- More than one row matches (same person, same password at two clinics): the response carries the list of matching practices (id, name) and no token. The client shows a picker and posts again with `practiceId`; the server then issues the token for that practice only.
- Zero rows match: 401, unchanged. A practice choice is never shown before the password is verified, so nothing is revealed about where a phone exists.
- The rate limiter treats a correct password as success even when a choice is still needed.
- A practice name is needed for the picker, and `practices` is under RLS with no tenant set. One new definer function returns id and name for the practices of the matched users. This is the second and last cross-tenant read; it reads no clinical data.
- The Angular login screen gains the practice picker step on phone and desktop, in `ar` and `en`.
- `LoginSamePhoneProbeIT` becomes the regression test inside `AuthIT`.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `users-and-roles`: "Login accepts phone or email" gains the rule that every matching active row is tried and the practice choice step when more than one matches. "Practice id comes from the token" gains the clarification that `practiceId` in the login body only narrows an already-verified match.

## Non-goals

- One identity with many practice memberships (`identities` plus `practice_members`). The long-term shape; goes to `docs/backlog.md`.
- Subdomain per practice. Would violate "no request header selects the practice". Backlog.
- Remembering the last chosen practice on the device. Not asked for.
- Any change to tokens, revocation, roles, or the password-change gate.

## Impact

- `src/main/java/com/qvety/auth/AuthService.java`, `LoginRequest`, `LoginResponse`, `UserRepository`, one new repository method for practice names.
- `V7__login_practices.sql`: one `SECURITY DEFINER` function, no table. `erd.md` migration table gets the row.
- `web/src/app/features/login/*`, `core/auth.service.ts`, i18n keys, regenerated `web/src/app/api`.
- `AuthIT` gains the two-practice scenario; `TenantIsolationIT` unchanged (no new tenant table).
- `docs/parts/03-users-and-login.md` and `docs/progress.md` note the change.
