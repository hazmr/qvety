-- Part 05: per-user language. Arabic by default; the UI direction follows it.
ALTER TABLE users ADD COLUMN locale text NOT NULL DEFAULT 'ar-EG';
ALTER TABLE users ADD CONSTRAINT users_locale_supported CHECK (locale IN ('ar-EG', 'en-EG'));

COMMENT ON COLUMN users.locale IS 'UI language and number/date format for this user. ar-EG (RTL) or en-EG (LTR).';
