## Why

A client is the person who owns the animal and pays the bill. Egyptian names are chains, not first/last; staff must type the name as the person says it. Clients are the first real tenant entity and set the pattern (base entity, validation, error shape, paging, generic list and form pages) that every later entity reuses.

## What Changes

- `clients` tenant table with `archived_at`, in the RLS and audit lists.
- `TenantEntity` mapped superclass; `User` refactored onto it.
- One error shape from a `@RestControllerAdvice`.
- Paged, sorted list; page size capped at 100 globally.
- CRUD and archive endpoints; duplicate warning on same phone or same name (non-blocking).
- Angular generic `list-page` and `form-page` components driven by config; client detail with a History tab reading the audit endpoint.

## Capabilities

### New Capabilities
- `clients`: pet owners; required fields, archiving, duplicates, what is deliberately not stored.

### Modified Capabilities
- none

## Non-goals

- Arabic-aware search and phone normalization (part 07). This part warns on exact phone or normalized-by-SQL name only.
- Patients tab content (part 08); the tab exists empty.
- National ID. Not stored; see the spec.

## Impact

- `V5__clients.sql`, `com.qvety.clients`, `com.qvety.tenant.TenantEntity`, `ApiExceptionHandler`.
- `TenantIsolationIT` gains the `clients` case.
- Angular `shared/list-page`, `shared/form-page`, `features/clients`.
