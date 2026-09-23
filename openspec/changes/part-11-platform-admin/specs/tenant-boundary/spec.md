## ADDED Requirements

### Requirement: System context is explicit and confined
Code SHALL run without a tenant only inside an explicit `SystemContext.run(...)` block. Only the `platform` package and `auth.AuthService` SHALL use it: login runs before any practice is known, so it reads the definer functions with no tenant set. An automated rule SHALL fail the build for any other caller. Feature packages SHALL NOT import `platform`.

#### Scenario: Tenant code enters system context
- **WHEN** a class other than `AuthService` and outside `platform` uses `SystemContext`
- **THEN** the build fails

#### Scenario: Login reads across practices
- **WHEN** a user logs in before any practice is known
- **THEN** `AuthService` reads the definer functions inside system context and the build allows it

#### Scenario: Super admin lists practices
- **WHEN** the super admin calls `GET /api/platform/practices`
- **THEN** all practices are returned, from inside system context

### Requirement: Export runs under tenant context
The practice export SHALL run under the practice's own tenant context, never under system context, so it passes through the same row-level security as every other read.

#### Scenario: Export of A excludes B
- **WHEN** practice A's export runs
- **THEN** every table contains only rows with A's `practice_id`

### Requirement: Three lists stay equal
The RLS table list, the audit trigger list, and the export table list SHALL be equal. The export list SHALL be derived from the database catalog — every table carrying a `practice_id` column — rather than maintained by hand, so a new tenant table joins the export by existing. The isolation test SHALL fail if the three sets differ.

#### Scenario: New tenant table
- **WHEN** a migration adds a tenant table with `practice_id`, its RLS policy and its audit trigger
- **THEN** the table appears in the export with no further change

#### Scenario: Table missing from a list
- **WHEN** a tenant table is in one of the three sets and not the others
- **THEN** `TenantIsolationIT` fails
