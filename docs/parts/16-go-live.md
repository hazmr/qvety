# Part 16 — Go-live: one clinic, for money

## Business

Parts 01–15 make software. This part makes a business. Nothing here is code you learn; it is what a clinic owner needs to trust you with their records and pay for it. Write `docs/domain/go-live.md` and keep it current; it is the operations manual.

**The offer.** One page, Arabic, that a clinic owner reads in two minutes:
- What it does: schedule, whiteboard, records, prescriptions, vaccination card, invoices, recalls. In Arabic.
- What it costs: 990 EGP per month or 9,900 per year, up to 5 users, 5 GB (part 12). 30 days free. Do not negotiate on the first visit; on the second, the only lever is the yearly price, never a custom monthly one.
- What happens to their data: it is theirs, exportable any time, kept 90 days after they leave, deleted after. Backed up nightly, off-site.
- What you do not do: no SMS, no messages sent on their behalf, no access to their clinical data without written consent.
- Support: one WhatsApp number, Saturday to Thursday 10:00–20:00 Cairo, reply same day. Outside hours the app keeps working; nothing in it depends on you being awake.

**The contract.** Short. Plain Arabic. The data promise above, the price, the trial length, the notice period (one month), what happens on non-payment (matches the status flow from parts 11–12: `past_due` banner, then `suspended` reads-only after grace days), and that you are not liable for clinical decisions. Have someone who has signed contracts before read it once.

**Onboarding checklist** (one visit, half a day, you present):
1. Create the practice in `/admin` (country `EG`, starter catalog applied). Hand the admin login on paper.
2. With the owner: fix the services list and prices (the starter catalog is a guess), rooms, appointment types, working hours, logo, address, phone, VAT number if any.
3. Add every staff member with role and vet flag. Each one logs in on their own device and changes the password while you watch.
4. Enter today's clients and patients as they come in, live. Do not batch-import old records on day one; that is the backlog CSV import, and only if they ask.
5. Book tomorrow's appointments together. Show the whiteboard on the front desk screen and the vet's phone.
6. First visit end to end: check in, vitals, note, finalize, prescription print, invoice, payment, close visit. You do nothing; they do it; you watch.
7. Show the export button and the "History" tab. Say out loud: "this is yours; this is who changed what."
8. Set the recall window and message. Show the WhatsApp button on one real patient.

**Training rule.** Nobody is trained on a screen they will not use. Front desk: schedule, whiteboard, clients, invoices, payments. Vet: visit, note, prescription, certificate, recalls. Admin: users, settings, export. Twenty minutes each, at their desk, on their data.

**Support rule.** The clinic talks to you on WhatsApp; you talk to yourself in GitHub Issues on the private repo. Every WhatsApp question becomes an issue the same day, labelled `clinic:<name>` and one of `bug`, `question`, `ask`. Nothing lives only in a chat. Weekly: `bug` fixed within a week or told when; `ask` goes to `docs/backlog.md` with the clinic name as evidence; `question` asked twice becomes a line in the user guide (`docs/help/`, seeded from `docs/domain/`). Close the issue with what you told the clinic. This is the whole support system until there are ten clinics.

**Success after 60 days**, decided before starting: the clinic uses it for every visit without you present; the owner can name one thing it saves them; at least one recall produced a booked visit; they pay the second month. If any is false, the pilot is a lesson, not a customer. Write down which and why.

## Stack you learn

- A VPS (2 vCPU, 4 GB, Ubuntu LTS) in a region close to Egypt with a fixed IP; a domain; DNS.
- `deploy/docker-compose.prod.yml`: `app` (the jar), `postgres:18` with a named volume, `minio`, `caddy` (automatic TLS), `backup` sidecar from part 11. No ports exposed except 80/443 on Caddy.
- Secrets: an `.env` on the server with permissions `600`, never in git; JWT secret, database passwords, MinIO keys generated with `openssl rand -base64 32`.
- Off-site backup: the nightly `pg_dump` from part 11 also copies to a bucket at a second provider (`mc mirror`). A restore drill from the off-site copy, before the first clinic.
- Uptime: an external free checker hitting `/actuator/health` every 5 minutes, alerting your phone. Disk usage alert at 80 %.
- Logs: JSON logs from the app to stdout, `docker logs` with rotation (`max-size`, `max-file`), 14 days. No log line ever contains a client name, phone, or note; log ids.
- Updates: `git pull`, `./mvnw package`, `docker compose up -d --build`; migrations run at startup; a `deploy/release.sh` that does exactly that and nothing clever. A rollback is the previous image tag plus a database restore only if the migration cannot be lived with; write down that forward-only means most rollbacks are a fix-forward.
- Actuator exposed on the internal network only; `/actuator/health` public through Caddy, nothing else.

## Steps

1. `deploy/` folder with compose, Caddyfile, `release.sh`, `restore.sh`, `README.md` with the exact commands to bring the stack up from a fresh VPS in under an hour. Test it on a fresh VPS; time it.
2. Domain and TLS working. Security headers from part 03 verified with a header checker; CSP has no `unsafe-inline`.
3. Off-site backup running; restore drill done from the off-site copy into a scratch database on your laptop; entry in `docs/domain/backup-restore.md`.
4. Uptime check and disk alert armed; deliberately stop the app once and confirm your phone rings.
5. Create your own real practice record as a second tenant ("Qvety demo") for showing the product; nothing clinical in it is real.
6. Write the offer page, the contract, and `docs/domain/go-live.md` with the onboarding checklist and the 60-day criteria.
7. Do the onboarding at the pilot clinic. Fill the support log for 30 days.

## Ask Claude

- "Write a production `docker-compose.yml` for a Spring Boot jar, Postgres 18, MinIO, and Caddy with automatic TLS, with no database port exposed. Explain each line."
- "Review my Caddyfile and Spring security headers for a SPA: what is missing, what is too permissive?"
- "Write `release.sh` and `restore.sh` as boring shell scripts that fail loudly."
- "What must a Spring Boot app never log in a clinic context, and how do I make sure with logback?"
- "Read `docs/domain/go-live.md` and tell me what a clinic owner would ask that it does not answer."

## Done when

- A fresh VPS to a working `https://` login in under one hour, following only `deploy/README.md`.
- Off-site restore drill logged.
- Uptime alert proven.
- Offer page and contract exist in Arabic.
- One clinic onboarded; support log started; 60-day criteria written down before day one.

## Self-check

- What is the first thing you do when the uptime alert fires at 21:00 on a Thursday? Write the runbook line.
- If the VPS disappears tonight, how many hours of clinic data are lost and how long until the clinic is working again? Both numbers must be in `go-live.md`.
- What did the owner ask on the first visit that the offer page did not answer?
