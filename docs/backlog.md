# Backlog — what the pilot does not do

Copy to `docs/backlog.md` in part 01. Nothing here is built until a clinic asks and the item is moved into a numbered part file with a business section, a design, and a test. Claude must not build any of this on its own initiative (see `CLAUDE.md`).

Two sources: features the previous Qvety system had and this rewrite drops on purpose, and things a real clinic will ask for that parts 01–15 do not cover. Each item says why it waits and what it would touch, so the cost is known before saying yes.

## Likely first asks after the pilot

| Item | Why it waits | What it touches |
| --- | --- | --- |
| Appointment reminder the day before (WhatsApp link, batch list) | Recalls (part 15) prove the contact loop first; reminders reuse it | `appointments`, recall work list, a template in `practice_settings` |
| Client merge and patient merge | Duplicates appear within weeks with Arabic names; merge must re-point every FK and keep both histories | Every table with `client_id` or `patient_id`; an append-only `merge_events` table with before/after snapshots |
| CSV import of clients and patients from Excel | Pilot clinic has Excel or paper; the previous system had an 18-table import engine, the pilot needs two tables | Preview then commit, per-row fingerprint to make re-import idempotent, duplicate warning from part 07 |
| Estimate / quote before surgery | Clinics quote a price and the owner accepts or declines; an `is_estimate` invoice or a separate table | `invoices` or `estimates`, print view, "convert to invoice" |
| Problem list (chronic conditions per patient) | Vets want "diabetic, on insulin" at the top of the chart, not buried in old notes | `patient_problems` (active/resolved/chronic), banner next to allergies |
| Boarding and multi-day stays (kennel board, daily notes, daily charges) | Grooming is a normal appointment type; boarding needs a stay entity and daily billing | `stays`, `stay_days`, whiteboard column, invoice lines per day |
| Multi-branch (locations) | Glossary: one location per practice; no `location_id` column exists | `users`, `rooms`, `appointments`, `practice_hours`, day view filter |

## Clinical, larger

| Item | Why it waits | What it touches |
| --- | --- | --- |
| Lab results (manual entry, inbox, review, follow-up assignment) | Egyptian clinics send to external labs; a PDF attachment on the visit covers the pilot | `lab_results`, review status, "entered in error" with replacement, safety queue |
| Inventory: products, stock, suppliers, purchase orders | Controlled-substance log (part 13) is the only stock tracked; full inventory is a product of its own | `products`, `stock_movements`, `suppliers`, `purchase_orders`, link from dispense to stock |
| Treatment templates (bundle of services, drugs, vaccines applied in one click) | Speeds up common visits; needs services and prescriptions stable first | `treatment_templates`, `treatment_template_items` |
| Dosing calculator and drug interaction warnings | Needs a drug reference table with per-kg doses; data quality is the work, not the code | `drugs`, `drug_interactions`, weight from `visit_vitals` |
| Consent forms with signature (surgery, anaesthesia, euthanasia) | Paper consent is legal and normal in the pilot | `consent_forms`, `consent_requests`, signature capture, PDF render, evidence hashes |
| Clinical corrections beyond void (typed correction records, replacement links for vitals and vaccinations) | Void with reason covers the pilot; a replacement link is one column later | `voided_*` columns plus `replaced_by` |
| Patient status `inactive` (moved away, no longer a client) separate from deceased | `archived_at` on the client covers most cases | `patients.inactive_at` |

## Front desk and clients

| Item | Why it waits | What it touches |
| --- | --- | --- |
| Client portal (records, vaccination card, invoices, online booking) | No proven demand in Egypt yet; WhatsApp is the portal | Separate auth, `portal_sessions`, public booking pages, tokens |
| Online booking page per practice | Same | `booking_pages`, public endpoint, rate limits |
| Waitlist for full days | One-doctor clinics manage by phone | `appointment_waitlist` |
| Recurring appointments (weekly physio, monthly injection) | Rare in pet clinics; book by hand | `recurring_series`, generation job |
| Staff schedules per veterinarian per weekday | `practice_hours` is one range per weekday for the whole clinic | `staff_schedules`, day view availability |
| Calendar feed (iCal) for the vet's phone | Nice, not asked for | signed token on `users`, read-only feed endpoint |
| Preferred contact method, emergency contact on client | `phone_secondary` covers the second number | two columns on `clients` |
| Client statement (all invoices and payments, balance) | Daily cash report covers the pilot | native query, print view |

