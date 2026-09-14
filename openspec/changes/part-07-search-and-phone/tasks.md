## 1. Helpers
- [ ] 1.1 `TextNormalizer` with a table-driven unit test (≥15 cases, glossary examples, kashida, Arabic-Indic digits)
- [ ] 1.2 `PhoneNormalizer` on libphonenumber; tests for mobile, landline, international, missing zero

## 2. Migration
- [ ] 2.1 `V6__search.sql`: extension, columns, rough backfill, indexes

## 3. Backend
- [ ] 3.1 `ClientService` sets the three normalized columns on create and update
- [ ] 3.2 Native search query (phone branch, name branch) returning a `Page`
- [ ] 3.3 Duplicate warning uses normalized values

## 4. Tests
- [ ] 4.1 Seed `أحمد محمد`; search `احمد` finds it
- [ ] 4.2 Search `01012345678` and `+201012345678` return the same client
- [ ] 4.3 Cross-tenant: search never returns the other practice

## 5. Angular
- [ ] 5.1 Search box with 300 ms debounce
- [ ] 5.2 Duplicate warning on the form, non-blocking, with links
- [ ] 5.3 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 6. Close
- [ ] 6.1 `erd.md` updated
- [ ] 6.2 `progress.md` entry
- [ ] 6.3 Commit
