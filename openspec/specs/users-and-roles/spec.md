# users-and-roles Specification

## Purpose

Defines staff accounts inside a practice: one role each, a separate veterinarian flag for clinical acts, password rules, login sessions, and immediate revocation.

## Requirements

### Requirement: A user belongs to exactly one practice and has one role
The system SHALL store each user under one practice with exactly one role from `admin`, `veterinarian`, `technician`, `front_desk`. Each user SHALL have a phone (stored E.164) and MAY have an email. Phone and email SHALL each be unique per practice, not globally.

#### Scenario: Same phone at two clinics
- **WHEN** practice A and practice B each create a user with phone `01012345678`
- **THEN** both users exist, each visible only inside its own practice

#### Scenario: Duplicate phone inside one practice
- **WHEN** practice A creates a second user with a phone that already exists in practice A
- **THEN** the request is rejected with 409

#### Scenario: Invalid phone
- **WHEN** a user is created with a phone that does not parse as an Egyptian number
- **THEN** the request is rejected with a validation error on `phone`

### Requirement: Roles decide what a user may manage
- `admin` SHALL do everything in the practice, including managing users and settings.
- `veterinarian` SHALL do clinical work: examine, diagnose, prescribe, finalize notes.
- `technician` SHALL record vitals and vaccinations and assist; SHALL NOT finalize notes or prescribe.
- `front_desk` SHALL manage clients, patients, scheduling, invoices, payments; SHALL NOT write clinical data.
Role checks SHALL be enforced on the server; the Angular UI only hides what the server would refuse.

#### Scenario: Front desk calls an admin endpoint
- **WHEN** a `front_desk` user calls `GET /api/v1/users`
- **THEN** the response is 403

### Requirement: Clinical identity is a flag, not a role
The system SHALL keep `is_veterinarian` separate from `role`. Clinical acts (finalize a note, prescribe, be booked as the vet) SHALL check the flag, never the role. A `veterinarian` role SHALL always have the flag; an `admin` MAY; `technician` and `front_desk` SHALL NOT. The database SHALL enforce these combinations.

#### Scenario: One-doctor clinic owner
- **WHEN** the practice admin has `is_veterinarian = true`
- **THEN** that user may finalize notes and prescribe while still managing users

#### Scenario: Invalid combination
- **WHEN** a user with role `technician` is saved with `is_veterinarian = true`
- **THEN** the database rejects the row

### Requirement: Password rules
Passwords SHALL be hashed with bcrypt cost 12, never stored in clear, never logged. Minimum length SHALL be 10 characters with no other composition rule.

#### Scenario: Short password
- **WHEN** a user is created or changes to a password of 9 characters
- **THEN** the request is rejected with a validation error on the password field

### Requirement: Staff onboarding and reset have no email step
The admin SHALL create a user with a temporary password and the flag `must_change_password`. Password reset SHALL be an admin action that sets a new temporary password and the flag. While the flag is set, every endpoint except `/api/v1/me` and `/api/v1/me/password` SHALL return 403 with code `password_change_required`.

#### Scenario: First login after creation
- **WHEN** a newly created user logs in with the temporary password and calls `GET /api/v1/users`
- **THEN** the response is 403 with code `password_change_required`

#### Scenario: Password changed
- **WHEN** the user calls `POST /api/v1/me/password` with the temporary and a new password
- **THEN** the flag clears, `session_version` increments, a fresh token is returned, and other endpoints work

### Requirement: Login accepts phone or email
`POST /api/v1/auth/login` SHALL take one `identifier` field: an email address (contains `@`) or a phone in any Egyptian shape (`01...`, `+20...`, `0020...`, with or without spaces), normalized to E.164 before lookup. A phone that does not parse SHALL be treated as unknown credentials (401), never as a validation error.

#### Scenario: Phone in local shape
- **WHEN** a user whose phone is `+201000000104` logs in with `01000000104`
- **THEN** a token is issued

#### Scenario: Email
- **WHEN** the same user logs in with their email
- **THEN** a token is issued

#### Scenario: Missing trunk zero
- **WHEN** login is attempted with `1000000104`
- **THEN** the response is 401

### Requirement: Login issues a stateless token for one working day
`POST /api/v1/auth/login` SHALL verify the password and return a signed token carrying user id, practice id, role, veterinarian flag, session version, and an expiry 12 hours after issue. There SHALL be no refresh token. The signing secret SHALL come from the environment; outside the `local` profile the application SHALL refuse to start without it.

#### Scenario: Wrong password
- **WHEN** login is attempted with a wrong password
- **THEN** the response is 401 and no token is issued

#### Scenario: Token after expiry
- **WHEN** a request carries a token older than 12 hours
- **THEN** the response is 401

### Requirement: Revocation is immediate
Every request SHALL load the user row and reject the token if the user is inactive or the token's session version differs from the row. Deactivating a user or resetting a password SHALL bump `session_version`, invalidating every token that user holds.

#### Scenario: Deactivated after token issued
- **WHEN** an admin deactivates a user who holds a valid token
- **THEN** the user's next request returns 401

#### Scenario: Password reset by admin
- **WHEN** the admin resets a user's password
- **THEN** the user's existing token returns 401 and a new login is forced to change the password

### Requirement: Deactivated users stay in history
A deactivated user SHALL NOT log in but SHALL remain in the table; records that reference the user (notes, vaccinations) SHALL keep showing that user's name.

#### Scenario: Deactivated vet on an old note
- **WHEN** a veterinarian is deactivated
- **THEN** notes they finalized still show their name

### Requirement: Login is rate limited
The system SHALL allow at most 10 failed logins per identifier (normalized phone or email) and 30 per IP in any 15 minute window. Beyond that it SHALL return 429 with a `Retry-After` header. A successful login SHALL reset the identifier bucket. Nothing SHALL be locked permanently.

#### Scenario: Eleventh failure
- **WHEN** an identifier has 10 failed logins in 15 minutes and an eleventh attempt arrives
- **THEN** the response is 429 with `Retry-After`

### Requirement: Practice id comes from the token
After login, the practice of every request SHALL be taken from the token. No request header SHALL select the practice.

#### Scenario: Header ignored
- **WHEN** a request carries `X-Practice-Id` for practice B and a token for practice A
- **THEN** the request is served for practice A

### Requirement: Security headers for the SPA
Responses SHALL carry `Content-Security-Policy: default-src 'self'` (no `unsafe-inline`), `frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, and HSTS when served over TLS.

#### Scenario: Header check
- **WHEN** any page or API response is inspected
- **THEN** the headers above are present
