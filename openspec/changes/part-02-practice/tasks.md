## 1. Migration
- [ ] 1.1 Write `V1__practices.sql` by hand (enum, table, checks); review naming and types
- [ ] 1.2 Start the app; `ddl-auto=validate` passes against the empty entity set

## 2. Backend
- [ ] 2.1 `Practice` entity with Postgres-generated `uuidv7()` id
- [ ] 2.2 `PracticeRepository`
- [ ] 2.3 `PracticeDto` record and `PracticeMapper`
- [ ] 2.4 `PracticeService.get` and `PracticeController` with `X-Practice-Id` header
- [ ] 2.5 Unknown id returns 404

## 3. Seed
- [ ] 3.1 `db/seed/R__dev_practice.sql` with fixed uuid, synthetic name

## 4. Test
- [ ] 4.1 Integration test: Testcontainers `postgres:18`, Flyway migrations + seed, `GET /api/v1/practice` returns 200 and the seeded name
- [ ] 4.2 Unknown id returns 404

## 5. Angular
- [ ] 5.1 `npm run api:generate` produces `PracticeService`
- [ ] 5.2 Page shows the practice name, in `ar` and `en`

## 6. Close
- [ ] 6.1 `docs/parts/erd.md` updated with `practices`
- [ ] 6.2 `/v3/api-docs` lists the endpoint
- [ ] 6.3 `docs/progress.md` entry: business rule, stack concept, surprise
- [ ] 6.4 Commit
