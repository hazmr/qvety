# Part 11 — Platform admin: practices, system context, status, backup

## Business

Qvety sells to practices. You (super admin) create a practice, watch its status, suspend one that does not pay (the money side is part 12), and answer the question every clinic owner asks before signing: "what happens to my data if you disappear?" Write `docs/domain/platform.md`:
- Super admin is Qvety staff. Platform role, no practice. Manages practices. Never sees clinical data through the normal path; when a clinic asks for help, the super admin uses the export, with the owner's written consent, and that access is audited.
- Practice status flow: `trial` → `active` (first payment, part 12); `trial` → `past_due` (`trial_ends_at` passed with no payment, part 12); `active` → `past_due` (due date passed, part 12); `past_due` → `suspended` (grace days elapsed, part 12); `suspended` → `active` (payment); any → `closed` (manual only).
- `past_due`: banner in the practice UI. `suspended`: writes blocked, reads allowed. `closed`: export only, no login for staff; data kept 90 days after `closed_at`, then deleted (platform setting; write the number down and put it in the contract).
- **Export.** Any practice, in any status, can get a full export of its own data: one JSON file with every tenant table for that practice, plus a zip of its attachments. The admin of the practice can request it; the super admin can produce it for a `closed` practice. This is the "you own your data" promise.
- **Backup.** The whole database is dumped nightly to MinIO (later, to a bucket in another provider). A restore is drilled before the first paying clinic and once a quarter after. The drill is written down with the date and who did it.

## Stack you learn

- A second role universe: `SUPER_ADMIN` claim, `platform` package, `@PreAuthorize("hasAuthority('PLATFORM')")`.
- System context: an explicit `SystemContext.run(() -> ...)` that lets the tenant hook skip setting the variable, only inside `platform`.
- A Spring Security filter that enforces practice status on every request.
- Streaming a large JSON response (`StreamingResponseBody`) instead of building it in memory.
- `pg_dump` in a sidecar container on a cron, uploading to MinIO with `mc`; `pg_restore` into a scratch database to prove the dump is good.
- Angular: a separate admin layout and routes, guarded by the platform claim.

## Design

`V10__platform.sql`: `platform_users` (`id`, `email` unique, `password_hash`, `full_name`, `active`, `session_version`, timestamps), `platform_settings` (`key`, `value`), `platform_audit_log` (append-only: `platform_user_id`, `action`, `target_type`, `target_id`, `details jsonb`, `at`), `practices.closed_at`. None in the RLS list; grants to `qvety_app` on these tables are `SELECT/INSERT/UPDATE` only where needed, `SELECT/INSERT` on the audit log.

Decision recorded here: platform users are a separate table, not a flag on `users`. `users` is a tenant table under RLS and a platform user has no `practice_id`; mixing them would need a nullable `practice_id` and a policy exception on the one table that must never have one.

`practices.status` enforcement filter reads a cached status per practice (refresh on change, and at most 60 s stale).

Creating a practice, in one transaction: insert the row with `trial_ends_at = now + trial_days`, seed the country starter catalog (part 09), create the first admin user with a temporary password and `must_change_password`, write a platform audit row. The super admin hands the clinic its login on paper or WhatsApp; no email.

Export list: this migration also declares the export table list and backfills every tenant table from parts 03–10. From here on every migration appends to all three lists.

Endpoints under `/api/platform/...`: `POST /auth/login`, `GET/POST /practices` (create takes name, country, admin email, admin full name), `PUT /practices/{id}`, `POST /practices/{id}/status` (`suspended`, `closed`, back to `active`, with a reason, one audit row each), `GET /practices/{id}/export`. Practice-side: `GET /api/v1/practice/export` (admin only).

Export shape: `{ "exported_at", "practice": {...}, "tables": { "users": [...], "clients": [...], ... }, "attachments": [ { "object_key", "filename", "sha256" } ] }`. Every later part adds its tables to the export; the export test asserts the list of tables in the export equals the RLS list.

`docker/docker-compose.yml`: add a `backup` service (`postgres:18` image, `crond`) that runs `pg_dump -Fc` at 03:00 Cairo time to `/backups`, then `mc cp` to the `qvety-backups` bucket, keeps 30 days. `docs/domain/backup-restore.md` holds the restore command and the drill log.

## Steps

1. Migration. `db/seed/R__dev_platform_user.sql` seeds one platform user for local use.
2. `SystemContext` and the hook change. Extend `TenantIsolationIT`: super admin can list practices; super admin calling a tenant endpoint without a practice gets 403; tenant code cannot enter system context (make the class package-private to `platform`).
3. Status filter: suspended practice → any write returns 423 (Locked) with a localized message; reads pass. `closed` → 403 on everything except `/practice/export`. Test both.
4. Export service: one query per tenant table under the practice's tenant context (not system context — the export must go through the same RLS as everything else), streamed. Test: export for practice A contains no row of B; the table list equals the RLS list.
5. Backup sidecar. Run it once by hand, restore into `qvety_drill` on your machine, start the app against it, log in. Write the drill entry.
6. Angular: `/admin` area (login, practices list, create practice, status change with reason, download export), practice-side `/settings/export` button.

## Ask Claude

- "Design a system-context mechanism that is opt-in, package-private, and testable, for a Spring app with an RLS session variable."
- "Write a Spring Security filter that blocks write methods for a suspended practice and passes reads."
- "Stream a multi-table JSON export from Spring without loading it all into memory."
- "Write a compose sidecar that runs `pg_dump -Fc` on a cron and uploads to MinIO, and the matching restore command."

## Done when

- You can create a practice, suspend it, and see writes blocked in the practice UI with reads still working.
- Export downloads and the export test is green.
- One restore drill done and logged in `docs/domain/backup-restore.md`.
- Isolation test covers the platform cases.

## Self-check

- What stops a feature package from calling `SystemContext`?
- Why does the export run under tenant context and not system context?
- What happens to a practice's data when it is `closed`? Is that the right promise? Write it down.
- When was the last restore drill? If you cannot answer from the doc, the part is not done.
