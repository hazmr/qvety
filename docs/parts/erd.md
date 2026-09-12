# Qvety — Entity Relationship Diagram

Target schema after part 15. Two zones:

- **Platform zone** — no `practice_id`. Reached only from the `platform` package under system context. Super admin only.
- **Tenant zone** — every table has `practice_id`, an RLS policy, and (where it references another tenant table) a composite foreign key `(x_id, practice_id)` so a cross-practice link is impossible at the database.

Conventions: every table has `id uuid` (Postgres `uuidv7()`), `created_at`, `updated_at`. Tenant tables also carry `version` for optimistic locking. Money is `numeric(12,2)` plus a `currency char(3)`. Enums are Postgres enums. Tables marked **append-only** reject `UPDATE`/`DELETE` for the application role by trigger. There is no soft delete (`deleted_at`) anywhere. Every tenant table is in three lists maintained in the migrations: RLS policy, audit trigger, practice export.

The Mermaid diagrams render in VS Code (Markdown Preview Mermaid Support), GitHub, and any Mermaid viewer.

---

## 1. Overview (relationships only)

```mermaid
erDiagram
    %% ---------- PLATFORM ZONE ----------
    practices ||--o{ users : "employs"
    practices ||--o| subscriptions : "has active"
    plans ||--o{ plan_versions : "versioned as"
    plan_versions ||--o{ subscriptions : "priced by"
    subscriptions ||--o{ subscription_invoices : "billed per period"
    subscriptions ||--o{ subscription_payments : "paid by"
    subscription_invoices o|--o{ subscription_payments : "settled by"
    platform_users ||--o{ subscription_payments : "recorded by"
    platform_users ||--o{ platform_audit_log : "acted"

    %% ---------- TENANT ZONE: people & animals ----------
    practices ||--o{ practice_hours : "opens"
    practices ||--o| practice_settings : "configured by"
    practices ||--o{ clients : "serves"
    clients ||--o{ patients : "owns"
    patients ||--o{ patient_weights : "weighed"
    patients ||--o{ patient_allergies : "reacts to"
    patients ||--o{ care_reminders : "followed up by"

    %% ---------- reference data ----------
    practices ||--o{ rooms : "configures"
    practices ||--o{ appointment_types : "configures"
    practices ||--o{ services : "sells"
    services o|--o{ appointment_types : "default fee"

    %% ---------- scheduling ----------
    patients ||--o{ appointments : "booked for"
    clients ||--o{ appointments : "booked by"
    users ||--o{ appointments : "assigned vet"
    rooms o|--o{ appointments : "in"
    appointment_types ||--o{ appointments : "typed as"

    %% ---------- clinical ----------
    appointments o|--o| visits : "becomes"
    patients ||--o{ visits : "seen in"
    users o|--o{ visits : "attending vet"
    visits ||--o{ visit_vitals : "measured"
    visits ||--o| clinical_notes : "documented by"
    clinical_notes ||--o{ clinical_note_addenda : "amended by"
    clinical_notes o|--o| clinical_notes : "replaced_by"
    visits ||--o{ vaccinations : "given during"
    visits ||--o{ prescriptions : "issued during"
    prescriptions ||--o{ prescription_events : "history"
    prescription_events o|--o| controlled_substance_log : "logged if dispense"
    users ||--o{ controlled_substance_log : "entered by"
    visits ||--o{ attachments : "files"

    %% ---------- billing ----------
    clients ||--o{ invoices : "billed"
    visits o|--o{ invoices : "charged for"
    invoices ||--o{ invoice_lines : "contains"
    services o|--o{ invoice_lines : "snapshot of"
    invoices ||--o{ payments : "paid by"
    payments o|--o{ payments : "refund_of"
    users ||--o{ payments : "received by"

    %% ---------- recalls ----------
    vaccinations o|--o{ recall_contacts : "chased by"
    care_reminders o|--o{ recall_contacts : "chased by"
    users ||--o{ recall_contacts : "contacted by"

    %% ---------- audit ----------
    practices ||--o{ audit_log : "tracks"
```

---

## 2. Platform zone (detail)

