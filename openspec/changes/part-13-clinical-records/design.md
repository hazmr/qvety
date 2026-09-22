## Migration `V13__clinical.sql`

Tables (all with base columns, `practice_id`, `UNIQUE (id, practice_id)`, composite same-practice FKs):

- `visits`: `patient_id`, `client_id`, `appointment_id` nullable, `attending_vet_id` nullable, `status` enum `open|closed`, `opened_at`, `closed_at`, `no_charge_reason`.
- `visit_vitals`: `visit_id`, `measured_at`, `temperature_c numeric(4,1)`, `heart_rate`, `respiratory_rate`, `weight_kg numeric(6,2)`, `bcs smallint CHECK 1..9`, `pain_score smallint CHECK 0..10`, `mucous_membrane`, `crt_seconds numeric(3,1)`, `notes`, `recorded_by`, `voided_at`, `void_reason`, `voided_by`.
- `clinical_notes`: `visit_id` unique, `subjective`, `objective`, `assessment`, `plan`, `finalized_at`, `finalized_by`, `finalized_by_name`, `voided_at`, `void_reason`, `voided_by`, `replaced_by`; `CHECK ((finalized_at IS NULL) = (finalized_by IS NULL) AND (finalized_at IS NULL) = (finalized_by_name IS NULL))`; composite FK `(finalized_by, practice_id) → users`.
- `clinical_note_addenda`: `note_id`, `text`, `author_id`, `author_name`, `created_at`.
- `vaccinations`: `visit_id`, `patient_id`, `vaccine_name`, `manufacturer`, `lot`, `product_expiry`, `dose_type` enum `initial|booster`, `given_at`, `next_due`, `given_by`, `supervising_vet_id`, `rabies_tag`, `voided_at`, `void_reason`, `voided_by`; `CHECK (attending vet exists OR supervising_vet_id IS NOT NULL)` enforced via trigger joining `visits`.
- `prescriptions`: `visit_id`, `patient_id`, `prescriber_id`, `drug`, `dose`, `route`, `frequency`, `duration`, `quantity numeric(10,2)`, `unit`, `notes`; composite FK `(prescriber_id, practice_id) → users`; trigger rejects insert when the visit has no attending vet.
- `prescription_events`: `prescription_id`, `event` enum `issued|dispensed|cancelled`, `at`, `by_user_id`, `note`. Append-only.
- `controlled_substance_log`: `drug`, `action` enum `receive|dispense|adjust`, `quantity numeric(10,2)`, `balance_after numeric(10,2) CHECK >= 0`, `prescription_event_id` nullable (set only for `dispense`), `supplier_reference`, `reason`, `by_user_id`, `at`. Append-only. Balance computed under `SELECT ... FOR UPDATE` on the latest row per `(practice_id, drug)`.
- `attachments`: `visit_id`, `object_key`, `filename`, `content_type`, `size_bytes`, `sha256`, `uploaded_by`.
- `ALTER TABLE practices ADD COLUMN logo_object_key text;` and extend the `qvety_app` column grant.

Triggers (all functions `SECURITY DEFINER`, owner `qvety_owner`, `REVOKE ALL ON FUNCTION ... FROM PUBLIC, qvety_app`):
- `protect_finalized_note()`: `BEFORE UPDATE ON clinical_notes`, when `OLD.finalized_at IS NOT NULL` allow changes only to `voided_at`, `void_reason`, `voided_by`, `replaced_by`, `updated_at`.
- `allow_void_only()`: `BEFORE UPDATE` on `vaccinations` and `visit_vitals` (part 08 allergy pattern); `BEFORE DELETE` raise.
- `reject_change()`: `BEFORE UPDATE OR DELETE` on `prescription_events`, `controlled_substance_log`, `clinical_note_addenda`.

All nine tables appended to RLS, audit, export lists. Test each trigger in `psql` as `qvety_app` before Java.

## Entities

`ClinicalNote` normal; `ClinicalNoteAddendum`, `PrescriptionEvent`, `ControlledSubstanceLog` are `@Immutable` (Hibernate ignores updates; the trigger is the source of truth, the entity annotation and the service check are the other two layers).

## Object storage

Same pre-signed pattern as part 12, keys prefixed `practices/<practice_id>/...`. Download URL issued only after the RLS-scoped row lookup. `PUT /practice/logo`, `PUT /patients/{id}/photo`, `POST /visits/{id}/attachments` return a signed upload URL then store the key.

## API

`/api/v1/visits` (POST from appointment or walk-in), `GET /visits?status=open&date`, `GET /visits/{id}`, `PUT /visits/{id}/note` (draft), `POST /visits/{id}/note/finalize`, `POST /visits/{id}/note/addenda`, `POST /visits/{id}/note/void`, `POST /visits/{id}/vitals`, `POST /vitals/{id}/void`, `POST /visits/{id}/vaccinations`, `POST /vaccinations/{id}/void`, `POST /visits/{id}/prescriptions`, `POST /prescriptions/{id}/dispense`, `POST /prescriptions/{id}/cancel`, `GET /prescriptions/{id}/print`, `GET /controlled-substances/log?drug`, `POST /controlled-substances/log` (`receive`/`adjust`, admin), `POST /visits/{id}/close` (`{noChargeReason?}`), `POST /visits/{id}/attachments`, `GET /attachments/{id}/url`, `PUT /practice/logo`, `PUT /patients/{id}/photo`, `GET /patients/{id}/vaccination-certificate`.

## Services

- `ClinicalNoteService`: draft/update/finalize/void/addendum; finalize copies `CurrentUser.fullName` into `finalized_by_name`; `@PreAuthorize("@access.isVeterinarian()")` on finalize.
- `PrescriptionService.dispense`: event row and, if the drug is controlled under `EG`, a log row, in one transaction with the balance lock.
- `VisitService.close`: note state; after part 14, invoice-or-no-charge.
- `VitalsService.record`: also inserts `patient_weights` when weight is present.
- `ControlledDrugCatalog`: per-framework list (`EG`), data file.

## Angular

Visit page from the schedule ("start visit"): allergy banner, vitals form, note editor (read-only after finalize), finalize button (vet flag only), addendum dialog, vaccinations and prescriptions tabs, attachments upload, "Close visit" with no-charge option. "Print vaccination certificate" (A4) on the patient page; "Print prescription" (A5) on the prescriptions tab; `@media print` in RTL. Patient photo on the header. Controlled-substance log under settings (admin). "Open visits" list on the schedule page.

## Phone layout

Reference: `docs/design/MOBILE.md` layout skeleton. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
