## Why

A practice is one clinic business and the tenant every other record belongs to. Nothing else can be built until the practice row exists and can be read end to end (migration, entity, DTO, endpoint, Angular page).

## What Changes

- First Flyway migration `V1__practices.sql` with the `practices` platform table and the `practice_status` enum.
- `GET /api/v1/practice` returning the caller's own practice.
- Temporary `X-Practice-Id` header to select the practice. No authentication yet; part 03 deletes the header.
- Dev seed `R__dev_practice.sql` with one synthetic practice and a fixed uuid.
- First Testcontainers integration test and first Angular page that calls the generated client.

## Capabilities

### New Capabilities
- `practice`: identity, country defaults, and status of one clinic business; the tenant root.

### Modified Capabilities
- none

## Non-goals

- Editing the practice (part 03, needs the admin role).
- Status enforcement and status changes (parts 11 and 12).
- Logo upload (part 13, needs object storage).
- Any country other than `EG`.

## Impact

- New: `src/main/resources/db/migration/V1__practices.sql`, `db/seed/R__dev_practice.sql`, package `com.qvety.practice`, Angular `features/practice`.
- `docs/parts/erd.md` gains the `practices` table.
- The `X-Practice-Id` header is scaffolding and is removed in part 03.
