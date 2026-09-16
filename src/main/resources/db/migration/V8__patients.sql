-- Part 08: patients (the animal), weight history, allergies. Introduces two patterns reused later:
-- the same-practice composite foreign key, and the first "retract, never edit" table guarded by a trigger.

CREATE TYPE species          AS ENUM ('dog', 'cat', 'bird', 'rabbit', 'rodent', 'reptile', 'horse', 'livestock', 'other');
CREATE TYPE patient_sex      AS ENUM ('male', 'female', 'male_neutered', 'female_spayed', 'unknown');
CREATE TYPE allergy_severity AS ENUM ('mild', 'moderate', 'severe');

CREATE TABLE patients (
    id                 uuid        PRIMARY KEY DEFAULT uuidv7(),
    practice_id        uuid        NOT NULL REFERENCES practices (id),
    client_id          uuid        NOT NULL,
    previous_client_id uuid,
    name               text        NOT NULL,
    species            species     NOT NULL,
    breed              text,
    sex                patient_sex NOT NULL DEFAULT 'unknown',
    date_of_birth      date,
    age_approximate    text,
    color              text,
    microchip          text,
    photo_object_key   text,
    deceased_at        date,
    notes              text,
    version            bigint      NOT NULL DEFAULT 0,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),

    -- Needed by the composite foreign keys of weights, allergies, and every later clinical table.
    CONSTRAINT patients_id_practice UNIQUE (id, practice_id),
    -- The client must belong to the same practice. RLS filters what a session sees; this makes a
    -- guessed id from another practice impossible to link even without RLS.
    CONSTRAINT patients_client_same_practice          FOREIGN KEY (client_id, practice_id)          REFERENCES clients (id, practice_id),
    CONSTRAINT patients_previous_client_same_practice FOREIGN KEY (previous_client_id, practice_id) REFERENCES clients (id, practice_id)
);

CREATE INDEX patients_practice_client_idx  ON patients (practice_id, client_id);
CREATE INDEX patients_practice_species_idx ON patients (practice_id, species);
CREATE INDEX patients_practice_name_idx    ON patients (practice_id, lower(name));

COMMENT ON TABLE  patients                    IS 'The animal. Belongs to one client of the same practice; transferred, marked deceased, never deleted.';
COMMENT ON COLUMN patients.previous_client_id IS 'Set on transfer; history stays with the patient.';
COMMENT ON COLUMN patients.age_approximate    IS 'Free text when date_of_birth is unknown, e.g. "about 3 years".';
COMMENT ON COLUMN patients.photo_object_key   IS 'MinIO object key; written from part 13.';
COMMENT ON COLUMN patients.deceased_at        IS 'Date of death; a deceased patient leaves recalls and default lists.';

CREATE TABLE patient_weights (
    id          uuid          PRIMARY KEY DEFAULT uuidv7(),
    practice_id uuid          NOT NULL REFERENCES practices (id),
    patient_id  uuid          NOT NULL,
    measured_at timestamptz   NOT NULL,
    weight_kg   numeric(6, 2) NOT NULL,
    voided_at   timestamptz,
    void_reason text,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT patient_weights_positive              CHECK (weight_kg > 0),
    -- A void always carries its reason.
    CONSTRAINT patient_weights_void_reason           CHECK ((voided_at IS NULL) = (void_reason IS NULL)),
    CONSTRAINT patient_weights_patient_same_practice FOREIGN KEY (patient_id, practice_id) REFERENCES patients (id, practice_id)
);

CREATE INDEX patient_weights_patient_idx ON patient_weights (patient_id, measured_at DESC);

COMMENT ON TABLE  patient_weights           IS 'Dated weights in kilograms; the latest non-voided row is the header value. A measurement is voided with a reason, never edited or deleted.';
COMMENT ON COLUMN patient_weights.voided_at IS 'Set by void; the trigger below allows no other change. Part 13 vitals write here and use the same word.';

-- Only voided_at and void_reason (and updated_at, written by the ORM) may change; delete is refused.
-- Same shape as patient_allergy_guard below; an allergy is a belief and is retracted, a weight is a measurement and is voided.
CREATE FUNCTION patient_weight_guard() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    mutable_columns text[] := ARRAY['voided_at', 'void_reason', 'updated_at'];
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'patient_weights: rows are never deleted, void instead'
            USING ERRCODE = 'check_violation';
    END IF;
    IF (to_jsonb(NEW) - mutable_columns) IS DISTINCT FROM (to_jsonb(OLD) - mutable_columns) THEN
        RAISE EXCEPTION 'patient_weights: only voided_at and void_reason may change'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION patient_weight_guard() FROM PUBLIC, qvety_app;

CREATE TRIGGER void_only BEFORE UPDATE OR DELETE ON patient_weights
    FOR EACH ROW EXECUTE FUNCTION patient_weight_guard();

CREATE TABLE patient_allergies (
    id               uuid             PRIMARY KEY DEFAULT uuidv7(),
    practice_id      uuid             NOT NULL REFERENCES practices (id),
    patient_id       uuid             NOT NULL,
    substance        text             NOT NULL,
    reaction         text,
    severity         allergy_severity NOT NULL,
    noted_by         uuid             NOT NULL,
    retracted_at     timestamptz,
    retracted_reason text,
    created_at       timestamptz      NOT NULL DEFAULT now(),
    updated_at       timestamptz      NOT NULL DEFAULT now(),

    -- A retraction always carries its reason.
    CONSTRAINT patient_allergies_retract_reason      CHECK ((retracted_at IS NULL) = (retracted_reason IS NULL)),
    CONSTRAINT patient_allergies_patient_same_practice FOREIGN KEY (patient_id, practice_id) REFERENCES patients (id, practice_id),
    CONSTRAINT patient_allergies_noted_by_same_practice FOREIGN KEY (noted_by, practice_id)   REFERENCES users (id, practice_id)
);

CREATE INDEX patient_allergies_patient_idx ON patient_allergies (patient_id);

COMMENT ON TABLE  patient_allergies              IS 'Never edited or deleted; a wrong entry is retracted with a reason and stays visible.';
COMMENT ON COLUMN patient_allergies.retracted_at IS 'Set by retract; the trigger below allows no other change.';

-- Only retracted_at and retracted_reason (and updated_at, written by the ORM) may change; delete is refused.
-- SECURITY DEFINER, owned by qvety_owner, EXECUTE revoked from qvety_app: the app cannot call or drop it.
-- Part 13 reuses this shape for voided_* columns on vaccinations and visit_vitals.
CREATE FUNCTION patient_allergy_guard() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    mutable_columns text[] := ARRAY['retracted_at', 'retracted_reason', 'updated_at'];
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'patient_allergies: rows are never deleted, retract instead'
            USING ERRCODE = 'check_violation';
    END IF;
    IF (to_jsonb(NEW) - mutable_columns) IS DISTINCT FROM (to_jsonb(OLD) - mutable_columns) THEN
        RAISE EXCEPTION 'patient_allergies: only retracted_at and retracted_reason may change'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION patient_allergy_guard() FROM PUBLIC, qvety_app;

CREATE TRIGGER retract_only BEFORE UPDATE OR DELETE ON patient_allergies
    FOR EACH ROW EXECUTE FUNCTION patient_allergy_guard();

-- RLS policy, grants, and audit trigger in one call (part 04).
SELECT tenant_table_setup('patients');
SELECT tenant_table_setup('patient_weights');
SELECT tenant_table_setup('patient_allergies');
