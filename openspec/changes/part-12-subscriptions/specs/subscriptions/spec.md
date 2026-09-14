## Purpose

Defines what Qvety sells to a practice, how payments are recorded, and how practice status follows due dates and payments without manual work.

## ADDED Requirements

### Requirement: Plans are versioned
A plan SHALL have a name and versions; each version SHALL have a price in EGP, a period (`monthly` or `yearly`), `max_users`, and `storage_gb`. A price change SHALL create a new version; existing subscriptions SHALL keep their version until renewal.

#### Scenario: Price change
- **WHEN** a new version of `Clinic` is created with a higher price
- **THEN** current subscriptions still reference their old version and amount

### Requirement: Launch pricing
The migration SHALL seed one plan `Clinic` with two versions: 990 EGP monthly and 9,900 EGP yearly, both `max_users = 5`, `storage_gb = 5`. Platform defaults SHALL be `trial_days = 30`, `grace_days = 14`, `closed_retention_days = 90`, stored in settings, not code.

#### Scenario: Fresh database
- **WHEN** migrations run on an empty database
- **THEN** the `Clinic` plan and the three settings exist

### Requirement: One active subscription per practice
A practice SHALL have at most one subscription: plan version, start, current period end, next due date.

#### Scenario: Second subscription
- **WHEN** a second subscription is assigned to a practice that has one
- **THEN** the request is rejected

### Requirement: Subscription invoices and payments
One subscription invoice SHALL be generated per period. A subscription payment SHALL record amount, method (`instapay`, `vodafone_cash`, `bank_transfer`, `cash`), external reference, received date, and a proof file, entered by the super admin with an idempotency key. Recording a payment SHALL, in one transaction: insert the payment, mark the invoice paid when covered, extend the period and next due date, set the practice `active` if it was `trial`, `past_due`, or `suspended`, and write a platform audit row.

#### Scenario: Payment reactivates
- **WHEN** a payment covering the invoice is recorded for a suspended practice
- **THEN** the practice is `active` and writes work again

#### Scenario: Same idempotency key twice
- **WHEN** the same payment is posted twice with the same key
- **THEN** one payment row exists and both responses are identical

### Requirement: Proof of payment is private
The proof file SHALL be stored in object storage under a key; only its key SHALL be persisted; it SHALL be fetched only by a platform user through a short-lived signed URL. No public URL SHALL exist.

#### Scenario: Fetch proof
- **WHEN** a platform user opens a payment's proof
- **THEN** a signed URL valid for minutes is issued; unauthenticated access to the object is refused

### Requirement: Status follows due dates automatically
A daily job SHALL: move `trial` practices whose `trial_ends_at` has passed with no payment to `past_due`; move `active` practices whose `next_due_at` has passed to `past_due`; move `past_due` practices whose due date is more than `grace_days` ago to `suspended`. Each change SHALL write a platform audit row. The job SHALL never touch `closed` practices, SHALL be idempotent (a second run the same day changes nothing), and SHALL support a dry-run that only logs.

#### Scenario: Trial expired
- **WHEN** the job runs after a practice's `trial_ends_at` with no payment
- **THEN** the practice is `past_due` with one audit row

#### Scenario: Grace elapsed
- **WHEN** the job runs and a practice has been `past_due` for more than `grace_days`
- **THEN** the practice is `suspended`

#### Scenario: Run twice
- **WHEN** the job runs twice in one day
- **THEN** no duplicate audit rows and no repeated transitions

### Requirement: Practice sees its subscription
`GET /api/v1/subscription` SHALL return, read-only, the plan, period end, amount due, and pay instructions (InstaPay handle, Vodafone Cash number, where to send the transfer number), in the user's language.

#### Scenario: Practice subscription page
- **WHEN** a practice admin opens the subscription page in Arabic
- **THEN** the amount and the pay instructions are shown in Arabic

### Requirement: Subscription payments are separate from clinic payments
Subscription payments (practice → Qvety) SHALL be a different table from clinic payments (client → practice); nothing SHALL be shared between them.

#### Scenario: Schema check
- **WHEN** the tables are inspected
- **THEN** `subscription_payments` is a platform table and `payments` is a tenant table with no relation between them
