# Part 12 — Subscriptions: selling to practices

## Business

You assign a plan to a practice, record payments that arrive by InstaPay or Vodafone Cash, and let the status flow from part 11 run on its own. Write `docs/domain/subscriptions.md`:
- Plans: name, price EGP, period (monthly/yearly), limits (`max_users`, `storage_gb`). Versioned; price changes create a new version, existing subscriptions keep theirs until renewal.
- One active subscription per practice: plan version, start, current period end, next due date.
- A subscription invoice is generated per period (what the practice owes). A subscription payment is money received against it: amount, method (`instapay`, `vodafone_cash`, `bank_transfer`, `cash`), external reference, received date, proof file. Recorded manually by the super admin. Idempotency key per record.
- Status flow (from part 11): `trial` → `active` on first payment; `active` → `past_due` when the due date passes (job, visible and reversible); `past_due` → `suspended` after grace days (platform setting); `suspended` → `active` on payment.
- Practice sees a read-only Subscription page: plan, period end, amount due, pay instructions (InstaPay handle, Vodafone Cash number), and "send the transfer number to ...".
- **Launch pricing, decided.** One plan, `Clinic`: 990 EGP per month or 9,900 EGP per year (two months free), up to 5 users, 5 GB storage. Reasoning: a Cairo consultation is 200–400 EGP; a clinic seeing ten patients a day turns over roughly 60,000 EGP a month; software at 990 is under 2 % of that and less than one consultation a week. One plan because a second plan is a decision the owner has to make before trusting you; add tiers when the third clinic asks for something the first plan lacks. Platform defaults: `trial_days = 30`, `grace_days = 14`, `closed_retention_days = 90`. Change the numbers in `platform_settings`, not in code, and change the price by adding a plan version.

## Stack you learn

- A scheduled job (`@Scheduled`) that runs under system context and is idempotent.
- File upload to MinIO (proof of payment): pre-signed PUT, object key stored, never a public URL. This is the storage pattern part 13 reuses for attachments.
- Multipart handling in Spring and NG-ZORRO upload.
- The first "money" table set. Same `numeric(12,2)` rule as part 14.

## Design

`V12__subscriptions.sql`: `plans`, `plan_versions`, `subscriptions` (unique `practice_id`), `subscription_invoices`, `subscription_payments` (unique `idempotency_key`, `proof_object_key`), `platform_settings` rows `grace_days` and `trial_days`. None in the RLS list.

Endpoints under `/api/platform/...`: plans CRUD, `POST /practices/{id}/subscription`, `GET /subscriptions?status`, `POST /subscription-invoices/{id}/payments` (multipart: payment fields + proof file, header `Idempotency-Key`). Practice-side: `GET /api/v1/subscription`.

Job `SubscriptionStatusJob`, daily: for each `trial` practice whose `trial_ends_at` is past and has no payment → `past_due`, audit row. For each subscription whose `next_due_at` is past and practice is `active` → `past_due`, audit row. For each `past_due` whose due date is more than `grace_days` ago → `suspended`, audit row. `--dry-run` flag logs what it would do. Never touches `closed`.

Payment recording, in one transaction: insert payment (idempotent on key), mark invoice `paid` if covered, extend `current_period_end` and `next_due_at`, set practice `active` if it was `trial`/`past_due`/`suspended`, audit row.

## Steps

1. Migration. Migration inserts the platform defaults (`trial_days = 30`, `grace_days = 14`, `closed_retention_days = 90`) and the `Clinic` plan with version 1 (990 EGP monthly, 9,900 EGP yearly as two versions of the same plan differing in `period`, `max_users = 5`, `storage_gb = 5`). Plans are platform data, not demo data, so they belong in the migration.
2. Plans and subscription services. Assign the seeded practice a subscription.
3. Payment recording with idempotency and MinIO proof upload.
4. Job with dry-run. Tests: trial ended with no payment → `past_due`; due date passed → `past_due`; grace elapsed → `suspended`; payment on a suspended practice → `active` and writes work again; same idempotency key twice → one payment; job run twice → no duplicate audit rows.
5. Angular: `/admin/plans`, `/admin/subscriptions`, record payment dialog with file; practice-side `/subscription` page with pay instructions.

## Ask Claude

- "Make this `@Scheduled` job idempotent and safe to run twice."
- "Show a pre-signed upload to MinIO from Spring and the matching Angular upload, storing only the object key."
- "Review the payment-recording transaction for the case where the job and a payment run at the same moment."

## Done when

- Assign a plan, let the job move a practice to `past_due` and `suspended`, record an InstaPay payment with a screenshot, watch it return to `active`.
- Practice-side subscription page shows the right amount and instructions in Arabic.

## Self-check

- Why are subscription payments and clinic payments different tables?
- What does the job do on the day the grace period ends if it already ran once that day?
- Where does the proof screenshot live, and who can fetch it?
