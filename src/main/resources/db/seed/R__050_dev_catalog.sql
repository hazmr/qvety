-- Dev seed. The same rows catalog/eg.sql gives a new Egyptian practice, for the dev practice, with fixed
-- uuids so later seeds and tests can name them. Local and test profiles only. Until part 11 wires
-- StarterCatalogSeeder into practice creation, this is what puts reference data on a developer machine.
-- One row of each table starts inactive, so the "show inactive" switch has something to show.

INSERT INTO rooms (id, practice_id, name, active) VALUES
    ('00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000001', 'غرفة الكشف 1', true),
    ('00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000001', 'غرفة الكشف 2', true),
    ('00000000-0000-7000-8000-000000000603', '00000000-0000-7000-8000-000000000001', 'غرفة العمليات', true),
    ('00000000-0000-7000-8000-000000000604', '00000000-0000-7000-8000-000000000001', 'غرفة الحجز',   false)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, active = EXCLUDED.active;

INSERT INTO appointment_types (id, practice_id, name, duration_minutes, color, active) VALUES
    ('00000000-0000-7000-8000-000000000611', '00000000-0000-7000-8000-000000000001', 'كشف',        20, '#1F6F5C', true),
    ('00000000-0000-7000-8000-000000000612', '00000000-0000-7000-8000-000000000001', 'إعادة كشف',  15, '#3D8B7A', true),
    ('00000000-0000-7000-8000-000000000613', '00000000-0000-7000-8000-000000000001', 'تطعيم',      15, '#C89B3C', true),
    ('00000000-0000-7000-8000-000000000614', '00000000-0000-7000-8000-000000000001', 'جراحة صغرى', 60, '#B4553C', true),
    ('00000000-0000-7000-8000-000000000615', '00000000-0000-7000-8000-000000000001', 'استشارة',    30, '#4A6FA5', false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, duration_minutes = EXCLUDED.duration_minutes, color = EXCLUDED.color, active = EXCLUDED.active;

INSERT INTO services (id, practice_id, name, price, currency, active) VALUES
    ('00000000-0000-7000-8000-000000000621', '00000000-0000-7000-8000-000000000001', 'كشف',           200.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000622', '00000000-0000-7000-8000-000000000001', 'إعادة كشف',     100.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000623', '00000000-0000-7000-8000-000000000001', 'تطعيم سعار',    250.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000624', '00000000-0000-7000-8000-000000000001', 'تطعيم ثلاثي',   350.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000625', '00000000-0000-7000-8000-000000000001', 'جراحة صغرى',   1500.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000626', '00000000-0000-7000-8000-000000000001', 'تحليل دم شامل', 400.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000627', '00000000-0000-7000-8000-000000000001', 'أشعة',          300.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000628', '00000000-0000-7000-8000-000000000001', 'حلاقة وتنظيف',  150.00, 'EGP', true),
    ('00000000-0000-7000-8000-000000000629', '00000000-0000-7000-8000-000000000001', 'مبيت ليلة',     200.00, 'EGP', false)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, price = EXCLUDED.price, currency = EXCLUDED.currency, active = EXCLUDED.active;
