-- SourceWatch terms-check fields for production PostgreSQL.
-- Review before applying. This migration is additive and does not delete rows.
--
-- Apply order:
-- 1. Add missing terms-check columns.
-- 2. Backfill existing SourceWatch rows to NOT_CHECKED.
-- 3. Verify recent rows and status distribution with the SELECT queries below.

ALTER TABLE source_watches
ADD COLUMN IF NOT EXISTS terms_check_status VARCHAR(30);

ALTER TABLE source_watches
ADD COLUMN IF NOT EXISTS terms_url VARCHAR(1000);

ALTER TABLE source_watches
ADD COLUMN IF NOT EXISTS terms_checked_at DATE;

ALTER TABLE source_watches
ADD COLUMN IF NOT EXISTS terms_memo TEXT;

ALTER TABLE source_watches
ADD COLUMN IF NOT EXISTS terms_link_candidates_json TEXT;

-- Existing SourceWatch rows start as not checked. Admins must review and update manually.
UPDATE source_watches
SET terms_check_status = 'NOT_CHECKED'
WHERE terms_check_status IS NULL;

ALTER TABLE source_watches
ALTER COLUMN terms_check_status SET DEFAULT 'NOT_CHECKED';

ALTER TABLE source_watches
ALTER COLUMN terms_check_status SET NOT NULL;

-- Optional but useful once admin filtering by terms status is added or the table grows.
CREATE INDEX IF NOT EXISTS idx_source_watches_terms_check_status
ON source_watches(terms_check_status);

-- Verification: recent SourceWatch rows.
SELECT id, title, terms_check_status, terms_url, terms_checked_at
FROM source_watches
ORDER BY id DESC
LIMIT 20;

-- Verification: status distribution.
SELECT terms_check_status, COUNT(*) AS source_watch_count
FROM source_watches
GROUP BY terms_check_status
ORDER BY terms_check_status;
