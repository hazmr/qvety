## 1. Repository
- [x] 1.1 Private repo, LICENSE, `.gitignore`
- [x] 1.2 `CLAUDE.md`, `docs/parts/`, `docs/domain/glossary.md`, `docs/backlog.md`

## 2. Backend skeleton
- [x] 2.1 Spring Boot 4.1 Maven project with the dependency set
- [x] 2.2 `docker/docker-compose.yml` with `postgres:18` and MinIO
- [x] 2.3 `application.yml`, `application-local.yml`, `application-prod.yml`
- [x] 2.4 `GET /actuator/health` returns `UP`

## 3. Frontend skeleton
- [x] 3.1 `web/` Angular workspace with NG-ZORRO, Transloco, openapi-generator, `proxy.conf.json`
- [x] 3.2 One route, one page "Qvety"
- [x] 3.3 `frontend-maven-plugin`; `./mvnw package` serves the page from the jar

## 4. CI
- [x] 4.1 GitHub Actions `./mvnw verify` with Docker
- [x] 4.2 `SmokeIT` green over Testcontainers

## 5. Business page
- [x] 5.1 `docs/domain/clinic-day.md` written in the owner's words (six sections, one page)
- [x] 5.2 Ask: "What did I miss that a real clinic would need on day one?"

## 6. Close
- [x] 6.1 `docs/progress.md` Part 01 entry
- [x] 6.2 Self-check answered (Boot parent POM, why the jar serves Angular, where Claude reads instructions)
- [x] 6.3 Committed (`4d22b61`)
