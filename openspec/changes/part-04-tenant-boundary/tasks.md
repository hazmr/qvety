## 1. Migration
- [ ] 1.1 `V3__rls.sql`: role `qvety_app`, practice grants and self policy, `audit_log`, `audit_row()`, `DO` loop over the tenant list
- [ ] 1.2 Verify in `psql` as `qvety_app`: set `app.practice_id` to A, `SELECT * FROM users` shows only A; unset shows nothing

## 2. Spring
- [ ] 2.1 Two datasources: Flyway as `qvety_owner`, app as `qvety_app`
- [ ] 2.2 `TenantTransactionHook` sets both variables with `SET LOCAL` at every transaction start
- [ ] 2.3 No user and no system context throws before any query

## 3. Audit endpoint
- [ ] 3.1 `GET /api/v1/audit` admin only, paged, read-only

## 4. Tests
- [ ] 4.1 `TenantIsolationIT`: A never sees B via API, repository, lazy load, native query
- [ ] 4.2 No-context transaction throws
- [ ] 4.3 Table-list assertion (RLS list = audit trigger list)
- [ ] 4.4 Audit: user rename yields one row with correct `user_id`, before/after, no `password_hash`
- [ ] 4.5 Audit: practice address change yields a `practices` row with the practice id
- [ ] 4.6 `UPDATE practices SET status` as `qvety_app` fails; `UPDATE`/`DELETE audit_log` fail; `SELECT audit_row()` fails
- [ ] 4.7 Practice B cannot read A's audit rows
- [ ] 4.8 `TenantIsolationIT` is a required check in CI

## 5. Close
- [ ] 5.1 `erd.md` updated with `audit_log` and the roles
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
