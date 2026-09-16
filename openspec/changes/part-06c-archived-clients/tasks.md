## 1. Backend
- [x] 1.1 `ClientRepository`: the plain list, `searchByName`, `searchByPhone` take `includeArchived`; duplicate query unchanged
- [x] 1.2 `ClientService.list(q, includeArchived, pageable)`; `unarchive(id)`; `validate` requires phone with `client.phone_required` on field `phone`; `client.unreachable` removed
- [x] 1.3 `ClientController`: `includeArchived` query param (default false), `POST /{id}/unarchive`
- [x] 1.4 `messages.properties`, `messages_en.properties`, `messages_ar.properties`: `client.phone_required`
- [x] 1.5 `R__030_dev_clients.sql`: every client has a phone

## 2. Tests (`ClientIT`)
- [x] 2.1 Name and email only → 400 with `fields.phone`; name only → 400 with `fields.phone`
- [x] 2.2 Archived client absent from default list and from name and phone search; present in all three with `includeArchived=true` and `archivedAt` set
- [x] 2.3 Unarchive: `archivedAt` null, back in the default list, update accepted; unarchive twice returns 200
- [x] 2.4 Cross-tenant unarchive is 404 and the row stays archived

## 3. Angular
- [x] 3.1 `npm run api:generate`
- [x] 3.2 `shared/list-page`: `[list-extra]` content slot beside the search box, wraps under it on phone
- [x] 3.3 `clients-list`: "Show archived" switch, reload on change, archived tag on rows; keys `clients.showArchived`, `clients.archived` reused
- [x] 3.4 `client-detail`: Unarchive as the title-bar action while archived; Edit and Archive hidden
- [x] 3.5 `client-form`: phone required, hint text updated; keys in `ar.json` and `en.json`
- [x] 3.6 Checked at 1280, 390, 360 in `ar` and `en`: switch row 44 px, tag visible, form error on phone

## 4. Close
- [x] 4.1 `docs/parts/06-clients.md`: "As built" paragraph on show-archived, unarchive, phone required
- [x] 4.2 `docs/progress.md` entry for part 06c
- [x] 4.3 `./mvnw verify` green, commit `part-06c: archived clients reachable, phone required`
