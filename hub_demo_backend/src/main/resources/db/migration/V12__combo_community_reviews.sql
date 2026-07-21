CREATE TABLE IF NOT EXISTS combo_review_votes (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL,
    verdict VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_combo_review_votes_user UNIQUE (combo_id, user_id),
    CONSTRAINT ck_combo_review_votes_verdict CHECK (verdict IN ('verified', 'issue'))
);

CREATE INDEX IF NOT EXISTS idx_combo_review_votes_combo_id ON combo_review_votes(combo_id);
CREATE INDEX IF NOT EXISTS idx_combo_review_votes_verdict ON combo_review_votes(verdict);
