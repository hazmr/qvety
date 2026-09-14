## Purpose

Defines how Arabic and Latin text is folded for search, sorting, and duplicate detection, and how Egyptian phone numbers are stored so that every way of typing the same number matches.

## ADDED Requirements

### Requirement: Text folding rules
Normalization SHALL: apply Unicode NFC; map alef variants (أ إ آ ٱ) to ا; map ة to ه; map ى to ي; strip tashkeel (harakat, shadda, sukun) and kashida; map Arabic-Indic digits to ASCII digits; collapse whitespace; lowercase Latin. The original text SHALL be stored unchanged; the folded form SHALL be stored beside it.

#### Scenario: Alef variant
- **WHEN** `أحمد` is normalized
- **THEN** the result equals the normalization of `احمد`

#### Scenario: Ta marbuta
- **WHEN** `فاطمة` is normalized
- **THEN** the result equals the normalization of `فاطمه`

#### Scenario: Diacritics
- **WHEN** `مُحَمَّد` is normalized
- **THEN** the result equals the normalization of `محمد`

### Requirement: Folding applies to search, duplicates, and sorting
The normalized form SHALL be used for name search, for the duplicate warning, and for sorting by name. The displayed value SHALL always be the original.

#### Scenario: Search finds variant spelling
- **WHEN** a client `أحمد محمد` exists and the search is `احمد`
- **THEN** the client is found and shown as `أحمد محمد`

### Requirement: Phones are stored in E.164 with default region Egypt
Phone input SHALL be parsed with default region `EG` and stored as E.164. Unparseable input SHALL be rejected with a validation error. The system SHALL never assume another default region.

#### Scenario: Local mobile
- **WHEN** `01012345678` is entered
- **THEN** `+201012345678` is stored

#### Scenario: Already international
- **WHEN** `+201012345678` is entered
- **THEN** it is stored unchanged

#### Scenario: Landline
- **WHEN** `0223456789` is entered
- **THEN** `+20223456789` is stored

#### Scenario: Missing leading zero
- **WHEN** `1012345678` is entered
- **THEN** the request is rejected with a validation error on the phone field

### Requirement: Phone search matches any typed shape
When the search term parses as a phone, the system SHALL match on the stored E.164 value of either phone column.

#### Scenario: Two shapes, same result
- **WHEN** the search is `01012345678` and then `+201012345678`
- **THEN** both return the same client
