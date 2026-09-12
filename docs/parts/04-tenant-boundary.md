# Part 04 — Tenant boundary (RLS) and audit log

## Business

Two clinics on the same system must never see each other's data. Not a feature; a promise. If it breaks once, no clinic trusts the product again.

Write in `docs/domain/tenant-boundary.md`: what data is tenant-scoped (everything except platform tables), what the app promises, and how it is proven.

Second promise, same part: **every change is attributable.** Who changed which row, when, from what to what. A clinic owner asks "who edited this invoice" and the answer must exist. Write in `docs/domain/audit.md`: what is recorded (every insert, update, delete on tenant tables), what is never recorded (password hashes, tokens), who can read it (admin, and later the super admin on request), and that it cannot be edited or deleted by anyone through the application.

## Stack you learn

- Postgres row-level security: `ALTER TABLE ... ENABLE ROW LEVEL SECURITY`, `CREATE POLICY`.
- Session variables: `SET LOCAL app.practice_id`.
- Two database roles: `qvety_owner` (Flyway, owns tables) and `qvety_app` (application, no `BYPASSRLS`).
- Setting the variable per transaction from Spring: a `TransactionSynchronization` or an aspect around `@Transactional`.
- The isolation test, which every later part extends.
- A generic audit trigger: one PL/pgSQL function attached to every tenant table, reading `app.user_id` from the same session variable mechanism, writing `before`/`after` as `jsonb` with secret columns stripped. Because it is a trigger it cannot be forgotten by a service and cannot be skipped by a native query.

## Design

`V3__rls.sql`:
- Create role `qvety_app` (login, no bypass). Grant `SELECT, INSERT, UPDATE, DELETE` on tenant tables. On `practices`: grant `SELECT` and `UPDATE` on the identity columns only (`name`, `address`, `phone`, `vat_number`, `tax_rate_percent`, later `logo_object_key`), policy `USING (id = current_setting('app.practice_id', true)::uuid)`. Column-level grants mean `qvety_app` cannot change its own `status` even with a bug in the service.
- A `DO` block that loops over a list of tenant table names and runs `ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`, and creates policy `tenant_isolation USING (practice_id = current_setting('app.practice_id', true)::uuid)`. Today the list is just `users`. Every later migration appends to it.
- Two datasources in `application.yml`: Flyway uses `qvety_owner`, the app uses `qvety_app`.
- Table `audit_log` (`id`, `practice_id`, `user_id` nullable, `table_name`, `row_id`, `action` enum `insert|update|delete`, `before jsonb`, `after jsonb`, `at`). In the RLS list, but grants for `qvety_app` are `SELECT, INSERT` only. Function `audit_row()` is `SECURITY DEFINER`, owned by `qvety_owner`, `EXECUTE` revoked from `qvety_app`, and strips a fixed list of columns (`password_hash`) before writing. The same `DO` loop that enables RLS also attaches `AFTER INSERT OR UPDATE OR DELETE ... FOR EACH ROW EXECUTE FUNCTION audit_row()` to every table in the list except `audit_log` itself. `practices` also gets the trigger, with `audit_log.practice_id` taken from `NEW.id` instead of `NEW.practice_id` (the function checks `TG_TABLE_NAME`), so a clinic sees who changed its address.

Spring: `TenantTransactionHook` that, at the start of every transaction, runs `SET LOCAL app.practice_id = ?` and `SET LOCAL app.user_id = ?` with the values from `CurrentUser`. If there is no current user and the code is not in system context, throw. Nothing runs without a tenant or an explicit opt-out.

Endpoint: `GET /api/v1/audit?table&rowId&page` (admin only), read-only. Angular: a "History" tab on client detail in part 06 uses it; later detail pages copy the tab.

## Steps

1. Migration. Test locally with `psql` as `qvety_app`: `SET app.practice_id` to practice A, `SELECT * FROM users` shows only A's users.
2. The hook. Register it so every `@Transactional` boundary sets the variable.
3. `TenantIsolationIT`: seeds practice A and B with users; logs in as A; asserts `GET /me` and `GET /users` never return B; asserts a direct repository call in a test transaction with A's context cannot `findById` B's user; asserts a query with no tenant context throws. Include one lazy-load case and one native query case.
4. Make this test a required check in CI.
5. Audit: update a user's `full_name` through the API, assert one `audit_log` row with the right `user_id`; update the practice address, assert a row with `table_name = 'practices'` and the right `practice_id`; assert `UPDATE practices SET status = 'active'` as `qvety_app` fails with permission denied. Original assertion continues: one row with the right `user_id`, `before.full_name`, `after.full_name`, and no `password_hash` key in either. Assert `UPDATE audit_log` and `DELETE FROM audit_log` as `qvety_app` fail. Assert practice B cannot read A's audit rows.

## Ask Claude

- "Explain `current_setting('app.practice_id', true)` — what does the second argument do and why does it matter?"
- "What is the difference between `ENABLE` and `FORCE ROW LEVEL SECURITY`?"
- "Show how to set a Postgres session variable at the start of every Spring transaction, and prove it runs before the first query."
- "List the ways Hibernate could run a query without my hook firing."
- "Write a generic PL/pgSQL audit trigger using `to_jsonb(OLD)` and `to_jsonb(NEW)` that removes a list of sensitive keys, and explain why it must be `SECURITY DEFINER`."

## Done when

- `TenantIsolationIT` green and in CI.
- App runs as `qvety_app`; Flyway as `qvety_owner`.
- Every tenant table has the audit trigger; the audit test is green.
- `docs/domain/tenant-boundary.md` and `docs/domain/audit.md` written.

## Self-check

- What does `qvety_app` see if `app.practice_id` is not set? (Should be nothing. Verify.)
- Why `SET LOCAL` and not `SET`?
- Name three ways a leak could still happen and which rule in CLAUDE.md prevents each.
- Why is the audit log a trigger and not a call in each service method? What does the trigger not know that a service would?
