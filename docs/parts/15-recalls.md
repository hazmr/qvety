# Part 15 — Recalls: bringing the patient back

## Business

A vaccination has a `next_due` date. A clinic that calls the owner the week before earns the visit; a clinic that does not, loses it to the shop next door. This is the feature that pays for the subscription. Write `docs/domain/recalls.md`:
- A **recall** is a patient with a vaccination due inside a window (default: overdue up to 30 days, or due in the next 14 days). It is computed, not stored: the list is a query over `vaccinations` and `patients`.
- Excluded: deceased patients, archived clients, patients with a later vaccination of the same vaccine name (already boosted), and patients with an appointment already booked after today.
- Contact happens by phone or WhatsApp, by a person, from their own phone. The system opens WhatsApp with a pre-filled Arabic message (`wa.me/<phone_e164>?text=...`); it never sends anything itself. No provider, no cost, no consent regime beyond the client having given the clinic the number.
- Every contact attempt is recorded: who, when, channel (`phone`, `whatsapp`, `in_person`), outcome (`reached`, `no_answer`, `wrong_number`, `declined`, `booked`). Append-only. A recall with a `reached`/`booked`/`declined` outcome leaves the list; `no_answer` keeps it with the attempt count visible.
- Message template per practice, in Arabic and English, with placeholders `{client_name}`, `{patient_name}`, `{vaccine}`, `{due_date}`, `{practice_name}`.
- **Care reminders.** Not every follow-up is a vaccination: deworming, a post-surgery check, a chronic-case review, "call about the lab result". A vet or technician creates a **care reminder** on a patient: title, due date, note. Status `open` → `done` or `dismissed` (with reason). It appears in the same work list as vaccination recalls, same contact recording, same WhatsApp button with a generic template. It is stored, unlike a vaccination recall, because there is nothing to compute it from.

## Stack you learn

- A read model: a native query joined across four tables, returned as a `Page` of a DTO with no entity behind it.
- A small append-only table (`recall_contacts`) using the part 13 trigger pattern.
- Practice-level settings row (`practice_settings`: `recall_window_before_days`, `recall_window_after_days`, `recall_message_ar`, `recall_message_en`).
- Angular: a work-list page with filters, a row action that opens an external URL, a dialog that records the outcome and refreshes the row.

## Design

`V14__recalls.sql`: `care_reminders` (base + `patient_id`, `title`, `note`, `due_date`, `status` enum `open|done|dismissed`, `created_by`, `resolved_by`, `resolved_at`, `dismissed_reason`); `recall_contacts` (base + `patient_id`, `vaccination_id` nullable, `care_reminder_id` nullable, check exactly one set, `contacted_by`, `channel` enum, `outcome` enum, `note`, `contacted_at`), append-only; `practice_settings` (one row per practice, `practice_id` unique; also `reminder_message_ar`, `reminder_message_en` for care reminders). All in RLS, audit, export lists.

Endpoints: `GET /api/v1/recalls?from&to&outcomeFilter&type&page` (union of vaccination recalls and open care reminders, one DTO shape with `type`), `POST /api/v1/recalls/{vaccinationId}/contacts`, `POST /api/v1/care-reminders`, `POST /care-reminders/{id}/contacts`, `POST /care-reminders/{id}/done`, `POST /care-reminders/{id}/dismiss`, `GET /patients/{id}/care-reminders`, `GET/PUT /api/v1/practice/settings`.

The recall query: latest vaccination per `(patient_id, vaccine_name)` with `next_due` in window, minus exclusions, left-joined to the latest `recall_contacts` row for that vaccination. Sort: overdue first, then soonest due.

## Steps

1. Migration with the trigger and the settings row seeded for the dev practice.
2. `RecallQuery` native SQL. Write it in `psql` first against the seeded data until the list looks right, then move it to Java.
3. `RecallService.recordContact`, with the `booked` outcome also accepting an optional `appointmentId`. `CareReminderService` create/done/dismiss; `booked` or `reached` on a care reminder does not close it, only `done` or `dismiss` do (a person decides).
4. Tests: due tomorrow is listed; boosted patient is not; deceased is not; voided vaccination is not; `reached` removes from list; `no_answer` keeps it with count 1; a care reminder due in window is listed with `type = care`; `done` removes it; cross-tenant.
5. Angular: `/recalls` page (default window, filter by type), WhatsApp button building the message from the template, outcome dialog, "Add reminder" on the patient page and on the visit page, settings page for window and both templates.

## Ask Claude

- "Write the recall query: latest vaccination per patient and vaccine, due in a window, excluding patients with a later booking, with the last contact outcome. Explain the `DISTINCT ON` or window-function choice."
- "Build a `wa.me` link with a URL-encoded Arabic message in Angular; what characters need care?"
- "Review the recall exclusions against `docs/domain/recalls.md`."

## Done when

- Seeded data produces a recall list that matches what you compute by hand from the seed.
- WhatsApp opens with the Arabic message on your phone.
- Outcomes recorded, list updates, append-only test green.
- A care reminder created on a visit appears in the list on its due date and disappears when marked done.

## Self-check

- Why is the recall computed and not stored as a row per due vaccination?
- What is the difference between a recall and an appointment reminder, and which one does a clinic actually ask for first?
- After this part: add what this pilot clinic still cannot do to `docs/backlog.md`. Only the clinic decides its order.
