# tenant-boundary Specification

## Purpose

Guarantees that no query, through any code path, returns rows of another practice. Enforced by Postgres row-level security and proven by an integration test on every commit.

## Requirements

### Requirement: Every tenant table is isolated by row-level security
Every table that holds clinic data SHALL have a `practice_id` column, row-level security enabled and forced, and a policy restricting rows to `practice_id = current_setting('app.practice_id', true)::uuid`. Platform tables (practices, plans, subscriptions, platform users, platform audit) are the only exception.

#### Scenario: Practice A lists users
- **WHEN** a user of practice A calls `GET /api/v1/users`
- **THEN** no row of practice B is returned

#### Scenario: Direct repository read across tenants
- **WHEN** code running under practice A's context calls `findById` with the id of a practice B row
- **THEN** the result is empty

#### Scenario: Lazy load and native query
- **WHEN** an entity of practice A is lazily loaded or a native query runs under practice A's context
- **THEN** rows of practice B are never returned

### Requirement: The application role cannot bypass isolation
The application SHALL connect as a database role without `BYPASSRLS` and without ownership of tenant tables. Migrations SHALL run as a separate owner role. Row-level security SHALL be forced so table owners are also subject to it.

#### Scenario: No tenant set
- **WHEN** `qvety_app` queries a tenant table with `app.practice_id` unset
- **THEN** zero rows are returned

### Requirement: Every transaction carries a tenant
At the start of every transaction the application SHALL set `app.practice_id` and `app.user_id` with `SET LOCAL` from the current user. If there is no current user and the code is not in an explicit system context, the transaction SHALL fail before any query runs.

#### Scenario: Query without tenant context
- **WHEN** service code runs a transaction with no authenticated user and no system context
- **THEN** an exception is thrown and no query executes

### Requirement: Cross-tenant access looks like not-found
When a caller of practice A requests a row of practice B by id, the response SHALL be 404, never 403, so the existence of B's row is not revealed.

#### Scenario: Client of another practice
- **WHEN** practice A requests `GET /api/v1/clients/{id}` with an id belonging to practice B
- **THEN** the response is 404

### Requirement: The boundary is proven on every commit
An integration test SHALL seed two practices and assert, for every tenant table, that reads under practice A never return practice B rows. The test SHALL assert that the set of tables with an isolation policy equals the set of tables with the audit trigger and the export list. A missing table SHALL fail the build.

#### Scenario: New tenant table without policy
- **WHEN** a migration adds a tenant table but omits it from the RLS list
- **THEN** `TenantIsolationIT` fails
