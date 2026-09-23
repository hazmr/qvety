-- Dev seed. One Qvety staff account for the /admin area. Local and test profiles only.
-- Password: password123 (bcrypt, cost 12), the same as every seeded practice user.
INSERT INTO platform_users (id, email, password_hash, full_name, active)
VALUES (
    '00000000-0000-7000-8000-000000000901',
    'staff@qvety.example.com',
    '$2a$12$tRrkJgHmsapCuGvMgs1yT.kiQJf.56xF7z9UuX6b517QZJL7UY0E2',
    'Qvety Staff',
    true
)
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    active = EXCLUDED.active;
