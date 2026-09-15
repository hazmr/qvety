## Context

See proposal.md, Why. Current state that shapes the fix:

- `login_lookup(text)` (`V3__rls.sql`) is the only cross-tenant read: `SECURITY DEFINER`, returns every active `users` row with that E.164 phone or lowercased email. `AuthService.login` takes `findFirst()`.
- `practices` is under forced RLS with policy `id = current_practice_id()`. With no tenant set, `qvety_app` sees zero practice rows, so practice names for the picker cannot come from `PracticeRepository`.
- `LoginRateLimiter` keys on the normalized identifier. `recordSuccess` clears that bucket.
- `LoginResponse` is `{token, user}`, both required in the OpenAPI schema; the Angular client is generated from it.
- Bcrypt cost 12 takes roughly 250 ms per check on the pilot host.

## Goals / Non-Goals

**Goals:**
- Every user at every practice can log in with their own password.
- Nothing about where an identifier exists leaks before the password is verified.
- One request shape, one response shape; the generated client changes minimally.

**Non-Goals:**
- Identity/membership split (backlog).
- Practice slug, subdomain, or any host-based tenant selection.
- Caching the last chosen practice on the device.

## Decisions

### Migration `V7__login_practices.sql`: one definer function, no table

```sql
-- Part 03b: practice names for the login picker. Called only after the password matched.
CREATE FUNCTION login_practices(p_ids uuid[]) RETURNS TABLE (id uuid, name text)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
    AS $$ SELECT id, name FROM practices WHERE id = ANY (p_ids) $$;

REVOKE ALL ON FUNCTION login_practices(uuid[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION login_practices(uuid[]) TO qvety_app;
```

Why a function and not a wider `login_lookup`: `login_lookup` stays untouched (never edit an applied migration), and a second small definer function is easier to reason about than a join that returns user and practice columns mixed. It returns `id` and `name` only; status, country, and everything else stay out of reach.

Alternative rejected: a `practice_self_or_login` policy on `practices`. Policies cannot know "the password just matched"; a definer function called only from `AuthService` after verification can.

### Request and response shapes

`LoginRequest` gains `UUID practiceId` (optional). `LoginResponse` becomes:

```java
public record LoginResponse(String token, UserDto user, List<PracticeChoice> practices) {}
public record PracticeChoice(UUID id, String name) {}
```

Exactly one of (`token` + `user`) or `practices` is non-null; the schema marks all three optional and the Javadoc says which pair appears when. Kept as one record so `AuthApi.login` in the generated client keeps one return type and `AuthService.accept()` in Angular only checks `r.token`.

Alternative rejected: HTTP 300 or a separate endpoint. Both add a second code path in the client for a case that is rare; a nullable field is enough.

### Login algorithm (`AuthService.login`)

1. Normalize the identifier; rate-limit check as today.
2. `findForLogin(identifier)` under `SystemContext`; keep active rows. If `practiceId` is present, keep only that practice's row.
3. For each row, `passwords.matches(password, hash)`. Collect matches. If there were zero rows, run one match against `DUMMY_HASH` so timing stays flat for an unknown identifier.
4. Zero matches: `recordFailure`, 401 `invalid_credentials`.
5. One match: `recordSuccess`, issue the token for that row.
6. Several matches: `recordSuccess`, call `login_practices(ids)` under `SystemContext`, return `{practices: [...]}`.

Rows are checked in a stable order (practice id) so behaviour does not depend on Postgres row order. Every hash of every matching row is checked even after the first match, so the number of rows is not observable from timing beyond the base cost.

### Angular

`Login` component keeps the form; when the response has `practices`, it shows the list as tappable rows (44 px, `list-page` row style) under the inputs with the title `login.choosePractice`. Tapping posts again with `practiceId`. The password field keeps its value between the two calls; the identifier field is locked. Desktop shows the same list inside the card. Keys in both `ar.json` and `en.json`.

### Tests

`LoginSamePhoneProbeIT` folds into `AuthIT` as `samePhoneAtTwoPractices`: seeds practice C and a front-desk user with the desk phone, and covers the scenarios in the delta spec (different passwords, same password with picker, `practiceId` narrowing, wrong `practiceId`, deactivated at one practice). Seed via the owner connection as `TenantIsolationIT` does; practice C uses a distinct id so the two tests do not collide in the shared container.

## Risks / Trade-offs

- [Bcrypt runs once per matching row] → Two clinics means two checks, about half a second. Bounded by the rate limiter and by how many clinics one person works at.
- [`login_practices` is a second cross-tenant read] → Only id and name, only callable with ids that just matched a password, `EXECUTE` limited to `qvety_app`. Noted in `erd.md` next to `login_lookup`.
- [Same password at both clinics shows a picker on every login] → Rare; the user can change one password. Remembering the choice is a backlog item.
- [Generated client changes] → `npm run api:generate` after the DTO change; `token` becomes optional in `LoginResponseDto`, so `accept()` guards on it.

## Migration Plan

Forward-only `V7`. Function only; no data, no table, no RLS or audit change, so `TenantIsolationIT.everyTenantTableIsIsolatedAndAudited` is unaffected. Rollback is a new migration dropping the function; the Java change must ship with `V7` because the service calls it.
