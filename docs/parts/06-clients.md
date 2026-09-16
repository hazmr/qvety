# Part 06 — Clients (pet owners)

## Business

A **client** is the person who owns the animal and pays the bill. In Egypt, names are chains (given name, father, grandfather, family), not first/last. Staff must be able to type the name as the person says it.

Write in `docs/domain/clients.md`:
- Required: `full_name`, at least one of phone or email.
- Optional: `preferred_name`, `phone_secondary` (a spouse or family phone is common; both are searchable in part 07), address, notes, preferred language.
- Not stored: national ID. It is personal data under Egypt's Personal Data Protection Law (151/2020) and no part of the pilot uses it. Add it only when a clinic gives a concrete reason, and write that reason down.
- A client can be archived, never deleted, once they have a patient with any record.
- Duplicates: the system warns on same phone or same normalized name, but does not block (part 07 makes the match smarter).

## Stack you learn

- The base entity: `@MappedSuperclass TenantEntity` with `id`, `practiceId`, `createdAt`, `updatedAt`, `@Version`, `@PrePersist` setting `practiceId` from `CurrentUser`.
- Bean Validation on request records, `@RestControllerAdvice` producing one error shape (`{code, message, fields: {name: message}}`).
- Paging: `Pageable` in, `Page<ClientDto>` out. Cap page size globally (`spring.data.web.pageable.max-page-size: 100`) so no client can pull a whole table.
- MapStruct with `unmappedTargetPolicy = ERROR`.
- Angular: generic list page (NG-ZORRO table, server-side paging and sort) and generic form page, both driven by a config object, plus a client-specific detail page.

## Design

Table `clients`: base columns + `full_name`, `preferred_name`, `phone`, `phone_secondary`, `email`, `address`, `notes`, `preferred_locale`, `archived_at`. Append `clients` to the RLS and audit table list in `V5__clients.sql`.

Endpoints: `GET /api/v1/clients?page&size&sort&q`, `GET /api/v1/clients/{id}`, `POST`, `PUT`, `POST /{id}/archive`. Create/update take `ClientRequest` record; responses are `ClientDto`. Roles: front desk and admin can write; everyone can read.

Part 06c, after part 08: a phone is required (`client.phone_required` on the field); email stays optional. The clinic reaches owners by phone and WhatsApp, and part 15 recalls build the link from `phone_e164`, so an email-only client is a client that cannot be recalled. Archived clients are reachable again: `GET /api/v1/clients?includeArchived=true` includes them in the plain list and both search branches with `archivedAt` set, the list has a "show archived" switch, and `POST /api/v1/clients/{id}/unarchive` clears `archived_at` (the detail page shows Unarchive in place of Edit and Archive). The duplicate warning still ignores archived clients. No migration; the column stays nullable and the rule lives in the service beside the phone-parse rule.

## Steps

1. Migration, RLS append, seed a few synthetic clients with Arabic and English names.
2. `TenantEntity`; refactor `User` to extend it.
3. `Client`, repository, `ClientRequest`, `ClientDto`, `ClientMapper`, `ClientService` (you write `create` and `update` by hand: validation of phone-or-email, archive rule), `ClientController`.
4. `ApiExceptionHandler` advice. Make one deliberate bad request and check the shape.
5. Test: create, list with paging, update, archive, cross-tenant read returns 404 (not 403 — do not reveal existence). Add the `clients` case to `TenantIsolationIT`.
6. Angular: `shared/list-page` and `shared/form-page` generic components; `features/clients` using them; detail page with an empty "Patients" tab for part 08 and a "History" tab reading `/api/v1/audit?table=clients&rowId=` (admin only; reuse on every later detail page).

## Ask Claude

- "Design a `TenantEntity` mapped superclass and show how `@PrePersist` reads the current practice."
- "Write a `@RestControllerAdvice` that maps `MethodArgumentNotValidException` and my `DomainException` to one JSON shape, localized."
- "Build a generic NG-ZORRO table component that takes a column config and a `(params) => Observable<Page<T>>` loader."
- "Review `ClientService` for tenant leaks and for rules I wrote in docs/domain/clients.md."

## Done when

- Full CRUD in the browser, Arabic and English, RTL correct in the table.
- Paging and sort work server-side.
- `TenantIsolationIT` covers clients.

## Self-check

- Why 404 and not 403 for another practice's client?
- What does `@Version` protect against? Try two edits in two tabs.
- Which parts of the list page are generic and which are client-specific? Could you add a second entity with only config?
