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

## Part 16 — Go-live
- Offer and price:
- Pilot clinic and start date:
- 60-day result:
