## 1. Backend
- [ ] 1.1 `ClientRepository`: the plain list, `searchByName`, `searchByPhone` take `includeArchived`; duplicate query unchanged
- [ ] 1.2 `ClientService.list(q, includeArchived, pageable)`; `unarchive(id)`; `validate` requires phone with `client.phone_required` on field `phone`; `client.unreachable` removed
- [ ] 1.3 `ClientController`: `includeArchived` query param (default false), `POST /{id}/unarchive`
- [ ] 1.4 `messages.properties`, `messages_en.properties`, `messages_ar.properties`: `client.phone_required`
- [ ] 1.5 `R__030_dev_clients.sql`: every client has a phone

## 2. Tests (`ClientIT`)
- [ ] 2.1 Name and email only → 400 with `fields.phone`; name only → 400 with `fields.phone`
- [ ] 2.2 Archived client absent from default list and from name and phone search; present in all three with `includeArchived=true` and `archivedAt` set
- [ ] 2.3 Unarchive: `archivedAt` null, back in the default list, update accepted; unarchive twice returns 200
- [ ] 2.4 Cross-tenant unarchive is 404 and the row stays archived

## 3. Angular
- [ ] 3.1 `npm run api:generate`
- [ ] 3.2 `shared/list-page`: `[list-extra]` content slot beside the search box, wraps under it on phone
- [ ] 3.3 `clients-list`: "Show archived" switch, reload on change, archived tag on rows; keys `clients.showArchived`, `clients.archived` reused
- [ ] 3.4 `client-detail`: Unarchive as the title-bar action while archived; Edit and Archive hidden
- [ ] 3.5 `client-form`: phone required, hint text updated; keys in `ar.json` and `en.json`
- [ ] 3.6 Checked at 1280, 390, 360 in `ar` and `en`: switch row 44 px, tag visible, form error on phone

## 4. Close
- [ ] 4.1 `docs/parts/06-clients.md`: "As built" paragraph on show-archived, unarchive, phone required
- [ ] 4.2 `docs/progress.md` entry for part 06c
- [ ] 4.3 `./mvnw verify` green, commit `part-06c: archived clients reachable, phone required`
