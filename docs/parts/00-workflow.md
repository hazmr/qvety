# Workflow — how every part is done

## The loop

Each part follows the same seven steps. Do not reorder.

1. **Read the part file.** Business section first. Write the business rules in your own words in `progress.md` before touching code. If you cannot, you do not understand it yet; ask Claude to explain the business, not the code.
2. **Design with Claude.** Ask for the table design and the API shape. Read the answer. Push back on anything you do not understand. Do not accept a design you cannot explain.
3. **Migration first.** Write `V<n>__<name>.sql` by hand, with Claude reviewing. You should be able to read every line.
4. **Backend slice.** Entity → repository → DTO records → mapper → service → controller. Let Claude scaffold; you write the service method body yourself at least once, then let Claude review it.
5. **Test.** One integration test over Testcontainers per part. It must include the tenant boundary (from part 04 onward). Run it. It must pass.
   Every new tenant table goes in three lists in the same migration: RLS, audit trigger, export (the export list exists from part 11; part 11 backfills every table before it). The `TenantIsolationIT` table-list assertion fails if one is missing.
6. **Angular slice.** Regenerate the API client. Build the page. Check it in Arabic and English, RTL and LTR.
7. **Close the part.** Commit. Fill the "self-check" section of the part file honestly. Write three lines in `progress.md`: what the business rule is, what the stack concept is, what surprised you.

## Working with Claude

Claude is fast and confident. Neither is the same as correct. Use it like this:

**Good uses**
- "Explain what a controlled-substance log is and why a clinic needs one."
- "Design the `clients` table for Egyptian names. Show the SQL and explain each column."
- "Scaffold the entity, repository, DTO records, and MapStruct mapper for this table."
- "Review this service method for tenant leaks and missing validation."
- "Write a Testcontainers test that proves practice A cannot read practice B's clients."
- "Why does `open-in-view=false` matter with RLS?"
- "This test fails with `<error>`. Explain the cause before suggesting a fix."

**Bad uses**
- "Build part 06." (You learn nothing and cannot maintain it.)
- "Port this file from the old codebase." (License violation.)
- "Make the test pass." (It will weaken the test.)
- Accepting a change you did not read.

**Prompt template for a new part**

```
We are on part <n>: <name>. Read CLAUDE.md and docs/parts/<n>.md in this repo.
Business rules for this part, in my words: <your summary>.
First: propose the migration SQL and the REST endpoints. Do not write Java yet.
Explain any column or endpoint whose purpose is not obvious.
```

**Prompt template for review**

```
Review only <files>. Check: tenant boundary, validation, error handling, anything
that violates CLAUDE.md. One finding per line with file:line. No praise.
```

**When Claude and this guide disagree**, the guide wins unless Claude gives a concrete reason and you understand it. Then update the guide.

## Definition of done for a part

- Migration applied, `ddl-auto=validate` passes on startup. `erd.md` updated.
- Integration test passes, including the tenant case.
- Endpoint documented in the generated OpenAPI (`/v3/api-docs`).
- Angular page works in `ar` and `en`, RTL and LTR.
- You can explain every file in the part without opening it.
- Committed.

## Seeds

Migrations never seed. Dev data lives in `src/main/resources/db/seed/R__*.sql` (Flyway repeatable), loaded only when `spring.flyway.locations` includes it, which `application-local.yml` and the test profile do and production does not. Every seed row is synthetic. Tests may rely on the seed for reads, but each test creates the rows it mutates.

Part 16 (go-live) has no migration and no Angular page; it follows its own checklist instead of the seven steps.

## Time

A part should take one to three days. If a part takes more than a week, the part is too big or you are building beyond it. Stop, write down what is left, and cut.

## Keeping the business knowledge

Every part has a "Business" section. Copy the rules you confirm into `docs/domain/<topic>.md` in the new repo, in plain language. That folder becomes the product's domain documentation and, later, the seed of the user guide. It is also what you show a clinic owner when they ask "how does it handle X".
