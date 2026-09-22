-- Part 10: the clinic day. Appointments with overlap made impossible at the database, and the opening
-- hours a booking is checked against. Introduces the pattern later parts reuse for any "no two of these
-- at once" rule: a range column plus a partial exclusion constraint.

-- Lets a GiST index mix an equality column (veterinarian_id, room_id) with a range column.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE appointment_status AS ENUM ('scheduled', 'checked_in', 'in_progress', 'completed', 'cancelled', 'no_show');
CREATE TYPE appointment_origin AS ENUM ('scheduled', 'walk_in');

CREATE TABLE appointments (
    id                  uuid               PRIMARY KEY DEFAULT uuidv7(),
    practice_id         uuid               NOT NULL REFERENCES practices (id),
    patient_id          uuid               NOT NULL,
    client_id           uuid               NOT NULL,
    veterinarian_id     uuid               NOT NULL,
    room_id             uuid               NOT NULL,
    appointment_type_id uuid               NOT NULL,
    starts_at           timestamptz        NOT NULL,
    ends_at             timestamptz        NOT NULL,
    -- Generated, so Java maps the two plain columns and Postgres keeps the range in step. The exclusion
    -- constraints below are the only readers.
    period              tstzrange          GENERATED ALWAYS AS (tstzrange(starts_at, ends_at, '[)')) STORED,
    status              appointment_status NOT NULL DEFAULT 'scheduled',
    origin              appointment_origin NOT NULL DEFAULT 'scheduled',
    checked_in_at       timestamptz,
    reason              text,
    notes               text,
    version             bigint             NOT NULL DEFAULT 0,
    created_at          timestamptz        NOT NULL DEFAULT now(),
    updated_at          timestamptz        NOT NULL DEFAULT now(),

    -- Zero length would be an empty range that overlaps nothing, so this is what refuses it. Backwards
    -- times fail earlier, inside the generated expression, with SQLSTATE 22000; the service refuses a
    -- non-positive duration before either is reached.
    CONSTRAINT appointments_period CHECK (ends_at > starts_at),
    CONSTRAINT appointments_id_practice UNIQUE (id, practice_id),

    -- Every reference must belong to the same practice. RLS filters what a session sees; these make a
    -- guessed id from another practice impossible to link even without RLS.
    CONSTRAINT appointments_patient_same_practice  FOREIGN KEY (patient_id, practice_id)          REFERENCES patients (id, practice_id),
    CONSTRAINT appointments_client_same_practice   FOREIGN KEY (client_id, practice_id)           REFERENCES clients (id, practice_id),
    CONSTRAINT appointments_vet_same_practice      FOREIGN KEY (veterinarian_id, practice_id)     REFERENCES users (id, practice_id),
    CONSTRAINT appointments_room_same_practice     FOREIGN KEY (room_id, practice_id)             REFERENCES rooms (id, practice_id),
    CONSTRAINT appointments_type_same_practice     FOREIGN KEY (appointment_type_id, practice_id) REFERENCES appointment_types (id, practice_id),

    -- The overlap rule. Two desk sessions can book the same slot in the same second, so a service check
    -- alone races; this refuses the second write. Partial, so cancelling or marking no-show frees the slot.
    -- practice_id is in the key because RLS is not visible to the index: without it, two practices could
    -- not both use ids that happen to collide.
    CONSTRAINT appointments_vet_no_overlap EXCLUDE USING gist (
        practice_id WITH =, veterinarian_id WITH =, period WITH &&
    ) WHERE (status NOT IN ('cancelled', 'no_show')),
    CONSTRAINT appointments_room_no_overlap EXCLUDE USING gist (
        practice_id WITH =, room_id WITH =, period WITH &&
    ) WHERE (status NOT IN ('cancelled', 'no_show'))
);

CREATE INDEX appointments_practice_day_idx ON appointments (practice_id, starts_at);
CREATE INDEX appointments_practice_status_idx ON appointments (practice_id, status);

COMMENT ON TABLE  appointments               IS 'One booking. Cancelled and no-show rows stay and free their slot; nothing is deleted.';
COMMENT ON COLUMN appointments.period        IS 'Generated from starts_at and ends_at; read only by the two exclusion constraints.';
COMMENT ON COLUMN appointments.origin        IS 'scheduled or walk_in, set at creation and never changed; the daily report counts one against the other.';
COMMENT ON COLUMN appointments.checked_in_at IS 'When the status became checked_in; the board shows minutes waited from it.';

CREATE TABLE practice_hours (
    id          uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id uuid        NOT NULL REFERENCES practices (id),
    weekday     smallint    NOT NULL,
    opens       time        NOT NULL,
    closes      time        NOT NULL,
    version     bigint      NOT NULL DEFAULT 0,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT practice_hours_weekday      CHECK (weekday BETWEEN 0 AND 6),
    CONSTRAINT practice_hours_range        CHECK (closes > opens),
    CONSTRAINT practice_hours_per_weekday  UNIQUE (practice_id, weekday)
);

COMMENT ON TABLE  practice_hours         IS 'One opening range per weekday. A booking outside it warns and still saves.';
COMMENT ON COLUMN practice_hours.weekday IS '0 = Sunday, the first day of the Egyptian working week.';

-- RLS policy, grants, and audit trigger in one call (part 04).
SELECT tenant_table_setup('appointments');
SELECT tenant_table_setup('practice_hours');
