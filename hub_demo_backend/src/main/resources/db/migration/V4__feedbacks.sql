CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(64),
    reason VARCHAR(128) NOT NULL,
    detail TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    handler_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    handler VARCHAR(64),
    resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_status_created ON feedbacks(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON feedbacks(user_id);

INSERT INTO feedbacks (user_id, username, reason, detail, status, handler_id, handler, resolution, created_at, handled_at)
SELECT reporter_id,
       reporter,
       COALESCE(NULLIF(reason, ''), 'other'),
       COALESCE(NULLIF(detail, ''), '历史站点反馈'),
       status,
       handler_id,
       handler,
       resolution,
       created_at,
       handled_at
FROM reports
WHERE target_type = 'site'
  AND NOT EXISTS (
      SELECT 1
      FROM feedbacks
      WHERE feedbacks.user_id IS NOT DISTINCT FROM reports.reporter_id
        AND feedbacks.username IS NOT DISTINCT FROM reports.reporter
        AND feedbacks.created_at = reports.created_at
        AND feedbacks.detail IS NOT DISTINCT FROM COALESCE(NULLIF(reports.detail, ''), '历史站点反馈')
  );

DELETE FROM reports WHERE target_type = 'site';
