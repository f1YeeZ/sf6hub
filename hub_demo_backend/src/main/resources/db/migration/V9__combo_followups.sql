CREATE TABLE IF NOT EXISTS combo_followups (
    id BIGSERIAL PRIMARY KEY,
    parent_combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    followup_combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    created_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_combo_followups_not_self CHECK (parent_combo_id <> followup_combo_id),
    CONSTRAINT uk_combo_followups_pair UNIQUE (parent_combo_id, followup_combo_id)
);

CREATE INDEX IF NOT EXISTS idx_combo_followups_parent ON combo_followups(parent_combo_id);
CREATE INDEX IF NOT EXISTS idx_combo_followups_followup ON combo_followups(followup_combo_id);
