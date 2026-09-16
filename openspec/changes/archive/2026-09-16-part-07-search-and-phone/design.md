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

No new components. The client list already runs on the shared `list-page` (part 06b), whose search box debounces 300 ms and resets paging; the client form already shows the non-blocking duplicate alert with links to the candidates (part 06). Part 07 changes what the server returns for the same `q`, so the Angular work is a browser check: variant spellings and phone shapes find the client, and the duplicate alert lists normalized matches.

## Phone layout

Already covered by the part 06b shell and shared components (`list-page` stacked rows, `form-page` sizes, title bar, action bar). Nothing new to lay out; task 5.3 re-checks the two screens at 390 px and 360 px because their content changes.
