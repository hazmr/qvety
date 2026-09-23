## Purpose

Defines the super admin side: who Qvety staff are, how practices are created and moved through their status lifecycle, what each status allows, and the export that makes the clinic own its data.

## ADDED Requirements

### Requirement: Super admin is a platform user, not a practice user
Platform users SHALL live in their own table with no `practice_id`. They SHALL log in at a platform endpoint and carry a platform authority. They SHALL never see clinical data through the normal tenant path.

#### Scenario: Super admin calls a tenant endpoint
- **WHEN** a platform user calls `GET /api/v1/clients` without a practice
- **THEN** the response is 403

#### Scenario: Practice user calls a platform endpoint
- **WHEN** a practice admin calls `GET /api/platform/practices`
- **THEN** the response is 403

### Requirement: The application role cannot change a practice directly
Creating a practice, listing every practice, and changing a status SHALL go through definer functions the
database owner holds. The role the application connects as SHALL NOT hold `INSERT` on `practices` or
`UPDATE` on its `status`, so no code path outside those functions can change a practice's lifecycle.

#### Scenario: Feature code tries to suspend a practice
- **WHEN** code running as the application role updates `practices.status` directly
- **THEN** the database refuses it with a permission error

### Requirement: Practice status lifecycle
Status SHALL move: `trial → active` (first payment), `trial → past_due` (trial ended, no payment), `active → past_due` (due date passed), `past_due → suspended` (grace elapsed), `suspended → active` (payment), any → `closed` (manual only). Manual changes SHALL require a reason and write a platform audit row.

#### Scenario: Manual suspend
- **WHEN** the super admin posts `suspended` with a reason for a practice
- **THEN** the status changes and one platform audit row records who, when, and why

### Requirement: Status is enforced on every request
- `past_due`: the practice UI SHALL show a banner; everything works.
- `suspended`: write requests SHALL return 423 with a localized message; reads SHALL pass.
- `closed`: every request SHALL return 403 except the practice's own export; staff SHALL NOT log in.
The enforcement SHALL read a cached status refreshed on change and at most 60 seconds stale.

#### Scenario: Suspended write
- **WHEN** a user of a suspended practice posts a new client
- **THEN** the response is 423 and the client list still loads

#### Scenario: Closed practice
- **WHEN** a user of a closed practice calls any endpoint other than export
- **THEN** the response is 403

### Requirement: Data retention after close
A closed practice's data SHALL be kept for `closed_retention_days` (platform setting, default 90) after `closed_at`, then deleted. The number SHALL be in the contract.

#### Scenario: Retention setting
- **WHEN** `platform_settings.closed_retention_days` is read
- **THEN** it is 90 unless changed by the super admin

### Requirement: Practice creation is one transaction
Creating a practice SHALL, atomically: insert the row with `trial_ends_at = now + trial_days`, apply the country starter catalog, create the first admin user with a temporary password and `must_change_password`, and write a platform audit row. The login SHALL be handed over by the super admin in person or by WhatsApp; no email.

#### Scenario: Create Egyptian practice
- **WHEN** the super admin creates a practice with country `EG`, admin phone (email optional), and admin name
- **THEN** the practice exists on trial, the Egyptian catalog rows exist, one admin user exists with `must_change_password`, and one platform audit row exists

#### Scenario: Catalog missing
- **WHEN** creation is attempted for a country without a catalog
- **THEN** nothing is written

### Requirement: Any practice can export all its data
The practice admin SHALL request `GET /api/v1/practice/export` in any status. The super admin SHALL produce the same export for a closed practice. The export SHALL be one JSON document `{exported_at, practice, tables: {<table>: [...]}, attachments: [{object_key, filename, sha256}]}` containing every tenant table for that practice, produced under the practice's own tenant context, streamed.

#### Scenario: Export contains only own rows
- **WHEN** practice A exports
- **THEN** no row of practice B appears in any table

#### Scenario: Export list matches tenant list
- **WHEN** the set of table names in the export is compared with the RLS table list
- **THEN** they are equal

### Requirement: Super admin access to clinical data is consented and audited
When a clinic asks for help, the super admin SHALL use the export, with the owner's written consent, and the access SHALL be recorded in the platform audit log.

#### Scenario: Export by super admin
- **WHEN** the super admin exports a practice
- **THEN** a platform audit row records the action and the practice
