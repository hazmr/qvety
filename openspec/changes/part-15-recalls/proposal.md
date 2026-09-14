## Why

A vaccination has a next-due date. The clinic that calls the owner the week before earns the visit. This is the feature that pays for the subscription. Recalls are computed from vaccinations; care reminders cover every other follow-up; contact happens by a person from their own phone.

## What Changes

- Recall list as a native read model over vaccinations, patients, clients, appointments, with exclusions and last-contact outcome.
- `recall_contacts` append-only table; `care_reminders` stored table; `practice_settings` with window and message templates.
- WhatsApp button that opens `wa.me/<phone>?text=...` with a prefilled Arabic or English message; the server never sends anything.
- Care reminder create/done/dismiss, shown in the same work list with `type = care`.

## Capabilities

### New Capabilities
- `recalls`: computed vaccination recalls, exclusions, contact recording, message templates, care reminders, practice recall settings.

### Modified Capabilities
- none

## Non-goals

- Server-side messaging of any kind: no SMS, email, WhatsApp API, provider, or cost.
- Appointment reminders (day-before "you have a booking"); that is a different feature and goes to the backlog if a clinic asks.
- Stored recall rows; recalls are a query.

## Impact

- `V14__recalls.sql`; `com.qvety.recalls`; three tables in RLS, audit, export lists.
- Angular `/recalls`, "Add reminder" on patient and visit pages, recall settings page.
