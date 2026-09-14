## Migration `V10__platform.sql`

```sql
CREATE TABLE platform_users (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  email text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  full_name text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  session_version integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE platform_settings (key text PRIMARY KEY, value text NOT NULL);
CREATE TABLE platform_audit_log (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  platform_user_id uuid REFERENCES platform_users(id),
  action text NOT NULL,
  target_type text NOT NULL,
  target_id uuid,
  details jsonb,
  at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE practices ADD COLUMN closed_at timestamptz;
INSERT INTO platform_settings VALUES ('trial_days','30'), ('closed_retention_days','90');
```
Grants for `qvety_app`: `SELECT/INSERT/UPDATE` on `platform_users` and `platform_settings`, `SELECT/INSERT` on `platform_audit_log`. Not in the RLS list. The migration also declares the export table list (a SQL comment or a `platform_settings` row `export_tables`) and backfills every tenant table from parts 03–10.

Platform users are a separate table, not a flag on `users`: `users` is under RLS and has a mandatory `practice_id`; mixing would need a nullable tenant column and a policy exception on the one table that must never have one.

## System context

`SystemContext.run(Runnable)` / `call(Callable)`, package-private in `com.qvety.platform`, sets a thread-local flag the `TenantTransactionHook` checks: flag set → skip `SET LOCAL app.practice_id`; flag not set and no user → throw. ArchUnit or package visibility keeps feature code out.

## Status filter

Spring Security filter after JWT: loads practice status from a cache (refresh on change, TTL 60 s). `suspended` + write method → 423; `closed` → 403 unless path is `/api/v1/practice/export`. `past_due` adds a response header the UI turns into a banner.

## Practice creation

`PracticeAdminService.create(name, country, adminPhone, adminEmail?, adminName)` in one transaction under system context: insert practice (`trial_ends_at = now + trial_days`), `StarterCatalogSeeder.apply(country, practiceId)` (fails on unmapped country), insert admin user (phone required, email optional, per part 03) with temporary password and `must_change_password`, insert platform audit row. Returns the temporary password once.

## Export

`ExportService.export(practiceId)` under the practice's tenant context (not system context): one query per table in the export list, streamed via `StreamingResponseBody`. Shape per the spec. Test asserts the export table set equals the RLS set.

## API

| Method | Path | Who |
| --- | --- | --- |
| POST | `/api/platform/auth/login` | platform user |
| GET / POST | `/api/platform/practices` | platform |
| PUT | `/api/platform/practices/{id}` | platform (country, currency, identity) |
| POST | `/api/platform/practices/{id}/status` | platform; `{status, reason}`; `suspended`, `closed`, `active` |
| GET | `/api/platform/practices/{id}/export` | platform; audited |
| GET | `/api/v1/practice/export` | practice admin |

## Backup sidecar

`docker/docker-compose.yml` service `backup`: `postgres:18` image with `crond`, runs `pg_dump -Fc` at 03:00 `Africa/Cairo` to `/backups`, then `mc cp` to `qvety-backups`, prunes older than 30 days. Restore: `pg_restore -d qvety_drill <dump>`, start the app against `qvety_drill`, log in. Drill log lives in the `backup-restore` spec's companion runbook (`deploy/README.md` from part 16) with date and person.

## Angular

`/admin`: platform login, practices list, create practice (shows the temporary password once), status change with reason, download export. Practice side: `/settings/export` button. `past_due` banner in the practice layout.

## Phone layout

Reference: `docs/design/MOBILE.md` layout skeleton. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
