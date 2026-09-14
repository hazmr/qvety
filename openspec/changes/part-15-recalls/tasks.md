## 1. Migration
- [ ] 1.1 `V14__recalls.sql`: three tables, enums, checks, append-only trigger on `recall_contacts`
- [ ] 1.2 Append to RLS, audit, export lists; settings row in dev seed and in practice creation

## 2. Backend
- [ ] 2.1 Recall query written in `psql` against the seed until the list matches a hand computation, then moved to Java
- [ ] 2.2 `RecallService.recordContact` (with optional `appointmentId` on `booked`)
- [ ] 2.3 `CareReminderService` create/done/dismiss and contacts
- [ ] 2.4 `PracticeSettingsService` get/put
- [ ] 2.5 Template rendering helper with unit test

## 3. Tests
- [ ] 3.1 Due tomorrow listed; boosted not; deceased not; voided not; future booking not
- [ ] 3.2 `reached` removes; `no_answer` keeps with count 1
- [ ] 3.3 Care reminder in window listed with `type = care`; `done` removes; `reached` does not close
- [ ] 3.4 `UPDATE recall_contacts` as `qvety_app` rejected
- [ ] 3.5 Cross-tenant; `TenantIsolationIT` covers three tables

## 4. Angular
- [ ] 4.1 `/recalls` list, filters, WhatsApp button, outcome dialog
- [ ] 4.2 "Add reminder" on patient and visit pages
- [ ] 4.3 Settings page for window and templates
- [ ] 4.4 WhatsApp opens with the Arabic message on a phone; checked in `ar` and `en`

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Add what the pilot clinic still cannot do to `docs/backlog.md`
- [ ] 5.4 Commit
