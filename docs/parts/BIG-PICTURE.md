# Qvety — the big picture on one page

Read this when the part files feel like too many trees. Everything below is explained in full somewhere else in this folder; this page only shows how it fits.

## What we are building

Software for a veterinary clinic in Egypt. One clinic = one **practice**. The clinic's day is:

```
client walks in with pet  →  front desk books / checks in  →  vet examines
→  vet writes note, gives vaccine, writes prescription  →  front desk makes invoice, takes money
→  weeks later: system says "this pet's vaccine is due", staff sends WhatsApp  →  client comes back
```

That loop is the product. Everything else exists to make that loop safe, Arabic, and sellable.

## The five ideas that never change

1. **One database, many clinics, no leaks.** Every clinic row has `practice_id`. Postgres itself (row-level security) refuses to show one clinic another clinic's rows. The app cannot bypass it even with a bug.
2. **Medical and money records are never edited or deleted.** A signed note, a vaccination, a payment: wrong ones are *voided* or *retracted* with a reason, and the wrong version stays visible. Corrections are added, not overwritten.
3. **Every change is logged by the database.** Who changed what, before and after. A trigger does it, so no code path can forget.
4. **The server never sends messages.** No SMS, no email, no WhatsApp API. Staff press a button that opens WhatsApp on their own phone with the text ready.
5. **The clinic owns its data.** One button exports everything. Nightly backup off-site. Written in the contract.

## The pieces

```
Browser (Angular, Arabic RTL / English LTR)
        │  /api/v1/...   JSON, JWT in header
        ▼
Spring Boot (one jar, also serves the Angular files)
        │  runs as DB user qvety_app (cannot bypass RLS)
        ▼
PostgreSQL 18  ──  MinIO (files: logo, photos, attachments, payment proofs)
        ▲
Flyway migrations run as qvety_owner (owns tables, sets up RLS + triggers)
```

Two kinds of tables:

| Zone | Has `practice_id`? | Who reaches it | Examples |
| --- | --- | --- | --- |
| Tenant | yes | the clinic, through RLS | clients, patients, appointments, visits, notes, invoices |
| Platform | no | you (super admin) only, in explicit "system context" | practices, plans, subscriptions, platform users |

## The data, in one breath

```
practice
 ├─ users (role + is_veterinarian flag)
 ├─ rooms, appointment types, services      ← reference data, generic CRUD
 ├─ clients ── patients ── weights, allergies, care reminders
 ├─ appointments (per vet, per room, no overlap)  →  visits
 │                                                    ├─ vitals
 │                                                    ├─ clinical note (draft → finalized → addenda / void)
 │                                                    ├─ vaccinations  →  recalls (computed)  →  recall contacts
 │                                                    ├─ prescriptions → events → controlled-substance log
 │                                                    └─ attachments
 ├─ invoices ── lines, payments (idempotent)
 └─ audit log (trigger-written)

platform: practices, platform users, plans → plan versions → subscriptions → sub. invoices → sub. payments
```

Full column-level diagram: `erd.md`. Same thing as one poster: `erd-full.md`.

## The sixteen parts, as a story

| Parts | You build | You learn | Clinic gets |
| --- | --- | --- | --- |
| 01–02 | repo, one table, one endpoint, one page | Spring Boot, Flyway, Angular, Docker, Testcontainers | nothing yet |
| 03 | login, roles, vet flag | Spring Security, JWT, revocation | staff accounts |
| 04 | RLS + audit trigger | Postgres security, transactions | the "no leaks" promise |
| 05 | Arabic + RTL | i18n, direction, locale | Arabic UI |
| 06–08 | clients, search, patients | base entity, validation, paging, Arabic normalization, phone, specifications | the file cabinet |
| 09–10 | reference data, schedule, whiteboard | generic CRUD, exclusion constraints, state machine | the day sheet |
| 11–12 | super admin, export, backup, plans, payments | system context, streaming, scheduled jobs, object storage | you can sell it and keep it running |
| 13 | visits, notes, vaccines, prescriptions, certificate | immutability triggers, `@Immutable`, print | the medical record |
| 14 | invoices, payments, daily cash | idempotency, money, gapless numbering | the money |
| 15 | recalls, care reminders | read model, native query | the feature that pays for itself |
| 16 | server, TLS, backups, contract, onboarding | ops, running a business | a real customer |

Every part has the same shape: **migration → entity → service → controller → test (with a cross-clinic case) → Angular page in both languages**. By part 08 that shape is muscle memory; after that only the business changes.

## What "done" means for the whole thing

After part 16, one clinic runs its whole day on Qvety without you in the room, at least one recall turned into a booked visit, and they paid the second month. Then, and only then, `backlog.md` becomes a to-do list, in the order the clinic asks.

## Where to look

| Question | File |
| --- | --- |
| How do I work through a part? | `00-workflow.md` |
| What does this word mean? | `glossary.md` |
| What are the tables? | `erd.md` |
| What are we deliberately not building? | `backlog.md` |
| What does Claude follow in the repo? | `CLAUDE.md.template` |
| Where am I? | `progress.md` |
