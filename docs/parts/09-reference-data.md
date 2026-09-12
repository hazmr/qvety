# Part 09 — Reference data (rooms, appointment types, services)

## Business

Each practice configures small lists: rooms, appointment types (with default duration and color), services (with price in practice currency). These are the only tables where a generic CRUD is allowed.

Write `docs/domain/reference-data.md`: the list of reference entities and the rule that anything with workflow is not reference data. Also the Egyptian starter catalog: when a practice is created with country `EG`, seed Arabic names and EGP prices (e.g., كشف, تطعيم, جراحة صغرى). This is data, so it lives in a seed file per country, not in code.

## Stack you learn

- A generic `ReferenceService<E extends ReferenceEntity, D>` and `ReferenceController<D>` with list/get/create/update/deactivate.
- Soft deactivate (`active` flag) instead of delete, because appointments reference them.
- Seeding per country at practice creation.
- Angular: reuse `list-page` and `form-page` with only a config object per entity; one route module for all reference data.

## Design

`V8__reference_data.sql`: `rooms` (base + `name`, `active`), `appointment_types` (base + `name`, `duration_minutes`, `color`, `active`), `services` (base + `name`, `price numeric(12,2)`, `currency char(3)`, `active`). All in RLS, audit, and export lists. Starter catalog: `src/main/resources/catalog/eg.sql` (plain SQL with a `:practice_id` parameter), applied by `StarterCatalogSeeder` inside the practice creation transaction in part 11. Until then the dev seed `db/seed/R__dev_catalog.sql` inserts the same rows for the dev practice. One file per country; an unmapped country fails creation (part 02 rule).

Endpoints: `/api/v1/reference/rooms`, `/appointment-types`, `/services`, same shape.

## Steps

1. Migration and seeds.
2. `ReferenceEntity` (extends `TenantEntity`, adds `name`, `active`), generic service and controller, three thin subclasses.
3. Test one entity fully and one cross-tenant case; the generic code covers the rest.
4. Angular: `features/settings` with a side menu of the three lists, each a config object.

## Ask Claude

- "Design a generic Spring controller for reference entities without losing OpenAPI typing per entity."
- "Where should the Egyptian starter catalog live so it is data, versioned, and applied once per practice?"

## Done when

- Adding a fourth reference entity takes one migration, one entity, one config object, no new service or page.
- Starter catalog present for the dev practice in Arabic with EGP.

## Self-check

- Why is `services` reference data but `invoices` is not?
- What breaks if you delete a room instead of deactivating it?
