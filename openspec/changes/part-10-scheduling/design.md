## Migration `V9__scheduling.sql`

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TYPE appointment_status AS ENUM ('scheduled','checked_in','in_progress','completed','cancelled','no_show');
CREATE TYPE appointment_origin AS ENUM ('scheduled','walk_in');

CREATE TABLE appointments (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  patient_id uuid NOT NULL,
  client_id uuid NOT NULL,
  veterinarian_id uuid NOT NULL,
  room_id uuid NOT NULL,
  appointment_type_id uuid NOT NULL,
  period tstzrange NOT NULL,
  status appointment_status NOT NULL DEFAULT 'scheduled',
  origin appointment_origin NOT NULL DEFAULT 'scheduled',
  reason text,
  notes text,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (id, practice_id),
  FOREIGN KEY (patient_id, practice_id) REFERENCES patients(id, practice_id),
  FOREIGN KEY (client_id, practice_id) REFERENCES clients(id, practice_id),
  FOREIGN KEY (veterinarian_id, practice_id) REFERENCES users(id, practice_id),
  FOREIGN KEY (room_id, practice_id) REFERENCES rooms(id, practice_id),
  FOREIGN KEY (appointment_type_id, practice_id) REFERENCES appointment_types(id, practice_id),
  EXCLUDE USING gist (veterinarian_id WITH =, period WITH &&) WHERE (status NOT IN ('cancelled','no_show')),
  EXCLUDE USING gist (room_id WITH =, period WITH &&) WHERE (status NOT IN ('cancelled','no_show'))
);

CREATE TABLE practice_hours (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  weekday smallint NOT NULL CHECK (weekday BETWEEN 0 AND 6),
  opens time NOT NULL,
  closes time NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (practice_id, weekday)
);
```
`users` needs `UNIQUE (id, practice_id)` for the composite FK; add it here if not already present. Both tables appended to RLS, audit, export lists.

- `EXCLUDE USING gist` with `&&` on `tstzrange` is the overlap check; the `WHERE` makes it partial so cancelled and no-show rows free the slot.
- Overlap is enforced in the database because two desk sessions can book the same slot in the same second; a service check alone races.

## API

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/v1/appointments?date&veterinarianId` | list |
| POST | `/api/v1/appointments` | `{patientId, veterinarianId, roomId, appointmentTypeId, start, durationMinutes, reason, walkIn}`; `warnings` for hours |
| PUT | `/api/v1/appointments/{id}` | reschedule / edit |
| POST | `/api/v1/appointments/{id}/status` | `{status}`; 409 on disallowed |
| GET | `/api/v1/schedule/day?date` | grid shape grouped by vet |
| GET | `/api/v1/schedule/board` | today's board grouped by status |

## Backend

- `AppointmentService` with the transition table as data: `Map<Status, Set<Status>>`.
- `period` mapped via a Hibernate range type or as two `Instant` columns plus a generated range; pick the simplest that validates, record it.
- Exclusion violation (`23P01`) mapped to 409 with a localized message.
- Board and day grid are native queries.

## Angular

- `/schedule`: day view, CSS grid, one column per vet, 15 min rows, placement by start and duration, RTL-safe (`inset-inline-start`, not `left`).
- Booking dialog with NG-ZORRO date picker; status buttons.
- `/board`: three columns (waiting, in exam, done), status buttons on cards, 30 s polling. Check RTL column order.
