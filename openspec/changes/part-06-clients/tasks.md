## 1. Migration and seed
- [ ] 1.1 `V5__clients.sql`, append to RLS and audit lists
- [ ] 1.2 Seed a few synthetic clients with Arabic and English names

## 2. Backend
- [ ] 2.1 `TenantEntity`; refactor `User` onto it
- [ ] 2.2 `Client`, `ClientRepository`, `ClientRequest`, `ClientDto`, `ClientMapper`
- [ ] 2.3 `ClientService.create` and `update` by hand: phone-or-email rule, archive rule, duplicate warnings
- [ ] 2.4 `ClientController`; page size cap in config
- [ ] 2.5 `ApiExceptionHandler`; check the shape with one deliberate bad request

## 3. Tests
- [ ] 3.1 Create, list with paging, update, archive
- [ ] 3.2 Cross-tenant read returns 404
- [ ] 3.3 Duplicate warning returned, creation succeeds
- [ ] 3.4 `TenantIsolationIT` covers `clients`

## 4. Angular
- [ ] 4.1 `shared/list-page`, `shared/form-page`
- [ ] 4.2 `features/clients` list, form, detail with empty Patients tab and History tab
- [ ] 4.3 Checked in `ar` and `en`; RTL table correct

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
