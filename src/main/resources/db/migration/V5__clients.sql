-- Part 06: clients (pet owners). First feature tenant table; sets the pattern for every later one.

CREATE TABLE clients (
    id               uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id      uuid        NOT NULL REFERENCES practices (id),
    full_name        text        NOT NULL,
    preferred_name   text,
    phone            text,
    phone_secondary  text,
    email            text,
    address          text,
    notes            text,
    preferred_locale text,
    archived_at      timestamptz,
    version          bigint      NOT NULL DEFAULT 0,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),

    -- A client must be reachable: at least one of phone or email.
    CONSTRAINT clients_reachable CHECK (phone IS NOT NULL OR email IS NOT NULL),
    -- Needed by later composite same-practice foreign keys (patients, appointments, invoices).
    CONSTRAINT clients_id_practice UNIQUE (id, practice_id)
);

CREATE INDEX clients_practice_name_idx ON clients (practice_id, lower(full_name));
CREATE INDEX clients_practice_phone_idx ON clients (practice_id, phone);

COMMENT ON TABLE  clients             IS 'The pet owner: a person who pays. Archived, never deleted. No national id column by design.';
COMMENT ON COLUMN clients.full_name   IS 'Typed as the person says it (name chain); no first/last split.';
COMMENT ON COLUMN clients.archived_at IS 'Set instead of deleting; archived clients leave default lists and recalls.';

-- RLS policy, grants, and audit trigger in one call (part 04).
SELECT tenant_table_setup('clients');

-- users joins the shared base entity (part 06): optimistic locking column it lacked.
ALTER TABLE users ADD COLUMN version bigint NOT NULL DEFAULT 0;