```mermaid
erDiagram
    practices {
        uuid id PK
        varchar name
        char2 country "EG only for now; check constraint"
        char3 currency "EGP"
        varchar locale "ar-EG"
        varchar timezone "Africa/Cairo"
        enum status "trial|active|past_due|suspended|closed"
        text address "print header"
        varchar phone
        varchar vat_number "nullable; printed when set"
        numeric tax_rate_percent "numeric(5,2), default 0"
        varchar logo_object_key "MinIO, part 13"
        timestamptz trial_ends_at "creation + trial_days"
        timestamptz closed_at
        timestamptz created_at
        timestamptz updated_at
    }

    platform_users {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        bool active
        int session_version
        timestamptz created_at
        timestamptz updated_at
    }

    plans {
        uuid id PK
        varchar name
        bool active
        timestamptz created_at
        timestamptz updated_at
    }

    plan_versions {
        uuid id PK
        uuid plan_id FK
        int version "unique per plan"
        numeric price
        char3 currency "EGP"
        enum period "monthly|yearly"
        int max_users
        int storage_gb
        jsonb features
        timestamptz created_at
    }

    subscriptions {
        uuid id PK
        uuid practice_id FK "unique: one per practice"
        uuid plan_version_id FK
        date started_at
        date current_period_start
        date current_period_end
        date next_due_at
        timestamptz created_at
        timestamptz updated_at
    }

    subscription_invoices {
        uuid id PK
        uuid subscription_id FK
        date period_start
        date period_end
        numeric amount
        char3 currency
        enum status "open|paid|void"
        timestamptz created_at
    }

    subscription_payments {
        uuid id PK
        uuid subscription_id FK
        uuid subscription_invoice_id FK "nullable"
        numeric amount
        char3 currency
        enum method "instapay|vodafone_cash|bank_transfer|cash"
        varchar external_reference "transfer number"
        date received_at
        date period_start
        date period_end
        varchar proof_object_key "MinIO key, nullable"
        text notes
        varchar idempotency_key UK
        uuid recorded_by FK "platform_users"
        timestamptz created_at
    }

    platform_settings {
        varchar key PK "grace_days, trial_days, closed_retention_days"
        varchar value
        timestamptz updated_at
    }

    platform_audit_log {
        uuid id PK "append-only"
        uuid platform_user_id FK
        varchar action
        varchar target_type
        uuid target_id
        jsonb details
        timestamptz at
    }

    plans ||--o{ plan_versions : ""
    plan_versions ||--o{ subscriptions : ""
    practices ||--o| subscriptions : ""
    subscriptions ||--o{ subscription_invoices : ""
    subscriptions ||--o{ subscription_payments : ""
    subscription_invoices o|--o{ subscription_payments : ""
    platform_users ||--o{ subscription_payments : ""
    platform_users ||--o{ platform_audit_log : ""
```

---

## 3. Tenant zone — people, animals, reference data

```mermaid
erDiagram
    users {
        uuid id PK
        uuid practice_id FK
        varchar email "unique per practice"
        varchar password_hash
        varchar full_name
        enum role "admin|veterinarian|technician|front_desk"
        bool is_veterinarian "clinical acts; check vs role"
        varchar license_number "syndicate no.; printed"
        varchar phone
        varchar locale "ar-EG default"
        bool active
        int session_version "bump = revoke all JWTs"
        bool must_change_password
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    practice_hours {
        uuid id PK
        uuid practice_id FK
        smallint weekday "0-6"
        time opens
        time closes
    }

    practice_settings {
        uuid id PK
        uuid practice_id FK "unique"
        int recall_window_before_days
        int recall_window_after_days
        text recall_message_ar
        text recall_message_en
        text reminder_message_ar "care reminders"
        text reminder_message_en
        int version
        timestamptz updated_at
    }

    clients {
        uuid id PK
        uuid practice_id FK
        varchar full_name "Egyptian name chain"
        varchar full_name_normalized "NFC, alef fold, no tashkeel; trgm index"
        varchar preferred_name
        varchar phone "as typed"
        varchar phone_e164 "+20..."
        varchar phone_secondary "as typed"
        varchar phone_secondary_e164
        varchar email
        text address
        text notes
        varchar preferred_locale
        timestamptz archived_at
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    patients {
        uuid id PK
        uuid practice_id FK
        uuid client_id FK "composite FK with practice_id"
        uuid previous_client_id FK "set on transfer"
        varchar name
        enum species "dog|cat|bird|rabbit|rodent|reptile|horse|livestock|other"
        varchar breed "free text"
        enum sex "male|female|male_neutered|female_spayed|unknown"
        date date_of_birth
        bool age_approximate
        varchar color
        varchar microchip
        varchar photo_object_key "nullable; writable from part 13"
        timestamptz deceased_at
        text notes
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    patient_weights {
        uuid id PK
        uuid practice_id FK
        uuid patient_id FK
        timestamptz measured_at
        numeric weight_kg
        uuid recorded_by FK
    }

    patient_allergies {
        uuid id PK "trigger: only retracted_* may change"
        uuid practice_id FK
        uuid patient_id FK
        varchar substance
        text reaction
        enum severity "mild|moderate|severe"
        uuid noted_by FK
        timestamptz retracted_at
        text retracted_reason
        timestamptz created_at
    }

    care_reminders {
        uuid id PK "part 15; stored, not computed"
        uuid practice_id FK
        uuid patient_id FK
        varchar title
        text note
        date due_date
        enum status "open|done|dismissed"
        uuid created_by FK
        uuid resolved_by FK
        timestamptz resolved_at
        text dismissed_reason
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    rooms {
        uuid id PK
        uuid practice_id FK
        varchar name
        bool active
        int version
    }

    appointment_types {
        uuid id PK
        uuid practice_id FK
        varchar name
        int duration_minutes
        varchar color
        uuid default_service_id FK "nullable; part 14 prefill"
        bool active
        int version
    }

    services {
        uuid id PK
        uuid practice_id FK
        varchar name
        numeric price
        char3 currency
        bool active
        int version
    }

    practices ||--o{ users : ""
    practices ||--o{ practice_hours : ""
    practices ||--o| practice_settings : ""
    practices ||--o{ clients : ""
    clients ||--o{ patients : "client_id"
    clients o|--o{ patients : "previous_client_id"
    patients ||--o{ patient_weights : ""
    patients ||--o{ patient_allergies : ""
    patients ||--o{ care_reminders : ""
    users o|--o{ care_reminders : "created_by"
    services o|--o{ appointment_types : "default_service_id"
    users o|--o{ patient_weights : "recorded_by"
    users o|--o{ patient_allergies : "noted_by"
    practices ||--o{ rooms : ""
    practices ||--o{ appointment_types : ""
    practices ||--o{ services : ""
```

