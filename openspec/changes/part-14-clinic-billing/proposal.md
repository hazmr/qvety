## Why

The clinic charges the client for a visit. Invoices must be gapless once issued, prices snapshotted, discounts explicit, payments idempotent, and nothing done in a visit forgotten at the desk.

## What Changes

- `invoice_counters`, `invoices`, `invoice_lines`, `payments` tenant tables; `appointment_types.default_service_id`.
- Invoice flow `draft → issued → paid | partially_paid | void`; gapless `YYYY-000123` number assigned at issue.
- Invoice-level discount amount and practice tax rate; trigger rejects line or discount changes after draft.
- "Create invoice from visit" prefilled from appointment type, vaccinations, dispensed prescriptions.
- Payments with idempotency key, refunds as negative payments, daily cash report.
- Unbilled visits list; visit close now requires invoice-or-no-charge.

## Capabilities

### New Capabilities
- `billing`: invoices, numbering, discount, tax, prefill from visit, payments, idempotency, refunds, daily cash report, unbilled visits.

### Modified Capabilities
- `clinical-records`: visit close requires an invoice or a no-charge reason.
- `reference-data`: appointment types may carry a default service.

## Non-goals

- Products or stock on invoices; free-text lines cover them.
- Credit notes as a separate document; a correction is a void plus a new invoice.
- Card terminal integration; `card` is a recorded method only.

## Impact

- `V14__billing.sql`; `com.qvety.billing`; `BigDecimal` scale 2, `RoundingMode.HALF_UP` recorded.
- `VisitService.close` gains the invoice check.
- Angular invoice builder, payment dialog, print view, unbilled list, daily report.
