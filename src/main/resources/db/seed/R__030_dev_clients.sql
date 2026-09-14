-- Dev seed. Synthetic clients for the dev practice, Arabic and English names. Fixed uuids for later seeds.
INSERT INTO clients (id, practice_id, full_name, preferred_name, phone, phone_secondary, email, address, preferred_locale)
VALUES
    ('00000000-0000-7000-8000-000000000301', '00000000-0000-7000-8000-000000000001', 'أحمد محمد علي حسن', 'أحمد', '+201011111101', NULL, NULL, 'مدينة نصر، القاهرة', 'ar-EG'),
    ('00000000-0000-7000-8000-000000000302', '00000000-0000-7000-8000-000000000001', 'فاطمة السيد إبراهيم', 'فاطمة', '+201011111102', '+201011111112', 'fatma@clients.example.com', 'المعادي، القاهرة', 'ar-EG'),
    ('00000000-0000-7000-8000-000000000303', '00000000-0000-7000-8000-000000000001', 'Sarah Thompson', 'Sarah', '+201011111103', NULL, 'sarah@clients.example.com', 'Zamalek, Cairo', 'en-EG'),
    ('00000000-0000-7000-8000-000000000304', '00000000-0000-7000-8000-000000000001', 'Omar Khaled', NULL, NULL, NULL, 'omar@clients.example.com', 'Heliopolis, Cairo', 'en-EG'),
    ('00000000-0000-7000-8000-000000000305', '00000000-0000-7000-8000-000000000001', 'مريم عبد الرحمن', 'مريم', '+201011111105', NULL, NULL, 'الدقي، الجيزة', 'ar-EG')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    preferred_name = EXCLUDED.preferred_name,
    phone = EXCLUDED.phone,
    phone_secondary = EXCLUDED.phone_secondary,
    email = EXCLUDED.email,
    address = EXCLUDED.address;
