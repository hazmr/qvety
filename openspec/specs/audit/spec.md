# audit Specification

## Purpose

Records who changed which row, when, and from what to what, for every tenant table and for the practice row, written by the database so no code path can skip it.

## Requirements

### Requirement: Every change to tenant data is logged by a trigger
The system SHALL write one `audit_log` row for every insert, update, and delete on every tenant table and on `practices`, containing practice id, user id (nullable), table name, row id, action, before and after as JSON, and the time. The write SHALL be performed by a database trigger, not by service code.

#### Scenario: Update through the API
- **WHEN** an admin updates a user's `full_name`
- **THEN** one audit row exists with that admin's user id, `before.full_name` old value, `after.full_name` new value

#### Scenario: Practice row change
- **WHEN** the practice address is updated
- **THEN** one audit row exists with `table_name = 'practices'` and the practice id taken from the practice row itself

### Requirement: Secrets are never recorded
The trigger SHALL strip a fixed list of sensitive columns (at least `password_hash`) from before and after images. Tokens are never stored and so never appear.

#### Scenario: Password change
- **WHEN** a user's password hash changes
- **THEN** the audit row for that update contains no `password_hash` key in before or after

### Requirement: Audit rows are append-only and tenant-scoped
The application role SHALL be able to insert and select audit rows only. Update and delete SHALL be refused by the database. Audit rows SHALL be under row-level security like any tenant table.

#### Scenario: Application tries to delete audit
- **WHEN** `qvety_app` runs `DELETE FROM audit_log`
- **THEN** the database refuses with permission denied

#### Scenario: Other practice reads audit
- **WHEN** practice B requests audit rows of practice A
- **THEN** none are returned

### Requirement: Audit trigger cannot be called or bypassed by the application
The audit function SHALL be `SECURITY DEFINER`, owned by the migration role, with `EXECUTE` revoked from the application role.

#### Scenario: Application invokes the function
- **WHEN** `qvety_app` runs `SELECT audit_row()`
- **THEN** the database refuses with permission denied

### Requirement: Admins read the audit log
`GET /api/v1/audit?table&rowId&page` SHALL return audit rows for the caller's practice to `admin` users only.

#### Scenario: Front desk reads audit
- **WHEN** a `front_desk` user calls `GET /api/v1/audit`
- **THEN** the response is 403

### Requirement: The practice cannot change its own status
Column-level grants SHALL let the application role update only identity columns of `practices` (`name`, `address`, `phone`, `vat_number`, `tax_rate_percent`, later `logo_object_key`). Status, country, and currency SHALL be unwritable by the application role.

#### Scenario: Buggy service sets status
- **WHEN** `qvety_app` runs `UPDATE practices SET status = 'active'`
- **THEN** the database refuses with permission denied
