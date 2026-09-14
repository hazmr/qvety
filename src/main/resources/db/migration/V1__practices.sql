-- Part 02: the practice (tenant root). Platform table: no practice_id, no RLS.
-- Grants for qvety_app and the self-read policy arrive in V3 (part 04).

CREATE TYPE practice_status AS ENUM ('trial', 'active', 'past_due', 'suspended', 'closed');

CREATE TABLE practices (
    id               uuid            PRIMARY KEY DEFAULT uuidv7(),
    name             text            NOT NULL,
    country          char(2)         NOT NULL,
    currency         char(3)         NOT NULL,
    locale           text            NOT NULL,
    timezone         text            NOT NULL,
    status           practice_status NOT NULL DEFAULT 'trial',
    address          text,
    phone            text,
    vat_number       text,
    tax_rate_percent numeric(5,2)    NOT NULL DEFAULT 0,
    trial_ends_at    timestamptz,
    created_at       timestamptz     NOT NULL DEFAULT now(),
    updated_at       timestamptz     NOT NULL DEFAULT now(),

    -- Supported countries only. Adding one is a migration plus a catalog file (part 09).
    CONSTRAINT practices_country_supported CHECK (country IN ('EG')),
    CONSTRAINT practices_tax_rate_range    CHECK (tax_rate_percent >= 0 AND tax_rate_percent <= 100)
);

COMMENT ON TABLE  practices                  IS 'One clinic business. The tenant. Platform zone.';
COMMENT ON COLUMN practices.country          IS 'ISO 3166-1 alpha-2. Decides currency, locale, timezone, regulatory framework.';
COMMENT ON COLUMN practices.vat_number       IS 'Printed on invoices when set. Most small clinics are not registered.';
COMMENT ON COLUMN practices.tax_rate_percent IS 'Applied on invoices (part 14). 0 for clinics not registered for VAT.';
COMMENT ON COLUMN practices.trial_ends_at    IS 'Set at creation from platform_settings.trial_days (part 11).';
