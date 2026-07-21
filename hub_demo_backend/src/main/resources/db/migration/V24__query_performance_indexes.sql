CREATE INDEX IF NOT EXISTS idx_combos_author_status_created
    ON combos (author_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_combos_public_listing
    ON combos (status, character_id, control_type, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_combos_popularity
    ON combos (favorites DESC, likes DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread_type_created
    ON notifications (user_id, read, type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_status_created
    ON reports (target_type, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
CREATE INDEX IF NOT EXISTS idx_users_banned ON users (banned) WHERE banned = TRUE;
CREATE INDEX IF NOT EXISTS idx_frame_change_history_frame_created
    ON frame_change_history (frame_id, created_at DESC);
