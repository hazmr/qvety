# Part 10 — Scheduling (the clinic day)

## Business

The front desk books appointments: patient, veterinarian, room, type, start time, duration. Write `docs/domain/scheduling.md`:
- No two appointments overlap for the same veterinarian or the same room.
- Working hours per practice (simple: one range per weekday). Booking outside hours warns, does not block.
- Status: `scheduled`, `checked_in`, `in_progress`, `completed`, `cancelled`, `no_show`. Allowed transitions written down.
- Walk-ins: create directly as `checked_in`, with `origin = walk_in`. The origin never changes; the end-of-day report needs to know how many were booked versus walked in.
- Cancelling keeps the row; nothing is deleted.
- Times are stored in UTC and displayed in the practice timezone.
- **Whiteboard.** Everyone in the clinic needs to see who is in the building right now: waiting, in the exam room, done. This is a view over today's appointments by status, not a new table. Front desk moves a patient from `checked_in` to `in_progress` to `completed`; the vet sees the same board. It replaces the paper list at the desk.

## Stack you learn

- `tstzrange` and an exclusion constraint (`EXCLUDE USING gist`) for overlap prevention at the database.
- `OffsetDateTime` / `Instant` handling in JPA and JSON.
- A native query for the day grid grouped by veterinarian.
- State machine in the service, tested per transition.
- Angular: a day view (columns per vet, rows per 15 minutes) built from plain CSS grid; NG-ZORRO date picker; status change buttons.
- A second read model over the same table: the whiteboard, grouped by status, polled every 30 seconds (no websockets in the pilot; write down why).

## Design

`V9__scheduling.sql`: `appointments` (base + `patient_id`, `client_id`, `veterinarian_id`, `room_id`, `appointment_type_id`, `period tstzrange`, `status` enum, `origin` enum (`scheduled|walk_in`), `reason`, `notes`), composite FKs for same-practice, `btree_gist` extension, two exclusion constraints (`veterinarian_id WITH =, period WITH &&` and the same for `room_id`, both excluding cancelled/no_show via a partial constraint). `practice_hours` (base + `weekday`, `opens`, `closes`). All in RLS, audit, and export lists.

Endpoints: `GET /api/v1/appointments?date&veterinarianId`, `POST`, `PUT`, `POST /{id}/status`, `GET /api/v1/schedule/day?date` (grid shape), `GET /api/v1/schedule/board` (today's non-cancelled appointments grouped by status, with patient name, client name, vet, room, minutes since check-in).

## Steps

1. Migration. Test the exclusion constraint in `psql` first.
2. Entities, DTOs, `AppointmentService` with the transition table as data (`Map<Status, Set<Status>>`), overlap error mapped to a 409 with a localized message.
3. Tests: overlap rejected, allowed transition, disallowed transition, cross-tenant.
4. Angular: `/schedule` day view, booking dialog, status buttons.
5. Angular: `/board` whiteboard with three columns (waiting, in exam, done), status buttons on each card, 30 s refresh. Check RTL column order.

## Ask Claude

- "Explain `EXCLUDE USING gist` with `tstzrange` and how to exclude cancelled rows from the constraint."
- "How does Hibernate 7 map `tstzrange`? Show the simplest safe option."
- "Build a CSS-grid day view in Angular that places appointments by start and duration, RTL-safe."

## Done when

- Two overlapping bookings for one vet fail with 409 in the browser.
- Day view shows the seeded day in the practice timezone, both directions.
- Checking a patient in on the schedule shows it on the whiteboard within 30 s on a second browser.

## Self-check

- Why enforce overlap in the database and not only in the service?
- Write the status transition table from memory.
- What timezone bug would appear if you stored `timestamp without time zone`?
