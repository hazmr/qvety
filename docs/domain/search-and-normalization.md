# Search and normalization

How names and phone numbers are folded so that every way of writing the same person finds the same record. The desk types fast, in Arabic or English, and rarely the same way twice. Draft written with Claude on 2026-09-15; correct it after watching a real front desk search.

## The problem

- `أحمد` and `احمد` are the same name. So are `فاطمة` and `فاطمه`, `مصطفى` and `مصطفي`, `مُحَمَّد` and `محمد`.
- `01012345678`, `+201012345678`, `0100 123 4567`, `٠١٠١٢٣٤٥٦٧٨` are the same number.
- What the desk typed must stay on the card exactly as typed. Folding is for finding, never for showing.

## Name folding

The folded form is computed once on every save and stored next to the original (`full_name_normalized`). It is never shown. Rules, applied in order:

1. Unicode NFC.
2. Alef variants `أ إ آ ٱ` become bare `ا`.
3. Ta marbuta `ة` becomes `ه`.
4. Alef maqsura `ى` becomes `ي`.
5. Tashkeel (fatha, damma, kasra, tanwin, shadda, sukun, superscript alef) and kashida `ـ` are removed.
6. Arabic-Indic digits `٠..٩` and `۰..۹` become `0..9`.
7. Whitespace is trimmed and collapsed to one space.
8. Latin letters are lowercased.

Deliberately not folded: hamza on waw and ya (`ؤ ئ`), and the letters `ك`/`گ`, `ي`/`ی` from other languages. Add a rule only after a real name was missed.

## Where folding applies

| Use | What is compared | What is shown |
| --- | --- | --- |
| Search by name | folded search term against the folded name: substring, or trigram similarity for close misspellings, best match first | the name as typed |
| Duplicate warning on save | folded name equal to an existing folded name | the existing card as typed |
| Sorting by name | folded name, so `أحمد` and `احمد` sit together | the name as typed |

## Phone numbers

- Every phone is parsed with Egypt as the default region and stored as E.164 (`+20...`) next to the typed value (`phone_e164`, `phone_secondary_e164`).
- Accepted: the trunk zero form (`010...`, `02...`), the international form (`+20...`), and the `0020...` prefix, with or without spaces.
- Rejected with a validation error on the phone field: anything that does not parse as a valid number for Egypt, including a mobile number missing its leading zero (`1012345678`). The system never guesses another country.
- Search: when the search term parses as a phone, it is matched against both E.164 columns, so any typed shape finds the card.
- Duplicate warning: any of the saved E.164 phones equal to either phone of an existing card.

## Why in the application, not the database

The folding rules are Egyptian-Arabic knowledge that will grow. One implementation in Java serves search, duplicates, and sorting, is unit-tested against a table of real cases, and the stored column is what the trigram index and the phone index read. A database expression would have to repeat the rules and could not be indexed the same way.

## Tuning

The similarity threshold starts at 0.3 (`pg_trgm.similarity_threshold`). Watch what the desk searches and does not find; raise or lower it with real names, not guesses.
