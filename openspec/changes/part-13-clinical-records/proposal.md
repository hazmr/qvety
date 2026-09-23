## Why

The medical record is why the clinic exists legally. Notes, vitals, vaccinations, prescriptions, and controlled-substance movements must be attributable, never silently changed, and printable in Arabic and English. Object storage arrives here for attachments, patient photos, and the practice logo.

## What Changes

- Nine tenant tables: `visits`, `visit_vitals`, `clinical_notes`, `clinical_note_addenda`, `vaccinations`, `prescriptions`, `prescription_events`, `controlled_substance_log`, `attachments`; `practices.logo_object_key`.
- Database triggers that reject edits to finalized notes and any edit or delete on evidence tables; trigger functions `SECURITY DEFINER` with `EXECUTE` revoked from the application role.
- Visit lifecycle `open → closed` with note-state check (invoice check added in part 14).
- Void with reason for vitals, notes, vaccinations; addenda for finalized notes.
- Prescriptions with quantity, events, printed روشتة; controlled-substance log with receipts, dispenses, adjustments, running balance.
- Pre-signed object storage for attachments, patient photo, practice logo.
- Bilingual print views: vaccination certificate (A4), prescription (A5).

## Capabilities

### New Capabilities
- `clinical-records`: visits, vitals, notes, finalize/addendum/void, vaccinations, prescriptions, controlled substances, attachments, certificate and prescription print.

### Modified Capabilities
- `patients`: photo becomes writable through object storage.
- `practice`: logo upload for printed documents.
- `scheduling`: clinical work drives the appointment status. Opening a visit moves its appointment to `in_progress`, closing it moves it to `completed`. The manual transitions stay as the escape hatch.

## Non-goals

- Full inventory. The controlled-substance log is the only stock tracked.
- Lab results, consents, or message delivery evidence (backlog).
- Rich text editor; plain textarea for the pilot.
- Invoice-or-no-charge check on visit close (part 14 adds it).

## Impact

- `V13__clinical.sql`; `com.qvety.clinical`; `@Immutable` entities; MinIO tenant pattern with `practice_id` key prefix.
- `TenantIsolationIT` and the export list gain nine tables.
- Angular visit page, print views, controlled-substance page, open-visits list.
