-- Part 03b: practice names for the login picker. The same phone or email may exist at more than one
-- practice; when the password matches several rows the user has to choose. practices is under forced
-- RLS and no tenant is set before login, so this definer function returns id and name for the given
-- ids only. AuthService calls it after the password matched, never before. Second and last cross-tenant read.

CREATE FUNCTION login_practices(p_ids uuid[]) RETURNS TABLE (id uuid, name text)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
    AS $$ SELECT p.id, p.name FROM practices p WHERE p.id = ANY (p_ids) ORDER BY p.name $$;

REVOKE ALL ON FUNCTION login_practices(uuid[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION login_practices(uuid[]) TO qvety_app;
