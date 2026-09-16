## Why

A patient is the animal; every clinical and billing record hangs off it. Patients also introduce two patterns used later: a same-practice composite foreign key, and the first append-only-in-practice table (allergies, voided not edited).

## What Changes

- `patients`, `patient_weights`, `patient_allergies` tenant tables, all in RLS and audit lists.
- Composite FK `(client_id, practice_id)` → `clients(id, practice_id)` so a cross-tenant link is impossible at the database.
- Species enum, sex enum, deceased marking, transfer to another client keeping history.
- Allergies: never edited or deleted; retracted with a reason; enforced by a small trigger.
- Filtered patient list; nested Angular routes and a detail page with tabs.

## Capabilities

### New Capabilities
- `patients`: the animal, its identity, weights, allergies, deceased and transfer rules.

### Modified Capabilities
- none

## Non-goals

- Patient photo upload (part 13); the nullable `photo_object_key` column exists now.
- Visits tab content (part 13).
- Breed as a coded list; breed stays free text.

## Impact

- `V7__patients.sql`, `com.qvety.patients`, `Specification` filters, `TenantIsolationIT` gains three tables.
- Angular `/clients/:id/patients/:pid`, patients tab on client detail.
