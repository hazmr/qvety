## Stack decisions

- Boot parent POM `spring-boot-starter-parent:4.1.1` supplies the dependency BOM and plugin defaults; only MapStruct, lombok-mapstruct-binding, springdoc, frontend-maven-plugin, node and npm versions are pinned by hand.
- One jar serves the Angular build from `static/`: one artifact, one port, no CORS, no second web server. Cost: Angular rebuilds on every `./mvnw package`.
- Profiles: `local` loads `db/migration` and `db/seed`; `test` the same; `prod` only `db/migration`. Secrets from the environment; the app refuses to start outside `local` without them.
- Flyway runs before Hibernate; `ddl-auto=validate` fails startup when entities and tables disagree.
- Claude reads `CLAUDE.md` at the repo root every session; `openspec/` holds the specs and changes.
