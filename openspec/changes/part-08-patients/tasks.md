## 1. Migration and seed
- [ ] 1.1 `V7__patients.sql`: enums, three tables, composite FKs, allergy trigger, RLS and audit append
- [ ] 1.2 Seed patients for the seeded clients

## 2. Backend
- [ ] 2.1 `Patient`, `PatientWeight`, `PatientAllergy` entities, LAZY relations, enum mapping
- [ ] 2.2 DTOs, mappers, repositories
- [ ] 2.3 `PatientService` with `transfer` by hand; `deceased`; weights; allergies with `retract`
- [ ] 2.4 Specifications for list filters
- [ ] 2.5 Controllers

## 3. Tests
- [ ] 3.1 List by client; filter by species
- [ ] 3.2 Transfer records previous client
- [ ] 3.3 Cross-tenant read 404
- [ ] 3.4 Insert with another practice's client fails at the database
- [ ] 3.5 `UPDATE patient_allergies SET substance` as `qvety_app` rejected; retract succeeds; delete rejected
- [ ] 3.6 `TenantIsolationIT` covers the three tables

## 4. Angular
- [ ] 4.1 Patients tab on client detail
- [ ] 4.2 Patient detail with Summary, Weights, Allergies, empty Visits
- [ ] 4.3 Allergy banner; species labels from i18n
- [ ] 4.4 Checked in `ar` and `en`

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
