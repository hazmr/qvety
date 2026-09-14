## Why

Two clinics on one database must never see each other's rows, and every change must be attributable. Both are promises the product is sold on; both must be enforced by the database, not by service code that can forget.

## What Changes

- Postgres row-level security on every tenant table, forced, keyed on the session variable `app.practice_id`.
- Two database roles: `qvety_owner` (Flyway, owns tables) and `qvety_app` (application, cannot bypass RLS).
- `TenantTransactionHook` sets `app.practice_id` and `app.user_id` with `SET LOCAL` at the start of every transaction; no tenant and no explicit system context throws.
- `audit_log` table written by a generic `SECURITY DEFINER` trigger on every tenant table and on `practices`, with secret columns stripped.
- Column-level grants so `qvety_app` cannot change `practices.status`.
- `GET /api/v1/audit` (admin only) and the `TenantIsolationIT` test that every later part extends.

## Capabilities

### New Capabilities
- `tenant-boundary`: no query ever returns another practice's rows; how it is enforced and proven.
- `audit`: every insert, update, delete on tenant rows is recorded with who, when, before, after.

### Modified Capabilities
- none

## Non-goals

- System context for platform code (part 11). Today nothing runs without a tenant.
- Audit UI beyond the read endpoint; the "History" tab arrives with clients in part 06.
- Export list (part 11).

## Impact

- New: `V3__rls.sql`, `com.qvety.tenant` (hook, `TenantEntity` later), `audit` read endpoint, `TenantIsolationIT` as a required CI check.
- `application.yml` gains two datasources: Flyway as `qvety_owner`, app as `qvety_app`.
- Every later migration appends its tables to the RLS and audit lists.
