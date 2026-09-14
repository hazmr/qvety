## Purpose

Defines how the clinic brings patients back: which vaccinations are due, who is excluded, how staff contact owners from their own phone, how each attempt is recorded, and care reminders for non-vaccination follow-ups.

## ADDED Requirements

### Requirement: A recall is computed, not stored
A recall SHALL be a patient whose latest non-voided vaccination per vaccine name has `next_due` inside the window (default: overdue up to 30 days, or due in the next 14 days). It SHALL be a query over vaccinations and patients, never a stored row.

#### Scenario: Due tomorrow
- **WHEN** a patient's latest vaccination has `next_due` tomorrow
- **THEN** the patient appears in the recall list

#### Scenario: Already boosted
- **WHEN** a later vaccination with the same vaccine name exists
- **THEN** the earlier due date does not produce a recall

### Requirement: Exclusions
The list SHALL exclude deceased patients, archived clients, voided vaccinations, patients already boosted, and patients with an appointment booked after today.

#### Scenario: Deceased
- **WHEN** a patient is marked deceased
- **THEN** it is absent from the list

#### Scenario: Future booking
- **WHEN** the patient has an appointment after today
- **THEN** it is absent from the list

### Requirement: Contact happens from a person's own phone
The system SHALL open WhatsApp on the user's device with a prefilled message (`wa.me/<phone_e164>?text=...`) or show the phone number. The server SHALL NOT send SMS, email, or WhatsApp messages, and SHALL NOT integrate a messaging provider.

#### Scenario: WhatsApp button
- **WHEN** the WhatsApp button is pressed for a recall
- **THEN** the device opens `wa.me` with the client's E.164 number and the rendered Arabic or English template; no server request is made to any messaging service

### Requirement: Every contact attempt is recorded
A contact SHALL record who, when, channel (`phone`, `whatsapp`, `in_person`), outcome (`reached`, `no_answer`, `wrong_number`, `declined`, `booked`), optional note, and optionally the booked appointment. The table SHALL be append-only. A recall whose last outcome is `reached`, `booked`, or `declined` SHALL leave the list; `no_answer` SHALL keep it with the attempt count visible.

#### Scenario: Reached
- **WHEN** an attempt with outcome `reached` is recorded
- **THEN** the recall leaves the list

#### Scenario: No answer
- **WHEN** an attempt with outcome `no_answer` is recorded
- **THEN** the recall stays with attempt count 1

#### Scenario: Edit contact
- **WHEN** the application role updates a contact row
- **THEN** the database refuses

### Requirement: Message templates per practice
Each practice SHALL have an Arabic and an English template for vaccination recalls and for care reminders, with placeholders `{client_name}`, `{patient_name}`, `{vaccine}`, `{due_date}`, `{practice_name}`, editable by an admin.

#### Scenario: Render template
- **WHEN** the WhatsApp message is built for a recall
- **THEN** every placeholder is replaced with the client's, patient's, vaccine, due date, and practice values in the client's preferred language

### Requirement: Recall window is a practice setting
The window (days before and days after due) SHALL be stored per practice and editable by an admin.

#### Scenario: Wider window
- **WHEN** the admin sets 30 days before
- **THEN** vaccinations due within 30 days appear

### Requirement: Care reminders
A vet or technician SHALL create a care reminder on a patient: title, due date, note. Status SHALL be `open → done | dismissed` (dismiss with reason). It SHALL appear in the same work list as recalls with `type = care`, use the same contact recording and WhatsApp button with the generic template. Only `done` or `dismiss` SHALL close it; a `reached` or `booked` contact SHALL NOT.

#### Scenario: Reminder due
- **WHEN** a care reminder's due date is inside the window
- **THEN** it appears in the list with `type = care`

#### Scenario: Done
- **WHEN** the reminder is marked done
- **THEN** it leaves the list

#### Scenario: Reached does not close
- **WHEN** a `reached` contact is recorded on a care reminder
- **THEN** the reminder stays open until a person marks it done or dismissed

### Requirement: Sort order
The list SHALL sort overdue first, then soonest due.

#### Scenario: Ordering
- **WHEN** one recall is 5 days overdue and one is due in 3 days
- **THEN** the overdue one is first
