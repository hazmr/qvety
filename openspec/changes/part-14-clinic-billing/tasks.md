## 1. Migration
- [ ] 1.1 `V13__billing.sql`: counters, invoices, lines, payments, `default_service_id`, trigger
- [ ] 1.2 Append four tables to RLS, audit, export lists

## 2. Backend
- [ ] 2.1 Entities, DTOs, mappers
- [ ] 2.2 `InvoiceService`: draft CRUD, from-visit prefill, issue with counter lock, void, totals with discount and tax
- [ ] 2.3 `PaymentService.recordPayment` by hand: key lookup, insert, status recompute, one transaction
- [ ] 2.4 Unbilled query; daily cash native query
- [ ] 2.5 `VisitService.close` now checks invoice-or-no-charge

## 3. Tests
- [ ] 3.1 Same key twice → one payment, same response
- [ ] 3.2 Two concurrent issues → consecutive numbers, no gap
- [ ] 3.3 Overpayment rejected; discount > subtotal rejected
- [ ] 3.4 Issue then edit line or discount → DB rejection
- [ ] 3.5 From-visit prefill: one line per vaccination and dispensed prescription
- [ ] 3.6 Unbilled drops the visit on issue or no-charge
- [ ] 3.7 Daily report totals; cross-tenant; `TenantIsolationIT` covers four tables

## 4. Angular
- [ ] 4.1 Invoice builder from visit, discount field, issue, void
- [ ] 4.2 Payment dialog with idempotency key
- [ ] 4.3 Print view in RTL with logo, VAT, discount line
- [ ] 4.4 `/billing/unbilled`; daily cash report page
- [ ] 4.5 Checked in `ar` and `en`
- [ ] 4.6 Checked at 390 px and 360 px: phone layout per MOBILE.md screen 12 (collect payment: line items, segmented method control, 52 px amount field, change due, action bar); no `nz-table` on phone, 16 px inputs, 44 px targets

## 5. Close
- [ ] 5.1 `erd.md` updated
- [ ] 5.2 `progress.md` entry
- [ ] 5.3 Commit
