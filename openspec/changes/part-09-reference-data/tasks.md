## 1. Migration and data
- [ ] 1.1 `V9__reference_data.sql`; append to RLS and audit lists
- [ ] 1.2 `catalog/eg.sql` with Arabic names and EGP prices
- [ ] 1.3 `R__dev_catalog.sql` for the dev practice

## 2. Backend
- [ ] 2.1 `ReferenceEntity`, generic `ReferenceService`, generic `ReferenceController`
- [ ] 2.2 `Room`, `AppointmentType`, `Service` entities, DTOs, mappers, thin controllers
- [ ] 2.3 `StarterCatalogSeeder` (used by part 11 practice creation)

## 3. Tests
- [ ] 3.1 One entity fully: list, create, update, deactivate, activate, inactive hidden by default
- [ ] 3.2 Cross-tenant case
- [ ] 3.3 `TenantIsolationIT` covers the three tables

## 4. Angular
- [ ] 4.1 `features/settings` side menu; three config objects
- [ ] 4.2 List shows inactive rows behind a "show inactive" switch with an Activate action, same as the clients list "show archived" switch
- [ ] 4.3 Checked in `ar` and `en`
- [ ] 4.4 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
