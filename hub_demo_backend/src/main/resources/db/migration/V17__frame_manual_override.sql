ALTER TABLE frame_data
    ADD COLUMN IF NOT EXISTS manual_override BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE frame_data
SET manual_override = TRUE
WHERE id IN (
    SELECT DISTINCT frame_id
    FROM frame_change_history
    WHERE frame_id IS NOT NULL
      AND action IN ('create', 'update')
);
