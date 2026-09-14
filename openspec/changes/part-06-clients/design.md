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

## Decisions

- `archived_at` instead of `active` because clients are people with history; reference data uses `active`.
- Duplicate check is a warning: two clients can share a family phone; the desk decides.
