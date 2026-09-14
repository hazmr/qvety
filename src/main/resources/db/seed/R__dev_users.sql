-- Dev seed. One user per role for the dev practice. Password for all: password123 (bcrypt, cost 12).
-- Local and test profiles only; the seed folder never reaches production.
INSERT INTO users (id, practice_id, email, password_hash, full_name, role, is_veterinarian, license_number, phone)
VALUES
    ('00000000-0000-7000-8000-000000000101', '00000000-0000-7000-8000-000000000001', 'admin@clinic.example.com',
     '$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2', 'د. سارة محمود', 'admin', true, 'VS-12345', '+201000000101'),
    ('00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000001', 'vet@clinic.example.com',
     '$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2', 'د. أحمد علي', 'veterinarian', true, 'VS-23456', '+201000000102'),
    ('00000000-0000-7000-8000-000000000103', '00000000-0000-7000-8000-000000000001', 'tech@clinic.example.com',
     '$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2', 'محمد حسن', 'technician', false, NULL, '+201000000103'),
    ('00000000-0000-7000-8000-000000000104', '00000000-0000-7000-8000-000000000001', 'desk@clinic.example.com',
     '$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2', 'Mona Adel', 'front_desk', false, NULL, '+201000000104')
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    is_veterinarian = EXCLUDED.is_veterinarian,
    license_number = EXCLUDED.license_number;
