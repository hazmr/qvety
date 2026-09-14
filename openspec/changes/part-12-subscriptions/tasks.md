## 1. Migration
- [ ] 1.1 `V11__subscriptions.sql`: five tables, enums, `grace_days`, `Clinic` plan with two versions
- [ ] 1.2 Dev seed assigns the dev practice a subscription

## 2. Backend
- [ ] 2.1 Plan and plan version services and endpoints
- [ ] 2.2 Subscription assignment; invoice generation per period
- [ ] 2.3 Payment recording transaction with idempotency and status reactivation
- [ ] 2.4 MinIO pre-signed PUT/GET for proofs; key stored only
- [ ] 2.5 `SubscriptionStatusJob` with dry-run under system context
- [ ] 2.6 `GET /api/v1/subscription` with pay instructions

## 3. Tests
- [ ] 3.1 Trial ended, no payment → `past_due`
- [ ] 3.2 Due date passed → `past_due`; grace elapsed → `suspended`
- [ ] 3.3 Payment on suspended → `active`, writes work
- [ ] 3.4 Same idempotency key twice → one payment, same response
- [ ] 3.5 Job run twice → no duplicate audit rows
- [ ] 3.6 Job and payment at the same moment: final status is `active`

## 4. Angular
- [ ] 4.1 `/admin/plans`, `/admin/subscriptions`, payment dialog with file
- [ ] 4.2 Practice `/subscription` page in `ar` and `en`
- [ ] 4.3 Checked at 390 px and 360 px: phone layout per MOBILE.md layout skeleton; no `nz-table` on phone, 16 px inputs, 44 px targets

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
