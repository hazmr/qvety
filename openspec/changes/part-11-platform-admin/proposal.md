## Why

Qvety sells to practices. The super admin creates a practice, watches its status, suspends one that does not pay, and must answer "what happens to my data if you disappear" with an export and a drilled backup.

## What Changes

- Platform tables `platform_users`, `platform_settings`, `platform_audit_log`; `practices.closed_at`. None under RLS.
- Explicit `SystemContext`, package-private to `platform`, that lets the tenant hook skip setting the variable.
- Practice status enforcement filter: `past_due` banner, `suspended` blocks writes (423), `closed` allows export only.
- Practice creation in one transaction: row, trial end, starter catalog, first admin user (by phone, email optional), platform audit row.
- Full practice export as streamed JSON plus attachments manifest; export table list backfilled for every tenant table so far.
- Nightly `pg_dump` sidecar to MinIO; restore drill logged.
- Angular `/admin` area and practice-side export button.

## Capabilities

### New Capabilities
- `platform`: super admin, practice lifecycle and status enforcement, practice creation, export.
- `backup-restore`: nightly dump, retention, restore drill cadence.

### Modified Capabilities
- `tenant-boundary`: system context as the only way to run without a tenant, and the export list joins the RLS/audit list assertion.

## Non-goals

- Plans, subscriptions, payments, and the automatic status job (part 12). Status changes here are manual.
- Off-site second-provider backup (part 16).
- Any platform UI beyond practices list, create, status, export.

## Impact

- `V11__platform.sql`, `com.qvety.platform` (only package allowed to use system context), status filter, `ExportService`, backup service in `docker/docker-compose.yml`.
- `TenantIsolationIT` gains platform cases and the export-list assertion.
