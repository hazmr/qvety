## MODIFIED Requirements

### Requirement: Login accepts phone or email
`POST /api/v1/auth/login` SHALL take one `identifier` field: an email address (contains `@`) or a phone in any Egyptian shape (`01...`, `+20...`, `0020...`, with or without spaces), normalized to E.164 before lookup. A phone that does not parse SHALL be treated as unknown credentials (401), never as a validation error.

The same identifier MAY exist at more than one practice. Login SHALL verify the password against every active user row that matches the identifier, across practices. Exactly one row matching the password SHALL yield a token for that row's practice. More than one row matching SHALL yield a 200 response with `practices` (id and name of each matching practice) and no token; the caller SHALL then repeat the request with the same `identifier` and `password` plus `practiceId` and receive the token for that practice only. No row matching SHALL yield 401. A practice name SHALL never be returned before the password has been verified.

#### Scenario: Phone in local shape
- **WHEN** a user whose phone is `+201000000104` logs in with `01000000104`
- **THEN** a token is issued

#### Scenario: Email
- **WHEN** the same user logs in with their email
- **THEN** a token is issued

#### Scenario: Missing trunk zero
- **WHEN** login is attempted with `1000000104`
- **THEN** the response is 401

#### Scenario: Same phone at two practices, different passwords
- **WHEN** practice A and practice C each have an active user with phone `+201000000104` and different passwords, and login is attempted with that phone and practice C's password
- **THEN** a token for practice C is issued and no data of practice A is returned

#### Scenario: Same phone at two practices, same password
- **WHEN** both users share the same password and login is attempted without `practiceId`
- **THEN** the response is 200 with `practices` listing A and C by id and name, and no token

#### Scenario: Practice chosen
- **WHEN** the same request is repeated with `practiceId` of practice C
- **THEN** a token for practice C is issued

#### Scenario: Practice chosen that did not match
- **WHEN** the request is repeated with a `practiceId` where the identifier and password do not match an active user
- **THEN** the response is 401 and no practice list is returned

#### Scenario: Wrong password at two practices
- **WHEN** the identifier exists at two practices and the password matches neither
- **THEN** the response is 401 with no practice list

#### Scenario: Deactivated at one practice
- **WHEN** the user is deactivated at practice A and active at practice C, and logs in with practice A's old password
- **THEN** the response is 401; with practice C's password a token for practice C is issued

### Requirement: Practice id comes from the token
After login, the practice of every request SHALL be taken from the token. No request header SHALL select the practice. The optional `practiceId` in the login body SHALL only choose among practices where the identifier and password already matched; it SHALL never widen access.

#### Scenario: Header ignored
- **WHEN** a request carries `X-Practice-Id` for practice B and a token for practice A
- **THEN** the request is served for practice A

#### Scenario: practiceId in the login body for a practice without a match
- **WHEN** login is sent with a correct password for practice A and `practiceId` of practice B, where the identifier does not exist
- **THEN** the response is 401

### Requirement: Login is rate limited
The system SHALL allow at most 10 failed logins per identifier (normalized phone or email) and 30 per IP in any 15 minute window. Beyond that it SHALL return 429 with a `Retry-After` header. A successful password verification SHALL reset the identifier bucket, whether or not a practice still has to be chosen. Nothing SHALL be locked permanently.

#### Scenario: Eleventh failure
- **WHEN** an identifier has 10 failed logins in 15 minutes and an eleventh attempt arrives
- **THEN** the response is 429 with `Retry-After`

#### Scenario: Practice choice pending does not count as failure
- **WHEN** a login returns the practice list
- **THEN** the identifier's failure count is reset
