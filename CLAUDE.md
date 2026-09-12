# CLAUDE.md

## What this is

Qvety is a veterinary practice management system for Egyptian clinics. Spring Boot backend, Angular frontend, PostgreSQL. Multi-tenant by practice. Arabic-first with English.

```text
pom.xml                 Spring Boot 4.1.x, Java 25, Maven
src/main/java/com/qvety/
  config/               security, jpa, openapi, tenant transaction hook
  tenant/               base entity, tenant context, RLS session variable, audit read endpoint
  auth/                 login, JWT, session version, rate limit
  platform/             super admin: practices, status, export, plans, subscriptions (system context only)
  <feature>/            one package per business area: clients, patients, scheduling, clinical, billing, recalls
src/main/resources/db/migration/   Flyway, forward-only; never seeds
src/main/resources/db/seed/        Flyway repeatable dev seeds; local and test profiles only
src/main/resources/catalog/        per-country starter catalog SQL, applied at practice creation
deploy/                 production compose, Caddyfile, backup sidecar, restore script (part 16)
src/test/java/com/qvety/           Testcontainers integration tests
web/                    Angular workspace; web/src/app/api is generated, never edited
docs/domain/            business rules in plain language
docs/parts/             the learning path, one file per part
docs/backlog.md         post-pilot items; never built without an explicit part
docs/help/              user guide, seeded from docs/domain after go-live
```

## Commands

```bash
docker compose -f docker/docker-compose.yml up -d     # postgres:18, minio
./mvnw spring-boot:run                                # backend on :8080
./mvnw verify                                         # all tests incl. Testcontainers
cd web && npm start                                   # angular on :4200, proxies /api to :8080
cd web && npm run api:generate                        # regenerate client from /v3/api-docs
./mvnw package                                        # one jar with the angular build inside
deploy/release.sh                                     # production: pull, build, compose up (part 16)
```

## Rules

- **This is a clean-room rewrite.** Never read, reference, port, or reproduce code from any other veterinary system, including the previous Qvety codebase. Work only from `docs/` and the task at hand.
- **Every tenant table has `practice_id`** and an RLS policy. The application connects as `qvety_app`, which cannot bypass RLS. Flyway connects as `qvety_owner`. Adding a tenant table means, in the same migration: its RLS policy, its audit trigger, its entry in the practice export, and a case in `TenantIsolationIT`.
- **No soft delete.** There is no `deleted_at`. Clients and patients get `archived_at`; reference data gets `active`; clinical and financial rows are never removed. Unique constraints stay simple because of this.
- **Authorization is `role` plus `is_veterinarian`.** Clinical acts (finalize, prescribe) check the flag, never the role. Every JWT carries `session_version`; the filter rejects a token whose version no longer matches the user row.
- **Platform tables** (practices, plans, subscriptions, subscription payments, platform audit) have no `practice_id` and are reached only from the `platform` package under an explicit system context. Never import `platform` from a feature package.
- **Migrations are forward-only.** Never edit an applied migration. `ddl-auto=validate` must pass.
- **JPA rules:** `open-in-view=false`; no second-level cache; entities use `@Getter @Setter @NoArgsConstructor` only; DTOs are records; native queries for search and reports.
- **MapStruct:** `unmappedTargetPolicy = ERROR`; never map `id`, `practiceId`, `createdAt`, `updatedAt` from inbound DTOs.
- **Generic CRUD** is allowed only for reference data listed in `docs/domain/reference-data.md`. Everything else gets explicit service code.
- **Evidence tables** are append-only; entities are `@Immutable`; triggers reject UPDATE/DELETE. Trigger functions are `SECURITY DEFINER`, owned by `qvety_owner`, with `EXECUTE` revoked from `qvety_app`.
- **Snapshot names and prices** into rows that must not change later (`finalized_by_name`, invoice line `description` and `unit_price`).
- **No outbound messaging.** Recalls open WhatsApp on the user's device with a prefilled message; the server never sends SMS, email, or WhatsApp.
- **Page size is capped** at 100 (`spring.data.web.pageable.max-page-size`).
- **Money** is `numeric(12,2)` with a currency column, never floating point.
- **Text** shown to users comes from `web/src/assets/i18n/*.json`. No hard-coded strings in templates. Every screen works in `ar` (RTL) and `en` (LTR).
- **Tests:** one integration test per feature over Testcontainers; it must include a cross-tenant case. Unit tests for pure helpers (normalization, phone).
- **`erd.md` is the schema of record.** Update it in the same commit as any migration.
- **Never commit real clinic, client, or patient data.** Synthetic names and `*.example.com` only.
- **Secrets come from the environment.** JWT secret, database passwords, MinIO keys. The app refuses to start outside `local` without them. Nothing secret in `application*.yml` or in git.
- **Do not build ahead of the current part.** If a task needs something from a later part, stop and say so.
- **`docs/backlog.md` is not a to-do list.** Items there are built only when a clinic asks and the owner moves them into a part file.

## Working style

- Propose the migration and API shape before writing Java.
- Explain any non-obvious design choice in one or two sentences.
- When reviewing, one finding per line with `file:line`, no praise.
- Keep code comments and docs in plain English.
- Never write a migration or a service method containing a business rule unless asked with the word "scaffold". Propose, explain, then wait.
- When a test fails: explain the cause first. Never change an assertion.
- When asked to review, one finding per line, `file:line`, no praise, no fixes unless asked.
