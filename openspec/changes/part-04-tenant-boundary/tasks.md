## 1. Migration
- [x] 1.1 `V3__rls.sql`: role `qvety_app`, practice grants and self policy, `audit_log`, `audit_row()`, `DO` loop over the tenant list
- [x] 1.2 Verify in `psql` as `qvety_app`: set `app.practice_id` to A, `SELECT * FROM users` shows only A; unset shows nothing

## 2. Spring
- [x] 2.1 Two datasources: Flyway as `qvety_owner`, app as `qvety_app`
- [x] 2.2 `TenantTransactionHook` sets both variables with `SET LOCAL` at every transaction start
- [x] 2.3 No user and no system context throws before any query

## 3. Audit endpoint
- [x] 3.1 `GET /api/v1/audit` admin only, paged, read-only

## 4. Tests
- [x] 4.1 `TenantIsolationIT`: A never sees B via API, repository, lazy load, native query
- [x] 4.2 No-context transaction throws
- [x] 4.3 Table-list assertion (RLS list = audit trigger list)
- [x] 4.4 Audit: user rename yields one row with correct `user_id`, before/after, no `password_hash`
- [x] 4.5 Audit: practice address change yields a `practices` row with the practice id
- [x] 4.6 `UPDATE practices SET status` as `qvety_app` fails; `UPDATE`/`DELETE audit_log` fail; `SELECT audit_row()` fails
- [x] 4.7 Practice B cannot read A's audit rows
- [x] 4.8 `TenantIsolationIT` is a required check in CI

## 5. Close
- [x] 5.1 `erd.md` updated with `audit_log` and the roles
- [x] 5.2 `progress.md` entry
- [x] 5.3 Commit
