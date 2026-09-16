## Migration `V7__patients.sql`

```sql
CREATE TYPE species AS ENUM ('dog','cat','bird','rabbit','rodent','reptile','horse','livestock','other');
CREATE TYPE patient_sex AS ENUM ('male','female','male_neutered','female_spayed','unknown');
CREATE TYPE allergy_severity AS ENUM ('mild','moderate','severe');

CREATE TABLE patients (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  client_id uuid NOT NULL,
  previous_client_id uuid,
  name text NOT NULL,
  species species NOT NULL,
  breed text,
  sex patient_sex NOT NULL DEFAULT 'unknown',
  date_of_birth date,
  age_approximate text,
  color text,
  microchip text,
  photo_object_key text,
  deceased_at date,
  notes text,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (id, practice_id),
  FOREIGN KEY (client_id, practice_id) REFERENCES clients(id, practice_id)
);

CREATE TABLE patient_weights (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  patient_id uuid NOT NULL,
  measured_at timestamptz NOT NULL,
  weight_kg numeric(6,2) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (patient_id, practice_id) REFERENCES patients(id, practice_id)
);

CREATE TABLE patient_allergies (
  id uuid PRIMARY KEY DEFAULT uuidv7(),
  practice_id uuid NOT NULL REFERENCES practices(id),
  patient_id uuid NOT NULL,
  substance text NOT NULL,
  reaction text,
  severity allergy_severity NOT NULL,
  noted_by uuid NOT NULL,
  retracted_at timestamptz,
  retracted_reason text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (patient_id, practice_id) REFERENCES patients(id, practice_id)
);
```

- Trigger on `patient_allergies` `BEFORE UPDATE`: raise unless only `retracted_at`, `retracted_reason`, `updated_at` differ; `BEFORE DELETE`: raise. Function `SECURITY DEFINER`, owner `qvety_owner`, `EXECUTE` revoked from `qvety_app` (pattern reused in part 13).
- Append all three tables to the RLS and audit lists.
- The composite FK prevents what RLS cannot: RLS filters what a session sees, but an insert with a foreign id from another practice would otherwise pass if the id is guessed. The FK makes the link impossible.
- `ALTER TYPE ... ADD VALUE` cannot run inside a transaction block; a species addition migration needs Flyway's non-transactional mode for that statement.

## API

| Method | Path |
| --- | --- |
| GET | `/api/v1/patients?clientId&species&deceased&q&page&size&sort` |
| GET | `/api/v1/patients/{id}` |
| POST | `/api/v1/patients` |
| PUT | `/api/v1/patients/{id}` |
| POST | `/api/v1/patients/{id}/deceased` `{date}` |
| POST | `/api/v1/patients/{id}/transfer` `{clientId}` |
| GET / POST | `/api/v1/patients/{id}/weights` |
| GET / POST | `/api/v1/patients/{id}/allergies` |
| POST | `/api/v1/patients/{id}/allergies/{allergyId}/retract` `{reason}` |
| POST | `/api/v1/patients/{id}/weights/{weightId}/void` `{reason}` |

## Backend

- `@ManyToOne(fetch = LAZY)` everywhere; `hibernate.default_batch_fetch_size` set. EAGER would pull the client (and its practice) on every patient read.
- Enums mapped as Postgres enums (`@JdbcType(PostgreSQLEnumJdbcType.class)`); decision recorded: matches the migration type, no string column drift.
- `JpaSpecificationExecutor<Patient>` for the list filters.
- `PatientService.transfer` written by hand: validate target client is same practice, record `previous_client_id`. A deceased patient is refused with 409 `patient.transfer_deceased`: the animal stays with the owner who lost it.
- `patient_weights` gains `voided_at`, `void_reason` (check: both null or both set) and a `patient_weight_guard` trigger with the same body as `patient_allergy_guard`, mutable columns `voided_at`, `void_reason`, `updated_at`. Word choice: an allergy is a belief, so it is *retracted*; a weight is a measurement, so it is *voided*, the word part 13 uses for vitals. `latestWeight` on the header skips voided rows; the list returns them with `voidedAt` and `voidReason`.

## Angular

Patients tab on client detail; patient detail at `/clients/:id/patients/:pid` with tabs Summary, Weights, Allergies, Visits (empty until part 13). Allergy banner in the patient header. Species labels from i18n. Arabic sex labels use معقّم / معقّمة for both sexes; مخصي reads harshly in Egyptian usage.

Actions sit next to what they act on, so a button is never mistaken for a tab action:
- Client detail: Edit is the title-bar action; Archive beside it on desktop, in the phone action bar.
- Patient detail: Edit is the title-bar action; Mark deceased beside it on desktop, in the phone action bar; Transfer is a small button on the owner row, hidden once deceased, and its client picker opens under that row. Nothing actionable inside the Summary tab.
- Weights tab: each non-voided row has a Void link that asks for a reason; voided rows are struck through with the reason on the secondary line.

## Phone layout

Reference: `docs/design/MOBILE.md screen 10 (patient record: title bar with back chevron, identity block, scrollable tab strip, timeline, action bar)`. Phone first (below 768 px): stacked rows, one scroller, bottom nav on top-level screens, action bar on detail screens; desktop layout above 768 px.
