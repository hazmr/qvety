## Migration `V5__clients.sql`

```sql
CREATE TABLE clients (
  id               uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id      uuid NOT NULL REFERENCES practices(id),
  full_name        text NOT NULL,
  preferred_name   text,
  phone            text,
  phone_secondary  text,
  email            text,
  address          text,
  notes            text,
  preferred_locale text,
  archived_at      timestamptz,
  version          bigint NOT NULL DEFAULT 0,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now(),
  CHECK (phone IS NOT NULL OR email IS NOT NULL),
  UNIQUE (id, practice_id)
);
```
Append `clients` to the RLS and audit `DO` list. `UNIQUE (id, practice_id)` exists so part 08 can add a composite same-practice FK from `patients`.

## Base entity

`@MappedSuperclass TenantEntity`: `id` (db default), `practiceId`, `createdAt`, `updatedAt`, `@Version version`, `@PrePersist` sets `practiceId` from `CurrentUser`. `User` refactored to extend it.

## API

| Method | Path | Who |
| --- | --- | --- |
| GET | `/api/v1/clients?page&size&sort&q` | all |
| GET | `/api/v1/clients/{id}` | all |
| POST | `/api/v1/clients` | front_desk, admin; response includes `warnings` |
| PUT | `/api/v1/clients/{id}` | front_desk, admin |
| POST | `/api/v1/clients/{id}/archive` | front_desk, admin |

`ClientRequest` record (validated), `ClientDto` record. `spring.data.web.pageable.max-page-size: 100`.

## Error handling

`ApiExceptionHandler` (`@RestControllerAdvice`) maps `MethodArgumentNotValidException`, `DomainException`, `OptimisticLockException` (409), not-found (404) to `{code, message, fields}` via `MessageSource`.

## Angular

- `shared/list-page`: NG-ZORRO table, server-side paging and sort, column config, loader `(params) => Observable<Page<T>>`.
- `shared/form-page`: config-driven form.
- `features/clients`: list, form, detail with tabs "Patients" (empty until part 08) and "History" (admin only, `GET /api/v1/audit?table=clients&rowId=`).

## As built (deviations)

- `ApiExceptionHandler` and `ApiError` already existed from part 05; this part adds `DomainException`, `stale_update` (409), `bad_sort` (400).
- Optimistic locking: the client sends `If-Match: <version>`; the service compares it to the loaded row and throws `stale_update` explicitly (deterministic, independent of Hibernate's handling of a manually set version). `@Version` still guards concurrent flushes.
- `users.version` added in V5 so `User` fits `TenantEntity`.
- Create/update return `ClientSaved {client, warnings}`; warnings are full `Client` rows.
- Search in part 06 is `lower(full_name) LIKE %q%` or exact phone/email; part 07 replaces it.
- Repeatable seeds renamed `R__010_dev_practice`, `R__020_dev_users`, `R__030_dev_clients` (Flyway orders repeatables by description).
- Controller method names are unique across the API (`listClients`, `getUser`, ...) so springdoc operationIds and generated client names stay clean.
- Spring Data `PagedModel` serialization (`spring.data.web.pageable.serialization-mode=via_dto`).
- Checked headless (Playwright + Firefox): list, search, create with duplicate warning, unreachable rule in Arabic, detail, history tab, edit prefill, both directions, zero console errors.

## Decisions

- `archived_at` instead of `active` because clients are people with history; reference data uses `active`.
- Duplicate check is a warning: two clients can share a family phone; the desk decides.
