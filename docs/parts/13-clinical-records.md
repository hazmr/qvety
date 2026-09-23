# Part 13 — Clinical records and immutability

## Business

The medical record is the reason the clinic exists legally. Write `docs/domain/clinical-records.md`:
- A **visit** is created when a patient is seen (from an appointment or walk-in). It holds vitals, one clinical note, vaccinations given, prescriptions issued.
- A visit has an **attending veterinarian**, nullable. A technician-only visit (booster, nail trim, weight check) has none. Anything that needs a vet still needs one: finalizing a note sets `finalized_by`, a prescription has a `prescriber_id`, a vaccination given by a non-vet needs `supervising_vet_id`. So a technician visit can hold vitals and a supervised vaccination and nothing else; the database says so, not only the service.
- The visit drives the appointment's status. Opening a visit puts its appointment `in_progress`; closing the visit puts it `completed`. The desk still checks people in and still cancels by hand, because arrival and cancellation are not clinical acts. A status nobody has to remember to set is a board that does not lie by six in the evening.
- A visit is `open` until it is **closed**. Closing requires: the clinical note is finalized or voided (or there is no note), and either an invoice exists for the visit (part 14) or the visit is marked `no_charge` with a reason. Until part 14 exists, closing only checks the note. An open visit at end of day is a question for the front desk, not a silent leak; the day-end list shows them.
- **Vitals** recorded per visit, any number of times: temperature (°C), heart rate, respiratory rate, weight (kg, also written to `patient_weights`), body condition score 1–9, pain score 0–10, mucous membrane colour, capillary refill time (s), notes. A wrong vitals row is voided with a reason, never edited.
- The clinical note has four optional sections (S, O, A, P). Draft until the veterinarian **finalizes** it. After that, the text never changes. Corrections are **addenda** (dated, attributed, appended). A wrong-patient note is **voided** with a reason and a replacement, never edited.
- Only a user with `is_veterinarian` finalizes or prescribes (the flag from part 03, not the role). Technicians can draft vitals and vaccinations.
- A finalized note records the signer's name as it was at signing (`finalized_by_name`). Renaming or deactivating the user later must not change what the signed record shows.
- The patient's allergies (part 08) are shown on the visit screen before anything is prescribed.
- Vaccination: vaccine name, manufacturer, lot, product expiry date, dose type (`initial`, `booster`), date given, next due, given by, supervising veterinarian (when a technician gives it), optional rabies tag number. A wrong vaccination row is **voided** with a reason, never edited or deleted: it drives recalls and the certificate, so a silent fix would hide a real error. Voided rows are excluded from recalls and the certificate but stay in the record.
- Prescription: drug, dose, route, frequency, duration, **quantity and unit** (what leaves the clinic: 20 tablets, 1 bottle 100 ml); events (`issued`, `dispensed`, `cancelled`) are append-only.
- **Printed prescription (روشتة).** Most Egyptian clinics do not stock every drug; the client buys at a pharmacy. Every prescription can be printed: practice header, vet name and license number, patient, drug lines, date, signature space, Arabic and English. An `issued` prescription that is never `dispensed` is the pharmacy case and is normal; nothing chases it.
- Controlled substances: under framework `EG`, every dispense writes a controlled-substance log row with running balance; the list of controlled drugs is per framework. A balance needs stock in: the log also accepts `receive` rows (quantity in, supplier reference, received by) and `adjust` rows (count correction with a reason), entered by an admin. Without receipts the balance is fiction. Full inventory stays out of scope; this log is the only stock the pilot tracks.
- **Practice logo and patient photo.** Same object-storage pattern: `practices.logo_object_key` (admin upload, used on printed invoice and certificate) and `patients.photo_object_key` (from part 08, now writable).
- Attachments (images, PDFs) stored in MinIO, referenced by the visit, never deleted once the note is finalized.
- **Vaccination certificate.** Clients need a printed vaccination card for travel, boarding, and the pet passport. One page per patient: practice name and stamp area, patient identity (name, species, breed, sex, date of birth, microchip), owner, and the vaccination rows (vaccine, date, lot, next due, vet). Arabic and English on the same page.

## Stack you learn

- Database triggers that reject `UPDATE`/`DELETE` on finalized rows and on evidence tables.
- `@Immutable` entities and how Hibernate treats them.
- Enforcing a rule in three layers and knowing which is the source of truth (the trigger).
- Object storage with pre-signed URLs (pattern from part 12, now for tenant data: object key prefixed with `practice_id`, download URL issued only after the RLS-scoped row lookup).
- `REVOKE EXECUTE` on trigger functions and `SECURITY DEFINER`, so the application role cannot call the trigger bodies or bypass them.
- Browser print view with `@media print` in RTL.
- Angular: a rich-ish form (plain textarea is fine for pilot), read-only mode after finalize, addendum dialog.

## Design

