ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS manual_review_reason TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS community_reviewed_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS community_review_result VARCHAR(32) DEFAULT '',
    ADD COLUMN IF NOT EXISTS video_review_status VARCHAR(32) NOT NULL DEFAULT 'unchecked',
    ADD COLUMN IF NOT EXISTS video_review_reason TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS video_reviewed_at TIMESTAMP NULL;

UPDATE combos
SET submitted_at = created_at::timestamp
WHERE submitted_at IS NULL;

ALTER TABLE combos
    ALTER COLUMN submitted_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN submitted_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_combo_status_submitted_at ON combos(status, submitted_at);