---

## 4. Tenant zone — scheduling and clinical

```mermaid
erDiagram
    appointments {
        uuid id PK
        uuid practice_id FK
        uuid patient_id FK
        uuid client_id FK
        uuid veterinarian_id FK "users"
        uuid room_id FK "nullable"
        uuid appointment_type_id FK
        tstzrange period "EXCLUDE gist per vet and per room, excluding cancelled/no_show"
        enum status "scheduled|checked_in|in_progress|completed|cancelled|no_show"
        enum origin "scheduled|walk_in"
        text reason
        text notes
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    visits {
        uuid id PK
        uuid practice_id FK
        uuid patient_id FK
        uuid client_id FK
        uuid appointment_id FK "nullable: walk-in"
        uuid attending_vet_id FK "nullable: technician-only visit"
        timestamptz started_at
        timestamptz ended_at
        enum status "open|closed"
        timestamptz closed_at
        text no_charge_reason "nullable; set = billed as nothing"
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    visit_vitals {
        uuid id PK
        uuid practice_id FK
        uuid visit_id FK
        uuid recorded_by FK
        timestamptz recorded_at
        numeric temperature_c
        int heart_rate
        int resp_rate
        numeric weight_kg "also written to patient_weights"
        smallint body_condition_score "1-9"
        smallint pain_score "0-10"
        varchar mucous_membrane
        numeric capillary_refill_sec
        text notes
        timestamptz voided_at "trigger: only voided_* may change"
        text void_reason
    }

    clinical_notes {
        uuid id PK
        uuid practice_id FK
        uuid visit_id FK "unique"
        uuid author_id FK
        text subjective
        text objective
        text assessment
        text plan
        timestamptz finalized_at "trigger: text frozen after this"
        uuid finalized_by FK "composite FK with practice_id"
        varchar finalized_by_name "snapshot at signing"
        timestamptz voided_at
        text void_reason
        uuid replaced_by FK "clinical_notes"
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    clinical_note_addenda {
        uuid id PK "append-only"
        uuid practice_id FK
        uuid note_id FK
        uuid author_id FK
        text body
        timestamptz created_at
    }

    vaccinations {
        uuid id PK
        uuid practice_id FK
        uuid visit_id FK
        uuid patient_id FK
        varchar vaccine_name
        varchar manufacturer
        varchar lot
        date product_expires_at
        enum dose_type "initial|booster"
        date given_at
        date next_due
        uuid given_by FK
        uuid supervising_vet_id FK "nullable; users, when given_by not vet"
        varchar rabies_tag_number "nullable"
        timestamptz voided_at "trigger: only voided_* may change; excluded from recalls and certificate"
        text void_reason
        uuid voided_by FK
        timestamptz created_at
    }

    prescriptions {
        uuid id PK
        uuid practice_id FK
        uuid visit_id FK
        uuid patient_id FK
        uuid prescriber_id FK "veterinarian"
        varchar drug_name
        varchar dose
        varchar route
        varchar frequency
        int duration_days
        numeric quantity "what leaves the clinic"
        varchar unit "tablet|ml|bottle|..."
        bool controlled "per regulatory framework"
        enum status "issued|dispensed|cancelled"
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    prescription_events {
        uuid id PK "append-only"
        uuid practice_id FK
        uuid prescription_id FK
        enum type "issued|dispensed|cancelled"
        numeric quantity
        uuid by_user_id FK
        text notes
        timestamptz at
    }

    controlled_substance_log {
        uuid id PK "append-only"
        uuid practice_id FK
        enum action "receive|dispense|adjust"
        uuid prescription_event_id FK "unique; nullable, set only for dispense"
        uuid patient_id FK "nullable; dispense only"
        uuid prescriber_id FK "nullable; dispense only"
        varchar drug_name
        numeric quantity "signed: + receive, - dispense, +/- adjust"
        numeric balance_after "per (practice, drug_name); never below 0"
        varchar reference "supplier ref or adjust reason"
        uuid entered_by FK
        enum framework "EG"
        timestamptz at
    }

    attachments {
        uuid id PK
        uuid practice_id FK
        uuid visit_id FK
        varchar object_key "MinIO, prefixed by practice_id"
        varchar filename
        varchar content_type
        bigint size_bytes
        varchar sha256
        uuid uploaded_by FK
        timestamptz uploaded_at
    }

    recall_contacts {
        uuid id PK "append-only"
        uuid practice_id FK
        uuid patient_id FK
        uuid vaccination_id FK "nullable"
        uuid care_reminder_id FK "nullable; check exactly one of the two"
        uuid contacted_by FK
        enum channel "phone|whatsapp|in_person"
        enum outcome "reached|no_answer|wrong_number|declined|booked"
        uuid appointment_id FK "nullable, when booked"
        text note
        timestamptz contacted_at
    }

    appointments o|--o| visits : ""
    visits ||--o{ visit_vitals : ""
    visits ||--o| clinical_notes : ""
    clinical_notes ||--o{ clinical_note_addenda : ""
    clinical_notes o|--o| clinical_notes : "replaced_by"
    visits ||--o{ vaccinations : ""
    visits ||--o{ prescriptions : ""
    prescriptions ||--o{ prescription_events : ""
    prescription_events o|--o| controlled_substance_log : ""
    visits ||--o{ attachments : ""
    vaccinations o|--o{ recall_contacts : ""
    care_reminders o|--o{ recall_contacts : ""
    users ||--o{ controlled_substance_log : "entered_by"
    appointments o|--o{ recall_contacts : "booked"
```

