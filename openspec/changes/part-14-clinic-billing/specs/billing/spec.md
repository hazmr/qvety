## Purpose

Defines how a clinic charges a client: invoice lifecycle, gapless numbering, snapshot pricing, discount and tax, prefill from the visit, idempotent payments, and the daily cash report.

## ADDED Requirements

### Requirement: Invoice structure and lifecycle
An invoice SHALL be for one client, optionally linked to one visit, with line items from services (name and unit price snapshotted at the time) or free text. Status SHALL be `draft → issued → paid | partially_paid | void`. Issued invoices SHALL NOT change lines or discount; corrections SHALL be a void and a new invoice. The database SHALL enforce the no-change rule.

#### Scenario: Edit issued line
- **WHEN** the application role updates a line of an issued invoice
- **THEN** the database refuses

#### Scenario: Service price change
- **WHEN** a service price changes after an invoice line was created from it
- **THEN** the line keeps its snapshot description and unit price

### Requirement: Gapless invoice numbers at issue
The number SHALL be assigned when the invoice is issued, not when drafted, as `YYYY-000123` per practice per year with no gaps. Drafts SHALL show "draft". A void invoice SHALL keep its number and show as void.

#### Scenario: Concurrent issue
- **WHEN** two invoices are issued at the same moment
- **THEN** they receive consecutive numbers with no gap

#### Scenario: Void keeps number
- **WHEN** an issued invoice is voided
- **THEN** its number remains and the next issued invoice takes the next number

### Requirement: Discount is one amount per invoice
An invoice SHALL have one discount amount (not percent, not per line) entered before issue and printed as its own line. `total = subtotal - discount + tax`. A discount larger than the subtotal SHALL be rejected. Who gave the discount SHALL be visible in the audit log.

#### Scenario: Discount too large
- **WHEN** a discount greater than the subtotal is entered
- **THEN** the request is rejected

### Requirement: Tax from the practice rate
`tax = round((subtotal - discount) * tax_rate_percent / 100, 2)` using the practice's rate. The VAT number SHALL print on the header when set. Most clinics have rate 0.

#### Scenario: Rate zero
- **WHEN** the practice rate is 0
- **THEN** tax is 0.00 and no VAT line prints

### Requirement: Invoice from visit
"Create invoice" on a visit SHALL prefill a draft with: the appointment type's default service if any, one line per non-voided vaccination (matched to a service by name, else free text at 0), one free-text line per dispensed prescription (drug and quantity). Staff SHALL edit before issue.

#### Scenario: Visit with vaccination and dispense
- **WHEN** an invoice is created from a visit with one vaccination and one dispensed prescription
- **THEN** the draft has one line for each without retyping

### Requirement: Unbilled visits
`GET /api/v1/visits/unbilled` SHALL list visits with no non-void invoice and no `no_charge_reason`, oldest first.

#### Scenario: Issue removes from list
- **WHEN** an invoice for a visit is issued or the visit is marked no-charge
- **THEN** the visit leaves the unbilled list

### Requirement: Payments are idempotent
A payment SHALL be against one invoice with amount, method (`cash`, `instapay`, `vodafone_cash`, `card`, `bank_transfer`), external reference for transfer methods, received by, received at. The client SHALL send an idempotency key per attempt; the same key SHALL return the same payment and never create a second. Overpayment SHALL be rejected. A refund SHALL be a negative payment with a reason. Recording a payment and updating the invoice status SHALL be one transaction.

#### Scenario: Double submit
- **WHEN** the same payment is posted twice with the same key
- **THEN** one row exists and both responses are identical

#### Scenario: Overpayment
- **WHEN** a payment exceeds the outstanding amount
- **THEN** the request is rejected

### Requirement: Money is exact
All amounts SHALL be `numeric(12,2)` with the practice currency; no floating point anywhere; rounding mode HALF_UP at scale 2.

#### Scenario: Rounding
- **WHEN** tax computes to 12.345
- **THEN** 12.35 is stored

### Requirement: Daily cash report
`GET /api/v1/reports/daily-cash?date` SHALL return totals per method per user for the day, plus visit counts by origin (booked vs walk-in).

#### Scenario: Two methods
- **WHEN** the day has cash and InstaPay payments by two users
- **THEN** the report has one total per method per user and the visit counts

### Requirement: Invoice print
`GET /api/v1/invoices/{id}/print` SHALL provide the data for a print view with logo, practice header, VAT number when set, lines, discount line, tax, total, in the user's language, RTL-correct.

#### Scenario: Print in Arabic
- **WHEN** an issued invoice is printed in Arabic
- **THEN** the layout is right-to-left with the discount as its own line
