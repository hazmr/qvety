## Why

Before any feature: a repository, a Spring Boot 4.1 + Angular 22 skeleton, Postgres 18 and MinIO in compose, one jar that serves the Angular build, and CI running `./mvnw verify` with Docker. Also the one page that seeds the product: a day in an Egyptian veterinary clinic, in the owner's words.

## What Changes

- Repository with `CLAUDE.md`, `docs/`, LICENSE, `.gitignore`.
- Maven project: Spring Boot 4.1.1 parent, Java 25, Web, Data JPA, Validation, Security, Flyway, PostgreSQL, Lombok, Testcontainers, Actuator, MapStruct 1.6.3, springdoc 3.1.1.
- `docker/docker-compose.yml`: `postgres:18` (database `qvety`, owner `qvety_owner`), `minio`.
- `application.yml` (`ddl-auto=validate`, `open-in-view=false`), `application-local.yml` (adds `db/seed`), `application-prod.yml` (never `db/seed`).
- Angular workspace `web/` with NG-ZORRO, Transloco, openapi-generator, `proxy.conf.json`, one page saying "Qvety".
- `frontend-maven-plugin` so `./mvnw package` puts the Angular build in the jar.
- GitHub Actions CI.

## Capabilities

### New Capabilities
- none (tooling and skeleton only; `skip_specs: true`)

### Modified Capabilities
- none

## Non-goals

- Any migration or table. The app starts against an empty database.
- Authentication.

## Impact

- Everything later builds on this layout. `web/src/app/api` is generated and never edited.
