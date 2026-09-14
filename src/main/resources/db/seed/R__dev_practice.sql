-- Dev seed. Loaded only by the local and test profiles; never in production.
-- Fixed uuid so later seeds (users, clients, ...) can reference this practice.
INSERT INTO practices (id, name, country, currency, locale, timezone, status, address, phone, tax_rate_percent, trial_ends_at)
VALUES (
    '00000000-0000-7000-8000-000000000001',
    'Neighborhood Vet',
    'EG', 'EGP', 'ar-EG', 'Africa/Cairo',
    'trial',
    '12 Example St, Nasr City, Cairo',
    '+201000000001',
    0,
    now() + interval '30 days'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    address = EXCLUDED.address,
    phone = EXCLUDED.phone;
