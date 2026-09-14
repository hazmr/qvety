## Why

Parts 01–15 make software. This part makes a business: one clinic, for money. A clinic owner needs to trust you with their records, understand the price, and know what happens to their data. No new tables; production deployment, an offer, a contract, an onboarding checklist, and support rules.

## What Changes

- Production stack on a VPS: `deploy/docker-compose.prod.yml` (app jar, Postgres 18, MinIO, Caddy with automatic TLS, backup sidecar), `Caddyfile`, `release.sh`, `restore.sh`, `deploy/README.md`.
- Secrets in a server `.env` (mode 600), generated with `openssl rand -base64 32`.
- Off-site backup mirror to a second provider; restore drill from the off-site copy.
- Uptime check on `/actuator/health` and a disk alert; JSON logs with rotation and no personal data.
- Offer page and contract in Arabic; onboarding checklist; training and support rules; 60-day success criteria.
- A second tenant "Qvety demo" with synthetic data for demonstrations.

## Capabilities

### New Capabilities
- `operations`: what is promised to a clinic (offer, contract, data, support), how the product is run in production, onboarding, and the pilot success criteria.

### Modified Capabilities
- `backup-restore`: off-site copy at a second provider and the pre-pilot drill from that copy.

## Non-goals

- Code features of any kind. Anything a clinic asks for goes to `docs/backlog.md` as an `ask` issue.
- More than one pilot clinic.
- Automated deployment pipelines beyond `release.sh`.

## Impact

- New `deploy/` folder; `docker logs` rotation; logback pattern with ids only.
- Support process in GitHub Issues with labels `clinic:<name>`, `bug`, `question`, `ask`.
- `docs/help/` seeded from the specs after go-live.
