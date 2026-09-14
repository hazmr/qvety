## Why

Qvety gets paid by practices through InstaPay, Vodafone Cash, or bank transfer, recorded by hand. The status flow from part 11 must run on its own from due dates and grace days, and the practice must see what it owes and how to pay.

## What Changes

- `plans`, `plan_versions`, `subscriptions`, `subscription_invoices`, `subscription_payments` platform tables; `platform_settings` `grace_days`.
- Launch pricing seeded by migration: one plan `Clinic`, 990 EGP monthly or 9,900 EGP yearly, 5 users, 5 GB.
- Manual payment recording with idempotency key and proof upload to MinIO (pre-signed PUT, object key stored).
- Daily `SubscriptionStatusJob` under system context, idempotent, with dry-run, driving `past_due` and `suspended`.
- Practice-side read-only subscription page with pay instructions.

## Capabilities

### New Capabilities
- `subscriptions`: plans, versions, one subscription per practice, invoices, payments, the automatic status job, launch pricing.

### Modified Capabilities
- `platform`: status transitions `trial → past_due`, `active → past_due`, `past_due → suspended`, `→ active` now happen automatically from the job and from payments.

## Non-goals

- Online payment gateway. Payments are recorded by the super admin from a transfer screenshot.
- Multiple plans or tiers. One plan until the third clinic asks for something it lacks.
- Automatic closing. `closed` stays manual.

## Impact

- `V11__subscriptions.sql`, `com.qvety.platform.subscriptions`, MinIO client and pre-signed upload pattern reused by part 13.
- `docker/docker-compose.yml` MinIO bucket for proofs.
