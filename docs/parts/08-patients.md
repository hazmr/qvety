# Part 08 — Patients (animals)

## Business

A **patient** is the animal. Belongs to one client. Write in `docs/domain/patients.md`:
- Required: name, species, client. Optional: breed (free text), sex (`male`, `female`, `male_neutered`, `female_spayed`, `unknown`), date of birth or approximate age, color, microchip number, weight history.
- Species is a coded value with a translated label; breed is free text because breed lists are long and clinic-specific. Egyptian pet clinics see dogs, cats, birds (budgies, canaries, parrots), rabbits, rodents (hamsters), reptiles (turtles); horses and livestock come from farm work. Start with `dog|cat|bird|rabbit|rodent|reptile|horse|livestock|other`; a new value is one migration (`ALTER TYPE ... ADD VALUE`) plus two i18n lines.
- A patient can be marked deceased (date) or transferred to another client (keeps history; the old client is recorded).
- A patient photo is optional and helps the front desk recognize the animal. Storage arrives in part 13; this part leaves a nullable `photo_object_key` column empty.
- Never deleted once it has any clinical record.
- **Allergies** are recorded per patient: substance, reaction, severity (`mild`, `moderate`, `severe`), noted by whom and when. Shown as a red banner on the patient header and on every visit screen. An allergy is never edited or deleted; a wrong entry is marked `retracted` with a reason, so the record shows it was once believed.

## Stack you learn

- `@ManyToOne(fetch = LAZY)` and why lazy; `default_batch_fetch_size`.
- Postgres enum mapped with Hibernate 7 (`@JdbcType(PostgreSQLEnumJdbcType.class)`) vs `EnumType.STRING`; pick one and write down why.
- `JpaSpecificationExecutor` for filters: by client, species, deceased flag.
- A child table with history: `patient_weights`.
- First append-only-in-practice table: `patient_allergies` allows `UPDATE` of `retracted_at`/`retracted_reason` only. The full trigger pattern comes in part 13; here it is one small trigger you write by hand.
- Angular: nested routes (`/clients/:id/patients/:pid`), detail page with tabs, a small form inside a tab.

## Design

`V7__patients.sql`: `patients` (base + `client_id`, `name`, `species` enum, `breed`, `sex` enum, `date_of_birth`, `age_approximate`, `color`, `microchip`, `photo_object_key`, `deceased_at`, `notes`), `patient_weights` (base + `patient_id`, `measured_at`, `weight_kg numeric(6,2)`), `patient_allergies` (base + `patient_id`, `substance`, `reaction`, `severity` enum, `noted_by`, `retracted_at`, `retracted_reason`). All three appended to RLS and audit lists. FK to `clients` must also be same-practice: add a composite FK `(client_id, practice_id)` referencing `clients(id, practice_id)` so a cross-tenant link is impossible at the database level.

Endpoints: `GET /api/v1/patients?clientId&species&q`, `GET /{id}`, `POST`, `PUT`, `POST /{id}/deceased`, `POST /{id}/transfer`, `GET /{id}/weights`, `POST /{id}/weights`, `GET /{id}/allergies`, `POST /{id}/allergies`, `POST /{id}/allergies/{allergyId}/retract`.

## Steps

1. Migration with the composite FK. Seed patients for the seeded clients.
2. Entities, DTOs, mapper, `PatientService` (you write `transfer` by hand: record previous client, validate same practice).
3. Specifications for the list filters.
4. Test: list by client, filter by species, transfer, cross-tenant 404, a test that inserting a patient pointing at another practice's client fails at the database, and a test that `UPDATE patient_allergies SET substance = ...` as `qvety_app` is rejected while retract succeeds.
5. Angular: patients tab on the client detail; patient detail with tabs "Summary", "Weights", "Allergies", and empty "Visits" for part 13. Allergy banner in the patient header. Species labels from i18n.

## Ask Claude

- "Explain the composite foreign key `(client_id, practice_id)` trick and why RLS alone is not enough here."
- "Show a `Specification<Patient>` builder for optional filters."
- "Why does `@ManyToOne` default to EAGER and why is that wrong for us?"
- "How do I add a value to a Postgres enum in Flyway, and why can `ALTER TYPE ... ADD VALUE` not run inside a transaction?"

## Done when

- Patient CRUD, transfer, weight history, allergies working; banner visible.
- Composite FK test green.
- `TenantIsolationIT` covers patients.

## Self-check

- Draw the tables: practices, users, clients, patients, patient_weights, patient_allergies with keys. From memory.
- What does the composite FK prevent that RLS does not?
- Why is breed free text and species an enum?
