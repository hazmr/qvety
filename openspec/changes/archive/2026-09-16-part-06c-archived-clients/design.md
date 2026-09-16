## Context

- `ClientRepository` has four read paths: `findByArchivedAtIsNull`, `searchByName` (native, trigram), `searchByPhone` (JPQL), and the duplicate query. All four hard-code `archived_at IS NULL`.
- `ClientService.archive` sets the timestamp; `update` refuses an archived client with 409 `client.archived`.
- Phone-or-email is a service rule (`client.unreachable`) surfaced through `DomainException` with a field, so it lands in `ApiError.fields` like bean validation.
- The list screen is the generic `shared/list-page` (search box, table on desktop, stacked rows on phone). It has no slot for a second control.
- The dev seed has one client with email only, used by the "name and email only" test.

## Decisions

### No migration

`phone` is nullable in `V5__clients.sql` and stays nullable. Making it `NOT NULL` would need a backfill for rows that do not exist outside the dev seed and would be a second migration on an applied one. The rule lives in the service, the same place the phone-or-email rule lives today; the column constraint can come with a later migration if a clinic's data proves clean.

### `includeArchived` is a query flag, not a second endpoint

Same list, same search, same paging. The four repository reads take a boolean and add `(:includeArchived OR archived_at IS NULL)`. The duplicate-warning query keeps `archived_at IS NULL`: an archived client with the same phone is a warning the desk cannot act on without unarchiving first, and the unarchived row will then warn on its own.

### Unarchive is the mirror of archive

`POST /{id}/unarchive`, `@PreAuthorize` on `admin` and `front_desk`, clears `archived_at`, returns the row. Not archived: return the row unchanged, no error; the desk pressing it twice is not a fault. Audit trigger records both.

### Phone required, email optional

`ClientService.validate`: `phone == null` → `DomainException.badRequest("client.phone_required", "phone")`. `client.unreachable` key removed from the three message files. The E.164 parse rule is unchanged.

### Angular

- `shared/list-page` gains `<ng-content select="[list-extra]" />` beside the search box; on phone it wraps under it. The clients list projects an `nz-switch` bound to `includeArchived` that reloads the list; archived rows get a muted tag in the title cell.
- `client-detail`: when `archived()`, the title-bar action becomes Unarchive (`shared.unarchive`), Edit and Archive hidden. Desktop and phone same.
- `client-form`: phone marked required, `Validators.required` client-side, hint text `clients.phoneHint` replaces the phone-or-email hint.

## API

| Method | Path |
| --- | --- |
| GET | `/api/v1/clients?q&includeArchived&page&size&sort` |
| POST | `/api/v1/clients/{id}/unarchive` |

## Phone layout

Reference: `docs/design/MOBILE.md` list screen. The switch sits under the search box as a 44 px row; the archived tag uses `row__tag--muted`. Checked at 390 px and 360 px.
