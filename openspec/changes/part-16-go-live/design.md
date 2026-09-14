## `deploy/`

- `docker-compose.prod.yml`: `app` (built jar image), `postgres:18` (named volume, no host port), `minio` (no host port), `caddy` (80/443, automatic TLS), `backup` sidecar from part 11 plus `mc mirror` to the second provider. Log driver options `max-size`, `max-file` for 14 days.
- `Caddyfile`: reverse proxy to `app:8080`; `/actuator/*` blocked except `/actuator/health`; HSTS.
- `.env` (not in git, mode 600): `QVETY_JWT_SECRET`, `POSTGRES_PASSWORD`, `QVETY_APP_DB_PASSWORD`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, offsite bucket credentials. Generated with `openssl rand -base64 32`.
- `release.sh`: `set -euo pipefail`; `git pull --ff-only`; `./mvnw -q package`; `docker compose -f deploy/docker-compose.prod.yml up -d --build`; wait for `/actuator/health`; fail loudly.
- `restore.sh`: download a dump, `pg_restore` into a named database, print the next command to point the app at it.
- `README.md`: exact commands from a fresh VPS to HTTPS login, timed.

## Logging

Logback JSON encoder to stdout; MDC with request id, practice id, user id; no DTO `toString` in logs; a unit test asserting log lines from a seeded request contain no client name or phone.

## Spring

`application-prod.yml`: actuator on the internal management port, `health` only public through Caddy; `spring.flyway.locations` without `db/seed`; app refuses to start without the secrets.

## Documents (not code)

- Offer page (Arabic, one page).
- Contract (Arabic, short).
- Operations manual: onboarding checklist, training rule, support rule, 60-day criteria, recovery numbers, runbook line for "alert at 21:00 Thursday", drill log.
- "Qvety demo" practice as a second tenant with synthetic data only.
