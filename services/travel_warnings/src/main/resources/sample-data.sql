-- Sample data for testing the Travel Warnings Microservice
-- This file can be used to populate the database with test data

-- Clear existing data (for testing only)
TRUNCATE TABLE warning_notifications CASCADE;
TRUNCATE TABLE user_trips CASCADE;
TRUNCATE TABLE travel_warnings CASCADE;

-- Insert sample travel warnings
INSERT INTO travel_warnings (content_id, last_modified, effective, title, country_code, iso3_country_code, country_name, warning, partial_warning, situation_warning, situation_part_warning, content, fetched_at)
VALUES
    ('199112', 1699681492000, 1699681492000, 'Afghanistan: Reise- und Sicherheitshinweise', 'AF', 'AFG', 'Afghanistan', true, false, false, false, '<h3>Sicherheit</h3><p>Vor Reisen nach Afghanistan wird gewarnt.</p>', NOW()),
    ('199124', 1699681492000, 1699681492000, 'Deutschland: Reise- und Sicherheitshinweise', 'DE', 'DEU', 'Deutschland', false, false, false, false, '<h3>Sicherheit</h3><p>Deutschland ist ein sicheres Reiseland.</p>', NOW()),
    ('199136', 1699681492000, 1699681492000, 'Frankreich: Reise- und Sicherheitshinweise', 'FR', 'FRA', 'Frankreich', false, false, true, false, '<h3>Sicherheit</h3><p>Erhöhte Sicherheitsmaßnahmen aufgrund von Terrorismus.</p>', NOW()),
    ('199148', 1699681492000, 1699681492000, 'Italien: Reise- und Sicherheitshinweise', 'IT', 'ITA', 'Italien', false, false, false, false, '<h3>Sicherheit</h3><p>Italien ist generell sicher. Vorsicht vor Taschendieben in Touristengebieten.</p>', NOW()),
    ('199160', 1699681492000, 1699681492000, 'Syrien: Reise- und Sicherheitshinweise', 'SY', 'SYR', 'Syrien', true, false, false, false, '<h3>Sicherheit</h3><p>Vor Reisen nach Syrien wird dringend gewarnt.</p>', NOW());

-- Insert sample user trips
INSERT INTO user_trips (user_id, country_code, country_name, start_date, end_date, trip_name, notifications_enabled)
VALUES
    ('user123', 'DE', 'Deutschland', '2025-12-01', '2025-12-15', 'Christmas in Berlin', true),
    ('user123', 'FR', 'Frankreich', '2026-01-10', '2026-01-20', 'New Year in Paris', true),
    ('user456', 'IT', 'Italien', '2025-11-20', '2025-11-30', 'Rome Adventure', true),
    ('user789', 'AF', 'Afghanistan', '2025-12-05', '2025-12-15', 'Dangerous Trip', false),
    ('user123', 'IT', 'Italien', '2026-03-15', '2026-03-25', 'Spring in Venice', true);

-- Sample notifications (to show history)
INSERT INTO warning_notifications (user_trip_id, user_id, warning_content_id, country_code, country_name, severity, sent_at, email_to, successful, warning_last_modified)
SELECT
    ut.id,
    ut.user_id,
    tw.content_id,
    tw.country_code,
    tw.country_name,
    CASE
        WHEN tw.warning THEN 'CRITICAL'
        WHEN tw.partial_warning THEN 'SEVERE'
        WHEN tw.situation_warning THEN 'MODERATE'
        WHEN tw.situation_part_warning THEN 'MINOR'
        ELSE 'NONE'
    END,
    NOW() - INTERVAL '1 day',
    ut.user_id || '@example.com',
    true,
    tw.last_modified
FROM user_trips ut
JOIN travel_warnings tw ON ut.country_code = tw.country_code
WHERE tw.warning = true OR tw.partial_warning = true OR tw.situation_warning = true OR tw.situation_part_warning = true;

-- Verify data
SELECT 'Travel Warnings' as table_name, COUNT(*) as count FROM travel_warnings
UNION ALL
SELECT 'User Trips', COUNT(*) FROM user_trips
UNION ALL
SELECT 'Warning Notifications', COUNT(*) FROM warning_notifications;

