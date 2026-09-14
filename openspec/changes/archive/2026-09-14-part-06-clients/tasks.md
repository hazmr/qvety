## 1. Migration and seed
- [x] 1.1 `V5__clients.sql`, append to RLS and audit lists
- [x] 1.2 Seed a few synthetic clients with Arabic and English names

## 2. Backend
- [x] 2.1 `TenantEntity`; refactor `User` onto it
- [x] 2.2 `Client`, `ClientRepository`, `ClientRequest`, `ClientDto`, `ClientMapper`
- [x] 2.3 `ClientService.create` and `update` by hand: phone-or-email rule, archive rule, duplicate warnings
- [x] 2.4 `ClientController`; page size cap in config
- [x] 2.5 `ApiExceptionHandler`; check the shape with one deliberate bad request

## 3. Tests
- [x] 3.1 Create, list with paging, update, archive
- [x] 3.2 Cross-tenant read returns 404
- [x] 3.3 Duplicate warning returned, creation succeeds
- [x] 3.4 `TenantIsolationIT` covers `clients`

## 4. Angular
- [x] 4.1 `shared/list-page`, `shared/form-page`
- [x] 4.2 `features/clients` list, form, detail with empty Patients tab and History tab
- [x] 4.3 Checked in `ar` and `en`; RTL table correct

## 5. Close
- [x] 5.1 `erd.md` updated
- [x] 5.2 `progress.md` entry
- [x] 5.3 Commit
