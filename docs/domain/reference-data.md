# Reference data

The small lists a practice configures for itself. `CLAUDE.md` points here for one rule: **generic CRUD is
allowed for the tables on this page and for nothing else.** Everything else gets explicit service code.

## What is on the list

| Table | What it is | Its own columns |
| --- | --- | --- |
| `rooms` | Consultation rooms, theatre, kennels | — |
| `appointment_types` | Kinds of visit | `duration_minutes` (5–480), `color` (`#rrggbb`, optional) |
| `services` | The price list | `price` (`numeric(12,2)`), `currency` |

Every one of them has a `name` and an `active` flag, is under row-level security, and is audited like any
tenant table.

## What makes something reference data

A row is reference data when **it is only ever picked from a list.** It is created, renamed, and one day
retired. It has no status that moves, no dates of its own, nobody works on it.

A table whose rows change status over time is not reference data, however small it looks. Invoices go
draft → issued → paid. Appointments go booked → arrived → in consultation → done. Visits open and close.
Those have workflow, and workflow means rules, and rules mean a service written for them.

The test when something new is proposed: *can a row be wrong in a way that matters to a patient or to
money?* If yes, it is not reference data.

## Deactivate, never delete

A reference row is never deleted. Appointments (part 10) point at a room and an appointment type; invoice
lines (part 14) point at a service. Deleting the row would orphan history, and creating a replacement row
with the same name would split it.

So:

- **Deactivate** hides the row from every picker. History keeps it and still shows its name.
- **Activate** brings it back. Both are idempotent: pressing either twice is not an error.
- Names are unique per practice, **active or not**. A name that looks free but belongs to a deactivated
  row is a signal to reactivate that row, not to make a second one.

## Prices do not change the past

A service price is what the clinic charges *today*. An invoice line snapshots the name and the price at
the moment it is issued, so editing a service never moves an old invoice. The currency is the practice
currency: the server copies it onto the row and no request can set it.

## Starter catalog

A new practice does not start empty. `src/main/resources/catalog/<country>.sql` holds that country's
opening rooms, appointment types, and services, and `StarterCatalogSeeder` applies it inside the
practice-creation transaction. Egypt's file is in Arabic with EGP prices. A country with no file fails
creation rather than leaving a clinic with nothing to pick from.

The catalog is data, not code: a clinic's opening lists change by editing SQL, with no Java release.

## Adding a fourth reference entity

One migration (table, `tenant_table_setup()`, unique name index), one entity extending `ReferenceEntity`,
one config object in `web/src/app/features/settings/reference/reference-config.ts`, one route entry. No
new service, no new screen. If a proposed entity cannot be added that way, it is not reference data.
