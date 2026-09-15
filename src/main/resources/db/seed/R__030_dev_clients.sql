-- Dev seed. Synthetic clients for the dev practice, Arabic and English names. Fixed uuids for later seeds.
-- The folded columns are written by hand here (TextNormalizer rules); the application rewrites them on save.
INSERT INTO clients (id, practice_id, full_name, full_name_normalized, preferred_name, phone, phone_e164, phone_secondary, phone_secondary_e164, email, address, preferred_locale)
VALUES
    ('00000000-0000-7000-8000-000000000301', '00000000-0000-7000-8000-000000000001', 'أحمد محمد علي حسن', 'احمد محمد علي حسن', 'أحمد', '+201011111101', '+201011111101', NULL, NULL, NULL, 'مدينة نصر، القاهرة', 'ar-EG'),
    ('00000000-0000-7000-8000-000000000302', '00000000-0000-7000-8000-000000000001', 'فاطمة السيد إبراهيم', 'فاطمه السيد ابراهيم', 'فاطمة', '+201011111102', '+201011111102', '+201011111112', '+201011111112', 'fatma@clients.example.com', 'المعادي، القاهرة', 'ar-EG'),
    ('00000000-0000-7000-8000-000000000303', '00000000-0000-7000-8000-000000000001', 'Sarah Thompson', 'sarah thompson', 'Sarah', '+201011111103', '+201011111103', NULL, NULL, 'sarah@clients.example.com', 'Zamalek, Cairo', 'en-EG'),
    ('00000000-0000-7000-8000-000000000304', '00000000-0000-7000-8000-000000000001', 'Omar Khaled', 'omar khaled', NULL, NULL, NULL, NULL, NULL, 'omar@clients.example.com', 'Heliopolis, Cairo', 'en-EG'),
    ('00000000-0000-7000-8000-000000000305', '00000000-0000-7000-8000-000000000001', 'مريم عبد الرحمن', 'مريم عبد الرحمن', 'مريم', '+201011111105', '+201011111105', NULL, NULL, NULL, 'الدقي، الجيزة', 'ar-EG')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    full_name_normalized = EXCLUDED.full_name_normalized,
    preferred_name = EXCLUDED.preferred_name,
    phone = EXCLUDED.phone,
    phone_e164 = EXCLUDED.phone_e164,
    phone_secondary = EXCLUDED.phone_secondary,
    phone_secondary_e164 = EXCLUDED.phone_secondary_e164,
    email = EXCLUDED.email,
    address = EXCLUDED.address;