---

## 5. Tenant zone — billing and audit

`audit_log` is created in part 04 and receives rows from every tenant table added afterwards; it is drawn here only because the billing tables are the last to feed it.

```mermaid
erDiagram
    invoices {
        uuid id PK
        uuid practice_id FK
        uuid client_id FK
        uuid visit_id FK "nullable"
        varchar number "YYYY-000123; null while draft; unique per practice"
        enum status "draft|issued|partially_paid|paid|void"
        char3 currency
        numeric subtotal
        numeric discount "amount, invoice-level, >= 0, <= subtotal"
        numeric tax "(subtotal - discount) * practice rate"
        numeric total "subtotal - discount + tax"
        date due_date "nullable"
        timestamptz issued_at
        timestamptz voided_at
        text void_reason
        int version
        timestamptz created_at
        timestamptz updated_at
    }

    invoice_counters {
        uuid id PK
        uuid practice_id FK
        int year "unique (practice_id, year)"
        int next_number "SELECT FOR UPDATE at issue"
    }

    invoice_lines {
        uuid id PK "trigger: frozen unless invoice draft"
        uuid practice_id FK
        uuid invoice_id FK
        uuid service_id FK "nullable; free-text line if null"
        varchar description "snapshot"
        numeric quantity
        numeric unit_price "snapshot"
        numeric line_total
    }

    payments {
        uuid id PK
        uuid practice_id FK
        uuid invoice_id FK
        numeric amount "negative = refund"
        char3 currency
        enum method "cash|instapay|vodafone_cash|card|bank_transfer"
        varchar external_reference "transfer number, nullable"
        varchar idempotency_key "unique per practice"
        uuid refund_of FK "payments, nullable"
        text refund_reason
        uuid received_by FK
        timestamptz received_at
        timestamptz created_at
    }

    audit_log {
        uuid id PK "append-only, written by trigger audit_row()"
        uuid practice_id FK
        uuid user_id FK "from app.user_id; null in system context"
        enum action "insert|update|delete"
        varchar table_name
        uuid row_id
        jsonb before "password_hash stripped"
        jsonb after "password_hash stripped"
        timestamptz at
    }

    invoices ||--o{ invoice_lines : ""
    services o|--o{ invoice_lines : ""
    invoices ||--o{ payments : ""
    payments o|--o{ payments : "refund_of"
    practices ||--o{ invoice_counters : ""
```

