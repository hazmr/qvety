## Migration `V8__reference_data.sql`

```sql
CREATE TABLE rooms (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  name text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (id, practice_id)
);
CREATE TABLE appointment_types (... name, duration_minutes integer NOT NULL, color text, active ...);
CREATE TABLE services (... name, price numeric(12,2) NOT NULL, currency char(3) NOT NULL, active ...);
```
All three appended to RLS, audit, and (from part 11) export lists.

## Starter catalog

`src/main/resources/catalog/eg.sql`: plain SQL with a `:practice_id` parameter inserting rooms, appointment types, services. Applied by `StarterCatalogSeeder` inside the practice-creation transaction (part 11). One file per country; missing file → creation fails (part 02 rule). Until part 11, `db/seed/R__dev_catalog.sql` inserts the same rows for the dev practice.

Data, not code: the catalog is versioned SQL, applied once per practice, editable without a Java change.

## Backend

- `ReferenceEntity extends TenantEntity` adds `name`, `active`.
- `ReferenceService<E extends ReferenceEntity, D>`: list (active by default, `includeInactive` flag), get, create, update, deactivate.
- `ReferenceController<D>` generic base; three thin subclasses so springdoc emits a typed schema per entity.

## API

`/api/v1/reference/rooms`, `/api/v1/reference/appointment-types`, `/api/v1/reference/services`: `GET` (list), `GET /{id}`, `POST`, `PUT /{id}`, `POST /{id}/deactivate`. Admin writes; everyone reads.

## Angular

`features/settings` with a side menu of the three lists, each a config object for `list-page` and `form-page`.
