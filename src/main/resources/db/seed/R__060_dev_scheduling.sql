-- Dev seed. Opening hours and a clinic day for the dev practice. Local and test profiles only.
-- Weekday numbering matches Postgres EXTRACT(DOW): 0 = Sunday. Friday (5) has no row: the clinic is shut.
-- The practice's local date. A dev-seed helper only; the application asks the practice row for its zone.
CREATE OR REPLACE FUNCTION clinic_date() RETURNS date
    LANGUAGE sql STABLE AS $$ SELECT (now() AT TIME ZONE 'Africa/Cairo')::date $$;

INSERT INTO practice_hours (id, practice_id, weekday, opens, closes) VALUES
    ('00000000-0000-7000-8000-000000000701', '00000000-0000-7000-8000-000000000001', 0, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000702', '00000000-0000-7000-8000-000000000001', 1, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000703', '00000000-0000-7000-8000-000000000001', 2, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000704', '00000000-0000-7000-8000-000000000001', 3, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000705', '00000000-0000-7000-8000-000000000001', 4, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000706', '00000000-0000-7000-8000-000000000001', 6, '10:00', '22:00')
ON CONFLICT (id) DO UPDATE SET weekday = EXCLUDED.weekday, opens = EXCLUDED.opens, closes = EXCLUDED.closes;

-- A day for the schedule and the board. Anchored to the practice's own date, not the server's: around
-- midnight in Cairo the two differ by up to three hours, and the board asks "what is today here?".
-- The anchor is the day this seed last ran, so a fresh database (every Testcontainers run) gets a full
-- day of "today"; a long-lived dev database drifts and is refreshed by re-running the seed.
INSERT INTO appointments (id, practice_id, patient_id, client_id, veterinarian_id, room_id, appointment_type_id,
                          starts_at, ends_at, status, origin, checked_in_at, reason)
VALUES
    -- done: seen and finished
    ('00000000-0000-7000-8000-000000000801', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000401', '00000000-0000-7000-8000-000000000301',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000611',
     (clinic_date() + time '10:00') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '10:20') AT TIME ZONE 'Africa/Cairo',
     'completed', 'scheduled', (clinic_date() + time '09:55') AT TIME ZONE 'Africa/Cairo', 'كشف دوري'),
    -- in the exam room now
    ('00000000-0000-7000-8000-000000000802', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000403', '00000000-0000-7000-8000-000000000302',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000613',
     (clinic_date() + time '10:30') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '10:45') AT TIME ZONE 'Africa/Cairo',
     'in_progress', 'scheduled', (clinic_date() + time '10:25') AT TIME ZONE 'Africa/Cairo', 'تطعيم سنوي'),
    -- waiting at the desk
    ('00000000-0000-7000-8000-000000000803', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000404', '00000000-0000-7000-8000-000000000303',
     '00000000-0000-7000-8000-000000000101', '00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000611',
     (clinic_date() + time '11:00') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '11:20') AT TIME ZONE 'Africa/Cairo',
     'checked_in', 'scheduled', (clinic_date() + time '10:50') AT TIME ZONE 'Africa/Cairo', 'Limping on the left front leg'),
    -- a walk-in, waiting
    ('00000000-0000-7000-8000-000000000804', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000406', '00000000-0000-7000-8000-000000000304',
     '00000000-0000-7000-8000-000000000101', '00000000-0000-7000-8000-000000000603', '00000000-0000-7000-8000-000000000611',
     (clinic_date() + time '11:30') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '11:50') AT TIME ZONE 'Africa/Cairo',
     'checked_in', 'walk_in', (clinic_date() + time '11:28') AT TIME ZONE 'Africa/Cairo', 'Walk-in, not eating'),
    -- still to come
    ('00000000-0000-7000-8000-000000000805', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000402', '00000000-0000-7000-8000-000000000301',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000614',
     (clinic_date() + time '15:00') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '16:00') AT TIME ZONE 'Africa/Cairo',
     'scheduled', 'scheduled', NULL, 'جراحة صغرى'),
    -- cancelled: keeps its row and frees its slot
    ('00000000-0000-7000-8000-000000000806', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000405', '00000000-0000-7000-8000-000000000303',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000611',
     (clinic_date() + time '15:00') AT TIME ZONE 'Africa/Cairo', (clinic_date() + time '15:20') AT TIME ZONE 'Africa/Cairo',
     'cancelled', 'scheduled', NULL, 'Owner cancelled')
ON CONFLICT (id) DO UPDATE SET
    starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, status = EXCLUDED.status,
    checked_in_at = EXCLUDED.checked_in_at, reason = EXCLUDED.reason;

DROP FUNCTION clinic_date();
