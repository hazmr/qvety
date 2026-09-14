## Purpose

Defines the small configurable lists a practice maintains (rooms, appointment types, services), the rule that anything with workflow is not reference data, and the per-country starter catalog.

## ADDED Requirements

### Requirement: Reference entities
The reference entities SHALL be: `rooms` (name), `appointment_types` (name, default duration in minutes, color), `services` (name, price with currency). Each SHALL have an `active` flag. Generic CRUD SHALL be allowed for these tables and for no other.

#### Scenario: Add a fourth reference entity
- **WHEN** a new reference entity is needed
- **THEN** it takes one migration, one entity, and one Angular config object; no new service or page

### Requirement: Anything with workflow is not reference data
A table whose rows change status over time (invoices, appointments, visits) SHALL NOT use the generic reference CRUD.

#### Scenario: Invoice through reference CRUD
- **WHEN** a developer proposes invoices as reference data
- **THEN** the proposal is rejected; invoices have a status flow

### Requirement: Deactivate, never delete
Reference rows SHALL be deactivated, not deleted, because appointments and invoice lines reference them. Inactive rows SHALL be hidden from pickers and kept in history.

#### Scenario: Room in use
- **WHEN** a room with past appointments is deactivated
- **THEN** past appointments still show the room and new bookings cannot pick it

### Requirement: Service price in practice currency
A service price SHALL be `numeric(12,2)` with the practice currency. Invoice lines snapshot the price at the time (see `billing`); changing a service price SHALL NOT change past invoices.

#### Scenario: Price change
- **WHEN** a service price is edited
- **THEN** existing invoice lines keep their snapshot price

### Requirement: Starter catalog per country
When a practice is created with a supported country, the system SHALL insert that country's starter rooms, appointment types, and services from a per-country data file. Egypt's catalog SHALL use Arabic names and EGP prices (e.g., كشف, تطعيم, جراحة صغرى). An unsupported country SHALL fail creation.

#### Scenario: Egyptian practice
- **WHEN** a practice is created with country `EG`
- **THEN** its services list contains the Egyptian starter rows in Arabic with EGP prices

#### Scenario: Unsupported country
- **WHEN** a practice is created with a country that has no catalog file
- **THEN** creation fails and no practice row is written

### Requirement: Reference data is tenant-scoped
Reference rows SHALL be under row-level security and audited like any tenant table.

#### Scenario: Other practice's rooms
- **WHEN** practice A lists rooms
- **THEN** practice B's rooms are not returned
