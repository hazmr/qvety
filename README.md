# Qvety

Veterinary practice management for Egyptian clinics. Spring Boot 4 + Angular 22 + PostgreSQL 18. Arabic-first.

Read `CLAUDE.md` for the rules, `docs/parts/00-workflow.md` for how work is done, `docs/parts/BIG-PICTURE.md` when lost.

## Run locally

```bash
docker compose -f docker/docker-compose.yml up -d        # postgres on :5433, minio on :9000 / console :9001
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # backend on :8080
cd web && npm install && npm start                        # angular on :4200, /api proxied to :8080
```

## Build and test

```bash
./mvnw verify                 # compiles, runs *IT tests over Testcontainers, builds angular into the jar
./mvnw verify -Dskip.web      # same, without the angular build (faster while working on the backend)
./mvnw package -DskipTests    # one jar: target/qvety-<version>.jar serves the app and /actuator/health
cd web && npm run api:generate   # regenerate web/src/app/api from a running backend's /v3/api-docs
```

## Layout

See the tree in `CLAUDE.md`.

## Design

`docs/design/brand.md` is the brand: mark, color tokens, type, icon rules, asset list. `docs/design/frontend.md` is how that brand is applied to screens and components; read it before building any Angular screen. Finished logo and icon files are in `web/public/brand/` and `web/public/icons/`; NG-ZORRO theme variables and CSS tokens are in `docs/design/tokens/`.
