# Glossary — veterinary practice vocabulary

Business terms as used in this product. When Claude uses a term differently, this file wins.

| Term | Meaning |
| --- | --- |
| **PIMS** | Practice Information Management System. The software a veterinary clinic runs its day on: who is coming, what was done, what was charged. |
| **Practice** | One clinic business. The tenant. Everything in the system belongs to exactly one practice, except platform data. |
| **Location** | A physical branch of a practice. Pilot supports one location per practice; there is no `location_id` column anywhere. Adding branches later touches `users`, `rooms`, `appointments`, `practice_hours`. See `backlog.md`. |
| **User** | A staff member with a login. Belongs to one practice. Has one role. |
| **Role** | `admin` (owns the practice account), `veterinarian` (clinical staff), `technician` (assists, records vitals, vaccinations), `front_desk` (scheduling, clients, payments). Decides what a user may manage. |
| **Veterinarian flag** | `is_veterinarian` on a user. Decides who may diagnose, prescribe, finalize notes, and be booked as the vet. Separate from role because the clinic owner is usually both `admin` and the vet. |
| **Session version** | Integer on the user row, copied into every JWT. Bumping it revokes all of that user's tokens at once (deactivation, password reset). |
| **Super admin** | Qvety staff. Platform role, no practice. Manages practices and subscriptions. Never sees clinical data through the normal path. |
| **Client** | The pet owner. A person, not an animal. Has contact details and one or more patients. |
| **Patient** | The animal. Belongs to one client. Has species, breed, sex, date of birth, weight history, allergies. |
| **Care reminder** | A stored follow-up on a patient with a due date: deworming, post-surgery check, call about a result. Created by staff, closed by staff (`done` or `dismissed`). Shown in the same work list as vaccination recalls. |
| **Allergy** | A substance a patient reacts to, with severity. Never edited or deleted; a wrong entry is retracted with a reason. Shown before any prescription. |
| **Species** | Coded value: dog, cat, bird, rabbit, horse, livestock, other. Stored as an enum; label translated in the UI. |
| **Appointment** | A booked slot: patient, veterinarian, room, appointment type, start and end time, status, origin (`scheduled` or `walk_in`). |
| **Appointment type** | Reference data per practice: wellness exam, vaccination, surgery, follow-up. Has a default duration. |
| **Room** | Reference data per practice: exam room 1, surgery, grooming. |
| **Whiteboard** | Today's checked-in patients grouped by status: waiting, in exam, done. A view over appointments, not a table. What the whole clinic looks at between bookings. |
| **Visit** | What actually happened when the patient came in. One appointment may produce one visit. A walk-in produces a visit without an appointment. Has an attending veterinarian, or none for a technician-only visit (booster, weight check). |
| **Visit close** | The end of a visit: note finalized or voided (or none), and an invoice exists or the visit is marked no-charge with a reason. Open visits at end of day are shown, not hidden. |
| **Vitals** | Measurements per visit: temperature, heart rate, respiratory rate, weight, body condition score, pain score, mucous membrane, capillary refill. Wrong rows are voided, never edited. |
| **Clinical note / SOAP** | The medical record of a visit. SOAP = Subjective (owner's report), Objective (findings, vitals), Assessment (diagnosis), Plan (treatment). All four sections optional. |
| **Finalized** | A clinical note the veterinarian has signed. After this it never changes in place. |
| **Addendum** | A dated, attributed addition to a finalized note. The only way to add to it. |
| **Vaccination record** | Vaccine, manufacturer, lot, expiry, dose type, date given, next due date, who gave it. Drives recalls and the vaccination certificate. A wrong row is voided with a reason. |
| **Void** | Marking a clinical row (note, vaccination, vitals) as wrong, with a reason and who did it. The row stays visible; it leaves recalls, certificates, and trends. Never a delete. |
| **Vaccination certificate** | Printed bilingual card listing a patient's vaccinations. Needed for travel, boarding, pet passport. |
| **Recall** | A patient whose vaccination is due or overdue inside a window. Computed from vaccination records, not stored. Contacted by a person by phone or WhatsApp; every attempt and outcome is recorded. |
| **Prescription** | Drug, dose, route, frequency, duration, quantity, prescriber. Creates events (issued, dispensed, cancelled) that are append-only. Printable (روشتة) for the pharmacy; issued-not-dispensed is normal. |
| **Controlled substance** | A drug regulated by law. Every receipt, dispense, and count adjustment is logged with quantity and running balance per drug. The balance never goes below zero. Rules depend on country (regulatory framework). This log is the only stock the pilot tracks. |
| **License number** | The veterinarian's syndicate registration number. On the user row; printed on prescriptions and certificates. |
| **Regulatory framework** | The country rule set for controlled substances and prescriptions. `EG` first. Unknown country is an error, never a default. |
| **Service** | Something the clinic sells: consultation, vaccine, surgery. Reference data with a price in the practice currency. |
| **Product** | Something the clinic stocks and sells: drugs, food. Inventory is out of scope until a clinic asks. |
| **Invoice** | What a client owes for a visit: line items (services, products), subtotal, discount, tax if any, total, status (draft, issued, paid, void). Numbered `YYYY-000123` at issue, gapless per practice. Can be prefilled from what the visit recorded. |
| **Discount** | One amount per invoice, given before issue, printed as its own line. Never a percent, never per line, never larger than the subtotal. |
| **Unbilled visit** | A visit with no invoice and no no-charge reason. The daily list of money not yet asked for. |
| **Payment** | Money received against an invoice. Method: cash, InstaPay, Vodafone Cash, card, bank transfer. Has an idempotency key and an optional external reference. |
| **Idempotency key** | A value the client sends so that retrying the same request does not create a second payment. Internal. |
| **External reference** | The transfer number from InstaPay or Vodafone Cash. Entered by staff. For reconciliation, not for uniqueness of the request. |
| **Plan** | What Qvety sells to a practice: price in EGP per month or year, limits. Versioned. |
| **Subscription** | A practice's current plan, period, and due date. One active per practice. |
| **Subscription payment** | Money a practice sends Qvety. Recorded by the super admin. Separate from clinic payments in every way. |
| **Trial** | The practice status at creation. Ends at `trial_ends_at` (creation plus the platform `trial_days` setting). First payment makes it `active`; expiry without payment makes it `past_due`. |
| **Practice status** | `trial`, `active`, `past_due`, `suspended`, `closed`. Set by the super admin or the subscription job. Enforced on every request. `suspended` blocks writes; `closed` allows export only. |
| **Export** | One JSON file with every tenant table for one practice plus its attachments. Any practice can request its own at any time. The "you own your data" promise. |
| **Audit log** | Append-only table written by a database trigger on every insert, update, delete of a tenant table: who, when, before, after. Secrets stripped. |
| **Reference data** | Small tables of options a practice configures: rooms, appointment types, services, species labels. Generic CRUD is allowed here and nowhere else. |
| **Evidence table** | An append-only table that records that something happened (prescription event, lab result, consent, message delivery). The application role can insert, never update or delete. |
| **Tenant boundary** | The rule that no query ever returns another practice's rows. Enforced in Postgres by RLS and tested on every commit. |
| **RLS** | Row-level security. A Postgres policy on each tenant table: `practice_id = current_setting('app.practice_id')::uuid`. |
| **System context** | The explicit, opt-in mode in which platform code reads platform tables without a practice. Never ambient. |
