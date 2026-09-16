# patients Specification

## Purpose

Defines the patient: the animal belonging to one client, with identity, weight history, allergies, deceased marking, and transfer between clients without losing history.

## Requirements

### Requirement: Patient identity
A patient SHALL have a name, a species, and a client. Optional: breed (free text), sex (`male`, `female`, `male_neutered`, `female_spayed`, `unknown`), date of birth or approximate age, color, microchip number, notes, photo.

#### Scenario: Missing species
- **WHEN** a patient is created without a species
- **THEN** the request is rejected with a validation error on `species`

### Requirement: Species is coded, breed is free text
Species SHALL be one of `dog, cat, bird, rabbit, rodent, reptile, horse, livestock, other`, stored as a database enum with a translated label. Breed SHALL be free text because breed lists are long and clinic-specific. Adding a species SHALL be one migration plus two translation lines.

#### Scenario: Species label
- **WHEN** a `cat` is shown in Arabic
- **THEN** the label comes from the translation file, not from the stored value

### Requirement: A patient belongs to a client of the same practice
The database SHALL refuse a patient whose client belongs to another practice, independently of row-level security.

#### Scenario: Cross-practice client link
- **WHEN** a row is inserted with `client_id` of practice B under practice A's `practice_id`
- **THEN** the database rejects the insert with a foreign key violation

### Requirement: Deceased and transfer keep history
A patient SHALL be markable deceased with a date. A living patient SHALL be transferable to another client of the same practice; the previous client SHALL be recorded and all records stay with the patient. A deceased patient SHALL NOT be transferred. A patient with any clinical record SHALL never be deleted.

#### Scenario: Transfer
- **WHEN** a patient is transferred from client A to client B
- **THEN** the patient's client is B, the previous client A is recorded, and visits and vaccinations are unchanged

#### Scenario: Deceased patient
- **WHEN** a patient is marked deceased
- **THEN** it is excluded from recalls and shown with the deceased date

#### Scenario: Transfer of a deceased patient
- **WHEN** a transfer is requested for a patient marked deceased
- **THEN** the request is rejected with 409 and the patient keeps its client

### Requirement: Weight history
Weights SHALL be recorded as dated rows in kilograms with two decimals; the latest non-voided weight SHALL be shown on the patient header. A weight is a measurement: it SHALL never be edited or deleted. A wrong weight SHALL be voided with a reason and stay listed as voided. The database SHALL allow updates only to `voided_at` and `void_reason`; any other update and any delete SHALL be refused. This is the shape part 13 reuses for vitals, which also write weights.

#### Scenario: Two weights
- **WHEN** two weights are recorded on different dates
- **THEN** both are listed and the most recent is the header value

#### Scenario: Void the latest weight
- **WHEN** `POST /patients/{id}/weights/{weightId}/void` is called with a reason on the most recent weight
- **THEN** the row stays listed as voided with its reason and the header shows the previous weight

#### Scenario: Edit a weight
- **WHEN** the application role runs `UPDATE patient_weights SET weight_kg = ...` or `DELETE FROM patient_weights`
- **THEN** the database refuses

#### Scenario: Void twice
- **WHEN** a void is requested on a weight already voided
- **THEN** the request is rejected with 409

### Requirement: Allergies are retracted, never edited or deleted
An allergy SHALL record substance, reaction, severity (`mild`, `moderate`, `severe`), who noted it, and when. The database SHALL allow updates only to `retracted_at` and `retracted_reason`; any other update and any delete SHALL be refused. Retracted allergies SHALL stay visible as retracted.

#### Scenario: Edit substance
- **WHEN** the application role runs `UPDATE patient_allergies SET substance = ...`
- **THEN** the database refuses

#### Scenario: Retract
- **WHEN** `POST /patients/{id}/allergies/{allergyId}/retract` is called with a reason
- **THEN** `retracted_at` and `retracted_reason` are set and the row remains

### Requirement: Allergies are shown before any clinical act
Active (non-retracted) allergies SHALL appear as a red banner on the patient header and on every visit screen, before anything is prescribed.

#### Scenario: Patient with allergy
- **WHEN** a patient with an active allergy is opened
- **THEN** the banner shows the substance and severity

### Requirement: Filtered patient list
`GET /api/v1/patients` SHALL filter by `clientId`, `species`, deceased flag, and a name search `q`.

#### Scenario: By client
- **WHEN** `GET /api/v1/patients?clientId=X` is called
- **THEN** only X's patients are returned, none of another practice
