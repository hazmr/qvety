## ADDED Requirements

### Requirement: Client search by name or phone
`GET /api/v1/clients?q=` SHALL: if `q` parses as a phone, match `phone_e164` or `phone_secondary_e164`; otherwise fold `q` and match `full_name_normalized` by substring or by trigram similarity above 0.3, ordered by similarity.

#### Scenario: Fuzzy name
- **WHEN** the search is a close misspelling of a stored name
- **THEN** the client appears, ranked by similarity

### Requirement: Duplicate warning uses normalized values
The create-time duplicate warning SHALL compare `phone_e164` (either column) and `full_name_normalized`.

#### Scenario: Variant spelling duplicate
- **WHEN** a client `احمد محمد` is created and `أحمد محمد` already exists
- **THEN** `warnings` lists the existing client and creation still succeeds

### Requirement: Normalized columns are maintained on every save
The service SHALL set `full_name_normalized`, `phone_e164`, and `phone_secondary_e164` on every create and update.

#### Scenario: Rename
- **WHEN** a client's `full_name` is updated
- **THEN** `full_name_normalized` reflects the new name
