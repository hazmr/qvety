-- Part 03: staff accounts. First tenant table. RLS policy and audit trigger arrive in V3 (part 04).

CREATE TYPE user_role AS ENUM ('admin', 'veterinarian', 'technician', 'front_desk');

CREATE TABLE users (
    id                   uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id          uuid        NOT NULL REFERENCES practices (id),
    phone                text        NOT NULL,
    email                text,
    password_hash        text        NOT NULL,
    full_name            text        NOT NULL,
    role                 user_role   NOT NULL,
    is_veterinarian      boolean     NOT NULL DEFAULT false,
    license_number       text,
    active               boolean     NOT NULL DEFAULT true,
    session_version      integer     NOT NULL DEFAULT 1,
    must_change_password boolean     NOT NULL DEFAULT false,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),

    -- Login is by phone or email. Both unique per practice, not globally: the same person may work
    -- at two clinics. Phone is required (every staff member has one); email is optional.
    CONSTRAINT users_phone_per_practice UNIQUE (practice_id, phone),
    CONSTRAINT users_email_per_practice UNIQUE (practice_id, email),
    CONSTRAINT users_phone_e164 CHECK (phone ~ '^\+[1-9][0-9]{6,14}$'),
    -- Needed by later composite same-practice foreign keys (appointments, notes, prescriptions).
    CONSTRAINT users_id_practice UNIQUE (id, practice_id),
    -- Role decides what a user manages; the flag decides clinical acts. Only these combinations exist.
    CONSTRAINT users_vet_role_has_flag CHECK (role <> 'veterinarian' OR is_veterinarian),
    CONSTRAINT users_non_clinical_no_flag CHECK (role NOT IN ('technician', 'front_desk') OR NOT is_veterinarian)
);

CREATE INDEX users_practice_idx ON users (practice_id);

COMMENT ON COLUMN users.phone                IS 'E.164 (+20...). Login identifier together with email.';
COMMENT ON COLUMN users.is_veterinarian      IS 'May finalize notes, prescribe, be booked as the vet. Checked by clinical acts instead of role.';
COMMENT ON COLUMN users.license_number       IS 'Veterinary syndicate number; printed on prescriptions and certificates.';
COMMENT ON COLUMN users.session_version      IS 'Copied into every JWT. Bumping it revokes all tokens of this user.';
COMMENT ON COLUMN users.must_change_password IS 'Set on creation and on admin reset; only /me and /me/password work until cleared.';
