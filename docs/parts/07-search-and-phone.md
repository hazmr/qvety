# Part 07 — Arabic search and Egyptian phone numbers

## Business

Staff search by name or phone. Arabic has spelling variants that are the same name: أحمد / احمد, فاطمة / فاطمه, with or without diacritics. Searching `احمد` must find `أحمد`. Phones are typed as `01012345678`, `+201012345678`, `0100 123 4567`; all are the same number.

Write in `docs/domain/search-and-normalization.md`: the folding rules (NFC, alef variants → ا, ة → ه, strip tashkeel, collapse spaces, lowercase Latin), where they apply (search, duplicate warning, sorting), and the phone rule (store E.164, default region EG, reject unparseable, never assume +1).

## Stack you learn

- A pure Java helper with unit tests (`TextNormalizer`, `PhoneNormalizer`).
- Postgres generated columns and `pg_trgm` GIN index.
- Native query with `similarity()` and `ILIKE` on the normalized column, returned as a `Page`.
- libphonenumber.
- Angular: search input with debounce, highlighting not needed.

## Design

`V6__search.sql`: `CREATE EXTENSION IF NOT EXISTS pg_trgm`; add `full_name_normalized`, `phone_e164`, and `phone_secondary_e164` columns to `clients` (application writes them; a generated column cannot call Java, so the service sets them on every save); GIN index `gin_trgm_ops` on `full_name_normalized`; btree on `phone_e164` and on `phone_secondary_e164`.

`GET /api/v1/clients?q=` now: if `q` parses as a phone, match `phone_e164` or `phone_secondary_e164`; else normalize `q` and search `full_name_normalized` with `ILIKE '%q%' OR similarity(...) > 0.3`, ordered by similarity.

Duplicate warning on create: same `phone_e164` (either column) or same `full_name_normalized` returns the candidates in the response (`warnings`), does not block.

## Steps

1. `TextNormalizer` with a table-driven unit test (at least 15 cases, including the glossary examples). Write the cases yourself; that is the domain knowledge.
2. `PhoneNormalizer` on libphonenumber, region `EG`. Tests: `01012345678` → `+201012345678`; `1012345678` → error; `+201012345678` → unchanged; landline `0223456789` → `+20223456789`.
3. Migration; backfill existing rows in the same migration with a simple SQL lowercase/trim (Java folding applies on next save).
4. `ClientService` sets normalized columns on create/update. Search endpoint with native query.
5. Test: seed `أحمد محمد`, search `احمد`, found; search `01012345678` and `+201012345678`, same result.
6. Angular: search box on the client list with 300 ms debounce; duplicate warning shown on the form as a non-blocking alert with links.

## Ask Claude

- "Explain Arabic Unicode normalization: NFC vs NFD, and which characters are tashkeel."
- "Write a native Spring Data query using pg_trgm similarity that returns a `Page`, with the count query."
- "Review `TextNormalizer` for characters I missed (e.g., kashida, Arabic-Indic digits)."

## Done when

- Unit tests for both helpers green.
- Search works in the browser for the variant spellings.
- Duplicate warning appears and can be ignored.

## Self-check

- Why is normalization done in Java and stored, rather than computed in SQL at query time?
- What does `similarity > 0.3` mean and how would you tune it with real data?
- Why is the duplicate check a warning and not a block?

## Answers

- **Java-side folding, stored column.** The normalized value is written once, on save, and indexed (`clients_name_trgm_idx`, `clients_practice_name_norm_idx`). Folding in SQL at query time would run on every row of every search and could not use those indexes, so each search becomes a full scan. Storing it also keeps one rule for search, the duplicate check, and any later import: the Java helper is the single source of truth and is unit-tested; a SQL expression would drift from it.
- **`similarity > 0.3`.** pg_trgm splits both strings into three-character trigrams and scores the overlap from 0 (nothing shared) to 1 (identical). 0.3 is the extension's default and is loose on purpose: Arabic names are short, and one misplaced letter removes several trigrams. The repository uses the `%` operator, which reads `pg_trgm.similarity_threshold` and hits the GIN index; a literal `similarity(...) > 0.3` in `WHERE` would not. To tune it: collect real searches, look at the ones with zero hits and the ones with a page of noise, and move the threshold with `SET pg_trgm.similarity_threshold` in a session until both lists shrink. Shorter names need a lower threshold, longer ones tolerate a higher one.
- **Warning, not block.** A shared phone is normal in Egypt: one number for a household, a parent bringing a child's cat, a walk-in giving a relative's number. Blocking would stop the receptionist at the desk with the client waiting, and they would work around it by mangling the name or phone, which poisons search. A warning shows the candidates, lets staff pick the existing client or continue, and keeps every record clean and searchable.
