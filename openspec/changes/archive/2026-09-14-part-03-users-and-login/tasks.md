## 1. Migration and seed
- [x] 1.1 `V2__users.sql` with enum, unique `(practice_id, email)`, both role/flag checks
- [x] 1.2 `R__dev_users.sql`: one user per role, admin with vet flag and license

## 2. Backend
- [x] 2.1 `User` entity, `UserRepository`
- [x] 2.2 `AuthService`: verify bcrypt, issue JWT with claims `sub, practiceId, role, vet, sv, exp`
- [x] 2.3 `JwtFilter`: verify signature, load user, reject inactive or version mismatch
- [x] 2.4 `SecurityConfig`: stateless chain, headers, `must_change_password` gate
- [x] 2.5 `CurrentUser` helper
- [x] 2.6 `UserService` admin-only endpoints; one method guarded by the veterinarian flag
- [x] 2.7 Login rate limit filter (Bucket4j, per email and per IP)
- [x] 2.8 `PUT /api/v1/practice` identity columns; mapper ignores country, currency, status
- [x] 2.9 Delete the `X-Practice-Id` header from part 02
- [x] 2.10 App refuses to start outside `local` without the JWT secret

## 3. Test
- [x] 3.1 Front desk login, `/me` 200, admin endpoint 403
- [x] 3.2 Wrong password 401
- [x] 3.3 Admin resets front desk password: old token 401, new login 403 on `/users` until changed
- [x] 3.4 Eleventh failed login 429 with `Retry-After`
- [x] 3.5 Admin edits practice address 200; front desk 403
- [x] 3.6 Deactivated user's token 401

## 4. Angular
- [x] 4.1 `/login` page, `AuthService`, interceptor, guard
- [x] 4.2 Header with user name and logout; refresh keeps login
- [x] 4.3 `/change-password` forced when `mustChangePassword`
- [x] 4.4 `/settings/users` list and form (admin only)
- [x] 4.5 Checked in `ar` and `en` (covered by part 06b tasks 3.2 and 4.1; `ar` did not exist before part 05)

## 5. Close
- [x] 5.1 `erd.md` updated with `users`
- [x] 5.2 `progress.md` entry
- [x] 5.3 Commit
