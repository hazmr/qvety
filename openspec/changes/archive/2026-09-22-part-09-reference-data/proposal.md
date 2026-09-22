## Why

Each practice configures small option lists: rooms, appointment types, services with prices. These are the only tables where generic CRUD is allowed, and an Egyptian starter catalog lets a new clinic start with sensible Arabic names and EGP prices.

## What Changes

- `rooms`, `appointment_types`, `services` tenant tables with an `active` flag; in RLS, audit, and export lists.
- Generic `ReferenceService` and `ReferenceController` with three thin subclasses.
- Starter catalog per country as SQL data (`catalog/eg.sql`), applied at practice creation (wired in part 11); dev seed inserts the same rows for the dev practice.
- Angular settings area reusing `list-page` and `form-page` with one config object per entity.

## Capabilities

### New Capabilities
- `reference-data`: what counts as reference data, deactivate-not-delete, the per-country starter catalog.

### Modified Capabilities
- none

## Non-goals

- Products or inventory.
- Anything with workflow (invoices, appointments) as reference data.
- `appointment_types.default_service_id` (part 14).

## Impact

- `V9__reference_data.sql`, `com.qvety.reference`, `src/main/resources/catalog/eg.sql`, `R__dev_catalog.sql`.
- `docs/domain/reference-data.md` is the allow-list that `CLAUDE.md` refers to; this spec replaces it.
