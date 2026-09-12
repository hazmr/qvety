# Part 14 — Clinic billing: invoices and payments

## Business

The clinic charges the client for a visit. Write `docs/domain/billing.md`:
- Invoice: for one client, optionally linked to a visit. Line items from services (price snapshot at the time, not a live reference) and free-text lines. Status `draft` → `issued` → `paid` / `partially_paid` / `void`. Issued invoices do not change lines; corrections are a credit and a new invoice.
- **Discount.** Egyptian clinics negotiate. One invoice-level `discount` amount (not percent), entered before issue, shown as its own line on the print. `total = subtotal - discount + tax`. Never negative; reject otherwise. Who gave the discount is in the audit log.
- **Tax.** `tax = round((subtotal - discount) * practice.tax_rate_percent / 100, 2)`. Most clinics have rate 0. The practice VAT number prints on the invoice header when set.
- **From visit to invoice.** "Create invoice" on a closed or open visit prefills lines from what the visit recorded: the appointment type's linked service if any, each vaccination (matched to a service by name if a match exists, else a free-text line at 0 to be priced), each dispensed prescription (free-text line: drug and quantity). Staff edit before issue. Nothing done in a visit should be forgotten at the desk.
- **Invoice number.** Assigned when the invoice is issued, not when drafted, so issued numbers have no gaps: `YYYY-000123` per practice per year. Drafts show "draft". Gapless numbering is what an accountant or the tax authority asks for; a void invoice keeps its number and shows as void.
- **Unbilled visits.** A list of visits with no non-void invoice and no `no_charge_reason`, oldest first. This is the money on the floor. Closing a visit (part 13) now also requires invoice-or-no-charge.
- Payment: against one invoice, amount, method (cash, InstaPay, Vodafone Cash, card, bank transfer), external reference for the transfer methods, received by (user), received at. Refund is a negative payment with a reason.
- Idempotency: the client (Angular) sends a key per payment attempt; the same key returns the same payment, never a second one.
- Money: `numeric(12,2)`, currency from the practice, no floating point anywhere.
- Daily cash report: totals per method per day per user, plus visit counts by `origin` (booked vs walk-in). Native query.

## Stack you learn

- Idempotency keys stored with a unique constraint and handled inside the transaction.
- Money handling in Java (`BigDecimal`, scale 2, rounding mode written down).
- Snapshotting reference data into line items.
- Transaction boundaries: invoice status updates and payment insert in one transaction, with optimistic locking.
- Angular: invoice builder, payment dialog that generates the idempotency key once per open, print view (browser print, RTL).

## Design

`V13__billing.sql`: `invoice_counters` (`practice_id`, `year`, `next_number`, unique on the pair; row locked with `SELECT ... FOR UPDATE` inside the issue transaction), `invoices` (with `number` nullable until issued, unique per practice, `discount`, `tax`, `due_date` nullable), `invoice_lines`, `payments` (unique `(practice_id, idempotency_key)`), all in RLS, audit, and export lists; composite FKs to client/visit. `appointment_types.default_service_id` (nullable) so an exam type prefills its consultation fee. Trigger: reject line changes and `discount` changes when invoice status is not `draft`.

Endpoints: `/api/v1/invoices` CRUD while draft, `POST /invoices/from-visit/{visitId}` (prefilled draft), `/issue`, `/void`, `/invoices/{id}/payments` (POST with `Idempotency-Key` header), `GET /invoices/{id}/print` (data for the print view), `GET /visits/unbilled`, `/reports/daily-cash?date`.

## Steps

1. Migration and trigger.
2. Services; you write `recordPayment` by hand: lookup by key, insert, recompute invoice status, all in one transaction.
3. Tests: same key twice → one payment, same response; two concurrent issues get consecutive numbers with no gap; overpayment rejected; discount larger than subtotal rejected; issue then edit line or discount → DB rejection; from-visit prefill has one line per vaccination and dispensed prescription; unbilled list drops a visit once its invoice is issued or it is marked no-charge; daily report totals; cross-tenant.
4. Angular: invoice page from the visit (prefilled), discount field, payment dialog, print view with logo, VAT number and discount line checked in RTL, `/billing/unbilled` list, daily cash report page.

## Ask Claude

- "Show an idempotent POST in Spring with a unique key, correct under concurrent duplicate requests."
- "Review my `BigDecimal` handling for scale and rounding."
- "Build a print stylesheet for an RTL invoice in Angular."

## Done when

- Double-submitting a payment creates one row.
- Invoice lifecycle, discount, unbilled list, and the daily cash report work in the browser.
- A visit with a vaccination produces an invoice with that line without retyping.
- Print view readable in Arabic.

## Self-check

- Why snapshot the service price into the line?
- Why is discount an amount and not a percent, and why is it invoice-level and not per line?
- What would go wrong with `double` for money? Give a concrete example.
- After part 15: add what this pilot clinic still cannot do to `docs/backlog.md`. Only the clinic decides its order.
