## Purpose

Defines the medical record of a visit and the rules that make it trustworthy: attribution to a veterinarian, immutability after signing, void instead of delete, append-only evidence for prescriptions and controlled substances, and bilingual printing.

## ADDED Requirements

### Requirement: A visit holds what happened
A visit SHALL be created for a patient from an appointment or as a walk-in and SHALL hold vitals, one clinical note, vaccinations given, prescriptions issued, and attachments. A visit SHALL have an attending veterinarian or none (technician-only visit).

#### Scenario: Start from appointment
- **WHEN** "start visit" is pressed on a checked-in appointment
- **THEN** a visit exists linked to that appointment and patient, status `open`

### Requirement: Clinical acts need a veterinarian
Finalizing a note SHALL set `finalized_by`; a prescription SHALL have a `prescriber_id`; a vaccination given by a non-vet SHALL have a `supervising_vet_id`. Only a user with `is_veterinarian` SHALL finalize or prescribe; the check SHALL be on the flag, never the role. A visit without an attending veterinarian SHALL accept vitals and supervised vaccinations and nothing else. The database SHALL enforce this, not only the service.

#### Scenario: Technician finalizes
- **WHEN** a `technician` calls finalize
- **THEN** the response is 403

#### Scenario: Admin with vet flag finalizes
- **WHEN** an `admin` with `is_veterinarian = true` calls finalize
- **THEN** the note is finalized

#### Scenario: Prescription on technician visit
- **WHEN** a prescription is created on a visit with no attending veterinarian
- **THEN** the request is rejected

#### Scenario: Supervised vaccination on technician visit
- **WHEN** a technician records a vaccination with a `supervising_vet_id` on a visit with no attending vet
- **THEN** the vaccination is accepted

### Requirement: Visit close
A visit SHALL be closable only when its note is finalized or voided (or there is no note). After billing exists, closing SHALL also require an invoice for the visit or a `no_charge_reason`. Open visits SHALL be listed at end of day, never hidden.

#### Scenario: Draft note
- **WHEN** close is called on a visit whose note is a draft
- **THEN** the response is 409

#### Scenario: Open visits list
- **WHEN** `GET /api/v1/visits?status=open&date=today` is called
- **THEN** every visit still open today is returned

### Requirement: Vitals are voided, never edited
Vitals SHALL be recorded any number of times per visit: temperature °C, heart rate, respiratory rate, weight kg (also written to the patient's weight history), body condition score 1–9, pain score 0–10, mucous membrane colour, capillary refill time s, notes. The database SHALL allow updates only to the void columns.

#### Scenario: Weight flows to history
- **WHEN** vitals with a weight are recorded
- **THEN** a `patient_weights` row is inserted for that patient

#### Scenario: Edit temperature
- **WHEN** the application role updates a vitals row's temperature
- **THEN** the database refuses

### Requirement: Clinical note lifecycle
A note SHALL have four optional sections (S, O, A, P). It SHALL be a draft until a veterinarian finalizes it. After finalization the text SHALL never change; the database SHALL refuse updates to text columns. Corrections SHALL be addenda (dated, attributed, appended). A wrong-patient note SHALL be voided with a reason and a replacement, never edited.

#### Scenario: Update after finalize
- **WHEN** the application role updates a finalized note's text
- **THEN** the database refuses (the rejection comes from Postgres, not from Java)

#### Scenario: Addendum
- **WHEN** a veterinarian adds an addendum to a finalized note
- **THEN** it is appended with date and author and the original text is unchanged

### Requirement: Signer's name is snapshotted
Finalizing SHALL copy the signer's name into `finalized_by_name`. Renaming or deactivating the user later SHALL NOT change what the signed record shows.

#### Scenario: Rename after signing
- **WHEN** the signing vet's `full_name` is changed after finalization
- **THEN** the note still shows the name at signing

### Requirement: Allergies shown before prescribing
The patient's active allergies SHALL be shown on the visit screen before any prescription is entered.

#### Scenario: Visit for allergic patient
- **WHEN** a visit for a patient with an active allergy is opened
- **THEN** the allergy banner is visible above the prescriptions tab

### Requirement: Vaccination record
A vaccination SHALL record vaccine name, manufacturer, lot, product expiry, dose type (`initial`, `booster`), date given, next due, given by, supervising veterinarian when given by a non-vet, and optional rabies tag number. A wrong row SHALL be voided with a reason, never edited or deleted. Voided rows SHALL be excluded from recalls and the certificate and stay in the record.

#### Scenario: Void vaccination
- **WHEN** a vaccination is voided
- **THEN** it is absent from the certificate and from recalls and still visible on the visit

### Requirement: Prescription and events
A prescription SHALL record drug, dose, route, frequency, duration, quantity and unit (what leaves the clinic), and prescriber. Events `issued`, `dispensed`, `cancelled` SHALL be append-only. An `issued` prescription that is never `dispensed` SHALL be normal (pharmacy case); nothing chases it.

#### Scenario: Delete event
- **WHEN** the application role deletes a prescription event
- **THEN** the database refuses

### Requirement: Printed prescription
Every prescription SHALL be printable (روشتة) with practice header and logo, veterinarian name and license number, patient, drug lines, date, signature space, in Arabic and English, sized A5.

#### Scenario: Print
- **WHEN** "Print prescription" is pressed
- **THEN** the print view shows the logo, the vet's license number, and every drug line in both languages

### Requirement: Controlled-substance log
Under framework `EG`, every dispense of a controlled drug SHALL write a log row with running balance. The log SHALL also accept `receive` rows (quantity in, supplier reference, received by) and `adjust` rows (count correction with reason), entered by an admin. The balance SHALL never go below zero; the log SHALL be append-only. The list of controlled drugs SHALL be per framework.

#### Scenario: Receive then dispense
- **WHEN** 10 units are received and two dispenses of 3 follow
- **THEN** the balances after are 10, 7, 4

#### Scenario: Dispense below zero
- **WHEN** a dispense exceeds the balance
- **THEN** the request is rejected and no event or log row is written

### Requirement: Attachments never leave a finalized record
Images and PDFs SHALL be stored in object storage under a key prefixed with the practice id, referenced by the visit, uploaded and downloaded through short-lived signed URLs issued only after the tenant-scoped row lookup. Attachments SHALL never be deleted once the note is finalized.

#### Scenario: Cross-tenant attachment
- **WHEN** practice A requests a download URL for practice B's attachment id
- **THEN** the response is 404 and no URL is issued

### Requirement: Vaccination certificate
One page per patient SHALL print: practice name, logo and stamp area, patient identity (name, species, breed, sex, date of birth, microchip), owner, and non-voided vaccination rows (vaccine, date, lot, next due, vet), Arabic and English on the same page, sized A4.

#### Scenario: Certificate content
- **WHEN** the certificate is printed for a patient with two valid and one voided vaccination
- **THEN** two rows appear

### Requirement: Trigger functions are out of the application's reach
Every protection trigger function SHALL be `SECURITY DEFINER`, owned by the migration role, with `EXECUTE` revoked from the application role.

#### Scenario: Call trigger function
- **WHEN** the application role runs `SELECT protect_finalized_note()`
- **THEN** the database refuses with permission denied
