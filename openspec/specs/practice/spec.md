## Purpose

Defines the practice: one clinic business, its identity, its country-driven defaults, and its lifecycle status. Every tenant row belongs to exactly one practice.

## Requirements

### Requirement: A practice has identity and country defaults
The system SHALL store for each practice a name, a country (ISO 3166-1 alpha-2), a currency (ISO 4217), a locale, a timezone, a status, an address, a phone, an optional VAT number, and a tax rate percent (default 0). Country SHALL decide currency, locale, timezone, and regulatory framework.

#### Scenario: Egyptian practice defaults
- **WHEN** a practice is created with country `EG`
- **THEN** its currency is `EGP`, locale `ar-EG`, timezone `Africa/Cairo`, and regulatory framework `EG`

#### Scenario: Unmapped country is rejected
- **WHEN** a practice is created with a country that has no mapping
- **THEN** creation fails with a validation error and no row is written; no other country's defaults are applied

### Requirement: Practice status starts as trial
The system SHALL create every practice with status `trial`. Only the super admin or the subscription job (later parts) SHALL change the status; the practice itself SHALL NOT.

#### Scenario: New practice is on trial
- **WHEN** a practice is created
- **THEN** its status is `trial` and `trial_ends_at` is set from the platform `trial_days` setting

### Requirement: Identity columns exist for printing
The system SHALL keep `address`, `phone`, `vat_number`, and `tax_rate_percent` on the practice so the invoice header (billing) and the vaccination certificate (clinical records) can print them. A practice not registered for VAT SHALL leave `vat_number` empty and `tax_rate_percent` at 0.

#### Scenario: Clinic without VAT registration
- **WHEN** a practice has no VAT number
- **THEN** `vat_number` is null and `tax_rate_percent` is 0, and printed documents show no VAT line

### Requirement: Practice is a platform table
The `practices` table SHALL have no `practice_id` column. A practice SHALL read only its own row; the super admin SHALL read all rows.

#### Scenario: Practice reads itself
- **WHEN** a caller identified as practice A requests `GET /api/v1/practice`
- **THEN** the response is practice A's row

#### Scenario: Practice cannot read another practice
- **WHEN** a caller identified as practice A requests practice B by any route
- **THEN** the response is 404 and no data of B is returned

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