`V13__clinical.sql`: `visits` (`attending_vet_id` nullable, `status` enum `open|closed`, `closed_at`, `no_charge_reason`), `visit_vitals` (all vitals columns plus `voided_at`, `void_reason`), `clinical_notes` (+ `finalized_at`, `finalized_by`, `finalized_by_name`, `voided_at`, `void_reason`, `replaced_by`), `clinical_note_addenda`, `vaccinations` (full column set plus `voided_at`, `void_reason`, `voided_by`), `prescriptions` (with `quantity`, `unit`), `prescription_events`, `controlled_substance_log` (`action` enum `receive|dispense|adjust`; `prescription_event_id` nullable, set only for `dispense`), `attachments`; plus `practices.logo_object_key`. All in RLS and audit lists. Composite same-practice FK on `(practice_id, finalized_by)` and `(practice_id, prescriber_id)` to `users`. Check: `finalized_at`, `finalized_by`, `finalized_by_name` are all null or all set. Triggers: `clinical_notes` reject update of text columns when `finalized_at` is not null (allow only `voided_at`, `void_reason`, `replaced_by`); `vaccinations` and `visit_vitals` allow update of `voided_*` columns only (the part 08 allergy pattern); `prescription_events`, `controlled_substance_log`, `clinical_note_addenda` reject any update/delete for `qvety_app`. Every trigger function is owned by `qvety_owner`, `SECURITY DEFINER`, with `REVOKE ALL ON FUNCTION ... FROM PUBLIC, qvety_app`. Add all nine tables to the export (part 11).

Endpoints: `/api/v1/visits` (create from appointment or walk-in), `/visits/{id}/note` (PUT while draft), `/note/finalize`, `/note/addenda`, `/note/void`, `/visits/{id}/vaccinations`, `POST /vaccinations/{id}/void`, `/visits/{id}/vitals`, `POST /vitals/{id}/void`, `POST /visits/{id}/close`, `GET /visits?status=open&date`, `/prescriptions`, `/prescriptions/{id}/dispense`, `/controlled-substances/log` (GET list with balance per drug; POST `receive`/`adjust`, admin only), `PUT /practice/logo` and `PUT /patients/{id}/photo` (pre-signed upload, store key), `/visits/{id}/attachments` (pre-signed upload), `GET /patients/{id}/vaccination-certificate` and `GET /prescriptions/{id}/print` (data for the print views; the pages themselves are Angular).

## Steps

1. Migration with triggers. Test the triggers in `psql` as `qvety_app` before Java.
2. Entities: `ClinicalNote` normal, `ClinicalNoteAddendum`, `PrescriptionEvent`, `ControlledSubstanceLog` as `@Immutable`.
3. `ClinicalNoteService`: draft/update/finalize/void/addendum, `is_veterinarian` checks. Finalize copies `CurrentUser.fullName` into `finalized_by_name`. `PrescriptionService.dispense` writes the event and, if controlled under `EG`, the log row, in one transaction. `VisitService.close` checks the note state and (after part 14) invoice-or-no-charge. `VitalsService.record` also inserts a `patient_weights` row when weight is present.
4. Tests: finalize then update → rejected at DB (assert the exception comes from Postgres, not only from Java); addendum works; technician cannot finalize; admin with `is_veterinarian` can; renaming the signer after finalize leaves `finalized_by_name` unchanged; `SELECT protect_finalized_note()` as `qvety_app` fails with permission denied; dispense of a controlled drug writes the log; `receive` then two dispenses give the right `balance_after`; dispense below zero balance is rejected; voided vaccination is absent from the certificate; closing a visit with a draft note is rejected; a visit with no attending vet accepts a supervised vaccination and rejects a prescription; cross-tenant.
5. Angular: visit page from the schedule ("start visit") with the allergy banner, vitals form, note editor, finalize button (vet flag only), addendum dialog, vaccinations and prescriptions tabs, attachments upload, "Close visit" button with the no-charge option, "Print vaccination certificate" on the patient page and "Print prescription" on the prescriptions tab (practice logo, vet name and license number in the header, A5 for the prescription), patient photo on the patient header, controlled-substance log page under settings (admin), "Open visits" list on the schedule page, checked in RTL.

## Ask Claude

- "Write a Postgres trigger that allows updates to `voided_at` and `void_reason` but rejects changes to any other column once `finalized_at` is set."
- "What does Hibernate do if I call `save` on an `@Immutable` entity? Show the behavior."
- "Design the controlled-substance log so the running balance is correct under concurrent dispenses."
- "Why must trigger functions be `SECURITY DEFINER` with `EXECUTE` revoked from the application role? Show what `qvety_app` could do otherwise."
- "Build an A4 print stylesheet for a bilingual Arabic/English vaccination certificate in Angular."

## Done when

- A finalized note cannot be changed by any path, and the test proves the rejection is from the database.
- A vet can finalize; a technician gets 403.
- Controlled dispense produces a log row with a correct balance.
- Vaccination certificate prints readably in Arabic on A4, and a prescription on A5, both with logo and license number.
- A visit cannot be closed while its note is a draft; an open-visits list shows what is left at end of day.

## Self-check

- Explain to a clinic owner, in two sentences, why the software refuses to edit a signed note.
- Which of the three layers (Angular, service, trigger) is the source of truth, and why keep the other two?
- What is the difference between void and delete?
