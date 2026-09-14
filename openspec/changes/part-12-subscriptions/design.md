## Migration `V11__subscriptions.sql`

```sql
CREATE TYPE billing_period AS ENUM ('monthly','yearly');
CREATE TYPE sub_payment_method AS ENUM ('instapay','vodafone_cash','bank_transfer','cash');
CREATE TYPE sub_invoice_status AS ENUM ('open','paid','void');

CREATE TABLE plans (id uuid PRIMARY KEY DEFAULT uuidv7(), name text NOT NULL UNIQUE, active boolean NOT NULL DEFAULT true, created_at ..., updated_at ...);
CREATE TABLE plan_versions (
  id uuid PRIMARY KEY DEFAULT uuidv7(), plan_id uuid NOT NULL REFERENCES plans(id),
  version integer NOT NULL, period billing_period NOT NULL,
  price numeric(12,2) NOT NULL, currency char(3) NOT NULL DEFAULT 'EGP',
  max_users integer NOT NULL, storage_gb integer NOT NULL,
  created_at ..., UNIQUE (plan_id, version, period)
);
CREATE TABLE subscriptions (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL UNIQUE REFERENCES practices(id),
  plan_version_id uuid NOT NULL REFERENCES plan_versions(id),
  started_at date NOT NULL, current_period_end date NOT NULL, next_due_at date NOT NULL,
  created_at ..., updated_at ...
);
CREATE TABLE subscription_invoices (
  id uuid PRIMARY KEY DEFAULT uuidv7(), subscription_id uuid NOT NULL REFERENCES subscriptions(id),
  period_start date NOT NULL, period_end date NOT NULL,
  amount numeric(12,2) NOT NULL, currency char(3) NOT NULL, status sub_invoice_status NOT NULL DEFAULT 'open',
  created_at ..., updated_at ...
);
CREATE TABLE subscription_payments (
  id uuid PRIMARY KEY DEFAULT uuidv7(), subscription_invoice_id uuid NOT NULL REFERENCES subscription_invoices(id),
  amount numeric(12,2) NOT NULL, currency char(3) NOT NULL, method sub_payment_method NOT NULL,
  external_reference text, received_at date NOT NULL, proof_object_key text,
  idempotency_key text NOT NULL UNIQUE, recorded_by uuid REFERENCES platform_users(id),
  created_at ...
);
INSERT INTO platform_settings VALUES ('grace_days','14');
INSERT INTO plans ... 'Clinic';
INSERT INTO plan_versions ... (1, 'monthly', 990), (1, 'yearly', 9900), max_users 5, storage_gb 5;
```
Plans are platform data, not demo data, so they live in the migration. None of these tables are in the RLS list.

## Job

`SubscriptionStatusJob` `@Scheduled(cron = "0 15 3 * * *", zone = "Africa/Cairo")`, runs under system context. Each transition is a conditional `UPDATE ... WHERE status = <from>` so a second run finds nothing; audit row inserted in the same transaction. `--dry-run` (property) logs the would-be changes. Never touches `closed`.

Concurrency with payment recording: both update `practices.status` with optimistic locking; the payment path wins by re-reading status after the job's commit (test the same-moment case).

## Proof upload

Pre-signed PUT from MinIO client (`minio` SDK), 10 min validity, object key `platform/proofs/<uuid>`; only the key is stored. Fetch via pre-signed GET for platform users only. Same pattern reused for tenant attachments in part 13 with a `practice_id` prefix.

## API

| Method | Path | Who |
| --- | --- | --- |
| GET / POST | `/api/platform/plans`, `/plans/{id}/versions` | platform |
| POST | `/api/platform/practices/{id}/subscription` | platform; `{planVersionId, startedAt}` |
| GET | `/api/platform/subscriptions?status` | platform |
| POST | `/api/platform/subscription-invoices/{id}/payments` | platform; multipart fields + proof; header `Idempotency-Key` |
| GET | `/api/v1/subscription` | practice admin; read-only with pay instructions |

## Angular

`/admin/plans`, `/admin/subscriptions`, record payment dialog with file upload (NG-ZORRO upload to the pre-signed URL); practice-side `/subscription` with pay instructions in both languages.

## Phone layout

Reference: `docs/design/MOBILE.md` layout skeleton. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
