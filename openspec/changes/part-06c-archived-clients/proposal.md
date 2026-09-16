## Why

Two gaps found in the first walkthrough of the client screens after part 08.

Archived clients vanish. `ClientService.list` and every search query filter `archived_at IS NULL`, and the detail page has no unarchive. The only way back to an archived client is a bookmarked URL. A desk that archives a client by mistake, or a client who returns after two years, has no path through the UI. The `clients` spec says archived clients "remain readable with their history" but gives no way to reach them.

A client can be created with an email and no phone. Egyptian clinics reach owners by phone and WhatsApp; part 15 recalls build a `wa.me` link from `phone_e164` and nothing else. A client without a phone is a client the clinic cannot recall. Email-only was allowed because it costs nothing on paper; in practice it produces rows that the feature that pays for the subscription cannot use.

## What Changes

- `GET /api/v1/clients` accepts `includeArchived=true`. Default stays living clients only. With the flag, archived clients are included in the plain list and in both search branches, and each row carries `archivedAt` so the UI can tag it.
- `POST /api/v1/clients/{id}/unarchive` clears `archived_at`. Same roles as archive. Unarchiving a client that is not archived is a no-op that returns the row.
- The clients list gets a "Show archived" switch next to the search box, off by default; archived rows show an "archived" tag. The client detail page shows Unarchive in place of Archive when the client is archived.
- `phone` becomes required on create and update. `email` stays optional. The service rule `client.unreachable` (phone or email) becomes `client.phone_required`. The Angular form marks phone required and drops email from the "one of" hint.
- No migration: `phone` is already a column; existing rows without a phone are dev seed only. The dev seed gets a phone on every client.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `clients`: "Client identity fields" requires phone. "Clients are archived, never deleted" gains the show-archived list, the unarchive endpoint, and the detail page behavior.

## Non-goals

- Client merge (backlog).
- Any change to the archive rule for patients or to `patients.deceased_at`.
- A separate "archived clients" page; one switch on the existing list is enough.
- Bulk unarchive.

## Impact

- `src/main/java/com/qvety/clients/`: `ClientController` (query param, endpoint), `ClientService` (`list`, `unarchive`, phone rule), `ClientRepository` (queries take the flag).
- `messages*.properties`: `client.phone_required` replaces `client.unreachable`.
- `src/main/resources/db/seed/R__030_dev_clients.sql`: every seeded client gets a phone.
- `web/src/app/features/clients/`: `clients-list.ts` switch, `client-detail` Unarchive, `client-form` phone required; `shared/list-page` gains an optional extra control slot; i18n keys; regenerated `web/src/app/api`.
- `ClientIT`: unarchive, list with and without the flag, phone-required, cross-tenant unarchive is 404.
- `openspec/specs/clients/spec.md` on archive. `docs/parts/06-clients.md` and `docs/progress.md` note the change. No `erd.md` change.
