-- Dev seed. Synthetic patients for the seeded clients, with a few weights and one allergy. Fixed uuids
-- for later seeds. Weights and allergies are insert-only here: the allergy trigger refuses any other change.
INSERT INTO patients (id, practice_id, client_id, name, species, breed, sex, date_of_birth, age_approximate, color, microchip, deceased_at, notes)
VALUES
    ('00000000-0000-7000-8000-000000000401', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000301', 'بسبس',   'cat',    'شيرازي',          'male_neutered', '2021-03-15', NULL,            'أبيض',        NULL,              NULL,         NULL),
    ('00000000-0000-7000-8000-000000000402', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000301', 'ريكس',   'dog',    'جيرمن شيبرد',      'male',          NULL,         'حوالي 4 سنوات', 'أسود وبني',   '900012345678901', NULL,         NULL),
    ('00000000-0000-7000-8000-000000000403', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000302', 'لولو',   'cat',    'بلدي',             'female_spayed', '2019-07-01', NULL,            'رمادي',       NULL,              NULL,         'تخاف من الأصوات العالية'),
    ('00000000-0000-7000-8000-000000000404', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000303', 'Max',    'dog',    'Golden Retriever', 'male',          '2020-11-20', NULL,            'golden',      '900098765432109', NULL,         NULL),
    ('00000000-0000-7000-8000-000000000405', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000303', 'Kiwi',   'bird',   'Budgerigar',       'unknown',       NULL,         'about 2 years', 'green',       NULL,              NULL,         NULL),
    ('00000000-0000-7000-8000-000000000406', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000304', 'Bunny',  'rabbit', NULL,               'female',        '2023-02-10', NULL,            'white',       NULL,              NULL,         NULL),
    ('00000000-0000-7000-8000-000000000407', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000305', 'سيمبا',  'cat',    'سيامي',            'male',          '2015-05-05', NULL,            'كريمي',       NULL,              '2026-01-12', 'توفي بعد فشل كلوي')
ON CONFLICT (id) DO UPDATE SET
    client_id = EXCLUDED.client_id,
    name = EXCLUDED.name,
    species = EXCLUDED.species,
    breed = EXCLUDED.breed,
    sex = EXCLUDED.sex,
    date_of_birth = EXCLUDED.date_of_birth,
    age_approximate = EXCLUDED.age_approximate,
    color = EXCLUDED.color,
    microchip = EXCLUDED.microchip,
    deceased_at = EXCLUDED.deceased_at,
    notes = EXCLUDED.notes;

INSERT INTO patient_weights (id, practice_id, patient_id, measured_at, weight_kg)
VALUES
    ('00000000-0000-7000-8000-000000000501', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000401', '2026-03-02 10:15+02', 4.20),
    ('00000000-0000-7000-8000-000000000502', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000401', '2026-08-18 11:00+03', 4.65),
    ('00000000-0000-7000-8000-000000000503', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000402', '2026-06-10 09:30+03', 31.50),
    ('00000000-0000-7000-8000-000000000504', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000404', '2026-07-22 16:45+03', 29.80)
ON CONFLICT (id) DO NOTHING;

INSERT INTO patient_allergies (id, practice_id, patient_id, substance, reaction, severity, noted_by)
VALUES
    ('00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000402', 'Penicillin', 'تورم في الوجه وحكة', 'severe',   '00000000-0000-7000-8000-000000000102'),
    ('00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000001', '00000000-0000-7000-8000-000000000404', 'Chicken',    'itching, ear infections', 'moderate', '00000000-0000-7000-8000-000000000102')
ON CONFLICT (id) DO NOTHING;
