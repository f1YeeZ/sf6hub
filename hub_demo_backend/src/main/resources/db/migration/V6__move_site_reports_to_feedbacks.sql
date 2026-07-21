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
