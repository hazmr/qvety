## 1. Helpers
- [x] 1.1 `TextNormalizer` with a table-driven unit test (≥15 cases, glossary examples, kashida, Arabic-Indic digits)
- [x] 1.2 `PhoneNormalizer` on libphonenumber; tests for mobile, landline, international, missing zero

## 2. Migration
- [x] 2.1 `V6__search.sql`: extension, columns, rough backfill, indexes

## 3. Backend
- [x] 3.1 `ClientService` sets the three normalized columns on create and update
- [x] 3.2 Native search query (phone branch, name branch) returning a `Page`
- [x] 3.3 Duplicate warning uses normalized values

## 4. Tests
- [x] 4.1 Seed `أحمد محمد`; search `احمد` finds it
- [x] 4.2 Search `01012345678` and `+201012345678` return the same client
- [x] 4.3 Cross-tenant: search never returns the other practice

## 5. Angular
- [x] 5.1 Browser check: the existing `list-page` search box (300 ms debounce, part 06b) finds `أحمد` from `احمد` and one client from `01012345678`, `+201012345678`, `0100 123 4567`; no new component
- [x] 5.2 Browser check: the existing duplicate alert on the client form (part 06) lists normalized-name and E.164 matches; no UI change expected
- [x] 5.3 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 6. Close
- [x] 6.1 `erd.md` updated
- [x] 6.2 `progress.md` entry
- [x] 6.3 Commit
