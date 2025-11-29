-- Database cleanup script to remove old/conflicting indexes
-- Run this before restarting the application with the fixed entity definitions

-- Drop old indexes if they exist
DROP INDEX IF EXISTS idx_country_code;
DROP INDEX IF EXISTS idx_content_id;
DROP INDEX IF EXISTS idx_last_modified;
DROP INDEX IF EXISTS idx_user_id;
DROP INDEX IF EXISTS idx_travel_dates;
DROP INDEX IF EXISTS idx_user_trip;
DROP INDEX IF EXISTS idx_warning;
DROP INDEX IF EXISTS idx_sent_at;

-- Optionally, if you want a completely fresh start:
-- DROP TABLE IF EXISTS warning_notifications CASCADE;
-- DROP TABLE IF EXISTS user_trips CASCADE;
-- DROP TABLE IF EXISTS travel_warnings CASCADE;

-- Verify no conflicting indexes remain
SELECT tablename, indexname
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;

