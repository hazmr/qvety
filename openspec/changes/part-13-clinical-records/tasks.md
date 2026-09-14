## 1. Migration
- [ ] 1.1 `V12__clinical.sql`: nine tables, enums, composite FKs, checks, `logo_object_key`
- [ ] 1.2 Trigger functions with `SECURITY DEFINER` and revoked `EXECUTE`; attach triggers
- [ ] 1.3 Append nine tables to RLS, audit, export lists
- [ ] 1.4 Test every trigger in `psql` as `qvety_app` before Java

## 2. Backend
- [ ] 2.1 Entities; `@Immutable` on addenda, events, log
- [ ] 2.2 `VisitService` create/close/open list
- [ ] 2.3 `VitalsService.record` (+ weight history) and void
- [ ] 2.4 `ClinicalNoteService` draft/finalize (snapshot name)/addendum/void with vet flag check
- [ ] 2.5 Vaccinations record/void with supervising vet rule
- [ ] 2.6 `PrescriptionService` create/dispense/cancel; controlled log with balance lock; `ControlledDrugCatalog` for `EG`
- [ ] 2.7 Object storage: logo, photo, attachments with tenant-prefixed keys and signed URLs
- [ ] 2.8 Print data endpoints: certificate, prescription

## 3. Tests
- [ ] 3.1 Finalize then update text → rejected by Postgres (assert the exception origin)
- [ ] 3.2 Addendum works; original unchanged
- [ ] 3.3 Technician finalize 403; admin with vet flag 200
- [ ] 3.4 Rename signer after finalize; `finalized_by_name` unchanged
- [ ] 3.5 `SELECT protect_finalized_note()` as `qvety_app` → permission denied
- [ ] 3.6 Controlled dispense writes log; receive 10, dispense 3, 3 → balances 10, 7, 4; below zero rejected
- [ ] 3.7 Voided vaccination absent from certificate
- [ ] 3.8 Close with draft note rejected
- [ ] 3.9 Visit without attending vet: supervised vaccination accepted, prescription rejected
- [ ] 3.10 Vitals weight inserts `patient_weights`
- [ ] 3.11 Cross-tenant attachment URL 404; `TenantIsolationIT` covers nine tables

## 4. Angular
- [ ] 4.1 Visit page: banner, vitals, note editor, finalize, addendum, tabs, attachments, close
- [ ] 4.2 Vaccination certificate A4 and prescription A5 print views, RTL, logo, license number
- [ ] 4.3 Patient photo, practice logo upload
- [ ] 4.4 Controlled-substance log page (admin); open visits list on schedule
- [ ] 4.5 Checked in `ar` and `en`
- [ ] 4.6 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
