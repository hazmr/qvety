-- Part 07: Arabic search and E.164 phones. The folded columns are written by the application on every
-- save (TextNormalizer, PhoneNormalizer); a generated column cannot call Java. Typed values stay as typed.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE clients
    ADD COLUMN full_name_normalized text,
    ADD COLUMN phone_e164           text,
    ADD COLUMN phone_secondary_e164 text;

-- Rough backfill so existing rows are searchable at once; the Java folding replaces it on the next save.
-- Phones: trunk zero plus 9 or 10 digits (Egyptian landline or mobile), or already international.
UPDATE clients SET
    full_name_normalized = lower(regexp_replace(trim(full_name), '\s+', ' ', 'g')),
    phone_e164 = CASE
        WHEN phone ~ '^0\d{9,10}$'    THEN '+20' || substr(phone, 2)
        WHEN phone ~ '^\+20\d{9,10}$' THEN phone
    END,
    phone_secondary_e164 = CASE
        WHEN phone_secondary ~ '^0\d{9,10}$'    THEN '+20' || substr(phone_secondary, 2)
        WHEN phone_secondary ~ '^\+20\d{9,10}$' THEN phone_secondary
    END;

ALTER TABLE clients ALTER COLUMN full_name_normalized SET NOT NULL;

CREATE INDEX clients_name_trgm_idx           ON clients USING gin (full_name_normalized gin_trgm_ops);
CREATE INDEX clients_practice_name_norm_idx  ON clients (practice_id, full_name_normalized);
CREATE INDEX clients_practice_phone_e164_idx ON clients (practice_id, phone_e164);
CREATE INDEX clients_practice_phone2_e164_idx ON clients (practice_id, phone_secondary_e164);

-- Part 06 indexes on the raw columns: nothing queries them after this part.
DROP INDEX clients_practice_name_idx;
DROP INDEX clients_practice_phone_idx;

COMMENT ON COLUMN clients.full_name_normalized IS 'Folded by TextNormalizer for search, duplicates, sorting; never shown.';
COMMENT ON COLUMN clients.phone_e164           IS 'E.164 of phone, default region EG; phone keeps the typed form.';
COMMENT ON COLUMN clients.phone_secondary_e164 IS 'E.164 of phone_secondary.';
