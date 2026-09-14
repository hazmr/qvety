## Why

The front desk books appointments and the whole clinic needs to see who is in the building right now. Overlap must be impossible at the database, and the whiteboard replaces the paper list at the desk.

## What Changes

- `appointments` with a `tstzrange` period, status and origin enums, exclusion constraints per veterinarian and per room.
- `practice_hours` per weekday; booking outside hours warns, does not block.
- Status transitions as data in the service; walk-ins created as `checked_in` with `origin = walk_in`.
- Day grid endpoint and whiteboard endpoint (today's appointments grouped by status).
- Angular day view (CSS grid), booking dialog, whiteboard polled every 30 s.

## Capabilities

### New Capabilities
- `scheduling`: appointments, overlap rule, working hours, status flow, walk-ins, whiteboard.

### Modified Capabilities
- none

## Non-goals

- Websockets or push; the whiteboard polls. One clinic, one screen, 30 s is enough and there is nothing to operate.
- Appointment reminders to clients (recalls are part 15 and are about vaccinations).
- Visits (part 13); an appointment does not yet produce a visit.

## Impact

- `V9__scheduling.sql` with `btree_gist`; `com.qvety.scheduling`; two new tables in RLS, audit, export lists.
- Angular `/schedule`, `/board`.
