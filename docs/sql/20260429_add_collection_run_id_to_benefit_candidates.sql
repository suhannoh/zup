-- 운영 적용 전 확인:
-- 1. ddl-auto=update에 의존하지 말고 이 SQL을 명시적으로 적용한다.
-- 2. 아래 snapshot_id 중복 점검 결과가 1건 이상이면 자동 backfill을 보류하고 수동 검토한다.

ALTER TABLE benefit_candidates
ADD COLUMN IF NOT EXISTS collection_run_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_benefit_candidates_collection_run_id
ON benefit_candidates(collection_run_id);

-- backfill 전 중복 snapshot_id 점검
SELECT snapshot_id, COUNT(*) AS run_count
FROM collection_runs
WHERE snapshot_id IS NOT NULL
GROUP BY snapshot_id
HAVING COUNT(*) > 1;

-- 안전 backfill:
-- snapshot_id가 유일하게 연결된 CollectionRun만 채운다.
WITH unique_snapshot_runs AS (
    SELECT snapshot_id, MIN(id) AS collection_run_id
    FROM collection_runs
    WHERE snapshot_id IS NOT NULL
    GROUP BY snapshot_id
    HAVING COUNT(*) = 1
)
UPDATE benefit_candidates bc
SET collection_run_id = usr.collection_run_id
FROM unique_snapshot_runs usr
WHERE bc.collection_run_id IS NULL
  AND bc.snapshot_id IS NOT NULL
  AND bc.snapshot_id = usr.snapshot_id;

-- backfill 후 검증
SELECT COUNT(*) AS candidates_without_collection_run
FROM benefit_candidates
WHERE collection_run_id IS NULL;

SELECT collection_run_id, COUNT(*) AS candidate_count
FROM benefit_candidates
WHERE collection_run_id IS NOT NULL
GROUP BY collection_run_id
ORDER BY collection_run_id DESC;
