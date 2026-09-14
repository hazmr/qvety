## 1. Migration and seed
- [ ] 1.1 `V2__users.sql` with enum, unique `(practice_id, email)`, both role/flag checks
- [ ] 1.2 `R__dev_users.sql`: one user per role, admin with vet flag and license

## 2. Backend
- [ ] 2.1 `User` entity, `UserRepository`
- [ ] 2.2 `AuthService`: verify bcrypt, issue JWT with claims `sub, practiceId, role, vet, sv, exp`
- [ ] 2.3 `JwtFilter`: verify signature, load user, reject inactive or version mismatch
- [ ] 2.4 `SecurityConfig`: stateless chain, headers, `must_change_password` gate
- [ ] 2.5 `CurrentUser` helper
- [ ] 2.6 `UserService` admin-only endpoints; one method guarded by the veterinarian flag
- [ ] 2.7 Login rate limit filter (Bucket4j, per email and per IP)
- [ ] 2.8 `PUT /api/v1/practice` identity columns; mapper ignores country, currency, status
- [ ] 2.9 Delete the `X-Practice-Id` header from part 02
- [ ] 2.10 App refuses to start outside `local` without the JWT secret

## 3. Test
- [ ] 3.1 Front desk login, `/me` 200, admin endpoint 403
- [ ] 3.2 Wrong password 401
- [ ] 3.3 Admin resets front desk password: old token 401, new login 403 on `/users` until changed
- [ ] 3.4 Eleventh failed login 429 with `Retry-After`
- [ ] 3.5 Admin edits practice address 200; front desk 403
- [ ] 3.6 Deactivated user's token 401

## 4. Angular
- [ ] 4.1 `/login` page, `AuthService`, interceptor, guard
- [ ] 4.2 Header with user name and logout; refresh keeps login
- [ ] 4.3 `/change-password` forced when `mustChangePassword`
- [ ] 4.4 `/settings/users` list and form (admin only)
- [ ] 4.5 Checked in `ar` and `en`

## 5. Close
- [ ] 5.1 `erd.md` updated with `users`
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
