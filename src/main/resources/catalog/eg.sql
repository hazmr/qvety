-- Egyptian starter catalog. Applied once per practice by StarterCatalogSeeder inside the
-- practice-creation transaction (wired in part 11). Data, not code: a clinic's opening lists change
-- without a Java release. One file per country; a country with no file fails practice creation.
--
-- :practice_id is bound by the seeder. Prices are opening suggestions in EGP; the clinic edits them
-- on the settings screen. Names are Arabic because the practice locale is ar-EG.

INSERT INTO rooms (practice_id, name) VALUES
    (:practice_id, 'غرفة الكشف 1'),
    (:practice_id, 'غرفة الكشف 2'),
    (:practice_id, 'غرفة العمليات'),
    (:practice_id, 'غرفة الحجز');

INSERT INTO appointment_types (practice_id, name, duration_minutes, color) VALUES
    (:practice_id, 'كشف',          20, '#1F6F5C'),
    (:practice_id, 'إعادة كشف',    15, '#3D8B7A'),
    (:practice_id, 'تطعيم',        15, '#C89B3C'),
    (:practice_id, 'جراحة صغرى',   60, '#B4553C'),
    (:practice_id, 'استشارة',      30, '#4A6FA5');

INSERT INTO services (practice_id, name, price, currency) VALUES
    (:practice_id, 'كشف',              200.00, 'EGP'),
    (:practice_id, 'إعادة كشف',        100.00, 'EGP'),
    (:practice_id, 'تطعيم سعار',       250.00, 'EGP'),
    (:practice_id, 'تطعيم ثلاثي',      350.00, 'EGP'),
    (:practice_id, 'جراحة صغرى',      1500.00, 'EGP'),
    (:practice_id, 'تحليل دم شامل',    400.00, 'EGP'),
    (:practice_id, 'أشعة',             300.00, 'EGP'),
    (:practice_id, 'حلاقة وتنظيف',     150.00, 'EGP'),
    (:practice_id, 'مبيت ليلة',        200.00, 'EGP');
