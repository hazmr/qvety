## Migration `V3__rls.sql`

1. `CREATE ROLE qvety_app LOGIN PASSWORD ...` (no `BYPASSRLS`, no `SUPERUSER`). Password comes from the environment at deploy; locally from compose.
2. Grants on `practices`: `SELECT`, and `UPDATE (name, address, phone, vat_number, tax_rate_percent)` only. Policy `practice_self USING (id = current_setting('app.practice_id', true)::uuid)`.
3. `audit_log` table:
   ```sql
   CREATE TYPE audit_action AS ENUM ('insert','update','delete');
   CREATE TABLE audit_log (
     id uuid PRIMARY KEY DEFAULT uuidv7(),
     practice_id uuid NOT NULL,
     user_id uuid,
     table_name text NOT NULL,
     row_id uuid NOT NULL,
     action audit_action NOT NULL,
     before jsonb,
     after jsonb,
     at timestamptz NOT NULL DEFAULT now()
   );
   ```
   Grants for `qvety_app`: `SELECT, INSERT`. Index on `(practice_id, table_name, row_id)`.
4. Function `audit_row()` PL/pgSQL, `SECURITY DEFINER`, owner `qvety_owner`, `REVOKE ALL ON FUNCTION audit_row() FROM PUBLIC, qvety_app`. Uses `to_jsonb(OLD)`/`to_jsonb(NEW)` minus `'{password_hash}'`, reads `current_setting('app.user_id', true)`. For `TG_TABLE_NAME = 'practices'` takes `practice_id` from `NEW.id`/`OLD.id`.
5. A `DO` block over the tenant table list (today: `users`, `audit_log`) that runs `ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`, creates policy `tenant_isolation USING (practice_id = current_setting('app.practice_id', true)::uuid)`, grants CRUD to `qvety_app`, and attaches `AFTER INSERT OR UPDATE OR DELETE FOR EACH ROW EXECUTE FUNCTION audit_row()` to every table except `audit_log`. `practices` also gets the trigger.

`current_setting(name, true)`: the second argument returns NULL instead of raising when the variable is unset, so an unset tenant yields zero rows rather than an error inside the policy.

## Spring

- `application.yml`: Flyway datasource as `qvety_owner`; main datasource as `qvety_app`.
- `TenantTransactionHook`: registered so every `@Transactional` boundary runs `SET LOCAL app.practice_id = ?; SET LOCAL app.user_id = ?` before the first statement. `SET LOCAL` scopes to the transaction; a pooled connection returns clean. No current user and no system context → throw.
- `open-in-view=false` matters here: with it on, lazy loads could run outside the transaction that set the variable.

## API

| Method | Path | Who |
| --- | --- | --- |
| GET | `/api/v1/audit?table&rowId&page&size` | admin |

## Tests

`TenantIsolationIT`: two seeded practices; `GET /me`, `GET /users` never return B; repository `findById` on B's id under A's context is empty; lazy-load and native-query cases; no-context throws; table-list assertion (RLS = audit; export list added in part 11). Audit assertions per the spec. Required check in CI.

## Decisions

- Trigger, not service code, for audit: cannot be forgotten, applies to native queries. It does not know the HTTP request or the business reason; those live in the service and its DTO, not in the audit row.
- `FORCE` so the owner role is also subject to policies when it ever reads through the app path.
