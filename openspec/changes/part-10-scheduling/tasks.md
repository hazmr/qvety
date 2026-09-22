## 1. Migration
- [ ] 1.1 `V10__scheduling.sql`: extension, enums, `appointments` (`starts_at`/`ends_at` with a generated `period`, `checked_in_at`) with composite FKs and two partial exclusion constraints, `practice_hours` (with `version`)
- [ ] 1.2 Test the exclusion constraint in `psql` before Java
- [ ] 1.3 Append both tables to RLS, audit, export lists; seed hours and a few appointments

## 2. Backend
- [ ] 2.1 Entities, DTOs, mappers; `period` is a generated column and is not mapped in Java
- [ ] 2.2 `AppointmentService`: create (vet flag check, hours warning), update, transition table, walk-in
- [ ] 2.3 Overlap violation → 409 localized
- [ ] 2.4 Day grid and board native queries; controllers

## 3. Tests
- [ ] 3.1 Overlap rejected; cancelled slot reusable
- [ ] 3.2 Allowed and disallowed transitions
- [ ] 3.3 Walk-in created `checked_in` with `origin = walk_in`
- [ ] 3.4 Board groups by status; cross-tenant
- [ ] 3.5 `TenantIsolationIT` covers both tables

## 4. Angular
- [ ] 4.1 `/schedule` day view and booking dialog
- [ ] 4.2 Status buttons
- [ ] 4.3 `/board` whiteboard with 30 s refresh; RTL column order checked
- [ ] 4.4 Two overlapping bookings fail with 409 in the browser
- [ ] 4.5 Checked at 390 px and 360 px: phone layout per MOBILE.md screen 09 (today: filter row All/Waiting/In exam, stacked rows with a fixed 46 px time column, bottom nav); no `nz-table` on phone, 16 px inputs, 44 px targets

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
