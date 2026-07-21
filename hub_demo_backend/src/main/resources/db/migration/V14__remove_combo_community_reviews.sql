DROP TABLE IF EXISTS combo_review_votes;

ALTER TABLE combos
    DROP COLUMN IF EXISTS community_reviewed_at,
    DROP COLUMN IF EXISTS community_review_result;