## Billing and money

| Item | Why it waits | What it touches |
| --- | --- | --- |
| Card and online payment (gateway) | Cash, InstaPay, Vodafone Cash by transfer number cover Egypt | processor tables, webhooks, settlements, refunds, disputes |
| Wellness plans (monthly bundle for a pet) | Product decision, not a pilot need | `wellness_plans`, `wellness_enrollments`, invoice interaction |
| Pet insurance claims | Almost no market in Egypt | `insurance_policies`, `insurance_claims` |
| Financial close (end-of-day lock, cash drawer count) | Daily cash report is read-only; a lock is a later control | `financial_closes`, trigger blocking payments before close date |
| Per-line discount, percent discount | Invoice-level amount covers negotiation | `invoice_lines.discount` |
| Multi-currency | EGP only; practice currency column exists | nothing until a non-EG country |
| Egyptian Tax Authority e-invoicing / e-receipt integration | Applies to VAT-registered businesses above a threshold; a small clinic is not one; gapless numbering and VAT number on the print are the preparation | signed submission, ETA API client, item codes |
| Payment receipt as a separate print | The invoice print with paid stamp and payment list covers it | print view |

## Communication (deliberately out; the server never sends)

| Item | Why it waits | What it touches |
| --- | --- | --- |
| SMS, email, WhatsApp Business API sending | Cost, provider registration, consent regime, delivery reconciliation; the previous system spent more tables on this than on medicine | provider events, send attempts, suppressions, consent events, delivery history |
| Two-way messaging inbox | Same | `communications` with direction, threads |
| Email verification, password reset by email | Admin resets passwords in person (part 03) | `auth_tokens`, mail templates, provider |

## Platform and operations

| Item | Why it waits | What it touches |
| --- | --- | --- |
| Backup run records with verification result | Cron sidecar plus a written drill log covers the pilot | `backup_runs` (platform), size, checksum, restore-test result, alert on failure |
| File replicas to a second object store | One MinIO plus nightly `pg_dump` and bucket sync | `file_object_replicas`, replica lease and verification |
| Usage metering and plan limits enforced (`max_users`, `storage_gb`) | Limits are on `plan_versions` but not enforced; enforce when a clinic hits one | check on user create, check on upload |
| Public REST API with API keys and signed webhooks for integrators | No integrator yet | `api_keys`, `webhooks`, scopes, rate limit per key |
| One identity with many practice memberships (`identities` plus `practice_members`) | Part 03b tries the password against every practice's row and asks when several match; that covers a vet at two clinics. The split means one password everywhere and a picker on every login | new tables, login rewrite, RLS on memberships, `users` refactor, every auth test |
| Remember the last chosen practice on the device | Only matters when the same password is used at two clinics; one extra tap today | `localStorage` key, skip the picker when the remembered practice is in the list |
| Optional MFA (TOTP, recovery codes) | Small staff, short sessions; add when a clinic with many users asks | columns on `users`, enrol and verify flows |
| Demo/sample data per new practice with "remove sample data" | Seed per country (part 09) is the starter; sample pets are marketing | flag on rows, delete path that respects append-only |
| In-app guided tours | Later | frontend only |
| Practice conversion funnel, pilot tracking tables | Track in a spreadsheet until there are ten practices | none |
| Clinic pilots workflow tables | Same | none |
| Closed-practice data deletion job after `closed_retention_days` | Write the job when the first practice closes; the setting exists (part 11) | scheduled job under system context, platform audit row |

## Decided against (not waiting, refused)

- Soft delete (`deleted_at`) anywhere. Archive, deactivate, void, retract instead.
- A `viewer` role. Four roles until a clinic names the person.
- Storing national ID on clients. Personal data law; no use case.
- Server-side messaging of any kind in the pilot. See above.
- Storing money as floating point. `numeric(12,2)` only.
