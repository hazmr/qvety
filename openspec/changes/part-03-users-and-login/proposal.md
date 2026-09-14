## Why

A clinic has staff who log in and hold one role each. Every later rule (who may finalize, who may manage users, which practice a request belongs to) depends on an authenticated user with a role and a veterinarian flag.

## What Changes

- `users` tenant table with role, `is_veterinarian`, `session_version`, `must_change_password`.
- Stateless JWT login (12 h), `session_version` check on every request, immediate revocation.
- Admin-only user management: create, edit, deactivate, reset password. No email step anywhere.
- Login rate limiting per email and per IP (429 with `Retry-After`).
- Security headers for the SPA served from the jar.
- `PUT /api/v1/practice` for the practice's own identity columns (admin only).
- **BREAKING**: the part 02 `X-Practice-Id` header is deleted; the practice comes from the token.
- Angular: login, interceptor, guard, forced password change, users settings page.

## Capabilities

### New Capabilities
- `users-and-roles`: staff accounts, roles, veterinarian flag, login, sessions, revocation, rate limiting.

### Modified Capabilities
- `practice`: the practice may now edit its own identity columns; country, currency, and status stay super-admin only.

## Non-goals

- Refresh tokens. `session_version` gives immediate revocation, so a 12 h token is acceptable for the pilot.
- Email sending of any kind. Onboarding and reset are done by the admin with a temporary password.
- A `viewer` read-only role. Four roles cover an Egyptian clinic; add a fifth only when a clinic names a person who needs it.
- Row-level security (part 04).

## Impact

- New: `V2__users.sql`, `R__dev_users.sql`, package `com.qvety.auth`, `com.qvety.users`, `config/SecurityConfig`, `CurrentUser` helper used by every later service.
- Removed: `X-Practice-Id` handling from part 02.
- Angular: `/login`, `/change-password`, `/settings/users`, auth interceptor and guard.
- `erd.md` gains `users`.
