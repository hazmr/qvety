## Migration `V1__practices.sql`

```sql
CREATE TYPE practice_status AS ENUM ('trial','active','past_due','suspended','closed');

CREATE TABLE practices (
  id               uuid PRIMARY KEY DEFAULT uuidv7(),
  name             text NOT NULL,
  country          char(2) NOT NULL CHECK (country IN ('EG')),
  currency         char(3) NOT NULL,
  locale           text NOT NULL,
  timezone         text NOT NULL,
  status           practice_status NOT NULL DEFAULT 'trial',
  address          text,
  phone            text,
  vat_number       text,
  tax_rate_percent numeric(5,2) NOT NULL DEFAULT 0,
  trial_ends_at    timestamptz,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now()
);
```

- `uuidv7()` (Postgres 18) gives time-ordered ids, so btree inserts stay append-friendly and rows sort by creation. The database generates it; the entity does not use `@GeneratedValue`.
- `country` check lists supported countries only. Adding a country is a migration plus a catalog file (part 09).
- `trial_ends_at` is filled by practice creation in part 11 from `platform_settings.trial_days`.
- No RLS on this table; it is a platform table. Column-level grants for `qvety_app` come in part 04.

## API

| Method | Path | Auth | Returns |
| --- | --- | --- | --- |
| GET | `/api/v1/practice` | header `X-Practice-Id` (temporary) | `PracticeDto` |

`PracticeDto` record: `id, name, country, currency, locale, timezone, status, address, phone, vatNumber, taxRatePercent, trialEndsAt`.

## Backend

- `Practice` entity: `@Getter @Setter @NoArgsConstructor`, `@Id` without generator, enum mapped as Postgres enum.
- `PracticeRepository extends JpaRepository<Practice, UUID>`.
- `PracticeMapper` (MapStruct, `unmappedTargetPolicy = ERROR`), `PracticeService.get(UUID)`, `PracticeController`.
- Missing or unknown practice id returns 404 (never 403, do not reveal existence).

## Seed

`db/seed/R__dev_practice.sql`: one practice `Neighborhood Vet`, `EG`, fixed uuid `00000000-0000-7000-8000-000000000001`, referenced by every later seed. Seeds run only with the `local` and `test` profiles.

## Angular

One page under `features/practice` calling generated `PracticeService.getPractice()` and showing the name. First end-to-end line.

## Decisions

- Header hack instead of auth so the first end-to-end line has nothing in the way. Deleted in part 03. If Spring Security is already familiar, do parts 02 and 03 together and skip the header.
- DTO record instead of returning the entity: the API shape is decoupled from the table, lazy proxies never reach Jackson, and springdoc gets a stable schema.
