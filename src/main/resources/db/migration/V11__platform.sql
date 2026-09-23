-- Part 11: the Qvety side of the product. Super admins, the practice lifecycle, and the settings that
-- drive it. None of these tables has a practice_id and none is under row-level security: they are about
-- practices, not inside one, and they are reached only from com.qvety.platform under system context.

-- Platform users are their own table, not a flag on `users`. `users` is under forced RLS with a mandatory
-- practice_id; a super admin belongs to no practice, so sharing the table would mean a nullable tenant
-- column and a policy exception on the one table that must never have one.
CREATE TABLE platform_users (
    id              uuid        PRIMARY KEY DEFAULT uuidv7(),
    email           text        NOT NULL UNIQUE,
    password_hash   text        NOT NULL,
    full_name       text        NOT NULL,
    active          boolean     NOT NULL DEFAULT true,
    session_version integer     NOT NULL DEFAULT 1,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE  platform_users                 IS 'Qvety staff. No practice_id, no RLS; they never read clinical data through the tenant path.';
COMMENT ON COLUMN platform_users.session_version IS 'Bumped to revoke every token of this staff member, the same rule as users.session_version.';

CREATE TABLE platform_settings (
    key   text NOT NULL PRIMARY KEY,
    value text NOT NULL
);

COMMENT ON TABLE platform_settings IS 'Product-wide numbers the super admin can change without a release.';

-- Who did what to which practice. Append-only: this is the record that answers "who suspended us?", and
-- the account the application runs as must not be able to rewrite it.
CREATE TABLE platform_audit_log (
    id               uuid        PRIMARY KEY DEFAULT uuidv7(),
    platform_user_id uuid        REFERENCES platform_users (id),
    action           text        NOT NULL,
    target_type      text        NOT NULL,
    target_id        uuid,
    details          jsonb,
    at               timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX platform_audit_log_target_idx ON platform_audit_log (target_type, target_id, at DESC);

COMMENT ON TABLE  platform_audit_log                  IS 'Append-only. Every practice status change and every super-admin export lands here.';
COMMENT ON COLUMN platform_audit_log.platform_user_id IS 'Null only for rows written by a job rather than a person (part 12).';

-- Same shape as the part 08 evidence guards: SECURITY DEFINER, owned by qvety_owner, EXECUTE revoked
-- from qvety_app, so the application can insert and read but can never edit or erase the trail.
CREATE FUNCTION platform_audit_guard() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    RAISE EXCEPTION 'platform_audit_log is append-only'
        USING ERRCODE = 'check_violation';
END $$;

REVOKE ALL ON FUNCTION platform_audit_guard() FROM PUBLIC, qvety_app;

CREATE TRIGGER append_only BEFORE UPDATE OR DELETE ON platform_audit_log
    FOR EACH ROW EXECUTE FUNCTION platform_audit_guard();

-- A closed practice keeps its data for closed_retention_days after this moment, then it is deleted.
ALTER TABLE practices ADD COLUMN closed_at timestamptz;

COMMENT ON COLUMN practices.closed_at IS 'Set when the practice is closed; retention is counted from here.';

INSERT INTO platform_settings (key, value) VALUES
    ('trial_days', '30'),
    ('closed_retention_days', '90');

-- The application reads and writes staff and settings, and may only append to the trail. No RLS: these
-- tables are outside the tenant boundary, and com.qvety.platform is the only package that reaches them.
GRANT SELECT, INSERT, UPDATE ON platform_users    TO qvety_app;
GRANT SELECT, INSERT, UPDATE ON platform_settings TO qvety_app;
GRANT SELECT, INSERT         ON platform_audit_log TO qvety_app;

-- `practices` stays out of reach of the application role. It is under forced RLS keyed on the current
-- practice, and part 04 withheld UPDATE on status on purpose: a bug in a feature service must not be able
-- to suspend a clinic. The super admin still has to list every practice, create one, and change a status,
-- so those three acts go through definer functions owned by qvety_owner. qvety_app gets EXECUTE and
-- nothing else, the same door part 03 opened for login_lookup.

CREATE FUNCTION platform_practices() RETURNS SETOF practices
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
    AS $$ SELECT * FROM practices ORDER BY created_at $$;

-- Inserts the practice row only. The starter catalog and the first admin user are written by the caller
-- under an ordinary tenant context for the practice that now exists, so they pass through normal RLS.
CREATE FUNCTION platform_practice_create(
        p_name text, p_country char(2), p_currency char(3), p_locale text, p_timezone text, p_trial_days integer)
    RETURNS uuid
    LANGUAGE sql SECURITY DEFINER SET search_path = public
    AS $$
        INSERT INTO practices (name, country, currency, locale, timezone, status, trial_ends_at)
        VALUES (p_name, p_country, p_currency, p_locale, p_timezone, 'trial', now() + make_interval(days => p_trial_days))
        RETURNING id
    $$;

-- closed_at is set by the function, never by the caller, so it always matches the status it belongs to.
CREATE FUNCTION platform_practice_set_status(p_id uuid, p_status practice_status) RETURNS practices
    LANGUAGE sql SECURITY DEFINER SET search_path = public
    AS $$
        UPDATE practices
           SET status = p_status,
               closed_at = CASE WHEN p_status = 'closed' THEN coalesce(closed_at, now()) ELSE NULL END,
               updated_at = now()
         WHERE id = p_id
        RETURNING *
    $$;

REVOKE ALL ON FUNCTION platform_practices()                                          FROM PUBLIC;
REVOKE ALL ON FUNCTION platform_practice_create(text, char, char, text, text, integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION platform_practice_set_status(uuid, practice_status)           FROM PUBLIC;
GRANT EXECUTE ON FUNCTION platform_practices()                                          TO qvety_app;
GRANT EXECUTE ON FUNCTION platform_practice_create(text, char, char, text, text, integer) TO qvety_app;
GRANT EXECUTE ON FUNCTION platform_practice_set_status(uuid, practice_status)           TO qvety_app;
