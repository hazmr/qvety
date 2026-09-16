## Why

Staff search by name or phone. Arabic spellings vary for the same name (أحمد / احمد, فاطمة / فاطمه) and phones are typed in several shapes for the same number. Search must find them all, and the duplicate warning from part 06 must use the same folding.

## What Changes

- `TextNormalizer` and `PhoneNormalizer` pure helpers with unit tests.
- `clients` gains `full_name_normalized`, `phone_e164`, `phone_secondary_e164`; `pg_trgm` GIN index and btree indexes.
- `GET /api/v1/clients?q=` searches by normalized name (ILIKE or trigram similarity) or by E.164 phone.
- Duplicate warning uses the normalized columns.
- No new Angular components: the `list-page` search box (300 ms debounce, part 06b) and the client form's duplicate alert (part 06) stay; only their results change.

## Capabilities

### New Capabilities
- `search-and-normalization`: Arabic text folding rules, phone storage rule, where they apply.

### Modified Capabilities
- `clients`: search behaviour and the duplicate check now use normalized name and E.164 phones.

## Non-goals

- Search across patients (part 08 adds its own `q`).
- Highlighting matches in the UI.
- Any region other than `EG` as default.

## Impact

- `V6__search.sql`; `com.qvety.common.TextNormalizer`, `PhoneNormalizer`; libphonenumber dependency.
- `ClientService` sets normalized columns on every save.
