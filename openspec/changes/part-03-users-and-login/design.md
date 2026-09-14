## Migration `V2__users.sql`

```sql
CREATE TYPE user_role AS ENUM ('admin','veterinarian','technician','front_desk');

CREATE TABLE users (
  id                   uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id          uuid NOT NULL REFERENCES practices(id),
  email                text NOT NULL,
  password_hash        text NOT NULL,
  full_name            text NOT NULL,
  role                 user_role NOT NULL,
  is_veterinarian      boolean NOT NULL DEFAULT false,
  license_number       text,
  phone                text,
  active               boolean NOT NULL DEFAULT true,
  session_version      integer NOT NULL DEFAULT 1,
  must_change_password boolean NOT NULL DEFAULT false,
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  UNIQUE (practice_id, email),
  CHECK (role <> 'veterinarian' OR is_veterinarian),
  CHECK (role NOT IN ('technician','front_desk') OR NOT is_veterinarian)
);
```

- `license_number`: veterinary syndicate number, printed on prescriptions and certificates.
- `session_version`: copied into the token; bumping it revokes every token of that user.
- RLS and audit trigger for `users` arrive in part 04 (`V3__rls.sql`); this table is the first entry in that list.

## API

| Method | Path | Who | Notes |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | anyone | `{email, password}` → `{token, user}`; 401 wrong, 429 rate limited |
| GET | `/api/v1/me` | any user | current user, allowed during `must_change_password` |
| POST | `/api/v1/me/password` | any user | `{current, new}`; bumps `session_version`, returns fresh token |
| GET | `/api/v1/users` | admin | list |
| POST | `/api/v1/users` | admin | create with temporary password, sets `must_change_password` |
| PUT | `/api/v1/users/{id}` | admin | edit name, role, flag, license, phone |
| POST | `/api/v1/users/{id}/deactivate` | admin | `active=false`, bump `session_version` |
| POST | `/api/v1/users/{id}/reset-password` | admin | temporary password, flag, bump `session_version` |
| PUT | `/api/v1/practice` | admin | identity columns only; MapStruct ignores country, currency, status |

## JWT

HS256, 256-bit secret from `QVETY_JWT_SECRET`. Claims: `sub` (user id), `practiceId`, `role`, `vet` (boolean), `sv` (session version), `exp` (+12 h). `JwtFilter` verifies the signature, loads the user, rejects if `!active` or `sv != session_version`. Stateless chain, no session cookie.

## Security

- `@EnableMethodSecurity`; `@PreAuthorize("hasRole('ADMIN')")` on `UserService`; `@PreAuthorize("@access.isVeterinarian()")` for the flag; `PracticeService.get()` requires any authenticated user.
- `CurrentUser` helper exposes `userId`, `practiceId`, `role`, `isVeterinarian`, `fullName` from the security context; every later service reads the tenant from here.
- Rate limit filter with Bucket4j in memory: 10 failures per email per 15 min, 30 per IP per 15 min. One JVM in the pilot; a second instance needs a shared store (recorded, not built).
- Headers: CSP `default-src 'self'`, `frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, HSTS behind TLS.
- `must_change_password` gate implemented in the filter chain: 403 `password_change_required` for all but `/me` and `/me/password`.

## Seed

`R__dev_users.sql`: one user per role for the dev practice, password `password123` (bcrypt), admin also `is_veterinarian = true` with a license number. Seed folder never reaches production.

## Angular

`/login`, `AuthService` (token in memory + `sessionStorage`), HTTP interceptor adding `Authorization: Bearer`, route guard on everything but `/login`, header with user name and logout, `/change-password` forced by the guard when `mustChangePassword`, `/settings/users` list and form (admin only).

## Decisions

- Stateless JWT instead of server sessions: one jar, no session store, revocation handled by `session_version` lookup which is one indexed read per request.
- No `viewer` role. Recorded here; revisit only when a clinic names the person.
