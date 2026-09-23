## Purpose

Defines appointments, the no-overlap rule per veterinarian and room, working hours, the status flow including walk-ins, and the whiteboard view of the clinic day.

## ADDED Requirements

### Requirement: Appointment fields
An appointment SHALL have a patient, its client, a veterinarian (a user with `is_veterinarian`), a room, an appointment type, a start time and duration, a status, an origin, and optional reason and notes.

#### Scenario: Non-vet as veterinarian
- **WHEN** an appointment is booked with a user whose `is_veterinarian` is false
- **THEN** the request is rejected with a validation error

### Requirement: No overlap per veterinarian or room
Two non-cancelled, non-no-show appointments SHALL NOT overlap in time for the same veterinarian or for the same room. The database SHALL enforce this; the service SHALL map the violation to 409 with a localized message.

#### Scenario: Double booking
- **WHEN** a second appointment is booked for the same vet overlapping an existing one
- **THEN** the response is 409 and no row is written

#### Scenario: Cancelled slot reused
- **WHEN** an appointment is cancelled and a new one is booked in the same slot for the same vet
- **THEN** the new booking succeeds

### Requirement: Working hours warn, do not block
Each practice SHALL have one opening range per weekday. Booking outside hours SHALL return a warning and still succeed.

#### Scenario: Evening booking
- **WHEN** an appointment is booked after closing time
- **THEN** the response is 201 with a warning about hours

### Requirement: Status flow
Status SHALL be one of `scheduled`, `checked_in`, `in_progress`, `completed`, `cancelled`, `no_show`. Allowed transitions: `scheduled → checked_in | cancelled | no_show`; `checked_in → in_progress | cancelled`; `in_progress → completed`. Any other transition SHALL be rejected. Cancelling SHALL keep the row.

#### Scenario: Allowed transition
- **WHEN** a `checked_in` appointment is moved to `in_progress`
- **THEN** the status changes and the change is audited

#### Scenario: Disallowed transition
- **WHEN** a `completed` appointment is moved to `scheduled`
- **THEN** the response is 409

### Requirement: Check-in time is recorded
When an appointment becomes `checked_in`, the system SHALL record the moment it happened, so the board can show how long a patient has waited.

#### Scenario: Minutes waited
- **WHEN** a patient checked in twenty minutes ago is shown on the board
- **THEN** the row reports twenty minutes waited

### Requirement: Walk-ins
A walk-in SHALL be created directly as `checked_in` with `origin = walk_in`. The origin SHALL never change after creation; the daily report counts booked versus walk-in.

#### Scenario: Walk-in
- **WHEN** the desk creates a walk-in
- **THEN** the row has `status = checked_in`, `origin = walk_in`, and updating the origin later is refused

### Requirement: Times are UTC in storage, practice timezone in display
Appointment times SHALL be stored with timezone (UTC) and displayed in the practice timezone.

#### Scenario: Cairo display
- **WHEN** an appointment stored at `08:00Z` is shown for a practice in `Africa/Cairo`
- **THEN** it displays as `10:00` (or `11:00` when DST applies)

### Requirement: Day grid
`GET /api/v1/schedule/day?date` SHALL return that day's appointments grouped per veterinarian for a grid view.

#### Scenario: Two vets
- **WHEN** the day has appointments for two vets
- **THEN** the response has one column per vet with their appointments

### Requirement: Whiteboard
`GET /api/v1/schedule/board` SHALL return today's non-cancelled appointments grouped by status (`checked_in` waiting, `in_progress` in exam, `completed` done) with patient name, client name, vet, room, and minutes since check-in. It SHALL be a view over appointments, not a stored table. The UI SHALL refresh it at least every 30 seconds.

#### Scenario: Check-in appears on the board
- **WHEN** the desk checks a patient in
- **THEN** within 30 seconds the patient appears in the waiting column on every open board

#### Scenario: Board is tenant-scoped
- **WHEN** practice A opens the board
- **THEN** no appointment of practice B is shown
