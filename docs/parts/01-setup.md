# Part 01 — Setup

## Business

Before any code: what does a veterinary clinic in Egypt do in a day, and what does software need to hold?

Write one page in `docs/domain/clinic-day.md` in your own words, covering: the front desk opening the day, a client arriving with a pet, the vet examining, a prescription, payment, and the reminder for next time. Use the glossary terms. Ask Claude to challenge it: "What did I miss that a real clinic would need on day one?" Keep the answer short; you will refine it in every later part.

This page is the seed of the product. Everything you build must trace back to a sentence in it.

## Stack you learn

- Maven project layout, `pom.xml`, the Spring Boot parent and BOM.
- Angular CLI workspace inside the same repository.
- Docker Compose for Postgres 18 and MinIO.
- How `CLAUDE.md` shapes what Claude does in a repository.
- Spring profiles: `local`, `test`, `prod`, and what each may load.

## Steps

1. Create the repository (`qvety`), private. Add a LICENSE of your choice (proprietary is fine: "All rights reserved" with your name). Add `.gitignore` for Java, Node, IDE files, `.env`.
2. Copy `CLAUDE.md.template` from this folder to `CLAUDE.md`. Copy this folder's part files into `docs/parts/`. Copy `glossary.md` to `docs/domain/glossary.md`. Copy `backlog.md` to `docs/backlog.md`.
3. Generate the Spring Boot project: Spring Boot 4.1.x, Java 25, Maven, packaging jar, group `com.qvety`. Dependencies: Web, Data JPA, Validation, Security, Flyway, PostgreSQL driver, Lombok, Testcontainers, Actuator. Add MapStruct 1.6.3 and `lombok-mapstruct-binding` by hand. Add springdoc-openapi 3.1.x.
4. `docker/docker-compose.yml`: `postgres:18` (database `qvety`, owner user `qvety_owner`), `minio/minio`. Ports 5432 and 9000/9001. If 5432 is taken on your machine, map 5433 and put that in `application-local.yml`.
5. `application.yml` with the JPA rules from the plan (`ddl-auto=validate`, `open-in-view=false`). `application-local.yml` adds `spring.flyway.locations: classpath:db/migration,classpath:db/seed` and the local database URL; production config never lists `db/seed`. No migrations yet; the app should start against an empty database and fail validation on nothing.
6. `GET /actuator/health` returns `UP`.
7. `web/`: `ng new web --standalone --routing --style=scss`, add `ng-zorro-antd`, add `@jsverse/transloco`, add `@openapitools/openapi-generator-cli`. Add `proxy.conf.json` so `/api` goes to `:8080`. One route, one page that says "Qvety".
8. Maven `frontend-maven-plugin` so `./mvnw package` builds Angular into `static/`. Verify the jar serves the page on `:8080`.
9. CI: GitHub Actions running `./mvnw verify` with Docker available (Testcontainers needs it).

## Ask Claude

- "Explain every line of the generated `pom.xml`. Which versions come from the Boot BOM and which did we pin?"
- "Why does Spring Boot run Flyway before Hibernate, and what happens if validate fails?"
- "Set up `frontend-maven-plugin` so the Angular build lands in the jar. Explain the phases."

## Done when

- `./mvnw verify` green (no tests yet, but compiles and starts a context against Testcontainers).
- `./mvnw package` produces one jar that serves the Angular page and `/actuator/health`.
- `CLAUDE.md`, `docs/domain/clinic-day.md`, `docs/parts/` committed.

## Self-check

- Can you explain what the Boot parent POM gives you?
- Can you explain why the Angular app is served from the jar rather than separately?
- Where does Claude read its instructions from in this repository?
