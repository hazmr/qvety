-- Dev seed. Opening hours and a clinic day for the dev practice. Local and test profiles only.
-- Weekday numbering matches Postgres EXTRACT(DOW): 0 = Sunday. Friday (5) has no row: the clinic is shut.
INSERT INTO practice_hours (id, practice_id, weekday, opens, closes) VALUES
    ('00000000-0000-7000-8000-000000000701', '00000000-0000-7000-8000-000000000001', 0, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000702', '00000000-0000-7000-8000-000000000001', 1, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000703', '00000000-0000-7000-8000-000000000001', 2, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000704', '00000000-0000-7000-8000-000000000001', 3, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000705', '00000000-0000-7000-8000-000000000001', 4, '10:00', '22:00'),
    ('00000000-0000-7000-8000-000000000706', '00000000-0000-7000-8000-000000000001', 6, '10:00', '22:00')
ON CONFLICT (id) DO UPDATE SET weekday = EXCLUDED.weekday, opens = EXCLUDED.opens, closes = EXCLUDED.closes;

-- A day for the schedule and the board. Times are anchored to the date this seed last ran, in the
-- practice timezone, so a fresh database (every Testcontainers run) has a full day of "today".
INSERT INTO appointments (id, practice_id, patient_id, client_id, veterinarian_id, room_id, appointment_type_id,
                          starts_at, ends_at, status, origin, checked_in_at, reason)
VALUES
    -- done: seen and finished
    ('00000000-0000-7000-8000-000000000801', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000401', '00000000-0000-7000-8000-000000000301',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000611',
     (current_date + time '10:00') AT TIME ZONE 'Africa/Cairo', (current_date + time '10:20') AT TIME ZONE 'Africa/Cairo',
     'completed', 'scheduled', (current_date + time '09:55') AT TIME ZONE 'Africa/Cairo', 'كشف دوري'),
    -- in the exam room now
    ('00000000-0000-7000-8000-000000000802', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000403', '00000000-0000-7000-8000-000000000302',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000613',
     (current_date + time '10:30') AT TIME ZONE 'Africa/Cairo', (current_date + time '10:45') AT TIME ZONE 'Africa/Cairo',
     'in_progress', 'scheduled', (current_date + time '10:25') AT TIME ZONE 'Africa/Cairo', 'تطعيم سنوي'),
    -- waiting at the desk
    ('00000000-0000-7000-8000-000000000803', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000404', '00000000-0000-7000-8000-000000000303',
     '00000000-0000-7000-8000-000000000101', '00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000611',
     (current_date + time '11:00') AT TIME ZONE 'Africa/Cairo', (current_date + time '11:20') AT TIME ZONE 'Africa/Cairo',
     'checked_in', 'scheduled', (current_date + time '10:50') AT TIME ZONE 'Africa/Cairo', 'Limping on the left front leg'),
    -- a walk-in, waiting
    ('00000000-0000-7000-8000-000000000804', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000406', '00000000-0000-7000-8000-000000000304',
     '00000000-0000-7000-8000-000000000101', '00000000-0000-7000-8000-000000000603', '00000000-0000-7000-8000-000000000611',
     (current_date + time '11:30') AT TIME ZONE 'Africa/Cairo', (current_date + time '11:50') AT TIME ZONE 'Africa/Cairo',
     'checked_in', 'walk_in', (current_date + time '11:28') AT TIME ZONE 'Africa/Cairo', 'Walk-in, not eating'),
    -- still to come
    ('00000000-0000-7000-8000-000000000805', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000402', '00000000-0000-7000-8000-000000000301',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000614',
     (current_date + time '15:00') AT TIME ZONE 'Africa/Cairo', (current_date + time '16:00') AT TIME ZONE 'Africa/Cairo',
     'scheduled', 'scheduled', NULL, 'جراحة صغرى'),
    -- cancelled: keeps its row and frees its slot
    ('00000000-0000-7000-8000-000000000806', '00000000-0000-7000-8000-000000000001',
     '00000000-0000-7000-8000-000000000405', '00000000-0000-7000-8000-000000000303',
     '00000000-0000-7000-8000-000000000102', '00000000-0000-7000-8000-000000000601', '00000000-0000-7000-8000-000000000611',
     (current_date + time '15:00') AT TIME ZONE 'Africa/Cairo', (current_date + time '15:20') AT TIME ZONE 'Africa/Cairo',
     'cancelled', 'scheduled', NULL, 'Owner cancelled')
ON CONFLICT (id) DO UPDATE SET
    starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, status = EXCLUDED.status,
    checked_in_at = EXCLUDED.checked_in_at, reason = EXCLUDED.reason;
