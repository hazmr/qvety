## Migration `V6__search.sql`

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
ALTER TABLE clients
  ADD COLUMN full_name_normalized text,
  ADD COLUMN phone_e164 text,
  ADD COLUMN phone_secondary_e164 text;
UPDATE clients SET full_name_normalized = lower(trim(full_name));  -- rough backfill; Java folding applies on next save
CREATE INDEX clients_name_trgm ON clients USING gin (full_name_normalized gin_trgm_ops);
CREATE INDEX clients_phone_e164 ON clients (practice_id, phone_e164);
CREATE INDEX clients_phone2_e164 ON clients (practice_id, phone_secondary_e164);
```

Normalization is done in Java and stored, not computed in SQL at query time: a generated column cannot call the Java folding, the index needs a stored value, and one implementation serves search, duplicates, and sorting.

## Helpers

- `TextNormalizer.fold(String)`: pure, table-driven unit test with at least 15 cases (glossary examples included). Write the cases yourself; that is the domain knowledge.
- `PhoneNormalizer.toE164(String)` on libphonenumber, region `EG`; throws a validation error when unparseable.

## Query

Native Spring Data query returning `Page<ClientDto>` with a count query:

```sql
SELECT ... FROM clients
WHERE practice_id = current_setting('app.practice_id')::uuid   -- RLS applies anyway; keep explicit for the planner
  AND (full_name_normalized ILIKE '%' || :q || '%' OR similarity(full_name_normalized, :q) > 0.3)
ORDER BY similarity(full_name_normalized, :q) DESC
```

Phone branch: `WHERE phone_e164 = :e164 OR phone_secondary_e164 = :e164`.

`0.3` is a starting threshold; tune with real data by looking at what the desk searches and misses.

## Angular

Search input on the client list, 300 ms debounce. Duplicate warning as a non-blocking alert with links to the candidates.

## Phone layout

Reference: `docs/design/MOBILE.md` layout skeleton. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
