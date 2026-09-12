# Part 03 — Users, roles, and login

## Business

A clinic has staff. Each staff member logs in and has one role. Confirm and write down in `docs/domain/users-and-roles.md`:

| Role | Can |
| --- | --- |
| `admin` | Everything in the practice, including managing users and settings |
| `veterinarian` | Clinical work: examine, diagnose, prescribe, finalize notes |
| `technician` | Record vitals, vaccinations, assist; cannot finalize notes or prescribe |
| `front_desk` | Clients, patients, scheduling, invoices, payments; no clinical writes |

Rules:
- A user belongs to exactly one practice.
- Email is unique per practice, not globally (the same person could work at two clinics; keep it simple and allow it).
- Passwords are hashed with bcrypt (cost 12). Never stored, never logged. Minimum 10 characters; no other composition rules.
- A login lasts one working day: the token expires after 12 hours; the user logs in again next morning. No refresh tokens in the pilot; `session_version` already gives immediate revocation, so a long-lived token is not the risk it would be elsewhere.
- A deactivated user cannot log in but stays in history (notes they wrote still show their name).
- **Role and clinical identity are separate.** In a one-doctor Egyptian clinic the owner is the `admin` and also the veterinarian who examines and signs notes. So `role` decides what a user may manage, and a separate `is_veterinarian` flag decides whether the user may do veterinarian clinical acts (finalize notes, prescribe, be booked as the vet). A `veterinarian` role always has the flag; an `admin` may have it; `technician` and `front_desk` never do.
- **Staff onboarding has no email step.** The admin creates the user with a temporary password and the user must change it at first login. Password reset is also done by the admin (set a new temporary password). No email sending in the pilot.
- **Revocation is immediate.** Deactivating a user, or the admin resetting a password, invalidates every token that user already holds.
- Login is rate limited per email and per IP. Too many failures returns 429 with a `Retry-After`; nothing is locked permanently.

## Stack you learn

- Spring Security 7 filter chain, stateless.
- JWT: issue on login, verify on each request. Claims: `sub` (user id), `practiceId`, `role`, `vet` (boolean), `sv` (session version), `exp` (12 h). HS256 with a 256-bit secret from the environment; the app refuses to start without it outside the `local` profile.
- `session_version`: an integer on the user row, copied into the token. The filter loads the user and rejects the token if the versions differ. Bumping the column revokes every token at once. This is the answer to "what if the user was deactivated after the token was issued": the same lookup checks `active`.
- `@PreAuthorize("hasRole('VETERINARIAN')")` on service methods, and a custom `@PreAuthorize("@access.isVeterinarian()")` style check for the flag.
- `SecurityContextHolder` and a small `CurrentUser` helper.
- Login rate limiting with Bucket4j (in-memory is fine: one JVM in the pilot). Write down that a second instance would need a shared store.
- Security headers for a SPA served from the jar: CSP `default-src 'self'`, `frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, HSTS when behind TLS.
- Angular: login form, HTTP interceptor adding the token, route guard, logout, forced password change screen.

## Design

Table `users`: `id`, `practice_id` (fk practices), `email`, `password_hash`, `full_name`, `role` (enum), `is_veterinarian` (bool), `license_number` (nullable; veterinary syndicate number, printed on prescriptions and certificates), `phone` (nullable), `active`, `session_version` (int, default 1), `must_change_password` (bool), timestamps. Unique `(practice_id, email)`. Check constraint: `role = 'veterinarian'` implies `is_veterinarian`; `role in ('technician','front_desk')` implies not.

Endpoints: `POST /api/v1/auth/login` `{email, password}` → `{token, user}`. `GET /api/v1/me`. `POST /api/v1/me/password` `{current, new}` (bumps `session_version`, returns a fresh token). Admin only: `GET/POST /api/v1/users`, `PUT /api/v1/users/{id}`, `POST /api/v1/users/{id}/deactivate`, `POST /api/v1/users/{id}/reset-password` (sets a temporary password, sets `must_change_password`, bumps `session_version`). Delete the `X-Practice-Id` header hack from part 02; the practice now comes from the token.

Also admin only: `PUT /api/v1/practice` for the practice's own identity columns from part 02 (`name`, `address`, `phone`, `vat_number`, `tax_rate_percent`). Country, currency, and status are not in the request record; MapStruct ignores them.

Decision recorded here: no `viewer` (read-only) role. Four roles cover an Egyptian clinic. Add a fifth only when a clinic names a person who needs it.

While `must_change_password` is true, every endpoint except `/me` and `/me/password` returns 403 with code `password_change_required`.

## Steps

1. Migration `V2__users.sql`. `db/seed/R__dev_users.sql` seeds one user per role for the seeded practice, all with password `password123` (dev only; the seed folder never reaches production). The seeded admin also has `is_veterinarian = true` and a license number.
2. `User` entity, repository, `AuthService` (verify password, issue JWT), `JwtFilter` (verifies signature, loads user, checks `active` and `session_version`), `SecurityConfig` with the header set and the stateless chain.
3. `CurrentUser` helper returning `userId`, `practiceId`, `role`, `isVeterinarian` from the security context. This helper is what every later service uses.
4. `@PreAuthorize` on `PracticeService.get()` for any authenticated user; `UserService` admin-only; one method guarded by the veterinarian flag to prove both checks work.
5. Login rate limit filter: 10 failures per email per 15 minutes, 30 per IP per 15 minutes. Successful login resets the email bucket.
6. Test: login as front desk, call `/me`, get 200; call the admin-only endpoint, get 403; wrong password, 401; admin resets the front desk password, old token now 401, new login is 403 on `/users` until the password is changed; 11th failed login returns 429.
7. Angular: `/login` page, `AuthService` storing the token (memory + `sessionStorage`), interceptor, guard on all routes except login, header showing the user's name and a logout button, `/change-password` screen the guard forces when `mustChangePassword` is true, `/settings/users` list and form (admin only).

## Ask Claude

- "Explain the Spring Security filter chain in Boot 4 and where my JWT filter sits."
- "Why stateless JWT instead of a session? What do we lose?"
- "Show me how `@PreAuthorize` and `@EnableMethodSecurity` work together."
- "Review `AuthService` for timing attacks and password handling."
- "Explain how `session_version` in the JWT makes logout and deactivation immediate without storing sessions."
- "Which security headers does Spring Security 7 set by default and which must I add for an Angular SPA served from the jar?"

## Done when

- Login works in the browser; refresh keeps you logged in; logout clears.
- 401/403/429 tests green, including the reset-password revocation case.
- Admin can add a second staff member and that person can log in and is forced to change the password.
- Admin can edit the practice address and phone; a front desk user gets 403 on the same call.
- Part 02 header hack removed.

## Self-check

- Where in the request does the practice id come from now?
- What happens if a JWT is valid but the user was deactivated after it was issued? Trace the request through `JwtFilter` and say where it is rejected.
- Why must the role check be on the service, not only in Angular?
- Why is `is_veterinarian` a flag and not a fifth role? What breaks in a one-doctor clinic if it is a role?
