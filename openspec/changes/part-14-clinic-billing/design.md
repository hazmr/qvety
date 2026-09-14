## Migration `V13__billing.sql`

```sql
CREATE TYPE invoice_status AS ENUM ('draft','issued','paid','partially_paid','void');
CREATE TYPE payment_method AS ENUM ('cash','instapay','vodafone_cash','card','bank_transfer');

CREATE TABLE invoice_counters (
  practice_id uuid NOT NULL REFERENCES practices(id),
  year integer NOT NULL,
  next_number integer NOT NULL DEFAULT 1,
  PRIMARY KEY (practice_id, year)
);
CREATE TABLE invoices (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL REFERENCES practices(id),
  client_id uuid NOT NULL, visit_id uuid,
  number text, status invoice_status NOT NULL DEFAULT 'draft',
  currency char(3) NOT NULL,
  subtotal numeric(12,2) NOT NULL DEFAULT 0, discount numeric(12,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
  tax numeric(12,2) NOT NULL DEFAULT 0, total numeric(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
  issued_at timestamptz, issued_by uuid, due_date date, voided_at timestamptz, void_reason text,
  version bigint NOT NULL DEFAULT 0, created_at ..., updated_at ...,
  UNIQUE (id, practice_id), UNIQUE (practice_id, number),
  FOREIGN KEY (client_id, practice_id) REFERENCES clients(id, practice_id),
  FOREIGN KEY (visit_id, practice_id) REFERENCES visits(id, practice_id)
);
CREATE TABLE invoice_lines (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL REFERENCES practices(id),
  invoice_id uuid NOT NULL, service_id uuid,
  description text NOT NULL, quantity numeric(10,2) NOT NULL DEFAULT 1,
  unit_price numeric(12,2) NOT NULL, line_total numeric(12,2) NOT NULL,
  created_at ..., updated_at ...,
  FOREIGN KEY (invoice_id, practice_id) REFERENCES invoices(id, practice_id)
);
CREATE TABLE payments (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL REFERENCES practices(id),
  invoice_id uuid NOT NULL, amount numeric(12,2) NOT NULL, currency char(3) NOT NULL,
  method payment_method NOT NULL, external_reference text, reason text,
  received_by uuid NOT NULL, received_at timestamptz NOT NULL DEFAULT now(),
  idempotency_key text NOT NULL, created_at ...,
  UNIQUE (practice_id, idempotency_key),
  FOREIGN KEY (invoice_id, practice_id) REFERENCES invoices(id, practice_id)
);
ALTER TABLE appointment_types ADD COLUMN default_service_id uuid;
ALTER TABLE appointment_types ADD FOREIGN KEY (default_service_id, practice_id) REFERENCES services(id, practice_id);
```
Trigger `protect_issued_invoice()`: reject `invoice_lines` insert/update/delete and `invoices.discount` change when status is not `draft`. Same `SECURITY DEFINER` pattern. Four tables appended to RLS, audit, export lists.

## Issue transaction

`SELECT ... FOR UPDATE` on `invoice_counters (practice_id, year)` (insert if missing), assign `YYYY-` + zero-padded 6 digits, increment, set `issued_at`, `issued_by`, status `issued`, all in one transaction. The row lock serializes concurrent issues so numbers are gapless.

## Payment transaction

`recordPayment(invoiceId, key, request)`: lookup by `(practice_id, key)` → return existing; else insert, recompute paid sum, set status `paid` or `partially_paid`, optimistic lock on the invoice. Overpayment → 409. Refund: negative amount with `reason` required.

## Money

`BigDecimal`, scale 2, `RoundingMode.HALF_UP`, currency from the practice. `double` would turn 0.1 + 0.2 into 0.30000000000000004 and drift totals over a month.

## API

`GET/POST /api/v1/invoices`, `GET/PUT /invoices/{id}` (draft only), `POST /invoices/from-visit/{visitId}`, `POST /invoices/{id}/issue`, `POST /invoices/{id}/void`, `POST /invoices/{id}/payments` (header `Idempotency-Key`), `GET /invoices/{id}/print`, `GET /visits/unbilled`, `GET /reports/daily-cash?date`.

## Angular

Invoice page from the visit (prefilled), discount field, payment dialog generating the idempotency key once per open, print view (logo, VAT, discount line, RTL), `/billing/unbilled`, daily cash report page.