---

## 6. Table index by part

| Part | Tables added |
| --- | --- |
| 02 | `practices` (with `address`, `phone`, `vat_number`, `tax_rate_percent`, `trial_ends_at`) |
| 03 | `users` (with `is_veterinarian`, `license_number`, `session_version`, `must_change_password`) |
| 04 | `audit_log`, function `audit_row()`, RLS policies, roles `qvety_owner` / `qvety_app` |
| 05 | `users.locale` |
| 06 | `clients` |
| 07 | `clients.full_name_normalized`, `clients.phone_e164`, `clients.phone_secondary_e164`, `pg_trgm` |
| 08 | `patients` (with empty `photo_object_key`), `patient_weights`, `patient_allergies` |
| 09 | `rooms`, `appointment_types`, `services` |
| 10 | `appointments`, `practice_hours`, `btree_gist` |
| 11 | `platform_users`, `platform_settings`, `platform_audit_log`, `practices.closed_at` |
| 12 | `plans`, `plan_versions`, `subscriptions`, `subscription_invoices`, `subscription_payments` |
| 13 | `visits`, `visit_vitals`, `clinical_notes`, `clinical_note_addenda`, `vaccinations`, `prescriptions`, `prescription_events`, `controlled_substance_log`, `attachments`, `practices.logo_object_key` |
| 14 | `invoice_counters`, `invoices`, `invoice_lines`, `payments`, `appointment_types.default_service_id` |
| 15 | `care_reminders`, `recall_contacts`, `practice_settings` |

## 7. Rules encoded in the schema, not in code

| Rule | Mechanism |
| --- | --- |
| No cross-practice reads | RLS policy on every tenant table |
| No cross-practice links | Composite FK `(x_id, practice_id)` on every tenant-to-tenant reference |
| No double booking | `EXCLUDE USING gist (veterinarian_id WITH =, period WITH &&)` and same for `room_id`, partial on active statuses |
| Signed note never edited | Trigger on `clinical_notes` when `finalized_at IS NOT NULL` |
| Evidence never altered | Triggers rejecting `UPDATE`/`DELETE` on `*_events`, `*_log`, `*_addenda`, `*_contacts` for `qvety_app`; trigger functions `SECURITY DEFINER` with `EXECUTE` revoked |
| Every change attributed | `audit_row()` trigger on every tenant table, `app.user_id` from the transaction hook |
| Signer identity frozen | `finalized_by_name` snapshot; composite FK `(practice_id, finalized_by)` |
| Cross-practice link impossible in FK | Composite FKs also on `finalized_by`, `prescriber_id`, `veterinarian_id`, `attending_vet_id` to `users(practice_id, id)` |
| Technician visit cannot prescribe | `prescriptions.prescriber_id` not null; `vaccinations` check: `given_by` is a vet or `supervising_vet_id` is set (enforced by trigger reading `users.is_veterinarian`) |
| Only vets sign | Check on `users`: role `veterinarian` ⇒ `is_veterinarian`; `technician`/`front_desk` ⇒ not |
| Token revocation | `session_version` compared on every request |
| Issued invoice lines frozen | Trigger on `invoice_lines` checking parent status; same trigger rejects `discount` change on a non-draft invoice |
| Wrong clinical entry never edited | `voided_*`-only update triggers on `vaccinations`, `visit_vitals`; `retracted_*`-only on `patient_allergies` |
| Controlled stock never negative | Check `balance_after >= 0`; balance computed inside the insert transaction with a row lock per `(practice_id, drug_name)` |
| Discount never exceeds subtotal | Check `discount >= 0 AND discount <= subtotal` on `invoices` |
| Contact belongs to one thing | Check on `recall_contacts`: exactly one of `vaccination_id`, `care_reminder_id` |
| One payment per attempt | Unique `(practice_id, idempotency_key)` |
| Subscription and clinic money never mixed | Separate tables, separate zones, no FK between them |
| Unknown country rejected | Check constraint on `practices.country` |
| Practice cannot change its own status | Column-level `UPDATE` grant on `practices` for `qvety_app`: identity columns only |
| Invoice numbers gapless | `invoice_counters` row locked in the issue transaction; `number` set once, unique per practice |
