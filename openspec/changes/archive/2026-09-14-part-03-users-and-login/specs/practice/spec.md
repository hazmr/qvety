## ADDED Requirements

### Requirement: Practice edits its own identity
An `admin` of the practice SHALL edit `name`, `address`, `phone`, `vat_number`, and `tax_rate_percent` via `PUT /api/v1/practice`. `country`, `currency`, and `status` SHALL NOT be accepted in that request; only the super admin changes them.

#### Scenario: Admin updates address
- **WHEN** an `admin` sends `PUT /api/v1/practice` with a new address and phone
- **THEN** the practice row is updated and returned

#### Scenario: Front desk updates address
- **WHEN** a `front_desk` user sends the same request
- **THEN** the response is 403

#### Scenario: Status in request body
- **WHEN** the request body contains `status` or `country`
- **THEN** those fields are ignored and the stored values are unchanged
