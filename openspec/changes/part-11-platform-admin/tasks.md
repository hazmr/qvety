## 1. Migration and seed
- [x] 1.1 `V11__platform.sql`: three platform tables (audit log append-only), `closed_at`, settings rows, grants, three definer functions for `practices`
- [x] 1.2 `R__dev_platform_user.sql`

## 2. Backend
- [x] 2.1 ArchUnit rule confining `SystemContext` to `platform` and `AuthService`
- [x] 2.2 Platform login, `PLATFORM` authority, `@PreAuthorize("hasAuthority('PLATFORM')")`
- [x] 2.3 Status enforcement filter with cache (60 s)
- [x] 2.4 Practice creation transaction with catalog, admin user, audit row
- [x] 2.5 Manual status change with reason and audit row
- [x] 2.6 `ExportService` streamed under tenant context; platform and practice endpoints

## 3. Tests
- [x] 3.1 `TenantIsolationIT`: super admin lists practices; super admin on tenant endpoint 403; tenant code cannot use `SystemContext`
- [x] 3.2 Suspended → write 423, read 200; closed → 403 except export
- [x] 3.3 Export of A contains no B row; export table set equals the RLS set, both derived from the catalog
- [x] 3.4 Practice creation: trial, catalog rows, admin with `must_change_password`, audit row; unmapped country writes nothing
- [x] 3.5 The application role cannot insert a practice or update a status outside the definer functions

## 4. Backup
- [x] 4.1 `backup` sidecar in compose
- [x] 4.2 Run once by hand, restore into `qvety_drill`, start the app, log in
- [x] 4.3 Log the drill with date and person

## 5. Angular
- [ ] 5.1 `/admin` layout and routes guarded by the platform claim
- [ ] 5.2 Practices list, create, status change with reason, export download
- [ ] 5.3 Practice-side `/settings/export`; `past_due` banner
- [ ] 5.4 Checked in `ar` and `en`
- [ ] 5.5 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 6. Close
- [ ] 6.1 `erd.md` updated
- [ ] 6.2 `progress.md` entry
- [ ] 6.3 Commit
