## Purpose

Defines the promises made to a paying clinic and the way the product is operated: the offer, the contract, production deployment, monitoring, logging, updates, onboarding, training, support, and the pilot's success criteria.

## ADDED Requirements

### Requirement: The offer
The offer SHALL be one Arabic page readable in two minutes stating: what it does (schedule, whiteboard, records, prescriptions, vaccination card, invoices, recalls, in Arabic); the price (990 EGP per month or 9,900 per year, up to 5 users, 5 GB, 30 days free); that data is the clinic's, exportable any time, kept 90 days after leaving, backed up nightly off-site; that nothing is sent on the clinic's behalf and no clinical data is accessed without written consent; support on one WhatsApp number, Saturday to Thursday 10:00–20:00 Cairo, same-day reply. Price SHALL NOT be negotiated on the first visit; on the second, the only lever SHALL be the yearly price.

#### Scenario: Owner reads the offer
- **WHEN** a clinic owner reads the page
- **THEN** price, trial, data ownership, support hours, and what is not done are all answered without asking

### Requirement: The contract
The contract SHALL be short, in plain Arabic, and state: the data promise, the price, the trial length, one month notice, what happens on non-payment (`past_due` banner, then `suspended` read-only after grace days, matching the platform flow), and no liability for clinical decisions. Someone who has signed contracts before SHALL read it once.

#### Scenario: Non-payment clause
- **WHEN** the contract's non-payment clause is compared with the platform status flow
- **THEN** they match

### Requirement: Production deployment
Production SHALL run on one VPS (2 vCPU, 4 GB, Ubuntu LTS, region close to Egypt, fixed IP) with a domain and automatic TLS. Only ports 80 and 443 on the reverse proxy SHALL be exposed. Secrets SHALL live in a server `.env` with mode 600, never in git. A fresh VPS SHALL reach a working `https://` login in under one hour following only `deploy/README.md`.

#### Scenario: Fresh VPS
- **WHEN** the README is followed on a new server
- **THEN** login over HTTPS works within one hour and no database port is reachable from outside

#### Scenario: Security headers
- **WHEN** the production site is checked with a header scanner
- **THEN** the part 03 headers are present and CSP contains no `unsafe-inline`

### Requirement: Monitoring and alerting
An external checker SHALL hit `/actuator/health` every 5 minutes and alert the operator's phone. Disk usage above 80 % SHALL alert. Actuator SHALL be internal only except `/actuator/health` through the proxy. A deliberate stop SHALL prove the alert once before go-live.

#### Scenario: App stopped
- **WHEN** the app container is stopped on purpose
- **THEN** the operator's phone is alerted within 10 minutes

### Requirement: Logs contain no personal data
The app SHALL emit JSON logs to stdout with rotation (14 days). No log line SHALL contain a client name, phone, or clinical text; only ids.

#### Scenario: Log scan
- **WHEN** a day of logs is searched for a seeded client's name or phone
- **THEN** nothing is found

### Requirement: Updates are forward-only
A release SHALL be `git pull`, `./mvnw package`, `docker compose up -d --build`, with migrations at startup, done by `deploy/release.sh`. A rollback SHALL be the previous image tag; a database restore SHALL be used only when a migration cannot be lived with. Most rollbacks are fix-forward.

#### Scenario: Release
- **WHEN** `release.sh` runs
- **THEN** the new jar is live and Flyway has applied pending migrations, or the script fails loudly and the old container keeps running

### Requirement: Onboarding checklist
Onboarding SHALL be one half-day visit: create the practice (country `EG`) and hand the admin login on paper; fix services, prices, rooms, appointment types, hours, logo, address, phone, VAT; add every staff member and have each change their password on their own device; enter today's clients and patients live (no batch import); book tomorrow together and show the whiteboard; one visit end to end done by the staff while you watch; show the export button and the History tab; set the recall window and message and show the WhatsApp button on one real patient.

#### Scenario: Staff do the first visit
- **WHEN** the first end-to-end visit is performed
- **THEN** staff do every step themselves; the operator only watches

### Requirement: Training rule
Nobody SHALL be trained on a screen they will not use. Front desk: schedule, whiteboard, clients, invoices, payments. Vet: visit, note, prescription, certificate, recalls. Admin: users, settings, export. Twenty minutes each, at their desk, on their data.

#### Scenario: Front desk training
- **WHEN** the front desk is trained
- **THEN** the session covers only their five screens

### Requirement: Support rule
The clinic SHALL reach the operator on WhatsApp. Every question SHALL become a GitHub Issue the same day, labelled `clinic:<name>` and one of `bug`, `question`, `ask`. `bug` SHALL be fixed within a week or the clinic told when. `ask` SHALL go to `docs/backlog.md` with the clinic name as evidence. A `question` asked twice SHALL become a user guide line under `docs/help/`. Issues SHALL close with what the clinic was told.

#### Scenario: Question asked twice
- **WHEN** the same question arrives from two people
- **THEN** a line is added to the user guide and the issue references it

### Requirement: 60-day success criteria
Before day one, success SHALL be defined as all of: the clinic uses it for every visit without the operator present; the owner names one thing it saves them; at least one recall produced a booked visit; the clinic pays the second month. If any is false, the pilot is a lesson, not a customer, and the reason SHALL be written down.

#### Scenario: Day 60 review
- **WHEN** the four criteria are checked
- **THEN** each is marked true or false with evidence

### Requirement: Recovery numbers are written
The maximum hours of clinic data lost if the server disappears (backup interval) and the hours until the clinic is working again (restore time) SHALL be written in the operations manual and match the drilled restore.

#### Scenario: Owner asks
- **WHEN** the owner asks what happens if the server disappears tonight
- **THEN** both numbers are answered from the manual
