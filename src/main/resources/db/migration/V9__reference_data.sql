-- Part 09: the three configurable lists a practice keeps. These are the only tables that a generic CRUD
-- is allowed to serve (docs/domain/reference-data.md). Rows are deactivated and reactivated, never deleted,
-- because appointments (part 10) and invoice lines (part 14) point at them.

CREATE TABLE rooms (
    id          uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id uuid        NOT NULL REFERENCES practices (id),
    name        text        NOT NULL,
    active      boolean     NOT NULL DEFAULT true,
    version     bigint      NOT NULL DEFAULT 0,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),

    -- Needed by the same-practice composite foreign key of appointments (part 10).
    CONSTRAINT rooms_id_practice UNIQUE (id, practice_id)
);

-- One name per practice, active or not: a deactivated room is reactivated, never duplicated, so the
-- constraint stays simple. Also the index the list ordering reads.
CREATE UNIQUE INDEX rooms_practice_name_idx ON rooms (practice_id, lower(name));

COMMENT ON TABLE  rooms        IS 'Consultation rooms, theatres, kennels. Deactivated when a room closes; past appointments keep it.';
COMMENT ON COLUMN rooms.active IS 'false hides the row from pickers; history keeps it. Reversible.';

CREATE TABLE appointment_types (
    id               uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id      uuid        NOT NULL REFERENCES practices (id),
    name             text        NOT NULL,
    duration_minutes integer     NOT NULL,
    color            text,
    active           boolean     NOT NULL DEFAULT true,
    version          bigint      NOT NULL DEFAULT 0,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT appointment_types_id_practice UNIQUE (id, practice_id),
    -- A visit is at least five minutes and never a whole working day.
    CONSTRAINT appointment_types_duration CHECK (duration_minutes BETWEEN 5 AND 480),
    -- The day view paints the slot with this; six hex digits or nothing.
    CONSTRAINT appointment_types_color    CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE UNIQUE INDEX appointment_types_practice_name_idx ON appointment_types (practice_id, lower(name));

COMMENT ON TABLE  appointment_types                  IS 'Kinds of visit with their default length and day-view colour. Part 14 adds default_service_id.';
COMMENT ON COLUMN appointment_types.duration_minutes IS 'Default slot length; the booking may still be shortened or lengthened.';

CREATE TABLE services (
    id          uuid          PRIMARY KEY DEFAULT uuidv7(),
    practice_id uuid          NOT NULL REFERENCES practices (id),
    name        text          NOT NULL,
    price       numeric(12,2) NOT NULL,
    currency    char(3)       NOT NULL,
    active      boolean       NOT NULL DEFAULT true,
    version     bigint        NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),

    -- Needed by the same-practice composite foreign key of invoice lines (part 14).
    CONSTRAINT services_id_practice UNIQUE (id, practice_id),
    CONSTRAINT services_price       CHECK (price >= 0)
);

CREATE UNIQUE INDEX services_practice_name_idx ON services (practice_id, lower(name));

COMMENT ON TABLE  services          IS 'The price list. An invoice line snapshots name and price, so a later price change never moves an old invoice.';
COMMENT ON COLUMN services.currency IS 'Copied from the practice on create; the application never sets it from a request.';

-- RLS policy, grants, and audit trigger in one call (part 04).
SELECT tenant_table_setup('rooms');
SELECT tenant_table_setup('appointment_types');
SELECT tenant_table_setup('services');
