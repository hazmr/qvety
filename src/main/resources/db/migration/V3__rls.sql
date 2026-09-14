-- Part 04: tenant boundary and audit log.
-- Two roles: qvety_owner (Flyway, owns everything) and qvety_app (the application, cannot bypass RLS).
-- The app password is a Flyway placeholder filled from the environment (QVETY_APP_DB_PASSWORD).

-- 1. Application role. CREATE ROLE is cluster-wide, so guard for re-runs on a shared local cluster.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'qvety_app') THEN
        CREATE ROLE qvety_app LOGIN PASSWORD '${app_password}' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END $$;

GRANT CONNECT ON DATABASE ${db_name} TO qvety_app;
GRANT USAGE ON SCHEMA public TO qvety_app;

-- 2. The tenant id for the current transaction. NULLIF guards against an empty string, which would
--    fail the uuid cast; unset or empty means "no tenant" and every policy returns no rows.
CREATE FUNCTION current_practice_id() RETURNS uuid
    LANGUAGE sql STABLE
    RETURN NULLIF(current_setting('app.practice_id', true), '')::uuid;

CREATE FUNCTION current_user_id() RETURNS uuid
    LANGUAGE sql STABLE
    RETURN NULLIF(current_setting('app.user_id', true), '')::uuid;

-- 3. Audit log. Written by trigger only; the app may read and insert, never update or delete.
CREATE TYPE audit_action AS ENUM ('insert', 'update', 'delete');

CREATE TABLE audit_log (
    id          uuid         PRIMARY KEY DEFAULT uuidv7(),
    practice_id uuid         NOT NULL,
    user_id     uuid,
    table_name  text         NOT NULL,
    row_id      uuid         NOT NULL,
    action      audit_action NOT NULL,
    before      jsonb,
    after       jsonb,
    at          timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_row_idx ON audit_log (practice_id, table_name, row_id, at DESC);

COMMENT ON TABLE audit_log IS 'Every insert/update/delete on tenant tables and on practices. Trigger-written, append-only.';

-- 4. Generic audit trigger. SECURITY DEFINER and owned by qvety_owner so it can always write audit_log;
--    EXECUTE revoked from qvety_app so the app cannot call it or write fake rows through it.
CREATE FUNCTION audit_row() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    secret_columns text[] := ARRAY['password_hash'];
    before_img jsonb;
    after_img  jsonb;
    row_practice uuid;
    row_uuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN before_img := to_jsonb(OLD) - secret_columns; END IF;
    IF TG_OP <> 'DELETE' THEN after_img  := to_jsonb(NEW) - secret_columns; END IF;

    IF TG_TABLE_NAME = 'practices' THEN
        row_practice := COALESCE(NEW.id, OLD.id);
    ELSE
        row_practice := COALESCE(NEW.practice_id, OLD.practice_id);
    END IF;
    row_uuid := COALESCE(NEW.id, OLD.id);

    INSERT INTO audit_log (practice_id, user_id, table_name, row_id, action, before, after)
    VALUES (row_practice, current_user_id(), TG_TABLE_NAME, row_uuid, lower(TG_OP)::audit_action, before_img, after_img);
    RETURN NULL;   -- AFTER trigger; return value ignored
END $$;

REVOKE ALL ON FUNCTION audit_row() FROM PUBLIC, qvety_app;

-- 5. One call per tenant table. Every later migration that adds a tenant table calls this.
--    Enables and forces RLS, creates the isolation policy, grants CRUD, attaches the audit trigger.
CREATE FUNCTION tenant_table_setup(tbl regclass) RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tbl);
    EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', tbl);
    EXECUTE format('CREATE POLICY tenant_isolation ON %s USING (practice_id = current_practice_id()) WITH CHECK (practice_id = current_practice_id())', tbl);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %s TO qvety_app', tbl);
    EXECUTE format('CREATE TRIGGER audit AFTER INSERT OR UPDATE OR DELETE ON %s FOR EACH ROW EXECUTE FUNCTION audit_row()', tbl);
END $$;

REVOKE ALL ON FUNCTION tenant_table_setup(regclass) FROM PUBLIC, qvety_app;

SELECT tenant_table_setup('users');

-- audit_log: isolated and readable, but the app only inserts through the trigger. No trigger on itself.
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON audit_log USING (practice_id = current_practice_id()) WITH CHECK (practice_id = current_practice_id());
GRANT SELECT, INSERT ON audit_log TO qvety_app;

-- 6. practices: the practice reads its own row and edits identity columns only. Status, country,
--    currency stay out of reach of the application role even with a bug in the service.
ALTER TABLE practices ENABLE ROW LEVEL SECURITY;
ALTER TABLE practices FORCE ROW LEVEL SECURITY;
CREATE POLICY practice_self ON practices USING (id = current_practice_id());
GRANT SELECT ON practices TO qvety_app;
GRANT UPDATE (name, address, phone, vat_number, tax_rate_percent, updated_at) ON practices TO qvety_app;
CREATE TRIGGER audit AFTER INSERT OR UPDATE OR DELETE ON practices FOR EACH ROW EXECUTE FUNCTION audit_row();

-- 7. Login runs before any tenant is known, and users is under forced RLS. This is the one
--    deliberate exception: a definer function that returns active users by email. Nothing else
--    reads across tenants without a session variable.
CREATE FUNCTION login_lookup(p_email text) RETURNS SETOF users
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
    AS $$ SELECT * FROM users WHERE lower(email) = lower(p_email) AND active $$;

REVOKE ALL ON FUNCTION login_lookup(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION login_lookup(text) TO qvety_app;
