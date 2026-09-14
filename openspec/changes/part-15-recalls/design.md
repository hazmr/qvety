## Migration `V14__recalls.sql`

```sql
CREATE TYPE reminder_status AS ENUM ('open','done','dismissed');
CREATE TYPE contact_channel AS ENUM ('phone','whatsapp','in_person');
CREATE TYPE contact_outcome AS ENUM ('reached','no_answer','wrong_number','declined','booked');

CREATE TABLE care_reminders (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL REFERENCES practices(id),
  patient_id uuid NOT NULL, title text NOT NULL, note text, due_date date NOT NULL,
  status reminder_status NOT NULL DEFAULT 'open', created_by uuid NOT NULL,
  resolved_by uuid, resolved_at timestamptz, dismissed_reason text,
  version bigint NOT NULL DEFAULT 0, created_at ..., updated_at ...,
  UNIQUE (id, practice_id), FOREIGN KEY (patient_id, practice_id) REFERENCES patients(id, practice_id)
);
CREATE TABLE recall_contacts (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL REFERENCES practices(id),
  patient_id uuid NOT NULL, vaccination_id uuid, care_reminder_id uuid, appointment_id uuid,
  contacted_by uuid NOT NULL, channel contact_channel NOT NULL, outcome contact_outcome NOT NULL,
  note text, contacted_at timestamptz NOT NULL DEFAULT now(), created_at ...,
  CHECK ((vaccination_id IS NULL) <> (care_reminder_id IS NULL)),
  FOREIGN KEY (patient_id, practice_id) REFERENCES patients(id, practice_id),
  FOREIGN KEY (vaccination_id, practice_id) REFERENCES vaccinations(id, practice_id),
  FOREIGN KEY (care_reminder_id, practice_id) REFERENCES care_reminders(id, practice_id)
);
CREATE TABLE practice_settings (
  id uuid PRIMARY KEY DEFAULT uuidv7(), practice_id uuid NOT NULL UNIQUE REFERENCES practices(id),
  recall_window_before_days integer NOT NULL DEFAULT 14,
  recall_window_after_days integer NOT NULL DEFAULT 30,
  recall_message_ar text NOT NULL, recall_message_en text NOT NULL,
  reminder_message_ar text NOT NULL, reminder_message_en text NOT NULL,
  version bigint NOT NULL DEFAULT 0, created_at ..., updated_at ...
);
```
`recall_contacts` gets the part 13 `reject_change()` trigger. Three tables appended to RLS, audit, export lists. Practice creation (part 11) inserts a `practice_settings` row with default templates; dev seed does the same.

## Recall query

Native SQL, written in `psql` first against the seed:

```sql
WITH latest AS (
  SELECT DISTINCT ON (v.patient_id, v.vaccine_name) v.*
  FROM vaccinations v WHERE v.voided_at IS NULL
  ORDER BY v.patient_id, v.vaccine_name, v.given_at DESC
), last_contact AS (
  SELECT DISTINCT ON (vaccination_id) vaccination_id, outcome, contacted_at,
         count(*) OVER (PARTITION BY vaccination_id) AS attempts
  FROM recall_contacts WHERE vaccination_id IS NOT NULL
  ORDER BY vaccination_id, contacted_at DESC
)
SELECT ... FROM latest l
JOIN patients p ON p.id = l.patient_id AND p.deceased_at IS NULL
JOIN clients c ON c.id = p.client_id AND c.archived_at IS NULL
LEFT JOIN last_contact lc ON lc.vaccination_id = l.id
WHERE l.next_due BETWEEN current_date - :after AND current_date + :before
  AND NOT EXISTS (SELECT 1 FROM appointments a WHERE a.patient_id = p.id AND lower(a.period) > now() AND a.status NOT IN ('cancelled','no_show'))
  AND (lc.outcome IS NULL OR lc.outcome IN ('no_answer','wrong_number'))
ORDER BY l.next_due ASC
```
`DISTINCT ON` picks the latest row per group in one pass; a window function would also work but reads less clearly here. Care reminders are unioned with `type = 'care'` and the same DTO shape. RLS applies to every table in the query.

## API

| Method | Path |
| --- | --- |
| GET | `/api/v1/recalls?from&to&type&outcomeFilter&page` |
| POST | `/api/v1/recalls/{vaccinationId}/contacts` `{channel, outcome, note, appointmentId?}` |
| POST | `/api/v1/care-reminders` |
| POST | `/api/v1/care-reminders/{id}/contacts` |
| POST | `/api/v1/care-reminders/{id}/done` |
| POST | `/api/v1/care-reminders/{id}/dismiss` `{reason}` |
| GET | `/api/v1/patients/{id}/care-reminders` |
| GET / PUT | `/api/v1/practice/settings` (admin) |

## Angular

`/recalls` work list with default window and type filter; WhatsApp button builds `https://wa.me/<e164 without +>?text=<encodeURIComponent(message)>` (Arabic and newlines need encoding); outcome dialog refreshes the row; "Add reminder" on patient and visit pages; settings page for window and the four templates.

## Phone layout

Reference: `docs/design/MOBILE.md screen 11 (recalls list with a 44 px WhatsApp button per row; bottom sheet, not a route, for the handoff)`. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
