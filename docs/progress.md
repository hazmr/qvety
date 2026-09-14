# Progress log

One entry per finished part. Three lines minimum: the business rule, the stack concept, what surprised you.

## Part 01 — Setup
- Business: a clinic day is book → check in → examine → note and vaccine → prescription → invoice and payment → recall. Half the day is walk-ins; most prescriptions go to a pharmacy; discounts are negotiated at the desk. Everything the software holds must trace to `docs/domain/clinic-day.md`.
- Stack: Spring Boot 4.1 parent POM supplies the BOM and plugin defaults; only MapStruct, springdoc, and the frontend plugin are pinned. Flyway runs before Hibernate and `ddl-auto=validate` makes the migration the schema of record. `frontend-maven-plugin` builds Angular into `static/` so one jar serves everything on one port.
- Surprise: the Angular CLI blocks a non-interactive build with an analytics prompt; had to disable it in `angular.json` (commit `f57d88b`). Also Testcontainers needs Docker in CI, so the runner choice matters.

### Self-check
- Boot parent POM gives: managed versions for every Spring, Hibernate, Flyway, Testcontainers, Lombok, and driver dependency (the BOM); default plugin configuration (compiler from `java.version`, surefire/failsafe, `spring-boot-maven-plugin` repackage); resource filtering. We pin only what the BOM does not manage.
- Angular is served from the jar so there is one artifact, one port, one TLS endpoint, no CORS, and no second web server to run on the VPS. Cost: every `./mvnw package` rebuilds Angular.
- Claude reads `CLAUDE.md` at the repository root at the start of every session, plus `openspec/config.yaml` context when an OpenSpec command runs.


## Part 02 — Practice
- Business: a practice is one clinic business and the tenant root; country decides currency, locale, timezone, and regulatory framework, and an unmapped country is an error, never a default. Status starts `trial` and only the platform changes it. Identity columns (address, phone, VAT number, tax rate) exist for printing invoices and certificates.
- Stack: Flyway migration first, entity second, `ddl-auto=validate` as the referee. Postgres generates the id (`uuidv7()`, time-ordered) so the entity has `@Id` without `@GeneratedValue`. DTO record plus MapStruct keeps the API shape independent of the table. `@SpringBootTest` with Testcontainers starts the whole context against a real Postgres 18, runs Flyway and the seed, then hits the HTTP port.
- Surprise: `validate` rejected `char(2)` on the first run. Postgres reports it as `bpchar` (JDBC `CHAR`) while a Java `String` defaults to `VARCHAR`; the fix is `@JdbcTypeCode(Types.CHAR)` on the entity, not a migration change. Also Hibernate 7 moved `PostgreSQLEnumJdbcType` to `org.hibernate.dialect.type`. Generated Angular names needed `@Tag(name = "practice")` and `@Schema(name = "Practice")` to avoid `PracticeControllerApi` and `PracticeDtoDto`.

## Part 03 — Users, roles, and login
- Business: one role per user decides what they manage; a separate `is_veterinarian` flag decides clinical acts, because the owner of a one-doctor clinic is admin and vet at once. Staff log in with their phone (any Egyptian shape) or their email; phone is required, email optional, both unique per clinic. Onboarding and reset have no email: the admin hands over a temporary password and the user must change it first. Revocation is immediate through `session_version`. Login is rate limited, never locked.
- Stack: stateless HS256 JWT via Spring Security's Nimbus encoder/decoder; `JwtFilter` loads the user row on every request and rejects inactive or version-mismatched tokens; `@EnableMethodSecurity` with `hasRole` on services and `@access.isVeterinarian()` for the flag; Bucket4j in memory for the login buckets; `FilterRegistrationBean.setEnabled(false)` so security-chain filters are not double-registered by Boot.
- Surprise: `@Transactional` on a `OncePerRequestFilter` breaks Tomcat startup (`logger` is null on the CGLIB proxy). An `@Id` without a generator makes Hibernate refuse a null id on insert; `@GeneratedValue(IDENTITY)` on Postgres uses `INSERT … RETURNING`, so the `uuidv7()` default still wins. Springdoc marks record components optional unless annotated, so the generated TypeScript had `token?:`; `@Schema(requiredMode = REQUIRED)` fixed the response DTOs, and `stringEnums: false` gives literal unions instead of TS enums.

## Part 04 — Tenant boundary and audit log
- Business: two clinics on one database never see each other's rows, and every change is attributable. Both are promises enforced by Postgres, not by service code: RLS on every table with `practice_id`, forced so even the owner is subject to it; an audit trigger on every tenant table and on the practice row, stripping `password_hash`; the application role cannot change `practices.status`, edit or delete audit rows, or call the trigger functions.
- Stack: two database roles (`qvety_owner` for Flyway, `qvety_app` for the app, `NOBYPASSRLS`). A `TenantTransactionManager` (subclass of `JpaTransactionManager`) writes `app.practice_id` and `app.user_id` with `set_config(..., true)` right after every transaction opens, so services, repositories, and the JWT filter's lookup all carry the tenant; no tenant and no `SystemContext` throws before any statement. `tenant_table_setup(regclass)` is the one call each later migration makes per table. `TenantIsolationIT` derives the expected table set from the catalog, so a forgotten table fails CI.
- Surprise: login cannot work under RLS without an exception, since no tenant is known yet; `login_lookup(email)` as a `SECURITY DEFINER` function is that single, explicit exception. The column-level grant caught Hibernate's full-row `UPDATE` on `practices` (it rewrites `status` too); `@DynamicUpdate` plus `updatable = false` fixed it, and the grant did exactly what it was for. `SystemContext` had to live in `tenant`, not `platform`, because `auth` needs it; part 11 fences callers with ArchUnit instead. Testcontainers `@ServiceConnection` cannot express two users, so the test config provides `JdbcConnectionDetails` (app) and `FlywayConnectionDetails` (owner) beans.

## Part 16 — Go-live
- Offer and price:
- Pilot clinic and start date:
- 60-day result:
