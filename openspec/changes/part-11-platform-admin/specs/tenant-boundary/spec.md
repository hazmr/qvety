## ADDED Requirements

### Requirement: System context is explicit and confined to platform code
Code SHALL run without a tenant only inside an explicit `SystemContext.run(...)` block. The mechanism SHALL be reachable only from the `platform` package. Feature packages SHALL NOT import `platform`.

#### Scenario: Tenant code enters system context
- **WHEN** a class outside `platform` tries to use `SystemContext`
- **THEN** it does not compile

#### Scenario: Super admin lists practices
- **WHEN** the super admin calls `GET /api/platform/practices`
- **THEN** all practices are returned, from inside system context

### Requirement: Export runs under tenant context
The practice export SHALL run under the practice's own tenant context, never under system context, so it passes through the same row-level security as every other read.

#### Scenario: Export of A excludes B
- **WHEN** practice A's export runs
- **THEN** every table contains only rows with A's `practice_id`

### Requirement: Three lists stay equal
The RLS table list, the audit trigger list, and the export table list SHALL be equal; the isolation test SHALL fail if any table is in one and not the others.

#### Scenario: Table missing from export
- **WHEN** a migration adds a tenant table to RLS and audit but not to the export list
- **THEN** `TenantIsolationIT` fails
