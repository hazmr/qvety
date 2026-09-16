## 1. Migration and seed
- [x] 1.1 `V7__patients.sql`: enums, three tables, composite FKs, allergy trigger, RLS and audit append
- [x] 1.2 Seed patients for the seeded clients

## 2. Backend
- [x] 2.1 `Patient`, `PatientWeight`, `PatientAllergy` entities, LAZY relations, enum mapping
- [x] 2.2 DTOs, mappers, repositories
- [x] 2.3 `PatientService` with `transfer` by hand; `deceased`; weights; allergies with `retract`
- [x] 2.4 Specifications for list filters
- [x] 2.5 Controllers

## 3. Tests
- [x] 3.1 List by client; filter by species
- [x] 3.2 Transfer records previous client
- [x] 3.3 Cross-tenant read 404
- [x] 3.4 Insert with another practice's client fails at the database
- [x] 3.5 `UPDATE patient_allergies SET substance` as `qvety_app` rejected; retract succeeds; delete rejected
- [x] 3.6 `TenantIsolationIT` covers the three tables

## 4. Angular
- [x] 4.1 Patients tab on client detail
- [x] 4.2 Patient detail with Summary, Weights, Allergies, empty Visits
- [x] 4.3 Allergy banner; species labels from i18n
- [x] 4.4 Checked in `ar` and `en`
- [x] 4.5 Checked at 390 px and 360 px: phone layout per MOBILE.md screen 10 (patient record: title bar with back chevron, identity block, scrollable tab strip, timeline, action bar); no `nz-table` on phone, 16 px inputs, 44 px targets

## 4b. Review edits (after the first walkthrough)
- [x] 4b.1 `ar.json` sex labels: `male_neutered` → `ذكر معقّم`
- [x] 4b.2 Client detail: Edit in the title bar, Archive beside it on desktop and in the phone action bar
- [x] 4b.3 Patient detail: Edit in the title bar, Mark deceased beside it on desktop and in the phone action bar, Transfer on the owner row; Summary tab has no buttons
- [x] 4b.4 Transfer of a deceased patient refused with 409 `patient.transfer_deceased`; messages in three files; assertion in `PatientIT.transferKeepsHistory`
- [x] 4b.5 `V8__patients.sql`: `patient_weights.voided_at`, `void_reason`, check constraint, `patient_weight_guard` trigger (uncommitted migration; reset the local database)
- [x] 4b.6 `PatientWeight` entity, `PatientWeightDto` gains `voidedAt`/`voidReason`, `PatientService.voidWeight` (409 `patient.weight_already_voided`), `latestWeight` skips voided, `POST /{id}/weights/{weightId}/void`
- [x] 4b.7 Tests: void latest weight moves the header to the previous one; `UPDATE patient_weights SET weight_kg` and `DELETE` as `qvety_app` rejected; void twice is 409
- [x] 4b.8 Weights tab: Void link per row with a reason prompt, voided rows struck through with the reason; `npm run api:generate`; keys in `ar.json` and `en.json`
- [x] 4b.9 Re-check patient and client detail at 1280, 390, 360 in `ar` and `en`: one title-bar action, action bar only on phone, 44 px targets

## 5. Close
- [x] 5.1 `erd.md` updated
- [x] 5.2 `progress.md` entry
- [x] 5.3 Commit
