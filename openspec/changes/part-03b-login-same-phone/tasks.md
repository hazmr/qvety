## 1. Database

- [x] 1.1 `V7__login_practices.sql`: `login_practices(uuid[])` definer function returning `(id, name)`, `EXECUTE` to `qvety_app` only; verify `./mvnw verify` still passes `ddl-auto=validate` and `TenantIsolationIT`
- [x] 1.2 `docs/parts/erd.md`: add the `V7` row to the migration table and the function next to `login_lookup` in the "Login before a tenant is known" line; verify both mention `login_practices`

## 2. Backend

- [x] 2.1 `UserRepository.findLoginPractices(List<UUID>)` native query over `login_practices(:ids)` returning a `PracticeChoice` projection; verify it compiles and returns id and name for two seeded practices
- [x] 2.2 `LoginRequest` gains optional `practiceId`; `LoginResponse` gains nullable `practices` and `token`/`user` become nullable in the schema with Javadoc on which pair is present; verify `/v3/api-docs` shows the three optional fields
- [x] 2.3 `AuthService.login` per design: every active row tried in practice-id order, `practiceId` narrows, dummy hash on zero rows, `recordSuccess` on any match, practice list on several; verify with the tests in 3.1
- [x] 2.4 `messages*.properties`: no new server message (login errors stay `invalid_credentials`); verify no missing key warnings in the test log

## 3. Tests

- [x] 3.1 Fold `LoginSamePhoneProbeIT` into `AuthIT.samePhoneAtTwoPractices`: seed practice C and a front-desk user with `DESK_PHONE` via the owner connection; assert different passwords each log in to their own practice, same password returns `practices` with two entries and no token, `practiceId` returns a token whose `practiceId` claim matches, wrong `practiceId` is 401, wrong password is 401 with no `practices`, deactivated at C is 401 with C's password; delete the probe class; verify `./mvnw verify` green
- [x] 3.2 `AuthIT.eleventhFailedLoginIs429WithRetryAfter` still passes and a new assertion shows a practice-list response resets the identifier bucket; verify green

## 4. Angular

- [x] 4.1 `npm run api:generate`; `core/auth.service.ts` `accept()` guards on `r.token` and `login()` takes optional `practiceId`; verify `npm run build` passes
- [x] 4.2 `features/login`: on a `practices` response show the picker rows (44 px, `list-page` row style) under the inputs, lock the identifier, keep the password, post again with `practiceId`; keys `login.choosePractice` in `ar.json` and `en.json`; verify unit test for the two-step flow passes in `npm test`
- [x] 4.3 Playwright walkthrough of the picker at 1280, 390, 360 in `ar` and `en`: no overflow, rows at least 44 px, chevron mirrors; verify zero console errors

## 5. Close

- [x] 5.1 `docs/parts/03-users-and-login.md`: one paragraph on the two-practice login under "As built"; `docs/backlog.md`: identity/membership split and "remember last practice" rows; verify both files updated
- [x] 5.2 `docs/progress.md` entry for part 03b (business, stack, surprise); verify present
- [x] 5.3 `./mvnw verify` green, commit `part-03b: login when the same phone exists at two practices`
